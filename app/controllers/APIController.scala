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

import config.featureSwitches.{CallAPI1812HIP, FeatureSwitching}
import connectors.getPenaltyDetails.{HIPPenaltyDetailsConnector, PenaltyDetailsConnector}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser
import connectors.parsers.getFinancialDetails.FinancialDetailsParser.FinancialDetailsSuccessResponse
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{GetPenaltyDetailsResponse, GetPenaltyDetailsSuccessResponse}
import controllers.auth.AuthAction
import models.api.APIModel
import models.auditing.{ThirdParty1812APIRetrievalRegimeAuditModel, ThirdPartyAPI1811RetrievalRegimeAuditModel, UserHasPenaltyRegimeAuditModel}
import models.getFinancialDetails.FinancialDetails
import models.getPenaltyDetails.GetPenaltyDetails
import models.hipPenaltyDetails.PenaltyDetails
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc._
import services.RegimeFilterService.tryJsonParseOrJsString
import services._
import services.auditing.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PenaltyDetailsConverter.convertHIPToGetPenaltyDetails
import utils.{DateHelper, PagerDutyHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class APIController @Inject()(auditService: AuditService,
                              apiService: APIService,
                              getPenaltyDetailsService: PenaltyDetailsService,
                              getFinancialDetailsService: FinancialDetailsService,
                              getPenaltyDetailsConnector: PenaltyDetailsConnector,
                              hipPenaltyDetailsConnector: HIPPenaltyDetailsConnector,
                              dateHelper: DateHelper,
                              cc: ControllerComponents,
                              filterService: FilterService,
                              authAction: AuthAction)(implicit ec: ExecutionContext, val config: Configuration) extends BackendController(cc) with FeatureSwitching {

  def getSummaryData(regime: Regime, idType: IdType, id: Id): Action[AnyContent] = authAction.async {
    implicit request => {
      val agnosticEnrolmenKey = AgnosticEnrolmentKey(regime, idType, id)

      getPenaltyDetailsService.getPenaltyDetails(agnosticEnrolmenKey).flatMap {
        handlePenaltyDetailsResponse(_, agnosticEnrolmenKey)
      }
    }
  }

  private def handlePenaltyDetailsResponse(response: GetPenaltyDetailsResponse, agnosticEnrolmenKey: AgnosticEnrolmentKey)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    response.fold({
      case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == BAD_REQUEST =>
        Future(NotFound(s"A downstream call returned 400 for ${agnosticEnrolmenKey.idType.value}: ${agnosticEnrolmenKey.id.value}"))
      case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND =>
        Future(NotFound(s"A downstream call returned 404 for ${agnosticEnrolmenKey.idType.value}: ${agnosticEnrolmenKey.id.value}"))
      case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == UNPROCESSABLE_ENTITY =>
        val responsePayload = GetPenaltyDetailsSuccessResponse(GetPenaltyDetails(totalisations = None, lateSubmissionPenalty = None, latePaymentPenalty = None, breathingSpace = None))
        Future(returnResponseForAPI(responsePayload.penaltyDetails, agnosticEnrolmenKey))
      case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) =>
        Future(InternalServerError(s"A downstream call returned an unexpected status: $status for $agnosticEnrolmenKey"))
      case PenaltyDetailsParser.GetPenaltyDetailsMalformed =>
        PagerDutyHelper.log("getSummaryDataForVRN", MALFORMED_RESPONSE_FROM_1812_API)
        Future(InternalServerError(s"We were unable to parse penalty data."))
      case PenaltyDetailsParser.GetPenaltyDetailsNoContent =>
        logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - API call returned no content for $agnosticEnrolmenKey")
        Future(NoContent)
    },
    success => {
      logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - API call returned 200 for $agnosticEnrolmenKey")
      val penaltyDetails = success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails
      if (penaltyDetails.latePaymentPenalty.exists(LPP =>
        LPP.ManualLPPIndicator.getOrElse(false))) {
        logger.info(s"[RegimeAPIController][getSummaryDataForVRN] - Data has ManualLPPIndicator set to true, calling 1811")
        callFinancialDetailsForManualLPPs(agnosticEnrolmenKey).map {
          financialDetails => {
            returnResponseForAPI(penaltyDetails, agnosticEnrolmenKey, financialDetails)
          }
        }
      } else {
        Future(returnResponseForAPI(penaltyDetails, agnosticEnrolmenKey))
      }
    })
  }

  private def callFinancialDetailsForManualLPPs(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[Option[FinancialDetails]] = {
    getFinancialDetailsService.getFinancialDetails(enrolmentKey, None).map {
      financialDetailsResponseWithoutClearedItems =>
        logger.info(s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - Calling 1811 for response without cleared items")
        financialDetailsResponseWithoutClearedItems.fold({
          case FinancialDetailsParser.FinancialDetailsFailureResponse(status) =>
            logger.info(s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - 1811 call (VATVC/BTA API)" +
              s" returned an unexpected status: $status, returning None")
            None
          case FinancialDetailsParser.FinancialDetailsMalformed =>
            PagerDutyHelper.log("callFinancialDetailsForManualLPPs", MALFORMED_RESPONSE_FROM_1811_API)
            logger.error(s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - 1811 call (VATVC/BTA API)" +
              s" returned invalid body - failed to parse penalty details response for $enrolmentKey, returning None")
            None
          case FinancialDetailsParser.FinancialDetailsNoContent =>
            logger.info(s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - 1811 call (VATVC/BTA API) returned no content for $enrolmentKey, returning None")
            None
        },
          financialDetailsResponseWithoutClearedItems => {
            logger.info(s"[RegimeAPIController][callFinancialDetailsForManualLPPs] - 1811 call (VATVC/BTA API) returned 200 for $enrolmentKey" )
            Some(financialDetailsResponseWithoutClearedItems.asInstanceOf[FinancialDetailsSuccessResponse].financialDetails)
          })
    }
  }

  private def returnResponseForAPI(penaltyDetails: GetPenaltyDetails, enrolmentKey: AgnosticEnrolmentKey,
                                   financialDetails: Option[FinancialDetails] = None)(implicit request: Request[_]): Result = {
    val pointsTotal = penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0)
    val penaltyAmountWithEstimateStatus = apiService.findEstimatedPenaltiesAmount(penaltyDetails)
    val noOfEstimatedPenalties = apiService.getNumberOfEstimatedPenalties(penaltyDetails)
    val crystallisedPenaltyAmount = apiService.getNumberOfCrystallisedPenalties(penaltyDetails, financialDetails)
    val crystallisedPenaltyTotal = apiService.getCrystallisedPenaltyTotal(penaltyDetails, financialDetails)
    val hasAnyPenaltyData = apiService.checkIfHasAnyPenaltyData(penaltyDetails)
    val responseData: APIModel = APIModel(
      noOfPoints = pointsTotal,
      noOfEstimatedPenalties = noOfEstimatedPenalties,
      noOfCrystalisedPenalties = crystallisedPenaltyAmount,
      estimatedPenaltyAmount = penaltyAmountWithEstimateStatus,
      crystalisedPenaltyAmountDue = crystallisedPenaltyTotal,
      hasAnyPenaltyData = hasAnyPenaltyData
    )
    if (hasAnyPenaltyData) {
      val auditModel = UserHasPenaltyRegimeAuditModel(
        penaltyDetails = penaltyDetails,
        enrolmentKey = enrolmentKey,
        arn = None,
        dateHelper = dateHelper)
      auditService.audit(auditModel)
      Ok(Json.toJson(responseData))
    } else {
      logger.info("[RegimeAPIController][returnResponseForAPI] - User had no penalty data, returning 204 to caller")
      NoContent
    }
  }

  def getFinancialDetails(regime: Regime, idType: IdType, id: Id,
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
                          addAccruingInterestDetails: Option[Boolean]): Action[AnyContent] = authAction.async {
    implicit request => {
      val enrolmentKey = AgnosticEnrolmentKey(regime, idType, id)
      val response = getFinancialDetailsService.getFinancialDetailsForAPI(enrolmentKey,
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

      response.map(
        res => {
          sendFinancialAudit(res, enrolmentKey)

          res.status match {
            case OK | CREATED =>
              logger.info(s"[RegimeAPIController][getFinancialDetails] - 1811 call (3rd party API) returned ${res.status} for $enrolmentKey")
              Ok(res.json)
            case NOT_FOUND =>
              logFinancialErrorMessage(s"returned 404: ${res.body}")
              returnErrorResult(NOT_FOUND, res.body)
            case UNPROCESSABLE_ENTITY if res.body.contains("Invalid ID Number") =>
              logFinancialErrorMessage(s"returned HIP error 422-016 Invalid ID Number: ${res.body}")
              returnErrorResult(NOT_FOUND, res.body)
            case UNPROCESSABLE_ENTITY if res.body.contains("No Data Identified") =>
              logFinancialErrorMessage(s"returned HIP error 422-018 No Data Identified: ${res.body}")
              returnErrorResult(NOT_FOUND, res.body)
            case status =>
              PagerDutyHelper.logStatusCode("getFinancialDetails", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
              logFinancialErrorMessage(s"returned an unknown ${res.status} error: ${res.body}")
              returnErrorResult(status, res.body)
          }
        })
    }
  }

  private def sendFinancialAudit(response: HttpResponse, enrolmentKey: AgnosticEnrolmentKey)
                                (implicit hc: HeaderCarrier, request: Request[_]): Unit = {
    val auditToSend = ThirdPartyAPI1811RetrievalRegimeAuditModel(enrolmentKey, response.status, response.body)
    auditService.audit(auditToSend)
  }

  private def logFinancialErrorMessage(message: String): Unit =
    logger.error(s"[RegimeAPIController][getFinancialDetails] - 1811 call (3rd party API) $message")

  def getPenaltyDetails(regime: Regime, idType: IdType, id: Id, dateLimit: Option[String]): Action[AnyContent] = authAction.async {
    implicit request => {
      val enrolmentKey = AgnosticEnrolmentKey(regime, idType, id)

      val connectorResponse = if (isEnabled(CallAPI1812HIP)) {
        logger.info(s"[RegimeAPIController][getPenaltyDetails] - Calling HIP connector - CallAPI1812HIP switch is on")
        hipPenaltyDetailsConnector.getPenaltyDetailsForAPI(enrolmentKey, dateLimit)
      } else {
        logger.info(s"[RegimeAPIController][getPenaltyDetails] - Calling IF connector - CallAPI1812HIP switch is off")
        getPenaltyDetailsConnector.getPenaltyDetailsForAPI(enrolmentKey, dateLimit)
      }

      connectorResponse.map(
        response => {
          sendPenaltiesAudit(response, enrolmentKey)

          response.status match {
            case OK =>
              logger.info(s"[RegimeAPIController][getPenaltyDetails] - API call (3rd party API) returned 200 for $enrolmentKey")
              processSuccessResponse(response, enrolmentKey)
            case NOT_FOUND =>
              logPenaltiesErrorMessage(s"returned 404: ${response.body}")
              returnErrorResult(NOT_FOUND, response.body)
            case UNPROCESSABLE_ENTITY if response.body.contains("Invalid ID Number") =>
              logPenaltiesErrorMessage(s"returned HIP error 422-016 Invalid ID Number: ${response.body}")
              returnErrorResult(NOT_FOUND, response.body)
            case status =>
              PagerDutyHelper.logStatusCode("getPenaltyDetails", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
              logPenaltiesErrorMessage(s"returned an unknown ${response.status} error: ${response.body}")
              returnErrorResult(status, response.body)
          }
        }
      )
    }
  }

  private def processSuccessResponse(response: HttpResponse, enrolmentKey: AgnosticEnrolmentKey): Result = {
    val jsonBody: JsValue = tryJsonParseOrJsString(response.body)
    val validateBody: JsResult[GetPenaltyDetails] = if (isEnabled(CallAPI1812HIP)) {
      convertHipResponseToGetPenaltyDetails(jsonBody)
    } else {
      jsonBody.validate[GetPenaltyDetails]
    }
    validateBody match {
      case JsSuccess(getPenaltyDetails, _) =>
        logger.info(s"[RegimeAPIController][getPenaltyDetails] - API call (3rd party API) - Successfully parsed 200 response body")
        Ok(filterPenaltiesResponseBody(getPenaltyDetails, enrolmentKey))
      case JsError(errors) =>
        logPenaltiesErrorMessage(s"Unable to validate 200 response body with errors: ${errors.mkString(", ")}")
        returnErrorResult(INTERNAL_SERVER_ERROR, errors.mkString(", "))
    }
  }

  private def filterPenaltiesResponseBody(getPenaltyDetailsBody: GetPenaltyDetails, enrolmentKey: AgnosticEnrolmentKey): JsValue = {
    implicit val loggingContext: LoggingContext = LoggingContext(
      "APIConnector",
      "getPenaltyDetails",
      enrolmentKey.toString
    )
    GetPenaltyDetails.format.writes(filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(
      filterService.filterPenaltiesWith9xAppealStatus(getPenaltyDetailsBody)
    ))
  }

  private def convertHipResponseToGetPenaltyDetails(responseBody: JsValue): JsResult[GetPenaltyDetails] =
    responseBody.validate[PenaltyDetails].map(convertHIPToGetPenaltyDetails)

  private def sendPenaltiesAudit(response: HttpResponse, enrolmentKey: AgnosticEnrolmentKey)
                                (implicit hc: HeaderCarrier, request: Request[_]): Unit = {
    val jsonBody: JsValue = tryJsonParseOrJsString(response.body)
    val auditToSend = ThirdParty1812APIRetrievalRegimeAuditModel(enrolmentKey, response.status, jsonBody)
    auditService.audit(auditToSend)
  }

  private def logPenaltiesErrorMessage(message: String): Unit =
    logger.error(s"[RegimeAPIController][getPenaltyDetails] - API call (3rd party API) $message")

  private def returnErrorResult(status: Int, responseBody: String): Result = Status(status)(tryJsonParseOrJsString(responseBody))

}
