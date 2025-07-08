/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.featureSwitches.FeatureSwitching
import connectors.getFinancialDetails.FinancialDetailsConnector
import connectors.getPenaltyDetails.PenaltyDetailsConnector
import connectors.parsers.getFinancialDetails.FinancialDetailsParser
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.GetPenaltyDetailsSuccessResponse
import controllers.auth.AuthAction
import models.api.APIModel
import models.auditing.{ThirdParty1812APIRetrievalRegimeAuditModel, ThirdPartyAPI1811RetrievalRegimeAuditModel, UserHasPenaltyRegimeAuditModel}
import models.getFinancialDetails.{FinancialDetails, FinancialDetailsRequestModel}
import models.getPenaltyDetails.GetPenaltyDetails
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import play.api.Configuration
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc._
import services._
import services.auditing.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{DateHelper, PagerDutyHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

//noinspection ScalaStyle
class RegimeAPIController @Inject() (auditService: AuditService,
                                     apiService: APIService,
                                     penaltyDetailsService: PenaltyDetailsService,
                                     financialDetailsService: FinancialDetailsService,
                                     financialDetailsConnector: FinancialDetailsConnector,
                                     penaltyDetailsConnector: PenaltyDetailsConnector,
                                     dateHelper: DateHelper,
                                     cc: ControllerComponents,
                                     filterService: RegimeFilterService,
                                     authAction: AuthAction)(implicit ec: ExecutionContext, val config: Configuration)
    extends BackendController(cc)
    with FeatureSwitching {

  def getSummaryData(regime: Regime, idType: IdType, id: Id): Action[AnyContent] = authAction.async { implicit request =>
    val agnosticEnrolmentKey = AgnosticEnrolmentKey(regime, idType, id)
    // composeEnrolmentKey(regime, id).andThen { enrolmentKey =>
    // import enrolmentKey._
    penaltyDetailsService.getDataFromPenaltyService(agnosticEnrolmentKey).flatMap {
      _.fold(
        {
          case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == BAD_REQUEST =>
            // logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned $status for VRN: $vrn")
            Future(NotFound(s"A downstream call returned 400 for ${agnosticEnrolmentKey.idType.value}: ${agnosticEnrolmentKey.id.value}"))
          case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND =>
            // logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned $status for VRN: $vrn")
            Future(NotFound(s"A downstream call returned 404 for ${agnosticEnrolmentKey.idType.value}: ${agnosticEnrolmentKey.id.value}"))
          case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == UNPROCESSABLE_ENTITY =>
            // Temporary measure to avoid 422 causing issues
            val responsePayload = GetPenaltyDetailsSuccessResponse(
              GetPenaltyDetails(totalisations = None, lateSubmissionPenalty = None, latePaymentPenalty = None, breathingSpace = None))
            // logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned $status for VRN: $vrn - Overriding response")
            Future(returnResponseForAPI(responsePayload.penaltyDetails, agnosticEnrolmentKey))
          case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) =>
            // logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned an unexpected status: $status")
            Future(InternalServerError(s"A downstream call returned an unexpected status: $status for $agnosticEnrolmentKey"))
          case PenaltyDetailsParser.GetPenaltyDetailsMalformed =>
            PagerDutyHelper.log("getSummaryDataForVRN", MALFORMED_RESPONSE_FROM_1812_API)
            // logger.error(s"[RegimeAPIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned invalid body - failed to parse penalty details response for VRN: $vrn")
            Future(InternalServerError(s"We were unable to parse penalty data."))
          case PenaltyDetailsParser.GetPenaltyDetailsNoContent =>
            logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned no content for $agnosticEnrolmentKey")
            Future(NoContent)
        },
        success => {
          logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned 200 for $agnosticEnrolmentKey")
          val penaltyDetails = success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails
          if (penaltyDetails.latePaymentPenalty.exists(LPP => LPP.ManualLPPIndicator.getOrElse(false))) {
            logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - 1812 data has ManualLPPIndicator set to true, calling 1811")
            callFinancialDetailsForManualLPPs(agnosticEnrolmentKey).map { financialDetails =>
              returnResponseForAPI(penaltyDetails, agnosticEnrolmentKey, financialDetails)
            }
          } else {
            Future(returnResponseForAPI(penaltyDetails, agnosticEnrolmentKey))
          }
        }
      )
    }
  }

  private def callFinancialDetailsForManualLPPs(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[Option[FinancialDetails]] =
    financialDetailsService.getFinancialDetails(enrolmentKey).map { financialDetailsResponseWithoutClearedItems =>
      logger.info(s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - Calling 1811 for response without cleared items")
      financialDetailsResponseWithoutClearedItems.fold(
        {
          case FinancialDetailsParser.GetFinancialDetailsFailureResponse(status) =>
            logger.info(
              s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - 1811 call (VATVC/BTA API)" +
                s" returned an unexpected status: $status, returning None")
            None
          case FinancialDetailsParser.GetFinancialDetailsMalformed =>
            PagerDutyHelper.log("callFinancialDetailsForManualLPPs", MALFORMED_RESPONSE_FROM_1811_API)
            logger.error(
              s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - 1811 call (VATVC/BTA API)" +
                s" returned invalid body - failed to parse penalty details response for $enrolmentKey, returning None")
            None
          case FinancialDetailsParser.GetFinancialDetailsNoContent =>
            logger.info(
              s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - 1811 call (VATVC/BTA API) returned no content for $enrolmentKey, returning None")
            None
        },
        financialDetailsResponseWithoutClearedItems => {
          logger.info(s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - 1811 call (VATVC/BTA API) returned 200 for $enrolmentKey")
          Some(financialDetailsResponseWithoutClearedItems.financialDetails)
        }
      )
    }

  private def returnResponseForAPI(penaltyDetails: GetPenaltyDetails,
                                   enrolmentKey: AgnosticEnrolmentKey,
                                   financialDetails: Option[FinancialDetails] = None)(implicit request: Request[_]): Result = {
    val pointsTotal                     = penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0)
    val penaltyAmountWithEstimateStatus = apiService.findEstimatedPenaltiesAmount(penaltyDetails)
    val noOfEstimatedPenalties          = apiService.getNumberOfEstimatedPenalties(penaltyDetails)
    val crystallisedPenaltyAmount       = apiService.getNumberOfCrystallisedPenalties(penaltyDetails, financialDetails)
    val crystallisedPenaltyTotal        = apiService.getCrystallisedPenaltyTotal(penaltyDetails, financialDetails)
    val hasAnyPenaltyData               = apiService.checkIfHasAnyPenaltyData(penaltyDetails)
    val responseData: APIModel = APIModel(
      noOfPoints = pointsTotal,
      noOfEstimatedPenalties = noOfEstimatedPenalties,
      noOfCrystalisedPenalties = crystallisedPenaltyAmount,
      estimatedPenaltyAmount = penaltyAmountWithEstimateStatus,
      crystalisedPenaltyAmountDue = crystallisedPenaltyTotal,
      hasAnyPenaltyData = hasAnyPenaltyData
    )
    if (hasAnyPenaltyData) {
      val auditModel =
        UserHasPenaltyRegimeAuditModel(penaltyDetails = penaltyDetails, enrolmentKey = enrolmentKey, arn = None, dateHelper = dateHelper)
      auditService.audit(auditModel)
      Ok(Json.toJson(responseData))
    } else {
      logger.info("[RegimeAPIController][returnResponseForAPI] - User had no penalty data, returning 204 to caller")
      NoContent
    }
  }

  def getFinancialDetails(regime: Regime,
                          idType: IdType,
                          id: Id,
                          searchType: Option[String],
                          searchItem: Option[String],
                          dateType: Option[String],
                          dateFrom: Option[String],
                          dateTo: Option[String],
                          includeClearedItems: Option[Boolean],
                          includeStatisticalItems: Option[Boolean],
                          includePaymentOnAccount: Option[Boolean],
                          addRegimeTotalisation: Option[Boolean],
                          addLockInformation: Option[Boolean],
                          addPenaltyDetails: Option[Boolean],
                          addPostedInterestDetails: Option[Boolean],
                          addAccruingInterestDetails: Option[Boolean]): Action[AnyContent] = authAction.async { implicit request =>
    val enrolmentKey = AgnosticEnrolmentKey(regime, idType, id)
    val requestBody = FinancialDetailsRequestModel(
      searchType,
      searchItem,
      dateType,
      dateFrom,
      dateTo,
      includeClearedItems,
      includeStatisticalItems,
      includePaymentOnAccount,
      addRegimeTotalisation,
      addLockInformation,
      addPenaltyDetails,
      addPostedInterestDetails,
      addAccruingInterestDetails
    )
    val response = financialDetailsConnector.getFinancialDetailsForAPI(enrolmentKey, requestBody)

    response.map { res =>
      val auditToSend = ThirdPartyAPI1811RetrievalRegimeAuditModel(enrolmentKey, res.status, res.body)
      auditService.audit(auditToSend)
      res.status match {
        case OK =>
          logger.info(s"[RegimeAPIController][getFinancialDetails] - 1811 call (3rd party API) returned 200 for $enrolmentKey")
          logger.debug("[RegimeAPIController][getFinancialDetails] Ok response received: " + res)
          Ok(res.json)
        case NOT_FOUND =>
          logger.error("[RegimeAPIController][getFinancialDetails] - 1811 call (3rd party API) returned 404 - error received: " + res)
          Status(res.status)(Json.toJson(res.body))
        case status =>
          PagerDutyHelper.logStatusCode("getFinancialDetails", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(
            s"[RegimeAPIController][getFinancialDetails] - 1811 call (3rd party API) returned an unknown error - status ${res.status} returned from EIS")
          Status(res.status)(Json.toJson(res.body))
      }
    }
  }

  def getPenaltyDetails(regime: Regime, idType: IdType, id: Id, dateLimit: Option[String]): Action[AnyContent] = authAction.async {
    implicit request =>
      // composeEnrolmentKey(regime, idType, id).andThen { enrolmentKey =>
      val enrolmentKey = AgnosticEnrolmentKey(regime, idType, id)
      val response     = penaltyDetailsConnector.getPenaltyDetailsForAPI(enrolmentKey, dateLimit)
      response.map { res =>
        val processedResBody = filterService.tryJsonParseOrJsString(res.body)
        val filteredResBody = if (res.status.equals(OK) || !processedResBody.isInstanceOf[JsString]) {
          filterResponseBody(processedResBody, enrolmentKey, "getPenaltyDetails")
        } else {
          processedResBody
        }
        val auditToSend = ThirdParty1812APIRetrievalRegimeAuditModel(enrolmentKey, res.status, filteredResBody)
        auditService.audit(auditToSend)
        res.status match {
          case OK =>
            logger.info(s"[RegimeAPIController][getPenaltyDetails] - 1812 call (3rd party API) returned 200 for $enrolmentKey")
            logger.debug("[RegimeAPIController][getPenaltyDetails] Ok response received: " + res)
            Ok(filteredResBody)
          case NOT_FOUND =>
            logger.error("[RegimeAPIController][getPenaltyDetails] - 1812 call (3rd party API) returned 404 - error received: " + res)
            Status(res.status)(Json.toJson(res.body))
          case status =>
            PagerDutyHelper.logStatusCode("getPenaltyDetails", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
            logger.error(
              s"[RegimeAPIController][getPenaltyDetails] - 1812 call (3rd party API) returned an unknown error - status ${res.status} returned from EIS")
            Status(res.status)(Json.toJson(res.body))
        }
      }
      // }
  }

  private def filterResponseBody(resBody: JsValue, enrolmentKey: AgnosticEnrolmentKey, method: String): JsValue = {
    val penaltiesDetails = GetPenaltyDetails.format.reads(resBody)
    implicit val loggingContext: LoggingContext = LoggingContext(
      "APIConnector",
      method,
      enrolmentKey.toString
    )
    GetPenaltyDetails.format.writes(
      filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(
        filterService.filterPenaltiesWith9xAppealStatus(penaltiesDetails.get)
      ))
  }
}
