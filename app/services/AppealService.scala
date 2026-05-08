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

package services

import config.AppConfig
import config.featureSwitches.{CallAPI1808HIP, FeatureSwitching, SanitiseFileName}
import connectors.parsers.submitAppeal.AppealsParser
import connectors.submitAppeal.{HIPSubmitAppealConnector, SubmitAppealConnector}
import models.AgnosticEnrolmentKey
import models.appeals.{AppealResponseModel, AppealSubmission, MultiplePenaltiesData}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum}
import models.notification._
import models.upload.{UploadDetails, UploadJourney}
import play.api.Configuration
import services.AppealService._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger
import utils.{DateHelper, FileHelper, UUIDGenerator}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AppealService @Inject()(appealsConnector: SubmitAppealConnector,
                              hipAppealsConnector: HIPSubmitAppealConnector,
                              appConfig: AppConfig,
                              idGenerator: UUIDGenerator)(implicit ec: ExecutionContext, val config: Configuration) extends FeatureSwitching {

  private val regexToSanitiseFileName: String = "[\\\\\\/:*?<>|\"‘’“”]"

  def submitAppeal(appealSubmission: AppealSubmission,
                   enrolmentKey: AgnosticEnrolmentKey,
                   penaltyNumber: String,
                   correlationId: String)
                  (implicit headerCarrier: HeaderCarrier): Future[Either[AppealsParser.ErrorResponse, AppealResponseModel]] = {
    val response: Future[AppealsParser.AppealSubmissionResponse] = if (isEnabled(CallAPI1808HIP)) {
      hipAppealsConnector.submitAppeal(appealSubmission, penaltyNumber, correlationId)
    } else {
      appealsConnector.submitAppeal(appealSubmission, penaltyNumber, correlationId)
    }
    response.map {
      _.fold(
        error => Left(error),
        responseModel => {
          logger.info(s"[RegimeAppealService][submitAppeal] - Retrieving response model for penalty: $penaltyNumber")
          Right(responseModel)
        }
      )
    }
  }

  def createSDESNotifications(optUploadJourney: Option[Seq[UploadJourney]],
                              caseID: String): Either[SDESNotificationCreationError, Seq[SDESNotification]] = {
    optUploadJourney match {
      case Some(uploads) =>
        val countOfUploadsWithUploadDetailsDefined = uploads.count(_.uploadDetails.isDefined)
        if (countOfUploadsWithUploadDetailsDefined != uploads.size) {
          logger.warn(s"[RegimeAppealService][createSDESNotifications] - There are ${uploads.size} uploads but" +
            s" only $countOfUploadsWithUploadDetailsDefined uploads have upload details defined (possible missing files for case ID: $caseID)")
        }
        sequence(uploads.flatMap { upload =>
          upload.uploadDetails.map(details => createSDESNotification(upload, details, caseID))
        })
      case None => Right(Seq.empty)
    }
  }

  def findMultiplePenalties(penaltyDetails: GetPenaltyDetails,
                            penaltyId: String,
                            regime: String): Either[MultiplePenaltiesError, Option[MultiplePenaltiesData]] = {
    val allLPPDetails: Seq[LPPDetails] = penaltyDetails.latePaymentPenalty.flatMap(_.details).getOrElse(Seq.empty)

    allLPPDetails.find(_.penaltyChargeReference.contains(penaltyId)).map { matchedPenalty =>
      val principalChargeReference = matchedPenalty.principalChargeReference
      val penaltiesForPrincipalCharge: Seq[LPPDetails] =
        allLPPDetails.filter(_.principalChargeReference.equals(principalChargeReference))

      val allLppsAreAppealable =
        if (regime == "ITSA") penaltiesForPrincipalCharge.forall(_.hasNoAppealsOrOnlyFirstStageRejectedAppeals)
        else penaltiesForPrincipalCharge.forall(_.hasNoAppeals)
      val areBothPenaltiesPostedAndVATPaid: Boolean = penaltiesForPrincipalCharge.forall { penalty =>
        penalty.penaltyStatus == LPPPenaltyStatusEnum.Posted && penalty.principalChargeLatestClearing.isDefined
      }

      val isReturnable = penaltiesForPrincipalCharge.size == 2 && allLppsAreAppealable && areBothPenaltiesPostedAndVATPaid

      if (!isReturnable) Right(None)
      else buildMultiplePenaltiesData(penaltiesForPrincipalCharge, principalChargeReference).map(Some(_))
    }.getOrElse(Right(None))
  }

  private def createSDESNotification(upload: UploadJourney,
                                     details: UploadDetails,
                                     caseID: String): Either[SDESNotificationCreationError, SDESNotification] =
    for {
      downloadUrl <- upload.downloadUrl.toRight(MissingDownloadUrl(upload.reference, caseID))
      fileName    <- sanitisedAndTruncatedFileName(details.fileName)(details.fileMimeType)(upload.reference)
    } yield SDESNotification(
      informationType = appConfig.SDESNotificationInfoType,
      file = SDESNotificationFile(
        recipientOrSender = appConfig.SDESNotificationFileRecipient,
        name = fileName,
        location = downloadUrl,
        checksum = SDESChecksum(algorithm = appConfig.checksumAlgorithmForFileNotifications, value = details.checksum),
        size = details.size,
        properties = Seq(
          SDESProperties(name = "CaseId", value = caseID),
          SDESProperties(name = "SourceFileUploadDate", value = details.uploadTimestamp.format(DateHelper.dateTimeFormatter))
        )
      ),
      audit = SDESAudit(correlationID = idGenerator.generateUUID)
    )

  private def buildMultiplePenaltiesData(penalties: Seq[LPPDetails],
                                         principalChargeReference: String): Either[MultiplePenaltiesError, MultiplePenaltiesData] =
    for {
      firstPenalty           <- penaltyByCategory(penalties, LPPPenaltyCategoryEnum.FirstPenalty, principalChargeReference)
      secondPenalty          <- penaltyByCategory(penalties, LPPPenaltyCategoryEnum.SecondPenalty, principalChargeReference)
      firstPenaltyChargeRef  <- chargeReference(firstPenalty)
      secondPenaltyChargeRef <- chargeReference(secondPenalty)
    } yield MultiplePenaltiesData(
      firstPenaltyChargeReference = firstPenaltyChargeRef,
      firstPenaltyAmount = firstPenalty.penaltyAmountOutstanding.getOrElse(BigDecimal(0)) + firstPenalty.penaltyAmountPaid.getOrElse(BigDecimal(0)),
      secondPenaltyChargeReference = secondPenaltyChargeRef,
      secondPenaltyAmount = secondPenalty.penaltyAmountOutstanding.getOrElse(BigDecimal(0)) + secondPenalty.penaltyAmountPaid.getOrElse(BigDecimal(0)),
      firstPenaltyCommunicationDate = firstPenalty.communicationsDate.getOrElse(appConfig.getTimeMachineDateTime.toLocalDate),
      secondPenaltyCommunicationDate = secondPenalty.communicationsDate.getOrElse(appConfig.getTimeMachineDateTime.toLocalDate)
    )

  private def penaltyByCategory(penalties: Seq[LPPDetails],
                                category: LPPPenaltyCategoryEnum.Value,
                                principalChargeReference: String): Either[MultiplePenaltiesError, LPPDetails] =
    penalties.find(_.penaltyCategory == category).toRight(MissingPenaltyCategory(category, principalChargeReference))

  private def chargeReference(penalty: LPPDetails): Either[MultiplePenaltiesError, String] =
    penalty.penaltyChargeReference.toRight(MissingPenaltyChargeReference(penalty.penaltyCategory, penalty.principalChargeReference))

  private def sequence[E, A](values: Seq[Either[E, A]]): Either[E, Seq[A]] =
    values.foldRight(Right(Seq.empty[A]): Either[E, Seq[A]]) { (value, acc) =>
      for {
        item  <- value
        items <- acc
      } yield item +: items
    }

  private def sanitiseFileName(fileName: String)(fileMimeType: String)(fileReference: String): String = {
    if (appConfig.isEnabled(SanitiseFileName)) {
      val fileNameWithoutSpecialCharacters = fileName.replaceAll(regexToSanitiseFileName, "_")
      FileHelper.appendFileExtension(fileNameWithoutSpecialCharacters)(fileMimeType)(fileReference)(appConfig)
    } else {
      fileName
    }
  }

  private def sanitisedAndTruncatedFileName(fileName: String)(fileMimeType: String)(reference: String): Either[InvalidFileName, String] = {
    val sanitisedFileName = sanitiseFileName(fileName)(fileMimeType)(reference)
    if (sanitisedFileName.length <= appConfig.maximumFilenameLength) {
      Right(sanitisedFileName)
    } else if (!sanitisedFileName.contains(".")) {
      logger.info(s"[RegimeAppealService][sanitisedAndTruncatedFileName] File name length: ${sanitisedFileName.length} with reference of: $reference, truncating to ${appConfig.maximumFilenameLength}")
      Right(sanitisedFileName.substring(0, Math.min(sanitisedFileName.length(), appConfig.maximumFilenameLength)))
    } else {
      val fileRegex = "^(.*)(\\.\\w{1,4})$".r
      sanitisedFileName match {
        case fileRegex(fileNameMain, fileExtension) =>
          logger.info(s"[RegimeAppealService][sanitisedAndTruncatedFileName] File name length: ${fileNameMain.length} with reference of: $reference, truncating to ${appConfig.maximumFilenameLength}")
          Right(fileNameMain.substring(0, Math.min(fileNameMain.length(), appConfig.maximumFilenameLength)) ++ fileExtension)
        case _ =>
          Left(InvalidFileName(reference, s"Bad filename: $sanitisedFileName"))
      }
    }
  }
}

object AppealService {

  sealed trait SDESNotificationCreationError {
    def message: String
  }

  final case class MissingDownloadUrl(reference: String, caseID: String) extends SDESNotificationCreationError {
    override val message: String =
      s"Upload with reference $reference has no downloadUrl (case ID: $caseID)"
  }

  final case class InvalidFileName(reference: String, reason: String) extends SDESNotificationCreationError {
    override val message: String =
      s"Unable to build file notification name for upload with reference $reference: $reason"
  }

  sealed trait MultiplePenaltiesError {
    def message: String
  }

  final case class MissingPenaltyCategory(category: LPPPenaltyCategoryEnum.Value,
                                          principalChargeReference: String) extends MultiplePenaltiesError {
    override val message: String =
      s"Missing $category penalty for principal charge reference $principalChargeReference"
  }

  final case class MissingPenaltyChargeReference(category: LPPPenaltyCategoryEnum.Value,
                                                 principalChargeReference: String) extends MultiplePenaltiesError {
    override val message: String =
      s"Missing penaltyChargeReference for $category penalty under principal charge reference $principalChargeReference"
  }
}
