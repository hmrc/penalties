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
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum}
import models.getPenaltyDetails.lateSubmission.LSPDetails
import models.notification._
import models.upload.UploadJourney
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.auditing.AuditService
import services.{AppealService, GetPenaltyDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{PagerDutyHelper, PenaltyPeriodHelper, RegimeHelper, UUIDGenerator}

import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AppealsController @Inject()(val appConfig: AppConfig,
                                  appealService: AppealService,
                                  getPenaltyDetailsService: GetPenaltyDetailsService,
                                  idGenerator: UUIDGenerator,
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
        dateCommunicationSent = penaltyBasedOnId.communicationsDate.get
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
        dateCommunicationSent = penaltyBasedOnId.communicationsDate.get
      )
      Ok(Json.toJson(dataToReturn))
    } else {
      logger.info("[AppealsController][getAppealsData] Data retrieved for enrolment but provided penalty ID was not found. Returning 404.")
      NotFound("Penalty ID was not found in users penalties.")
    }
  }

  def getReasonableExcuses: Action[AnyContent] = Action {
    Ok(ReasonableExcuse.allExcusesToJson(appConfig))
  }

  def submitAppeal(enrolmentKey: String, isLPP: Boolean, penaltyNumber: String, correlationId: String, isMultiAppeal: Boolean): Action[AnyContent] = Action.async {
    implicit request => {
      request.body.asJson.fold({
        logger.error("[AppealsController][submitAppeal] Failed to validate request body as JSON")
        Future(BadRequest("Invalid body received i.e. could not be parsed to JSON"))
      })(
        jsonBody => {
          val parseResultToModel = Json.fromJson(jsonBody)(AppealSubmission.apiReads)
          parseResultToModel.fold(
            failure => {
              logger.error("[AppealsController][submitAppeal] Fail to parse request body to model")
              logger.debug(s"[AppealsController][submitAppeal] Parse failure(s): $failure")
              Future(BadRequest("Failed to parse to model"))
            },
            appealSubmission => {
              submitAppealToPEGA(appealSubmission, enrolmentKey, isLPP, penaltyNumber, correlationId, isMultiAppeal)
            }
          )
        }
      )
    }
  }

  private def submitAppealToPEGA(appealSubmission: AppealSubmission, enrolmentKey: String,
                                 isLPP: Boolean, penaltyNumber: String, correlationId: String, isMultiAppeal: Boolean)
                                (implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    appealService.submitAppeal(appealSubmission, enrolmentKey, isLPP, penaltyNumber, correlationId).flatMap {
      _.fold(
        error => {
          logger.error(s"[AppealsController][submitAppeal] Received error from PEGA with status ${error.status} and error message: ${error.body}")
          logger.debug(s"[AppealsController][submitAppeal] Returning ${error.status} to calling service.")
          Future(Status(error.status)(error.body))
        },
        responseModel => {
          logger.info(s"[AppealsController][submitAppeal] - Successfully sent appeal submission to PEGA")
          val appeal = appealSubmission.appealInformation
          logger.debug(s"[AppealsController][submitAppeal] Received caseID response: ${responseModel.caseID} from downstream.")
          val seqOfNotifications = appeal match {
            case otherAppeal: OtherAppealInformation if otherAppeal.uploadedFiles.isDefined =>
              createSDESNotifications(otherAppeal.uploadedFiles, responseModel.caseID)
            case obligationAppeal: ObligationAppealInformation if obligationAppeal.uploadedFiles.isDefined =>
              createSDESNotifications(obligationAppeal.uploadedFiles, responseModel.caseID)
            case _ => Seq.empty
          }
          if (seqOfNotifications.nonEmpty) {
            val redactedNotification = seqOfNotifications.map(notification=> notification.copy(file = notification.file.copy(location = "HIDDEN")))
            logger.debug(s"[AppealsController][submitAppeal] Posting SDESNotifications: $redactedNotification to Orchestrator")
            fileNotificationOrchestratorConnector.postFileNotifications(seqOfNotifications).map {
              response =>
                response.status match {
                  case OK =>
                    logger.info(s"[AppealsController][submitAppeal] - Received OK from file notification orchestrator")
                    Ok(responseModel.caseID)
                  case status =>
                    PagerDutyHelper.logStatusCode("submitAppeal", status)(RECEIVED_4XX_FROM_FILE_NOTIFICATION_ORCHESTRATOR, RECEIVED_5XX_FROM_FILE_NOTIFICATION_ORCHESTRATOR)
                    logger.error(s"[AppealsController][submitAppeal] - Received unknown response ($status) from file notification orchestrator. Response body: ${response.body}")
                    auditStorageFailureOfFileNotifications(seqOfNotifications)
                    returnErrorResponseIfMultiAppeal(isMultiAppeal)(s"Appeal submitted (case ID: ${responseModel.caseID}) but received $status response from file notification orchestrator")(responseModel.caseID)
                }
            }.recover {
              case e => {
                logger.error(s"[AppealsController][submitAppeal] - An unknown exception occurred when attempting to store file notifications, with error: ${e.getMessage}")
                auditStorageFailureOfFileNotifications(seqOfNotifications)
                returnErrorResponseIfMultiAppeal(isMultiAppeal)(s"Appeal submitted (case ID: ${responseModel.caseID}) but failed to store file uploads with unknown error")(responseModel.caseID)
              }
            }
          } else {
            Future(Ok(responseModel.caseID))
          }
        }
      )
    }
  }

  private def returnErrorResponseIfMultiAppeal(isMultiAppeal: Boolean)(messageIfReturningError: String)(caseId: String): Result = {
    if (isMultiAppeal) {
      MultiStatus(messageIfReturningError)
    } else {
      Ok(caseId)
    }
  }

  def createSDESNotifications(optUploadJourney: Option[Seq[UploadJourney]], caseID: String): Seq[SDESNotification] = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    optUploadJourney match {
      case Some(uploads) => uploads.flatMap { upload =>
        upload.uploadDetails.flatMap { details =>
          upload.uploadFields.map(
            fields => {
              val uploadAlgorithm = fields("x-amz-algorithm") match {
                case "AWS4-HMAC-SHA256" => "SHA-256"
                case _ => throw new Exception("[AppealsController][createSDESNotifications] failed to recognise Checksum algorithm")
              }
              SDESNotification(
                informationType = appConfig.SDESNotificationInfoType,
                file = SDESNotificationFile(
                  recipientOrSender = appConfig.SDESNotificationFileRecipient,
                  name = details.fileName,
                  location = upload.downloadUrl.get,
                  checksum = SDESChecksum(algorithm = uploadAlgorithm, value = details.checksum),
                  size = details.size,
                  properties = Seq(
                    SDESProperties(name = "CaseId", value = caseID),
                    SDESProperties(name = "SourceFileUploadDate", value = details.uploadTimestamp.format(dateTimeFormatter))
                  )
                ),
                audit = SDESAudit(correlationID = idGenerator.generateUUID)
              )
            }
          )
        }
      }
      case None => Seq.empty
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
            val lppPenaltyIdInPenaltyDetailsPayload: Option[LPPDetails] = penaltyDetails.latePaymentPenalty.flatMap {
              _.details.flatMap(_.find(_.penaltyChargeReference.contains(penaltyId)))
            }
            val principalChargeReference: String = lppPenaltyIdInPenaltyDetailsPayload.get.principalChargeReference
            val penaltiesForPrincipalCharge: Seq[LPPDetails] = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.filter(_.principalChargeReference.equals(principalChargeReference)))).get
            val underAppeal = penaltiesForPrincipalCharge.exists(_.appealInformation.isDefined)

            if (penaltiesForPrincipalCharge.size == 2 && !underAppeal) {
              val secondPenalty = penaltiesForPrincipalCharge.find(_.penaltyCategory.equals(LPPPenaltyCategoryEnum.SecondPenalty)).get
              val firstPenalty = penaltiesForPrincipalCharge.find(_.penaltyCategory.equals(LPPPenaltyCategoryEnum.FirstPenalty)).get
              val returnModel = MultiplePenaltiesData(
                firstPenaltyChargeReference = firstPenalty.penaltyChargeReference.get,
                firstPenaltyAmount = firstPenalty.penaltyAmountOutstanding.getOrElse(BigDecimal(0)) + firstPenalty.penaltyAmountPaid.getOrElse(BigDecimal(0)),
                secondPenaltyChargeReference = secondPenalty.penaltyChargeReference.get,
                secondPenaltyAmount = secondPenalty.penaltyAmountOutstanding.getOrElse(BigDecimal(0)) + secondPenalty.penaltyAmountPaid.getOrElse(BigDecimal(0)),
                firstPenaltyCommunicationDate = firstPenalty.communicationsDate.get,
                secondPenaltyCommunicationDate = secondPenalty.communicationsDate.get
              )
              Ok(Json.toJson(returnModel))
            } else {
              NoContent
            }
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
        logger.error(s"[AppealsController][$callingMethod] - 1812 call returned an unexpected status: $status")
        InternalServerError(s"A downstream call returned an unexpected status: $status")
      }
      case GetPenaltyDetailsParser.GetPenaltyDetailsMalformed => {
        PagerDutyHelper.log(callingMethod, MALFORMED_RESPONSE_FROM_1812_API)
        logger.error(s"[AppealsController][$callingMethod] - Failed to parse penalty details response")
        InternalServerError("We were unable to parse penalty data.")
      }
      case GetPenaltyDetailsParser.GetPenaltyDetailsNoContent => {
        logger.info(s"s[AppealsController][$callingMethod] - 1812 call returned no content")
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
