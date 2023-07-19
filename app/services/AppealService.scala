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
import models.appeals.{AppealResponseModel, AppealSubmission, MultiplePenaltiesData}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum}
import models.notification._
import models.upload.UploadJourney
import play.api.Configuration
import utils.Logger.logger
import utils.{DateHelper, FileHelper, UUIDGenerator}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AppealService @Inject()(appealsConnector: PEGAConnector,
                              appConfig: AppConfig,
                              idGenerator: UUIDGenerator)(implicit ec: ExecutionContext, val config: Configuration) extends FeatureSwitching {

  private val regexToSanitiseFileName: String = "[\\\\\\/:*?<>|\"‘’“”]"

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
      case Some(uploads) =>
        val countOfUploadsWithUploadDetailsDefined = uploads.count(_.uploadDetails.isDefined)
        if (countOfUploadsWithUploadDetailsDefined != uploads.size) {
          logger.warn(s"[AppealService][createSDESNotifications] - There are ${uploads.size} uploads but" +
            s" only $countOfUploadsWithUploadDetailsDefined uploads have upload details defined (possible missing files for case ID: $caseID)")
        }
        uploads.flatMap { upload =>
          upload.uploadDetails.map { details =>
            val uploadAlgorithm = appConfig.checksumAlgorithmForFileNotifications
            SDESNotification(
              informationType = appConfig.SDESNotificationInfoType,
              file = SDESNotificationFile(
                recipientOrSender = appConfig.SDESNotificationFileRecipient,
                name = sanitisedAndTruncatedFileName(details.fileName)(details.fileMimeType)(upload.reference),
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
        }
      case None => Seq.empty
    }
  }

  def findMultiplePenalties(penaltyDetails: GetPenaltyDetails, penaltyId: String): Option[MultiplePenaltiesData] = {
    val lppPenaltyIdInPenaltyDetailsPayload: Option[LPPDetails] = penaltyDetails.latePaymentPenalty.flatMap {
      _.details.flatMap(_.find(_.penaltyChargeReference.contains(penaltyId)))
    }
    val principalChargeReference: String = lppPenaltyIdInPenaltyDetailsPayload.get.principalChargeReference
    val penaltiesForPrincipalCharge: Seq[LPPDetails] = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.filter(_.principalChargeReference.equals(principalChargeReference)))).get
    val underAppeal = penaltiesForPrincipalCharge.exists(_.appealInformation.isDefined)
    val areBothPenaltiesPostedAndVATPaid: Boolean = penaltiesForPrincipalCharge.forall(penalty => {
      penalty.penaltyStatus == LPPPenaltyStatusEnum.Posted && penalty.principalChargeLatestClearing.isDefined
    })

    if (penaltiesForPrincipalCharge.size == 2 && !underAppeal && areBothPenaltiesPostedAndVATPaid) {
      val secondPenalty = penaltiesForPrincipalCharge.find(_.penaltyCategory.equals(LPPPenaltyCategoryEnum.SecondPenalty)).get
      val firstPenalty = penaltiesForPrincipalCharge.find(_.penaltyCategory.equals(LPPPenaltyCategoryEnum.FirstPenalty)).get
      val returnModel = MultiplePenaltiesData(
        firstPenaltyChargeReference = firstPenalty.penaltyChargeReference.get,
        firstPenaltyAmount = firstPenalty.penaltyAmountOutstanding.getOrElse(BigDecimal(0)) + firstPenalty.penaltyAmountPaid.getOrElse(BigDecimal(0)),
        secondPenaltyChargeReference = secondPenalty.penaltyChargeReference.get,
        secondPenaltyAmount = secondPenalty.penaltyAmountOutstanding.getOrElse(BigDecimal(0)) + secondPenalty.penaltyAmountPaid.getOrElse(BigDecimal(0)),
        firstPenaltyCommunicationDate = firstPenalty.communicationsDate.getOrElse(appConfig.getTimeMachineDateTime.toLocalDate),
        secondPenaltyCommunicationDate = secondPenalty.communicationsDate.getOrElse(appConfig.getTimeMachineDateTime.toLocalDate)
      )
      Some(returnModel)
    } else {
      None
    }
  }

  private def sanitiseFileName(fileName: String)(fileMimeType: String)(fileReference: String): String = {
    if (appConfig.isEnabled(SanitiseFileName)) {
      val fileNameWithoutSpecialCharacters = fileName.replaceAll(regexToSanitiseFileName, "_")
      FileHelper.appendFileExtension(fileNameWithoutSpecialCharacters)(fileMimeType)(fileReference)(appConfig)
    } else {
      fileName
    }
  }

  private def sanitisedAndTruncatedFileName(fileName: String)(fileMimeType: String)(reference: String): String = {
    val sanitisedFileName = sanitiseFileName(fileName)(fileMimeType)(reference)
    if (sanitisedFileName.length > appConfig.maximumFilenameLength) {
      if (sanitisedFileName.contains(".")) {
        val fileRegex = "^(.*)(\\.\\w{1,4})$".r
        val fileRegex(fileNameMain, fileExtension) = sanitisedFileName
        logger.info(s"[AppealService][sanitisedAndTruncatedFileName] File name length: ${fileNameMain.length} with reference of: $reference, truncating to ${appConfig.maximumFilenameLength}")
        fileNameMain.substring(0, Math.min(fileNameMain.length(), appConfig.maximumFilenameLength)) ++ fileExtension
      } else {
        logger.info(s"[AppealService][sanitisedAndTruncatedFileName] File name length: ${sanitisedFileName.length} with reference of: $reference, truncating to ${appConfig.maximumFilenameLength}")
        sanitisedFileName.substring(0, Math.min(sanitisedFileName.length(), appConfig.maximumFilenameLength))
      }
    } else {
      sanitisedFileName
    }

  }
}
