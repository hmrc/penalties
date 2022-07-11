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
import utils.RegimeHelper

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PenaltiesFrontendController @Inject()(auditService: AuditService,
                               getPenaltyDetailsService: GetPenaltyDetailsService,
                               getFinancialDetailsService: GetFinancialDetailsService,
                               penaltiesFrontendService: PenaltiesFrontendService,
                               cc: ControllerComponents)
  extends BackendController(cc) {

  def getPenaltiesData(enrolmentKey: String, arn: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request => {
      val vrn: String = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
      getPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(vrn).flatMap {
        _.fold({
          case GetPenaltyDetailsNoContent => {
            logger.info(s"[PenaltiesFrontendController][handleGetPenaltyDetailsCall] - Received 404 for VRN: $vrn with NO_DATA_FOUND in response body")
            Future(NoContent)
          }
          case GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND => {
            logger.info(s"[PenaltiesFrontendController][handleGetPenaltyDetailsCall] - 1812 call returned 404 for VRN: $vrn")
            Future(NotFound(s"A downstream call returned 404 for VRN: $vrn"))
          }
          case GetPenaltyDetailsFailureResponse(status) => {
            logger.error(s"[PenaltiesFrontendController][handleGetPenaltyDetailsCall] - 1812 call returned an unexpected status: $status")
            Future(InternalServerError(s"A downstream call returned an unexpected status: $status"))
          }
          case GetPenaltyDetailsMalformed => {
            logger.error(s"[PenaltiesFrontendController][handleGetPenaltyDetailsCall] - Failed to parse penalty details response")
            Future(InternalServerError(s"We were unable to parse penalty data."))
          }
        },
          penaltyDetailsSuccess => {
            handleAndCombineGetFinancialDetailsData(penaltyDetailsSuccess.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, enrolmentKey, arn)
          }
        )
      }
    }
  }

  private def handleAndCombineGetFinancialDetailsData(penaltyDetails: GetPenaltyDetails, enrolmentKey: String, arn: Option[String])
                                           (implicit request: Request[_]): Future[Result] = {
    val vrn: String = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
    getFinancialDetailsService.getDataFromFinancialServiceForVATVCN(vrn).map {
      _.fold(
        {
          case GetFinancialDetailsNoContent => {
            logger.info(s"[PenaltiesFrontendController][handleGetFinancialDetailsCall] - Received 404 for VRN: $vrn with NO_DATA_FOUND in response body")
            if(penaltyDetails.latePaymentPenalty.isEmpty ||
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
            logger.error(s"[PenaltiesFrontendController][handleGetFinancialDetailsCall] - Failed to parse financial details response")
            InternalServerError(s"We were unable to parse penalty data.")
          }
        },
        financialDetailsSuccess => {
          val newPenaltyDetails = penaltiesFrontendService.combineAPIData(penaltyDetails,
            financialDetailsSuccess.asInstanceOf[GetFinancialDetailsSuccessResponse].financialDetails)
          returnResponse(newPenaltyDetails, enrolmentKey, arn)
        }
      )
    }
  }

  private def returnResponse(penaltyDetails: GetPenaltyDetails, enrolmentKey: String, arn: Option[String] = None)(implicit request: Request[_]): Result = {
    if (penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0) > 0) {
      val auditModel = UserHasPenaltyAuditModel(penaltyDetails, RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
        RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey), arn)
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(penaltyDetails))
  }
}
