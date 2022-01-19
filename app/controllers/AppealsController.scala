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

import config.AppConfig
import connectors.FileNotificationOrchestratorConnector
import connectors.parsers.ETMPPayloadParser.GetETMPPayloadNoContent
import models.ETMPPayload
import models.appeals.AppealTypeEnum._
import models.appeals.reasonableExcuses.ReasonableExcuse
import models.appeals._
import models.notification._
import models.payment.LatePaymentPenalty
import models.point.PenaltyPoint
import models.upload.UploadJourney
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.ETMPService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.{PenaltyPeriodHelper, UUIDGenerator}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AppealsController @Inject()(appConfig: AppConfig,
                                  etmpService: ETMPService,
                                  idGenerator: UUIDGenerator,
                                  fileNotificationOrchestratorConnector: FileNotificationOrchestratorConnector,
                                  cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  private def getAppealDataForPenalty(penaltyId: String,
                                      enrolmentKey: String, penaltyType: AppealTypeEnum.Value)(implicit hc: HeaderCarrier): Future[Result] = {
    etmpService.getPenaltyDataFromETMPForEnrolment(enrolmentKey).map {
      result => {
        result._1.fold {
          result._2 match {
            case Left(GetETMPPayloadNoContent) => NotFound(s"Could not retrieve ETMP penalty data for $enrolmentKey")
            case _ => InternalServerError("Something went wrong.")
          }
        }(
          etmpData => {
            checkAndReturnResponseForPenaltyData(etmpData, penaltyId, enrolmentKey, penaltyType)
          }
        )
      }
    }
  }

  def getIsMultiplePenaltiesInSamePeriod(penaltyId: String, enrolmentKey: String, isLPP: Boolean): Action[AnyContent] = Action.async {
    implicit request => {
      etmpService.isMultiplePenaltiesInSamePeriod(penaltyId, enrolmentKey, isLPP).map {
        if (_) {
          logger.info("[AppealsController][getIsMultiplePenaltiesInSamePeriod] - User has multiple penalties in same period - returning OK")
          Ok("")
        } else {
          logger.debug("[AppealsController][getIsMultiplePenaltiesInSamePeriod]" +
            " - User has NO multiple penalties in same period or something went wrong - returning NO_CONTENT")
          NoContent
        }
      }
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

  private def checkAndReturnResponseForPenaltyData(etmpData: ETMPPayload,
                                                   penaltyIdToCheck: String,
                                                   enrolmentKey: String,
                                                   appealType: AppealTypeEnum.Value): Result = {
    val lspPenaltyIdInETMPPayload: Option[PenaltyPoint] = etmpData.penaltyPoints.find(_.id == penaltyIdToCheck)
    val lppPenaltyIdInETMPPayload: Option[LatePaymentPenalty] = etmpData.latePaymentPenalties.flatMap(_.find(_.id == penaltyIdToCheck))
    if (appealType == AppealTypeEnum.Late_Submission && lspPenaltyIdInETMPPayload.isDefined) {
      logger.debug(s"[AppealsController][getAppealsData] Penalty ID: $penaltyIdToCheck for enrolment key: $enrolmentKey found in ETMP for $appealType.")
      val penaltyBasedOnId = lspPenaltyIdInETMPPayload.get
      val dataToReturn: AppealData = AppealData(`type` = appealType,
        startDate = penaltyBasedOnId.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.startDate,
        endDate = penaltyBasedOnId.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.endDate,
        dueDate = penaltyBasedOnId.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.submission.dueDate,
        dateCommunicationSent = penaltyBasedOnId.communications.head.dateSent
      )
      Ok(Json.toJson(dataToReturn))
    } else if ((appealType == AppealTypeEnum.Late_Payment || appealType == AppealTypeEnum.Additional) && lppPenaltyIdInETMPPayload.isDefined) {
      logger.debug(s"[AppealsController][getAppealsData] Penalty ID: $penaltyIdToCheck for enrolment key: $enrolmentKey found in ETMP for $appealType.")
      val penaltyBasedOnId = lppPenaltyIdInETMPPayload.get
      val dataToReturn: AppealData = AppealData(`type` = appealType,
        startDate = penaltyBasedOnId.period.startDate,
        endDate = penaltyBasedOnId.period.endDate,
        dueDate = penaltyBasedOnId.period.dueDate,
        dateCommunicationSent = penaltyBasedOnId.communications.head.dateSent
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

  def submitAppeal(enrolmentKey: String, isLPP: Boolean, penaltyId: String): Action[AnyContent] = Action.async {
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
              etmpService.submitAppeal(appealSubmission, enrolmentKey, isLPP, penaltyId).map {
                _.fold(
                  error => {
                    logger.error(s"[AppealsController][submitAppeal] Received status ${error.status}, with error message: ${error.body}")
                    logger.debug(s"[AppealsController][submitAppeal] Returning ${error.status} to calling service.")
                    Status(error.status)
                  },
                  responseModel => {
                    val appeal = appealSubmission.appealInformation
                    logger.debug(s"[AppealsController][submitAppeal] Received caseID response: ${responseModel.caseID} from downstream.")
                    val seqOfNotifications = appeal match {
                      case otherAppeal: OtherAppealInformation if otherAppeal.uploadedFiles.isDefined =>
                        createSDESNotifications(otherAppeal.uploadedFiles, responseModel.caseID)
                      case obligationAppeal: ObligationAppealInformation if obligationAppeal.uploadedFiles.isDefined =>
                        createSDESNotifications(obligationAppeal.uploadedFiles, responseModel.caseID)
                      case _ => Seq.empty
                    }
                    if (!seqOfNotifications.isEmpty) {
                      logger.debug(s"[AppealsController][submitAppeal] Posting SDESNotifications: $seqOfNotifications to Orchestrator")
                      fileNotificationOrchestratorConnector.postFileNotifications(seqOfNotifications).map {
                        response =>
                          response.status match {
                            case OK =>
                              logger.debug(s"[AppealsController][submitAppeal] - Received OK from file notification orchestrator")
                            case status =>
                              logger.error(s"[AppealsController][submitAppeal] - Received unknown response (${status}) from file notification orchestrator. Response body: ${response.body}")
                          }
                      }
                    }
                    Ok("")
                  }
                )
              }
            }
          )
        }
      )
    }
  }

  def createSDESNotifications(optUploadJourney: Option[Seq[UploadJourney]], caseID: String): Seq[SDESNotification] = {
    optUploadJourney match {
      case Some(uploads) => uploads.flatMap { upload =>
        upload.uploadDetails.flatMap { details =>
          upload.uploadFields.map(
            fields => {
              val uploadAlgorithm = fields("x-amz-algorithm")
              SDESNotification(
                informationType = appConfig.SDESNotificationInfoType,
                file = SDESNotificationFile(
                  recipientOrSender = appConfig.SDESNotificationFileRecipient,
                  name = details.fileName,
                  location = upload.downloadUrl.getOrElse(""),
                  checksum = SDESChecksum(algorithm = uploadAlgorithm, value = details.checksum),
                  size = details.size,
                  properties = Seq(SDESProperties(name = "caseID", value = caseID))
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
}
