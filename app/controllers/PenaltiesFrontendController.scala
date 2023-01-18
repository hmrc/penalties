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

import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsSuccessResponse, _}
import models.auditing.UserHasPenaltyAuditModel
import models.getPenaltyDetails.GetPenaltyDetails
import play.api.libs.json.Json
import play.api.mvc._
import services.auditing.AuditService
import services.{GetFinancialDetailsService, GetPenaltyDetailsService, PenaltiesFrontendService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{DateHelper, PagerDutyHelper, RegimeHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PenaltiesFrontendController @Inject()(
                                             auditService: AuditService,
                                             getPenaltyDetailsService: GetPenaltyDetailsService,
                                             getFinancialDetailsService: GetFinancialDetailsService,
                                             penaltiesFrontendService: PenaltiesFrontendService,
                                             dateHelper: DateHelper,
                                             cc: ControllerComponents
                                           )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getPenaltiesData(enrolmentKey: String, arn: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request => {
      val vrn: String = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
      getPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(vrn).flatMap {
        _.fold({
          case GetPenaltyDetailsNoContent => {
            logger.info(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned 404 for VRN: $vrn with NO_DATA_FOUND in response body")
            Future(NoContent)
          }
          case GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND => {
            logger.info(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned 404 for VRN: $vrn")
            Future(NotFound(s"A downstream call returned 404 for VRN: $vrn"))
          }
          case GetPenaltyDetailsFailureResponse(status) => {
            logger.error(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned an unexpected status: $status")
            Future(InternalServerError(s"A downstream call returned an unexpected status: $status"))
          }
          case GetPenaltyDetailsMalformed => {
            PagerDutyHelper.log("getPenaltiesData", MALFORMED_RESPONSE_FROM_1812_API)
            logger.error(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned invalid body - failed to parse penalty details response")
            Future(InternalServerError(s"We were unable to parse penalty data."))
          }
        },
          penaltyDetailsSuccess => {
            logger.info(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned 200 for VRN: $vrn")
            handleAndCombineGetFinancialDetailsData(penaltyDetailsSuccess.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, enrolmentKey, arn)
          }
        )
      }
    }
  }

  private def handleAndCombineGetFinancialDetailsData(penaltyDetails: GetPenaltyDetails, enrolmentKey: String, arn: Option[String])
                                                     (implicit request: Request[_]): Future[Result] = {
    val vrn: String = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
    getFinancialDetailsService.getFinancialDetails(vrn).map {
      _.fold(
        {
          case GetFinancialDetailsNoContent => {
            logger.info(s"[PenaltiesFrontendController][handleGetFinancialDetailsCall] - 1811 call returned 404 for VRN: $vrn with NO_DATA_FOUND in response body")
            if (penaltyDetails.latePaymentPenalty.isEmpty ||
              penaltyDetails.latePaymentPenalty.get.details.isEmpty ||
              penaltyDetails.latePaymentPenalty.get.details.get.isEmpty) {
              returnResponse(penaltyDetails, enrolmentKey, arn)
            } else {
              NoContent
            }
          }
          case GetFinancialDetailsFailureResponse(status) if status == NOT_FOUND => {
            logger.info(s"[PenaltiesFrontendController][handleGetFinancialDetailsCall] - 1811 call returned 404 for VRN: $vrn")
            NotFound(s"A downstream call returned 404 for VRN: $vrn")
          }
          case GetFinancialDetailsFailureResponse(status) => {
            logger.error(s"[PenaltiesFrontendController][handleGetFinancialDetailsCall] - 1811 call returned an unexpected status: $status")
            InternalServerError(s"A downstream call returned an unexpected status: $status")
          }
          case GetFinancialDetailsMalformed => {
            PagerDutyHelper.log("getPenaltiesData", MALFORMED_RESPONSE_FROM_1811_API)
            logger.error(s"[PenaltiesFrontendController][handleGetFinancialDetailsCall] - 1811 call returned invalid body - failed to parse financial details response")
            InternalServerError(s"We were unable to parse penalty data.")
          }
        },
        financialDetailsSuccess => {
          val newPenaltyDetails = penaltiesFrontendService.combineAPIData(penaltyDetails,
            financialDetailsSuccess.asInstanceOf[GetFinancialDetailsSuccessResponse].financialDetails)
          logger.info(s"[PenaltiesFrontendController][handleGetFinancialDetailsCall] - 1811 call returned 200 for VRN: $vrn")
          returnResponse(newPenaltyDetails, enrolmentKey, arn)
        }
      )
    }
  }

  private def returnResponse(penaltyDetails: GetPenaltyDetails, enrolmentKey: String, arn: Option[String])(implicit request: Request[_]): Result = {
    val hasLSP = penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0) > 0
    val hasLPP = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.length)).getOrElse(0) > 0

    if (hasLSP || hasLPP) {
      val auditModel = UserHasPenaltyAuditModel(
        penaltyDetails = penaltyDetails,
        identifier = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
        identifierType = RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey),
        arn = arn,
        dateHelper = dateHelper)
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(penaltyDetails))
  }
}
