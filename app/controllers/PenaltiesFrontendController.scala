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

import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser._
import controllers.auth.AuthAction
import models.auditing.UserHasPenaltyRegimeAuditModel
import models.getPenaltyDetails.GetPenaltyDetails
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.PenaltiesFrontendService._
import services.auditing.AuditService
import services.{PenaltyDetailsService, PenaltiesFrontendService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.DateHelper
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PenaltiesFrontendController @Inject()(
                                             penaltyDetailsService: PenaltyDetailsService,
                                             penaltiesFrontendService: PenaltiesFrontendService,
                                             auditService: AuditService,
                                             dateHelper: DateHelper,
                                             cc: ControllerComponents,
                                             authAction: AuthAction
                                           )(implicit ec: ExecutionContext, val config: Configuration) extends BackendController(cc) {

  def getPenaltiesData(regime: Regime, idType: IdType, id: Id, arn: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
    val agnosticEnrolmentKey = AgnosticEnrolmentKey(regime, idType, id)
    
    penaltyDetailsService.getPenaltyDetails(agnosticEnrolmentKey).flatMap {
      handlePenaltyDetailsResponse(_, agnosticEnrolmentKey, arn)
    }
  }

  private def handlePenaltyDetailsResponse(response: GetPenaltyDetailsResponse, agnosticEnrolmentKey: AgnosticEnrolmentKey, arn: Option[String])(
      implicit request: Request[_],
      hc: HeaderCarrier): Future[Result] =
    response match {
      case Left(GetPenaltyDetailsNoContent) =>
        logger.info(
          s"[RegimePenaltiesFrontendController][getPenaltiesData] - call returned 404 for $agnosticEnrolmentKey with NO_DATA_FOUND in response body")
        Future.successful(NoContent)
      case Left(GetPenaltyDetailsFailureResponse(status)) if status == NOT_FOUND =>
        logger.info(s"[RegimePenaltiesFrontendController][getPenaltiesData] - call returned 404 for $agnosticEnrolmentKey")
        Future.successful(NotFound(s"A downstream call returned 404 for $agnosticEnrolmentKey"))
      case Left(GetPenaltyDetailsFailureResponse(status)) =>
        logger.error(
          s"[RegimePenaltiesFrontendController][getPenaltiesData] - call returned an unexpected status: $status for $agnosticEnrolmentKey")
        Future.successful(InternalServerError(s"A downstream call returned an unexpected status: $status"))
      case Left(GetPenaltyDetailsMalformed) =>
        PagerDutyHelper.log("getPenaltiesData", MALFORMED_RESPONSE_FROM_1812_API)
        logger.error(
          s"[RegimePenaltiesFrontendController][getPenaltiesData] - call returned invalid body - failed to parse penalty details response for $agnosticEnrolmentKey")
        Future.successful(InternalServerError(s"We were unable to parse penalty data."))
      case Right(GetPenaltyDetailsSuccessResponse(penaltyDetails)) =>
        logger.info(s"[RegimePenaltiesFrontendController][getPenaltiesData] - call returned 200 for $agnosticEnrolmentKey")
        penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
          penaltyDetails,
          agnosticEnrolmentKey,
          arn
        ).map {
          case Left(error)   => handlePenaltiesFrontendError(error, agnosticEnrolmentKey)
          case Right(result) => handleFinancialDetailsCombinationResult(result, agnosticEnrolmentKey, arn)
        }
    }

  private def handleFinancialDetailsCombinationResult(result: FinancialDetailsCombinationResult,
                                                      enrolmentKey: AgnosticEnrolmentKey,
                                                      arn: Option[String])(implicit request: Request[_],
                                                                            hc: HeaderCarrier): Result =
    result match {
      case PenaltyDetailsFromFirstNoContent(penaltyDetails) =>
        logFinancialDetailsNoContent(enrolmentKey)
        returnResponse(penaltyDetails, enrolmentKey, arn)
      case NoPenaltyDetailsFromFirstNoContent =>
        logFinancialDetailsNoContent(enrolmentKey)
        NoContent
      case PenaltyDetailsFromSecondNoContent(penaltyDetails) =>
        logFinancialDetailsWithClearedItemsSuccess(enrolmentKey)
        logFinancialDetailsNoContent(enrolmentKey)
        returnResponse(penaltyDetails, enrolmentKey, arn)
      case CombinedPenaltyDetails(penaltyDetails) =>
        logFinancialDetailsWithClearedItemsSuccess(enrolmentKey)
        logger.info(
          s"[RegimePenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 clearedItems=false call returned 200 for $enrolmentKey")
        logger.info(s"[RegimePenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned 200 for $enrolmentKey")
        returnResponse(penaltyDetails, enrolmentKey, arn)
    }

  private def handlePenaltiesFrontendError(error: PenaltiesFrontendError, enrolmentKey: AgnosticEnrolmentKey): Result = {
    if (error.clearedItemsCallSucceeded) {
      logFinancialDetailsWithClearedItemsSuccess(enrolmentKey)
    }

    error match {
      case FinancialDetailsNotFound(_, _) =>
        logger.info(error.message)
        NotFound(s"A downstream call returned 404 for $enrolmentKey")
      case FinancialDetailsUnexpectedStatus(status, _) =>
        logger.error(error.message)
        InternalServerError(s"A downstream call returned an unexpected status: $status")
      case FinancialDetailsMalformedResponse(_, _) =>
        PagerDutyHelper.log("getPenaltiesData", MALFORMED_RESPONSE_FROM_1811_API)
        logger.error(error.message)
        InternalServerError(s"We were unable to parse penalty data.")
      case FinancialDetailsNoDataFound(_, _) =>
        logger.info(error.message)
        NoContent
      case MissingManualLPPField(_) =>
        logger.error(error.message)
        InternalServerError(s"We were unable to parse penalty data.")
    }
  }

  private def returnResponse(penaltyDetails: GetPenaltyDetails, enrolmentKey: AgnosticEnrolmentKey, arn: Option[String])(implicit
      request: Request[_],
      hc: HeaderCarrier): Result = {
    val hasLSP = penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0) > 0
    val hasLPP = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.length)).getOrElse(0) > 0

    if (hasLSP || hasLPP) {
      val auditModel =
        UserHasPenaltyRegimeAuditModel(penaltyDetails = penaltyDetails, enrolmentKey = enrolmentKey, arn = arn, dateHelper = dateHelper)
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(penaltyDetails))
  }

  private def logFinancialDetailsNoContent(enrolmentKey: AgnosticEnrolmentKey): Unit =
    logger.info(
      s"[RegimePenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned 404 for $enrolmentKey with NO_DATA_FOUND in response body")

  private def logFinancialDetailsWithClearedItemsSuccess(enrolmentKey: AgnosticEnrolmentKey): Unit =
    logger.info(
      s"[RegimePenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 clearedItems=true call returned 200 for $enrolmentKey")
}
