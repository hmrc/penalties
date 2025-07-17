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

import config.AppConfig
import connectors.FileNotificationOrchestratorConnector
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{GetPenaltyDetailsSuccessResponse, GetPenaltyDetailsResponse}
import controllers.auth.AuthAction
import models.appeals.AppealTypeEnum._
import models.appeals._
import models.appeals.reasonableExcuses.ReasonableExcuse
import models.auditing.PenaltyAppealFileNotificationStorageFailureModel
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment.LPPDetails
import models.getPenaltyDetails.lateSubmission.LSPDetails
import models.notification._
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.auditing.AuditService
import services.{PenaltyDetailsService, RegimeAppealService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{PagerDutyHelper, PenaltyPeriodHelper}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegimeAppealsController @Inject()(val appConfig: AppConfig,
                                  appealService: RegimeAppealService,
                                  getPenaltyDetailsService: PenaltyDetailsService,
                                  fileNotificationOrchestratorConnector: FileNotificationOrchestratorConnector,
                                  auditService: AuditService,
                                  cc: ControllerComponents,
                                  authAction: AuthAction)(implicit ec: ExecutionContext, val config: Configuration)
  extends BackendController(cc) {

  private def getAppealDataForPenalty(penaltyId: String, enrolmentKey: AgnosticEnrolmentKey,
                                    penaltyType: AppealTypeEnum.Value)(implicit hc: HeaderCarrier): Future[Result] = {

    getPenaltyDetailsService.getPenaltyDetails(enrolmentKey).map {
      handlePenaltyDetailsResponseForAppeals(_, enrolmentKey.toString, penaltyId, penaltyType)
    }
  }

  private def handlePenaltyDetailsResponseForAppeals(response: GetPenaltyDetailsResponse, enrolmentKey: String, penaltyId: String, penaltyType: AppealTypeEnum.Value): Result = {
    response.fold(
        handleFailureResponse(_, enrolmentKey)("getAppealDataForPenalty"),
        success => {
          checkAndReturnResponseForPenaltyData(success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, penaltyId, enrolmentKey, penaltyType)
        }
      )
  }

  def getAppealsDataForLateSubmissionPenalty(penaltyId: String, regime: Regime, idType: IdType, id: Id): Action[AnyContent] = authAction.async {
    implicit request => {
      val agnosticEnrolmentKey = AgnosticEnrolmentKey(regime, idType, id)
      getAppealDataForPenalty(penaltyId, agnosticEnrolmentKey, Late_Submission)
    }
  }

  def getAppealsDataForLatePaymentPenalty(penaltyId: String, regime: Regime, idType: IdType, id: Id, isAdditional: Boolean): Action[AnyContent] = authAction.async {
    implicit request => {
      val agnosticEnrolmentKey = AgnosticEnrolmentKey(regime, idType, id)
      getAppealDataForPenalty(penaltyId, agnosticEnrolmentKey, if (isAdditional) Additional else Late_Payment)
    }
  }

  private def checkAndReturnResponseForPenaltyData(penaltyDetails: GetPenaltyDetails,
                                                   penaltyIdToCheck: String,
                                                   enrolmentKey: String,
                                                   appealType: AppealTypeEnum.Value): Result = {
    val lspPenaltyIdInPenaltyDetailsPayload: Option[LSPDetails] = penaltyDetails.lateSubmissionPenalty.flatMap {
      _.details.find(_.penaltyNumber == penaltyIdToCheck)
    }
    val lppPenaltyIdInPenaltyDetailsPayload: Option[LPPDetails] = penaltyDetails.latePaymentPenalty.flatMap {
      _.details.flatMap(_.find(_.penaltyChargeReference.contains(penaltyIdToCheck)))
    }

    if (appealType == AppealTypeEnum.Late_Submission && lspPenaltyIdInPenaltyDetailsPayload.isDefined) {
      logger.info(s"[RegimeAppealsController][getAppealsData] Penalty ID: $penaltyIdToCheck for enrolment key: $enrolmentKey found in ETMP for $appealType.")
      val penaltyBasedOnId = lspPenaltyIdInPenaltyDetailsPayload.get
      val sortedDate = penaltyBasedOnId.lateSubmissions.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head
      val dataToReturn: AppealData = AppealData(
        `type` = appealType,
        startDate = sortedDate.taxPeriodStartDate.get,
        endDate = sortedDate.taxPeriodEndDate.get,
        dueDate = sortedDate.taxPeriodDueDate.get,
        dateCommunicationSent = penaltyBasedOnId.communicationsDate.getOrElse(appConfig.getTimeMachineDateTime.toLocalDate)
      )
      Ok(Json.toJson(dataToReturn))
    } else if ((appealType == AppealTypeEnum.Late_Payment || appealType == AppealTypeEnum.Additional) && lppPenaltyIdInPenaltyDetailsPayload.isDefined) {
      logger.info(s"[RegimeAppealsController][getAppealsData] Penalty ID: $penaltyIdToCheck for enrolment key: $enrolmentKey found in ETMP for $appealType.")
      val penaltyBasedOnId = lppPenaltyIdInPenaltyDetailsPayload.get
      val dataToReturn: AppealData = AppealData(
        `type` = appealType,
        startDate = penaltyBasedOnId.principalChargeBillingFrom,
        endDate = penaltyBasedOnId.principalChargeBillingTo,
        dueDate = penaltyBasedOnId.principalChargeDueDate,
        dateCommunicationSent = penaltyBasedOnId.communicationsDate.getOrElse(appConfig.getTimeMachineDateTime.toLocalDate)
      )
      Ok(Json.toJson(dataToReturn))
    } else {
      logger.info(s"[RegimeAppealsController][getAppealsData] Data retrieved for enrolment: $enrolmentKey but provided penalty ID: $penaltyIdToCheck was not found. Returning 404.")
      NotFound("Penalty ID was not found in users penalties.")
    }
  }

  def getReasonableExcuses(regime: Regime): Action[AnyContent] = Action {
    Ok(ReasonableExcuse.allExcusesToJson(appConfig, regime))
  }

  def submitAppeal(regime: Regime, idType: IdType, id: Id, penaltyNumber: String, correlationId: String, isMultiAppeal: Boolean): Action[AnyContent] = authAction.async {
    implicit request => {
      val agnosticEnrolmenKey = AgnosticEnrolmentKey(regime, idType, id)
      request.body.asJson.fold({
        logger.error(s"[RegimeAppealsController][submitAppeal] Unable to submit appeal for user with enrolment: $agnosticEnrolmenKey penalty $penaltyNumber - Failed to validate request body as JSON")
        Future(BadRequest("Invalid body received i.e. could not be parsed to JSON"))
      })(
        jsonBody => {
          val parseResultToModel = Json.fromJson(jsonBody)(AppealSubmission.apiReads)
          parseResultToModel.fold(
            failure => {
              logger.warn(s"[RegimeAppealsController][submitAppeal] Unable to submit appeal for user with enrolment: $agnosticEnrolmenKey penalty $penaltyNumber - Failed to parse request body to model")
              logger.error(s"[RegimeAppealsController][submitAppeal] Parse failure(s): $failure")
              Future(BadRequest("Failed to parse to model"))
            },
            appealSubmission => {
              submitAppealToPEGA(appealSubmission, agnosticEnrolmenKey, penaltyNumber, correlationId, isMultiAppeal).map {
                responseModel => {
                  Status(responseModel.status)(Json.toJson(responseModel))
                }
              }
            }
          )
        }
      )
    }
  }

  private def submitAppealToPEGA(appealSubmission: AppealSubmission, enrolmentKey: AgnosticEnrolmentKey,
                                 penaltyNumber: String, correlationId: String, isMultiAppeal: Boolean)
                                (implicit hc: HeaderCarrier, request: Request[_]): Future[AppealSubmissionResponseModel] = {
    appealService.submitAppeal(appealSubmission, enrolmentKey, penaltyNumber, correlationId).flatMap {
      _.fold(
        error => {
          logger.error(s"[RegimeAppealsController][submitAppeal] Error submiting appeal to PEGA for user with enrolment: $enrolmentKey penalty $penaltyNumber - Received error from PEGA with status ${error.status} and error message: ${error.body} " +
            s"for correlation ID: $correlationId")
          val responseModel = AppealSubmissionResponseModel(error = Some(error.body), status = error.status)
          Future(responseModel)
        },
        responseModel => {
          logger.info(s"[RegimeAppealsController][submitAppeal] - Successfully sent appeal submission to PEGA for user with enrolment: $enrolmentKey and penalty number: $penaltyNumber" +
            s" (correlation ID: $correlationId)")
          val appeal = appealSubmission.appealInformation
          logger.info(s"[RegimeAppealsController][submitAppeal] Received caseID response: ${responseModel.caseID} from downstream.")
          val seqOfNotifications = appeal match {
            case otherAppeal: OtherAppealInformation if otherAppeal.uploadedFiles.isDefined =>
              appealService.createSDESNotifications(otherAppeal.uploadedFiles, responseModel.caseID)
            case _ => Seq.empty
          }
          if (seqOfNotifications.nonEmpty) {
            val redactedNotification = seqOfNotifications.map(notification => notification.copy(file = notification.file.copy(location = "HIDDEN")))
            logger.info(s"[RegimeAppealsController][submitAppeal] Posting SDESNotifications: $redactedNotification to Orchestrator")
            fileNotificationOrchestratorConnector.postFileNotifications(seqOfNotifications).map {
              response =>
                response.status match {
                  case OK =>
                    logger.info(s"[RegimeAppealsController][submitAppeal] - Received OK from file notification orchestrator for correlation ID: $correlationId")
                    val submissionResponseModel = AppealSubmissionResponseModel(caseId = Some(responseModel.caseID), status = OK)
                    submissionResponseModel
                  case status =>
                    PagerDutyHelper.logStatusCode("submitAppeal", status)(RECEIVED_4XX_FROM_FILE_NOTIFICATION_ORCHESTRATOR, RECEIVED_5XX_FROM_FILE_NOTIFICATION_ORCHESTRATOR)
                    logger.error(s"[RegimeAppealsController][submitAppeal] Unable to store file notification for user with enrolment: $enrolmentKey penalty $penaltyNumber (correlation ID: $correlationId) - Received unknown response ($status) from file notification orchestrator. Response body: ${response.body}")
                    auditStorageFailureOfFileNotifications(seqOfNotifications)
                    returnErrorResponseIfMultiAppeal(isMultiAppeal)(s"Appeal submitted (case ID: ${responseModel.caseID}, correlation ID: $correlationId) but received $status response from file notification orchestrator")(responseModel.caseID)
                }
            }.recover {
              case e => {
                logger.error(s"[RegimeAppealsController][submitAppeal] Unable to store file notification for user with enrolment: $enrolmentKey penalty $penaltyNumber (correlation ID: $correlationId) - An unknown exception occurred when attempting to store file notifications, with error: ${e.getMessage}")
                auditStorageFailureOfFileNotifications(seqOfNotifications)
                returnErrorResponseIfMultiAppeal(isMultiAppeal)(s"Appeal submitted (case ID: ${responseModel.caseID}, correlation ID: $correlationId) but failed to store file uploads with unknown error")(responseModel.caseID)
              }
            }
          } else {
            val submissionResponseModel = AppealSubmissionResponseModel(caseId = Some(responseModel.caseID), status = OK)
            Future(submissionResponseModel)
          }
        }
      )
    }
  }

  private def returnErrorResponseIfMultiAppeal(isMultiAppeal: Boolean)(messageIfReturningError: String)(caseId: String): AppealSubmissionResponseModel = {
    if (isMultiAppeal) {
      AppealSubmissionResponseModel(caseId = Some(caseId), status = MULTI_STATUS, error = Some(messageIfReturningError))
    } else {
      AppealSubmissionResponseModel(caseId = Some(caseId), status = OK)
    }

  }

  def getMultiplePenaltyData(penaltyId: String, regime: Regime, idType: IdType, id: Id): Action[AnyContent] = authAction.async {
    implicit request => {
      val agnosticEnrolmentKey = AgnosticEnrolmentKey(regime, idType, id)
      getPenaltyDetailsService.getPenaltyDetails(agnosticEnrolmentKey).map {
        _.fold(
          handleFailureResponse(_, agnosticEnrolmentKey.toString)("getMultiplePenaltyData"),
          success => {
            val penaltyDetails = success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails
            val multiplePenaltiesData: Option[MultiplePenaltiesData] = appealService.findMultiplePenalties(penaltyDetails, penaltyId)
            multiplePenaltiesData.fold(NoContent)(data => Ok(Json.toJson(data)))
          }
        )
      }
    }
  }

  private def handleFailureResponse(response: PenaltyDetailsParser.GetPenaltyDetailsFailure, enrolmentKey: String)(callingMethod: String): Result = {
    response match {
      case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND => {
        logger.info(s"[RegimeAppealsController][$callingMethod] - call returned 404 for enrolment key: $enrolmentKey")
        NotFound(s"A downstream call returned 404 for ${enrolmentKey}")
      }
      case PenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) => {
        logger.error(s"[RegimeAppealsController][$callingMethod] - call returned an unexpected status: $status for ${enrolmentKey}")
        InternalServerError(s"A downstream call returned an unexpected status: $status")
      }
      case PenaltyDetailsParser.GetPenaltyDetailsMalformed => {
        PagerDutyHelper.log(callingMethod, MALFORMED_RESPONSE_FROM_1812_API)
        logger.error(s"[RegimeAppealsController][$callingMethod] - Failed to parse penalty details response for ${enrolmentKey}")
        InternalServerError("We were unable to parse penalty data.")
      }
      case PenaltyDetailsParser.GetPenaltyDetailsNoContent => {
        logger.info(s"s[RegimeAppealsController][$callingMethod] - call returned no content for ${enrolmentKey}")
        InternalServerError(s"Returned no content for ${enrolmentKey}")
      }
    }
  }

  private def auditStorageFailureOfFileNotifications(notifications: Seq[SDESNotification])
                                                    (implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Unit = {
    val auditModel = PenaltyAppealFileNotificationStorageFailureModel(notifications)
    logger.info(s"[RegimeAppealsController][auditStorageFailureOfFileNotifications] - Auditing ${notifications.size} notifications that were not stored successfully")
    auditService.audit(auditModel)
  }
}
