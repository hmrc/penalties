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

package services

import config.AppConfig
import config.featureSwitches.{FeatureSwitching, SanitiseFileName}
import connectors.PEGAConnector
import connectors.parsers.AppealsParser
import models.appeals.{AppealResponseModel, AppealSubmission}
import models.notification.{SDESAudit, SDESChecksum, SDESNotification, SDESNotificationFile, SDESProperties}
import models.upload.UploadJourney
import play.api.Configuration
import utils.{DateHelper, UUIDGenerator}
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AppealService @Inject()(appealsConnector: PEGAConnector,
                              appConfig: AppConfig,
                              idGenerator: UUIDGenerator)(implicit ec: ExecutionContext, val config: Configuration) extends FeatureSwitching {

  private val regexToSanitiseFileName: String = "[^a-zA-Z0-9_\\-.]"

  private val regexToRemoveSpacesAndControlCharacters: String = "\\s+"

  def submitAppeal(appealSubmission: AppealSubmission,
                   enrolmentKey: String, isLPP: Boolean, penaltyNumber: String, correlationId: String): Future[Either[AppealsParser.ErrorResponse, AppealResponseModel]] = {
    appealsConnector.submitAppeal(appealSubmission, enrolmentKey, isLPP, penaltyNumber, correlationId).flatMap {
      _.fold(
        error => {
          logger.error(s"[AppealService][submitAppeal] - Submit appeal call failed with error: ${error.body} and status: ${error.status} for enrolment: $enrolmentKey")
          Future(Left(error))
        },
        responseModel => {
          logger.debug(s"[AppealService][submitAppeal] - Retrieving response model for penalty: $penaltyNumber")
          Future(Right(responseModel))
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
              val uploadAlgorithm = fields("x-amz-algorithm") match {
                case "AWS4-HMAC-SHA256" => "SHA-256"
                case _ => throw new Exception("[AppealsController][createSDESNotifications] failed to recognise Checksum algorithm")
              }
              val sanitisedFileName: String = sanitiseFileName(details.fileName)
              SDESNotification(
                informationType = appConfig.SDESNotificationInfoType,
                file = SDESNotificationFile(
                  recipientOrSender = appConfig.SDESNotificationFileRecipient,
                  name = sanitisedFileName,
                  location = upload.downloadUrl.get,
                  checksum = SDESChecksum(algorithm = uploadAlgorithm, value = details.checksum),
                  size = details.size,
                  properties = Seq(
                    SDESProperties(name = "CaseId", value = caseID),
                    SDESProperties(name = "SourceFileUploadDate", value = details.uploadTimestamp.format(DateHelper.dateTimeFormatter))
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

  private def sanitiseFileName(fileName: String): String = {
    if(appConfig.isEnabled(SanitiseFileName)) {
      fileName.replaceAll(regexToRemoveSpacesAndControlCharacters, "_").replaceAll(regexToSanitiseFileName, "_")
    } else {
      fileName
    }
  }
}
