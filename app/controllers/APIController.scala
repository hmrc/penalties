/*
 * Copyright 2022 HM Revenue & Customs
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

import config.featureSwitches.{FeatureSwitching, UseAPI1812Model}
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser.GetPenaltyDetailsSuccessResponse
import connectors.v3.getFinancialDetails.GetFinancialDetailsConnector
import connectors.v3.getPenaltyDetails.GetPenaltyDetailsConnector
import models.ETMPPayload
import models.api.APIModel
import models.auditing.UserHasPenaltyAuditModel
import models.auditing.v2.{UserHasPenaltyAuditModel => AuditModelV2}
import models.v3.getPenaltyDetails.GetPenaltyDetails
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.auditing.AuditService
import services.v2.APIService
import services.{ETMPService, GetPenaltyDetailsService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.RegimeHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class APIController @Inject()(etmpService: ETMPService,
                              auditService: AuditService,
                              apiService: APIService,
                              getPenaltyDetailsService: GetPenaltyDetailsService,
                              getFinancialDetailsConnector: GetFinancialDetailsConnector,
                              getPenaltyDetailsConnector: GetPenaltyDetailsConnector,
                              cc: ControllerComponents)(implicit ec: ExecutionContext, val config: Configuration) extends BackendController(cc) with FeatureSwitching {

  private val vrnRegex: Regex = "^[0-9]{1,9}$".r

  def getSummaryDataForVRN(vrn: String): Action[AnyContent] = Action.async {
    implicit request => {
      if (!vrn.matches(vrnRegex.regex)) {
        Future(BadRequest(s"VRN: $vrn was not in a valid format."))
      } else {
        val enrolmentKey = RegimeHelper.constructMTDVATEnrolmentKey(vrn)
        if (!isEnabled(UseAPI1812Model)) {
          etmpService.getPenaltyDataFromETMPForEnrolment(enrolmentKey).map {
            _._1.fold(
              NotFound(s"Unable to find data for VRN: $vrn")
            )(
              etmpPayload => {
                returnResponseForAPI(etmpPayload, enrolmentKey)
              }
            )
          }
        } else {
          getPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(vrn).map {
            _.fold({
              case GetPenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND || status == NO_CONTENT => {
                logger.info(s"[APIController][getSummaryDataForVRN] - 1812 call returned $status for VRN: $vrn")
                NotFound(s"A downstream call returned 404 for VRN: $vrn")
              }
              case GetPenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) => {
                logger.info(s"[APIController][getSummaryDataForVRN] - 1812 call returned an unexpected status: $status")
                InternalServerError(s"A downstream call returned an unexpected status: $status")
              }
              case GetPenaltyDetailsParser.GetPenaltyDetailsMalformed => {
                logger.error(s"[APIController][getSummaryDataForVRN] - Failed to parse penalty details response")
                InternalServerError(s"We were unable to parse penalty data.")
              }
            },
              success => {
                returnResponseForAPI(success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, enrolmentKey)
              }
            )
          }
        }
      }
    }
  }

  private def returnResponseForAPI(etmpPayload: ETMPPayload, enrolmentKey: String)(implicit request: Request[_]): Result = {
    val pointsTotal = etmpPayload.pointsTotal
    val penaltyAmountWithEstimateStatus = etmpService.findEstimatedPenaltiesAmount(etmpPayload)
    val noOfEstimatedPenalties = etmpService.getNumberOfEstimatedPenalties(etmpPayload)
    val crystalizedPenaltyAmount = etmpService.getNumberOfCrystalizedPenalties(etmpPayload)
    val crystalizedPenaltyTotal = etmpService.getCrystalisedPenaltyTotal(etmpPayload)
    val hasAnyPenaltyData = etmpService.checkIfHasAnyPenaltyData(etmpPayload)
    val responseData: APIModel = APIModel(
      noOfPoints = pointsTotal,
      noOfEstimatedPenalties = noOfEstimatedPenalties,
      noOfCrystalisedPenalties = crystalizedPenaltyAmount,
      estimatedPenaltyAmount = penaltyAmountWithEstimateStatus,
      crystalisedPenaltyAmountDue = crystalizedPenaltyTotal,
      hasAnyPenaltyData = hasAnyPenaltyData)
    if (pointsTotal > 0) {
      val auditModel = UserHasPenaltyAuditModel(
        etmpPayload = etmpPayload,
        identifier = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
        identifierType = RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey),
        arn = None) //TODO: need to check this
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(responseData))
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
      val auditModel = AuditModelV2(
        penaltyDetails = penaltyDetails,
        identifier = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
        identifierType = RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey),
        arn = None) //TODO: need to check this
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(responseData))
  }

  def getFinancialDetails(vrn: String,
                          docNumber: Option[String],
                          dateFrom: Option[String],
                          dateTo: Option[String],
                          onlyOpenItems: Boolean,
                          includeStatistical: Boolean,
                          includeLocks: Boolean,
                          calculateAccruedInterest: Boolean,
                          removePOA: Boolean,
                          customerPaymentInformation: Boolean): Action[AnyContent] = Action.async {
    implicit request => {
      val response = getFinancialDetailsConnector.getFinancialDetailsForAPI(vrn,
        docNumber,
        dateFrom,
        dateTo,
        onlyOpenItems,
        includeStatistical,
        includeLocks,
        calculateAccruedInterest,
        removePOA,
        customerPaymentInformation)

        response.map(
          res => res.status match {
            case OK =>
              logger.debug("[APIController][getFinancialDetails] Ok response received: " + res)
              Ok(res.json)
            case NOT_FOUND =>
              logger.debug("[APIController][getFinancialDetails] Error received: " + res)
              Status(res.status)(Json.toJson(res.body))
            case _ =>
              println("res.status getFinancialDetails..."+ res.status)
              logger.warn(s"[APIController][getFinancialDetails] status ${res.status} returned from EIS " +
                s"Status code:'${res.status}', Body: '${res.body}")
              Status(res.status)(Json.toJson(res.body))
          }
        )
      }
    }

  def getPenaltyDetailsForThirdPartyAPI(vrn: String, dateLimit: Option[String]): Action[AnyContent] = Action.async {
    implicit request => {
      val response = getPenaltyDetailsConnector.getPenaltyDetailsForThirdPartyAPI(vrn, dateLimit)
      response.map(
        res => res.status match {
          case OK =>
            logger.debug("[APIController][getPenaltyDetailsForThirdPartyAPI] Ok response received: " + res)
            Ok(res.json)
          case NOT_FOUND =>
            println("case NOT_FOUND..."+ res.status)
            logger.debug("[APIController][getPenaltyDetailsForThirdPartyAPI] Error received: " + res)
            Status(res.status)(Json.toJson(res.body))
          case _ =>
            println("res.status getPenaltyDetailsForThirdPartyAPI..."+ res.status)
            logger.warn(s"[APIController][getPenaltyDetailsForThirdPartyAPI] status ${res.status} returned from EIS " +
              s"Status code:'${res.status}', Body: '${res.body}")
            Status(res.status)(Json.toJson(res.body))
        }
      )
    }
  }
}
