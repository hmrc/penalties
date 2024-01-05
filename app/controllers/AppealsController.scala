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

import config.AppConfig
import config.featureSwitches.FeatureSwitching
import connectors.FileNotificationOrchestratorConnector
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.GetPenaltyDetailsSuccessResponse
import models.appeals.AppealTypeEnum._
import models.appeals._
import models.appeals.reasonableExcuses.ReasonableExcuse
import models.auditing.PenaltyAppealFileNotificationStorageFailureModel
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment.LPPDetails
import models.getPenaltyDetails.lateSubmission.LSPDetails
import models.notification._
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.auditing.AuditService
import services.{AppealService, GetPenaltyDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{PagerDutyHelper, PenaltyPeriodHelper, RegimeHelper}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AppealsController @Inject()(val appConfig: AppConfig,
                                  appealService: AppealService,
                                  getPenaltyDetailsService: GetPenaltyDetailsService,
                                  fileNotificationOrchestratorConnector: FileNotificationOrchestratorConnector,
                                  auditService: AuditService,
                                  cc: ControllerComponents)(implicit ec: ExecutionContext, val config: Configuration)
  extends BackendController(cc) with FeatureSwitching {

  private def getAppealDataForPenalty(penaltyId: String, enrolmentKey: String,
                                      penaltyType: AppealTypeEnum.Value)(implicit hc: HeaderCarrier): Future[Result] = {
    val vrn = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
    getPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(vrn).map {
      _.fold(
        handleFailureResponse(_, vrn, enrolmentKey)("getAppealDataForPenalty"),
        success => {
          checkAndReturnResponseForPenaltyData(success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, penaltyId, enrolmentKey, penaltyType)
        }
      )
    }
  }

  def getAppealsDataForLateSubmissionPenalty(penaltyId: String, enrolmentKey: String): Action[AnyContent] = Action.async {
    implicit request => {
      getAppealDataForPenalty(penaltyId, enrolmentKey, Late_Submission)
    }
  }

  def getAppealsDataForLatePaymentPenalty(penaltyId: String, enrolmentKey: String, isAdditional: Boolean): Action[AnyContent] = Action.async {
    implicit request => {
      getAppealDataForPenalty(penaltyId, enrolmentKey, if (isAdditional) Additional else Late_Payment)
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
      logger.debug(s"[AppealsController][getAppealsData] Penalty ID: $penaltyIdToCheck for enrolment key: $enrolmentKey found in ETMP for $appealType.")
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
      logger.debug(s"[AppealsController][getAppealsData] Penalty ID: $penaltyIdToCheck for enrolment key: $enrolmentKey found in ETMP for $appealType.")
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
      logger.info(s"[AppealsController][getAppealsData] Data retrieved for enrolment: $enrolmentKey but provided penalty ID: $penaltyIdToCheck was not found. Returning 404.")
      NotFound("Penalty ID was not found in users penalties.")
    }
  }

  def getReasonableExcuses: Action[AnyContent] = Action {
    Ok(ReasonableExcuse.allExcusesToJson(appConfig))
  }

  def submitAppeal(enrolmentKey: String, isLPP: Boolean, penaltyNumber: String, correlationId: String, isMultiAppeal: Boolean): Action[AnyContent] = Action.async {
    implicit request => {
      request.body.asJson.fold({
        logger.error(s"[AppealsController][submitAppeal] Unable to submit appel for user with enrolment: $enrolmentKey penalty $penaltyNumber - Failed to validate request body as JSON")
        Future(BadRequest("Invalid body received i.e. could not be parsed to JSON"))
      })(
        jsonBody => {
          val parseResultToModel = Json.fromJson(jsonBody)(AppealSubmission.apiReads)
          parseResultToModel.fold(
            failure => {
              logger.error(s"[AppealsController][submitAppeal] Unable to submit appel for user with enrolment: $enrolmentKey penalty $penaltyNumber - Failed to parse request body to model")
              logger.debug(s"[AppealsController][submitAppeal] Parse failure(s): $failure")
              Future(BadRequest("Failed to parse to model"))
            },
            appealSubmission => {
              submitAppealToPEGA(appealSubmission, enrolmentKey, isLPP, penaltyNumber, correlationId, isMultiAppeal).map {
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

  private def submitAppealToPEGA(appealSubmission: AppealSubmission, enrolmentKey: String,
                                 isLPP: Boolean, penaltyNumber: String, correlationId: String, isMultiAppeal: Boolean)
                                (implicit hc: HeaderCarrier, request: Request[_]): Future[AppealSubmissionResponseModel] = {
    appealService.submitAppeal(appealSubmission, enrolmentKey, isLPP, penaltyNumber, correlationId).flatMap {
      _.fold(
        error => {
          logger.error(s"[AppealsController][submitAppeal] Error submiting appeal to PEGA for user with enrolment: $enrolmentKey penalty $penaltyNumber - Received error from PEGA with status ${error.status} and error message: ${error.body} " +
            s"for correlation ID: $correlationId")
          logger.debug(s"[AppealsController][submitAppeal] Returning ${error.status} to calling service.")
          val responseModel = AppealSubmissionResponseModel(error = Some(error.body), status = error.status)
          Future(responseModel)
        },
        responseModel => {
          logger.info(s"[AppealsController][submitAppeal] - Successfully sent appeal submission to PEGA for user with enrolment: $enrolmentKey and penalty number: $penaltyNumber" +
            s" (correlation ID: $correlationId)")
          val appeal = appealSubmission.appealInformation
          logger.debug(s"[AppealsController][submitAppeal] Received caseID response: ${responseModel.caseID} from downstream.")
          val seqOfNotifications = appeal match {
            case otherAppeal: OtherAppealInformation if otherAppeal.uploadedFiles.isDefined =>
              appealService.createSDESNotifications(otherAppeal.uploadedFiles, responseModel.caseID)
            case _ => Seq.empty
          }
          if (seqOfNotifications.nonEmpty) {
            val redactedNotification = seqOfNotifications.map(notification => notification.copy(file = notification.file.copy(location = "HIDDEN")))
            logger.debug(s"[AppealsController][submitAppeal] Posting SDESNotifications: $redactedNotification to Orchestrator")
            fileNotificationOrchestratorConnector.postFileNotifications(seqOfNotifications).map {
              response =>
                response.status match {
                  case OK =>
                    logger.info(s"[AppealsController][submitAppeal] - Received OK from file notification orchestrator for correlation ID: $correlationId")
                    val submissionResponseModel = AppealSubmissionResponseModel(caseId = Some(responseModel.caseID), status = OK)
                    submissionResponseModel
                  case status =>
                    PagerDutyHelper.logStatusCode("submitAppeal", status)(RECEIVED_4XX_FROM_FILE_NOTIFICATION_ORCHESTRATOR, RECEIVED_5XX_FROM_FILE_NOTIFICATION_ORCHESTRATOR)
                    logger.error(s"[AppealsController][submitAppeal] Unable to store file notification for user with enrolment: $enrolmentKey penalty $penaltyNumber (correlation ID: $correlationId) - Received unknown response ($status) from file notification orchestrator. Response body: ${response.body}")
                    auditStorageFailureOfFileNotifications(seqOfNotifications)
                    returnErrorResponseIfMultiAppeal(isMultiAppeal)(s"Appeal submitted (case ID: ${responseModel.caseID}, correlation ID: $correlationId) but received $status response from file notification orchestrator")(responseModel.caseID)
                }
            }.recover {
              case e => {
                logger.error(s"[AppealsController][submitAppeal] Unable to store file notification for user with enrolment: $enrolmentKey penalty $penaltyNumber (correlation ID: $correlationId) - An unknown exception occurred when attempting to store file notifications, with error: ${e.getMessage}")
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

  def getMultiplePenaltyData(penaltyId: String, enrolmentKey: String): Action[AnyContent] = Action.async {
    implicit request => {
      val vrn = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
      getPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(vrn).map {
        _.fold(
          handleFailureResponse(_, vrn, enrolmentKey)("getMultiplePenaltyData"),
          success => {
            val penaltyDetails = success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails
            val multiplePenaltiesData: Option[MultiplePenaltiesData] = appealService.findMultiplePenalties(penaltyDetails, penaltyId)
            multiplePenaltiesData.fold(NoContent)(data => Ok(Json.toJson(data)))
          }
        )
      }
    }
  }

  private def handleFailureResponse(response: GetPenaltyDetailsParser.GetPenaltyDetailsFailure,
                                    vrn: String, enrolmentKey: String)(callingMethod: String): Result = {
    response match {
      case GetPenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND => {
        logger.info(s"[AppealsController][$callingMethod] - 1812 call returned 404 for enrolment key: $enrolmentKey")
        NotFound(s"A downstream call returned 404 for VRN: $vrn")
      }
      case GetPenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) => {
        logger.error(s"[AppealsController][$callingMethod] - 1812 call returned an unexpected status: $status for VRN: $vrn")
        InternalServerError(s"A downstream call returned an unexpected status: $status")
      }
      case GetPenaltyDetailsParser.GetPenaltyDetailsMalformed => {
        PagerDutyHelper.log(callingMethod, MALFORMED_RESPONSE_FROM_1812_API)
        logger.error(s"[AppealsController][$callingMethod] - Failed to parse penalty details response for VRN: $vrn")
        InternalServerError("We were unable to parse penalty data.")
      }
      case GetPenaltyDetailsParser.GetPenaltyDetailsNoContent => {
        logger.info(s"s[AppealsController][$callingMethod] - 1812 call returned no content for VRN: $vrn")
        InternalServerError(s"Returned no content for VRN: $vrn")
      }
    }
  }

  private def auditStorageFailureOfFileNotifications(notifications: Seq[SDESNotification])
                                                    (implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Unit = {
    val auditModel = PenaltyAppealFileNotificationStorageFailureModel(notifications)
    logger.info(s"[AppealsController][auditStorageFailureOfFileNotifications] - Auditing ${notifications.size} notifications that were not stored successfully")
    auditService.audit(auditModel)
  }
}
