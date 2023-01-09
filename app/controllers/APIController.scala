/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.getFinancialDetails.GetFinancialDetailsConnector
import connectors.getPenaltyDetails.GetPenaltyDetailsConnector
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.GetPenaltyDetailsSuccessResponse
import controllers.actions.InternalAuthActions
import models.api.APIModel
import models.auditing.{ThirdParty1812APIRetrievalAuditModel, ThirdPartyAPI1811RetrievalAuditModel, UserHasPenaltyAuditModel}
import models.getPenaltyDetails.GetPenaltyDetails
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.auditing.AuditService
import services.{APIService, GetPenaltyDetailsService}
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{DateHelper, PagerDutyHelper, RegimeHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class APIController @Inject()(auditService: AuditService,
                              apiService: APIService,
                              getPenaltyDetailsService: GetPenaltyDetailsService,
                              getFinancialDetailsConnector: GetFinancialDetailsConnector,
                              getPenaltyDetailsConnector: GetPenaltyDetailsConnector,
                              dateHelper: DateHelper,
                              cc: ControllerComponents)(implicit ec: ExecutionContext,
                                                        val config: Configuration,
                                                        val auth: BackendAuthComponents) extends BackendController(cc) with FeatureSwitching with InternalAuthActions {

  private val vrnRegex: Regex = "^[0-9]{1,9}$".r

  def getSummaryDataForVRN(vrn: String): Action[AnyContent] = authoriseService.async {
    implicit request => {
      if (!vrn.matches(vrnRegex.regex)) {
        Future(BadRequest(s"VRN: $vrn was not in a valid format."))
      } else {
        val enrolmentKey = RegimeHelper.constructMTDVATEnrolmentKey(vrn)
        getPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(vrn).map {
          _.fold({
            case GetPenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND => {
              logger.info(s"[APIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned $status for VRN: $vrn")
              NotFound(s"A downstream call returned 404 for VRN: $vrn")
            }
            case GetPenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) => {
              logger.info(s"[APIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned an unexpected status: $status")
              InternalServerError(s"A downstream call returned an unexpected status: $status")
            }
            case GetPenaltyDetailsParser.GetPenaltyDetailsMalformed => {
              PagerDutyHelper.log("getSummaryDataForVRN", MALFORMED_RESPONSE_FROM_1812_API)
              logger.error(s"[APIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned invalid body - failed to parse penalty details response")
              InternalServerError(s"We were unable to parse penalty data.")
            }
            case GetPenaltyDetailsParser.GetPenaltyDetailsNoContent => {
              logger.info(s"[APIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned no content for VRN: $vrn")
              NoContent
            }
          },
            success => {
              logger.info(s"[APIController][getSummaryDataForVRN] - 1812 call (VATVC/BTA API) returned 200 for VRN: $vrn")
              returnResponseForAPI(success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, enrolmentKey)
            }
          )
        }
      }
    }
  }

  private def returnResponseForAPI(penaltyDetails: GetPenaltyDetails, enrolmentKey: String)(implicit request: Request[_]): Result = {
    val pointsTotal = penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0)
    val penaltyAmountWithEstimateStatus = apiService.findEstimatedPenaltiesAmount(penaltyDetails)
    val noOfEstimatedPenalties = apiService.getNumberOfEstimatedPenalties(penaltyDetails)
    val crystallisedPenaltyAmount = apiService.getNumberOfCrystallisedPenalties(penaltyDetails)
    val crystallisedPenaltyTotal = apiService.getCrystallisedPenaltyTotal(penaltyDetails)
    val hasAnyPenaltyData = apiService.checkIfHasAnyPenaltyData(penaltyDetails)
    val responseData: APIModel = APIModel(
      noOfPoints = pointsTotal,
      noOfEstimatedPenalties = noOfEstimatedPenalties,
      noOfCrystalisedPenalties = crystallisedPenaltyAmount,
      estimatedPenaltyAmount = penaltyAmountWithEstimateStatus,
      crystalisedPenaltyAmountDue = crystallisedPenaltyTotal,
      hasAnyPenaltyData = hasAnyPenaltyData)
    if (hasAnyPenaltyData) {
      val auditModel = UserHasPenaltyAuditModel(
        penaltyDetails = penaltyDetails,
        identifier = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
        identifierType = RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey),
        arn = None, //TODO: need to check this
        dateHelper = dateHelper)
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(responseData))
  }

  def getFinancialDetails(vrn: String,
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
                          addAccruingInterestDetails: Option[Boolean]): Action[AnyContent] = authoriseService.async {
    implicit request => {
      val response = getFinancialDetailsConnector.getFinancialDetailsForAPI(vrn,
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
          val auditToSend = ThirdPartyAPI1811RetrievalAuditModel(vrn, res.status, res.body)
          auditService.audit(auditToSend)

          res.status match {
            case OK =>
              logger.info(s"[APIController][getFinancialDetails] - 1811 call (3rd party API) returned 200 for VRN: $vrn")
              logger.debug("[APIController][getFinancialDetails] Ok response received: " + res)
              Ok(res.json)
            case NOT_FOUND =>
              logger.error("[APIController][getFinancialDetails] - 1811 call (3rd party API) returned 404 - error received: " + res)
              Status(res.status)(Json.toJson(res.body))
            case status =>
              PagerDutyHelper.logStatusCode("getFinancialDetails", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
              logger.error(s"[APIController][getFinancialDetails] - 1811 call (3rd party API) returned an unknown error - status ${res.status} returned from EIS")
              Status(res.status)(Json.toJson(res.body))
          }
        })
    }
  }

  def getPenaltyDetails(vrn: String, dateLimit: Option[String]): Action[AnyContent] = authoriseService.async {
    implicit request => {
      val response = getPenaltyDetailsConnector.getPenaltyDetailsForAPI(vrn, dateLimit)
      response.map(
        res => {
          val auditToSend = ThirdParty1812APIRetrievalAuditModel(vrn, res.status, res.body)
          auditService.audit(auditToSend)
          res.status match {
            case OK =>
              logger.info(s"[APIController][getPenaltyDetails] - 1812 call (3rd party API) returned 200 for VRN: $vrn")
              logger.debug("[APIController][getPenaltyDetails] Ok response received: " + res)
              Ok(res.json)
            case NOT_FOUND =>
              logger.error("[APIController][getPenaltyDetails] - 1812 call (3rd party API) returned 404 - error received: " + res)
              Status(res.status)(Json.toJson(res.body))
            case status =>
              PagerDutyHelper.logStatusCode("getPenaltyDetails", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
              logger.error(s"[APIController][getPenaltyDetails] - 1812 call (3rd party API) returned an unknown error - status ${res.status} returned from EIS")
              Status(res.status)(Json.toJson(res.body))
          }
        }
      )
    }
  }
}
