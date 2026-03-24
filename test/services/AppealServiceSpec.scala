/*
 * Copyright 2025 HM Revenue & Customs
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

import base.{LogCapturing, SpecBase}
import config.AppConfig
import config.featureSwitches.{CallAPI1808HIP, FeatureSwitching, SanitiseFileName}
import connectors.parsers.submitAppeal.AppealsParser
import connectors.parsers.submitAppeal.AppealsParser.UnexpectedFailure
import connectors.submitAppeal.{HIPSubmitAppealConnector, SubmitAppealConnector}
import models.appeals.AppealLevel.FirstStageAppeal
import models.appeals.{AppealResponseModel, AppealSubmission, CrimeAppealInformation, MultiplePenaltiesData}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum}
import models.getPenaltyDetails.latePayment.PrincipalChargeMainTr.VATReturnCharge
import models.getPenaltyDetails.latePayment._
import models.hipPenaltyDetails.appealInfo.AppealStatusEnum
import models.notification._
import models.upload.{UploadDetails, UploadJourney, UploadStatusEnum}
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration
import play.api.test.Helpers._
import services.AppealServiceSpec._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger
import utils.UUIDGenerator

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class AppealServiceSpec extends SpecBase with LogCapturing with FeatureSwitching {
  implicit val ec: ExecutionContext               = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier                  = HeaderCarrier(otherHeaders = Seq("CorrelationId" -> "id"))
  val mockAppealsConnector: SubmitAppealConnector = mock(classOf[SubmitAppealConnector])
  val mockHIPConnector: HIPSubmitAppealConnector  = mock(classOf[HIPSubmitAppealConnector])
  val correlationId: String                       = "correlationId"
  val mockAppConfig: AppConfig                    = mock(classOf[AppConfig])
  val mockUUIDGenerator: UUIDGenerator            = mock(classOf[UUIDGenerator])
  implicit val config: Configuration              = mockAppConfig.config

  class Setup(enableHIP: Boolean = false) {
    if (enableHIP) {
      enableFeatureSwitch(CallAPI1808HIP)
    } else {
      disableFeatureSwitch(CallAPI1808HIP)
    }
    val service = new AppealService(
      mockAppealsConnector,
      mockHIPConnector,
      mockAppConfig,
      mockUUIDGenerator
    )
    reset(mockAppealsConnector)
    reset(mockHIPConnector)
    reset(mockAppConfig)
    reset(mockUUIDGenerator)
    when(mockAppConfig.isEnabled(ArgumentMatchers.eq(SanitiseFileName))).thenReturn(false)
    when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
    when(mockAppConfig.SDESNotificationInfoType).thenReturn("S18")
    when(mockAppConfig.SDESNotificationFileRecipient).thenReturn("123456789012")
    when(mockAppConfig.maximumFilenameLength).thenReturn(150)
  }

  "submitAppeal" when {
    val enrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("HMRC-MTD-VAT"), IdType("VRN"), Id("123456789"))
    val modelToPassToServer: AppealSubmission = AppealSubmission(
      taxRegime = "VAT",
      appealLevel = FirstStageAppeal,
      customerReferenceNo = "123456789",
      dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
      isLPP = false,
      appealSubmittedBy = "client",
      agentDetails = None,
      appealInformation = CrimeAppealInformation(
        startDateOfEvent = "2021-04-23T00:00",
        reportedIssueToPolice = "yes",
        reasonableExcuse = "crime",
        honestyDeclaration = true,
        statement = None,
        lateAppeal = false,
        lateAppealReason = None,
        isClientResponsibleForSubmission = None,
        isClientResponsibleForLateSubmission = None
      )
    )

    "calling PEGA" should {

      "return the response from the connector i.e. act as a pass-through function" in new Setup {
        when(mockAppealsConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(appealResponseModel)))

        val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] =
          await(service.submitAppeal(modelToPassToServer, enrolmentKey, penaltyNumber = "123456789", correlationId = correlationId))
        result shouldBe Right(appealResponseModel)
      }

      "return the response from the connector on error i.e. act as a pass-through function" in new Setup {
        when(mockAppealsConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))))

        val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] =
          await(service.submitAppeal(modelToPassToServer, enrolmentKey, penaltyNumber = "123456789", correlationId = correlationId))
        result shouldBe Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))
      }

      "throw an exception when the connector throws an exception" in new Setup {
        when(mockAppealsConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Exception("Something went wrong")))

        val result: Exception = intercept[Exception](
          await(service.submitAppeal(modelToPassToServer, enrolmentKey, penaltyNumber = "123456789", correlationId = correlationId)))
        result.getMessage shouldBe "Something went wrong"
      }
    }

    "calling HIP" should {
      "return the response from the connector i.e. act as a pass-through function" in new Setup(enableHIP = true) {
        when(mockHIPConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(appealResponseModel)))

        val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] =
          await(service.submitAppeal(modelToPassToServer, enrolmentKey, penaltyNumber = "123456789", correlationId = correlationId))
        result shouldBe Right(appealResponseModel)
      }

      "return the response from the connector on error i.e. act as a pass-through function" in new Setup(enableHIP = true) {
        when(mockHIPConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))))

        val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] =
          await(service.submitAppeal(modelToPassToServer, enrolmentKey, penaltyNumber = "123456789", correlationId = correlationId))
        result shouldBe Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))
      }

      "throw an exception when the connector throws an exception" in new Setup(enableHIP = true) {
        when(mockHIPConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Exception("Something went wrong")))

        val result: Exception = intercept[Exception](
          await(service.submitAppeal(modelToPassToServer, enrolmentKey, penaltyNumber = "123456789", correlationId = correlationId)))
        result.getMessage shouldBe "Something went wrong"
      }
    }

  }

  "createSDESNotifications" should {
    "return an empty Seq" when {
      "None is passed to the uploadJourney" in new Setup {
        val result = service.createSDESNotifications(None, "")
        result shouldBe Seq.empty
      }
    }

    "return a Seq of SDES notifications" when {
      "Some uploadJourneys are passed in" in new Setup {
        when(mockAppConfig.checksumAlgorithmForFileNotifications).thenReturn("SHA-256")
        val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
        val uploads = Seq(
          UploadJourney(
            reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(
              UploadDetails(
                fileName = "file1",
                fileMimeType = "text/plain",
                uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
                checksum = "check123456789",
                size = 1
              )),
            lastUpdated = mockDateTime,
            uploadFields = Some(
              Map(
                "key"             -> "abcxyz",
                "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
              ))
          )
        )

        val expectedResult = Seq(
          SDESNotification(
            informationType = "S18",
            file = SDESNotificationFile(
              recipientOrSender = "123456789012",
              name = "file1",
              location = "/",
              checksum = SDESChecksum(algorithm = "SHA-256", value = "check123456789"),
              size = 1,
              properties = Seq(
                SDESProperties(name = "CaseId", value = "PR-1234"),
                SDESProperties(name = "SourceFileUploadDate", value = "2018-04-24T09:30:00Z")
              )
            ),
            audit = SDESAudit(
              correlationID = correlationId
            )
          )
        )

        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result = service.createSDESNotifications(Some(uploads), caseID = "PR-1234")
        result shouldBe expectedResult
      }

      "uploads are passed through but some uploads don't have an 'uploadDetails' field" in new Setup {
        when(mockAppConfig.checksumAlgorithmForFileNotifications).thenReturn("SHA-256")
        val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
        val uploads = Seq(
          UploadJourney(
            reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(
              UploadDetails(
                fileName = "file1",
                fileMimeType = "text/plain",
                uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
                checksum = "check123456789",
                size = 1
              )),
            lastUpdated = mockDateTime,
            uploadFields = Some(
              Map(
                "key"             -> "abcxyz",
                "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
              ))
          ),
          UploadJourney(
            reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = None,
            lastUpdated = mockDateTime,
            uploadFields = Some(
              Map(
                "key"             -> "abcxyz",
                "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
              ))
          ),
          UploadJourney(
            reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(
              UploadDetails(
                fileName = "file3",
                fileMimeType = "text/plain",
                uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
                checksum = "check123456789",
                size = 1
              )),
            lastUpdated = mockDateTime,
            uploadFields = Some(
              Map(
                "key"             -> "abcxyz",
                "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
              ))
          )
        )

        val expectedResult = Seq(
          SDESNotification(
            informationType = "S18",
            file = SDESNotificationFile(
              recipientOrSender = "123456789012",
              name = "file1",
              location = "/",
              checksum = SDESChecksum(algorithm = "SHA-256", value = "check123456789"),
              size = 1,
              properties = Seq(
                SDESProperties(name = "CaseId", value = "PR-1234"),
                SDESProperties(name = "SourceFileUploadDate", value = "2018-04-24T09:30:00Z")
              )
            ),
            audit = SDESAudit(
              correlationID = correlationId
            )
          ),
          SDESNotification(
            informationType = "S18",
            file = SDESNotificationFile(
              recipientOrSender = "123456789012",
              name = "file3",
              location = "/",
              checksum = SDESChecksum(algorithm = "SHA-256", value = "check123456789"),
              size = 1,
              properties = Seq(
                SDESProperties(name = "CaseId", value = "PR-1234"),
                SDESProperties(name = "SourceFileUploadDate", value = "2018-04-24T09:30:00Z")
              )
            ),
            audit = SDESAudit(
              correlationID = correlationId
            )
          )
        )
        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        withCaptureOfLoggingFrom(logger) { logs =>
          val result = service.createSDESNotifications(Some(uploads), caseID = "PR-1234")
          result shouldBe expectedResult
          logs.exists(_.getMessage == "[RegimeAppealService][createSDESNotifications] - There are 3 uploads but" +
            " only 2 uploads have upload details defined (possible missing files for case ID: PR-1234)") shouldBe true
        }

      }

      "notifications are sent and then should sanitise the file names (when feature switch is enabled)" in new Setup {
        when(mockAppConfig.isEnabled(ArgumentMatchers.eq(SanitiseFileName))).thenReturn(true)
        when(mockAppConfig.getMimeType(ArgumentMatchers.eq("text.plain"))).thenReturn(Some(".txt"))
        when(mockAppConfig.checksumAlgorithmForFileNotifications).thenReturn("SHA-256")
        val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
        val uploads = Seq(
          UploadJourney(
            reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(
              UploadDetails(
                fileName = "file 1 / * 3  something‘ ’ “ ” <4 x>y_z |\" \\",
                fileMimeType = "text/plain",
                uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
                checksum = "check123456789",
                size = 1
              )),
            lastUpdated = mockDateTime,
            uploadFields = Some(
              Map(
                "key"             -> "abcxyz",
                "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
              ))
          )
        )

        val expectedResult = Seq(
          SDESNotification(
            informationType = "S18",
            file = SDESNotificationFile(
              recipientOrSender = "123456789012",
              name = "file 1 _ _ 3  something_ _ _ _ _4 x_y_z __ _.txt",
              location = "/",
              checksum = SDESChecksum(algorithm = "SHA-256", value = "check123456789"),
              size = 1,
              properties = Seq(
                SDESProperties(name = "CaseId", value = "PR-1234"),
                SDESProperties(name = "SourceFileUploadDate", value = "2018-04-24T09:30:00Z")
              )
            ),
            audit = SDESAudit(
              correlationID = correlationId
            )
          )
        )

        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result = service.createSDESNotifications(Some(uploads), caseID = "PR-1234")
        result shouldBe expectedResult
      }
    }

    "truncate file name" when {
      val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
      val longFilename                = Random.alphanumeric.take(160).mkString
      "filename is above maximumFilenameLength and includes file extension" in new Setup {
        val uploads = Seq(
          UploadJourney(
            reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(
              UploadDetails(
                fileName = longFilename + ".txt",
                fileMimeType = "text/plain",
                uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
                checksum = "check123456789",
                size = 1
              )),
            lastUpdated = mockDateTime,
            uploadFields = Some(
              Map(
                "key"             -> "abcxyz",
                "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
              ))
          )
        )
        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result: Seq[SDESNotification] = service.createSDESNotifications(Some(uploads), caseID = "PR-5678")
        val resultFileName: String        = result.head.file.name
        resultFileName.length shouldBe mockAppConfig.maximumFilenameLength + 4
        resultFileName.contains(".txt") shouldBe true
      }

      "reduce a filename to maximum character length based on maximumFilenameLength when there is no file extension" in new Setup {
        val uploads = Seq(
          UploadJourney(
            reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(
              UploadDetails(
                fileName = longFilename,
                fileMimeType = "text/plain",
                uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
                checksum = "check123456789",
                size = 1
              )),
            lastUpdated = mockDateTime,
            uploadFields = Some(
              Map(
                "key"             -> "abcxyz",
                "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
              ))
          )
        )
        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result: Seq[SDESNotification] = service.createSDESNotifications(Some(uploads), caseID = "PR-5678")
        val resultFileName: String        = result.head.file.name
        resultFileName.length shouldBe mockAppConfig.maximumFilenameLength
      }

      "correctly remove file extension when filename includes periods" in new Setup {
        val longFilename: String = Random.alphanumeric.take(60).mkString
        val uploads = Seq(
          UploadJourney(
            reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(
              UploadDetails(
                fileName = longFilename + "." + longFilename + ".." + longFilename + ".txt",
                fileMimeType = "text/plain",
                uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
                checksum = "check123456789",
                size = 1
              )),
            lastUpdated = mockDateTime,
            uploadFields = Some(
              Map(
                "key"             -> "abcxyz",
                "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
              ))
          )
        )
        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result: Seq[SDESNotification] = service.createSDESNotifications(Some(uploads), caseID = "PR-5678")
        val resultFileName: String        = result.head.file.name
        resultFileName.length shouldBe mockAppConfig.maximumFilenameLength + 4
        resultFileName.contains(".txt") shouldBe true
        resultFileName.count(_ == '.') shouldBe 4
      }
    }
  }

  "getMultiplePenaltyData" should {

    "return None" when {
      "there is only one penalty under this principal charge" in new Setup {
        val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(getPenaltyDetailsOnePenalty, "1234567891", "VATC")
        result shouldBe None
      }

      "either penalty under the principal charge has appeal in any state" in new Setup {
        val result: Option[MultiplePenaltiesData] =
          service.findMultiplePenalties(getPenaltyDetailsTwoPenaltiesWithAppeal, "1234567891", "VATC")
        result shouldBe None
      }

      "either penalty is accruing" in new Setup {
        val result: Option[MultiplePenaltiesData] =
          service.findMultiplePenalties(getPenaltyDetailsTwoPenaltiesLPP2Accruing, "1234567891", "VATC")
        result shouldBe None
      }

      "the VAT has not been paid" in new Setup {
        val result: Option[MultiplePenaltiesData] =
          service.findMultiplePenalties(getPenaltyDetailsTwoPenaltiesVATNotPaid, "1234567891", "VATC")
        result shouldBe None
      }
    }

    "return Some" when {
      "there is two penalties under this principal charge and they are both POSTED and VAT has been paid" in new Setup {
        val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(getPenaltyDetailsTwoPenalties, "1234567892", "VATC")
        val expectedReturnModel: MultiplePenaltiesData = MultiplePenaltiesData(
          firstPenaltyChargeReference = "1234567891",
          firstPenaltyAmount = 113.45,
          secondPenaltyChargeReference = "1234567892",
          secondPenaltyAmount = 113.44,
          firstPenaltyCommunicationDate = LocalDate.of(2022, 8, 8),
          secondPenaltyCommunicationDate = LocalDate.of(2022, 9, 8)
        )
        result shouldBe Some(expectedReturnModel)
      }

      "there is two penalties under this principal charge and they are both POSTED and VAT has been paid" +
        " (defaulting the comms date if not present)" in new Setup {
          when(mockAppConfig.getTimeMachineDateTime).thenReturn(LocalDateTime.now)
          val result: Option[MultiplePenaltiesData] =
            service.findMultiplePenalties(getPenaltyDetailsTwoPenaltiesNoCommunicationsDate, "1234567891", "VATC")
          val expectedReturnModel: MultiplePenaltiesData = MultiplePenaltiesData(
            firstPenaltyChargeReference = "1234567891",
            firstPenaltyAmount = 113.45,
            secondPenaltyChargeReference = "1234567892",
            secondPenaltyAmount = 113.44,
            firstPenaltyCommunicationDate = LocalDate.now,
            secondPenaltyCommunicationDate = LocalDate.now
          )
          result shouldBe Some(expectedReturnModel)
        }
    }

    "when regime is 'ITSA'" should {
      "allow and return the multiple penalties data" when {
        "the LPPs have not been appealed or are first stage appeals that have been rejected" in new Setup {
          private val firstAppealableLpp            = makeFirstLPP(appealInfo = Some(Seq(firstStageRejectedAppeal)))
          private val secondAppealableLpp           = makeSecondLPP(appealInfo = None)
          private val penaltyDetails                = makePenaltyDetailsWithLpp(Some(Seq(firstAppealableLpp, secondAppealableLpp)))
          val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(penaltyDetails, "1234567891", "ITSA")

          result shouldBe Some(returnMultiplePenaltiesModel)
        }
      }
      "return no data" when {
        "the penalty is appealable but there is only one LPP" in new Setup {
          private val firstAppealableLpp            = makeFirstLPP(appealInfo = Some(Seq(firstStageRejectedAppeal)))
          private val penaltyDetails                = makePenaltyDetailsWithLpp(Some(Seq(firstAppealableLpp)))
          val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(penaltyDetails, "1234567891", "ITSA")

          result shouldBe None
        }
        "the penalties are not appealable as they are not first stage" in new Setup {
          private val firstAppealableLpp            = makeFirstLPP(appealInfo = Some(Seq(secondStageRejectedAppeal)))
          private val secondAppealableLpp           = makeSecondLPP(appealInfo = Some(Seq(secondStageRejectedAppeal)))
          private val penaltyDetails                = makePenaltyDetailsWithLpp(Some(Seq(firstAppealableLpp, secondAppealableLpp)))
          val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(penaltyDetails, "1234567891", "ITSA")

          result shouldBe None
        }
        "the penalties are not appealable as they were not rejected" in new Setup {
          private val firstAppealableLpp            = makeFirstLPP(appealInfo = Some(Seq(firstStageNotRejectedAppeal)))
          private val secondAppealableLpp           = makeSecondLPP(appealInfo = Some(Seq(firstStageNotRejectedAppeal)))
          private val penaltyDetails                = makePenaltyDetailsWithLpp(Some(Seq(firstAppealableLpp, secondAppealableLpp)))
          val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(penaltyDetails, "1234567891", "ITSA")

          result shouldBe None
        }
      }
    }

    "when regime is 'VATC'" should {
      "allow and return the multiple penalties data" when {
        "the LPPs have not been appealed" in new Setup {
          private val firstAppealableLpp            = makeFirstLPP(appealInfo = None)
          private val secondAppealableLpp           = makeSecondLPP(appealInfo = None)
          private val penaltyDetails                = makePenaltyDetailsWithLpp(Some(Seq(firstAppealableLpp, secondAppealableLpp)))
          val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(penaltyDetails, "1234567891", "VATC")

          result shouldBe Some(returnMultiplePenaltiesModel)
        }
      }
      "return no data" when {
        "the penalty is appealable but there is only one LPP" in new Setup {
          private val firstAppealableLpp            = makeFirstLPP(appealInfo = None)
          private val penaltyDetails                = makePenaltyDetailsWithLpp(Some(Seq(firstAppealableLpp)))
          val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(penaltyDetails, "1234567891", "VATC")

          result shouldBe None
        }
        "the penalties are not appealable as they already have appeal information" in new Setup {
          private val firstAppealableLpp            = makeFirstLPP(appealInfo = Some(Seq(firstStageRejectedAppeal)))
          private val secondAppealableLpp           = makeSecondLPP(appealInfo = Some(Seq(firstStageRejectedAppeal)))
          private val penaltyDetails                = makePenaltyDetailsWithLpp(Some(Seq(firstAppealableLpp, secondAppealableLpp)))
          val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(penaltyDetails, "1234567891", "VATC")

          result shouldBe None
        }
      }
    }
  }

}

object AppealServiceSpec {

  val firstStageRejectedAppeal = AppealInformationType(
    appealStatus = Some(AppealStatusEnum.Rejected),
    appealLevel = Some(AppealLevelEnum.HMRC),
    appealDescription = Some("Some value")
  )
  val firstStageNotRejectedAppeal = AppealInformationType(
    appealStatus = Some(AppealStatusEnum.Under_Appeal),
    appealLevel = Some(AppealLevelEnum.HMRC),
    appealDescription = Some("Some value")
  )
  val secondStageRejectedAppeal = AppealInformationType(
    appealStatus = Some(AppealStatusEnum.Rejected),
    appealLevel = Some(AppealLevelEnum.TribunalOrSecond),
    appealDescription = Some("Some value")
  )
  val returnMultiplePenaltiesModel: MultiplePenaltiesData = MultiplePenaltiesData(
    firstPenaltyChargeReference = "1234567891",
    firstPenaltyAmount = 113.45,
    secondPenaltyChargeReference = "1234567892",
    secondPenaltyAmount = 113.45,
    firstPenaltyCommunicationDate = LocalDate.of(2022, 8, 8),
    secondPenaltyCommunicationDate = LocalDate.of(2022, 8, 8)
  )

  val sampleLPP1 = LPPDetails(
    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
    principalChargeReference = "123456801",
    penaltyChargeReference = Some("1234567891"),
    penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
    penaltyStatus = LPPPenaltyStatusEnum.Posted,
    appealInformation = None,
    principalChargeBillingFrom = LocalDate.of(2022, 4, 1),
    principalChargeBillingTo = LocalDate.of(2022, 6, 30),
    principalChargeDueDate = LocalDate.of(2022, 8, 7),
    communicationsDate = Some(LocalDate.of(2022, 8, 8)),
    penaltyAmountOutstanding = Some(100),
    penaltyAmountPaid = Some(13.45),
    penaltyAmountPosted = 113.45,
    LPP1LRDays = None,
    LPP1HRDays = None,
    LPP2Days = None,
    LPP1HRCalculationAmount = None,
    LPP1LRCalculationAmount = None,
    LPP2Percentage = None,
    LPP1LRPercentage = None,
    LPP1HRPercentage = None,
    penaltyChargeDueDate = Some(LocalDate.of(2022, 8, 7)),
    principalChargeLatestClearing = Some(LocalDate.of(2022, 10, 1)),
    metadata = LPPDetailsMetadata(),
    penaltyAmountAccruing = BigDecimal(0),
    principalChargeMainTransaction = VATReturnCharge,
    vatOutstandingAmount = Some(BigDecimal(123.45)),
    supplement = false
  )

  val sampleLPP2 = LPPDetails(
    penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
    principalChargeReference = "123456801",
    penaltyChargeReference = Some("1234567892"),
    penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
    penaltyStatus = LPPPenaltyStatusEnum.Posted,
    appealInformation = None,
    principalChargeBillingFrom = LocalDate.of(2022, 4, 1),
    principalChargeBillingTo = LocalDate.of(2022, 6, 30),
    principalChargeDueDate = LocalDate.of(2022, 8, 7),
    communicationsDate = Some(LocalDate.of(2022, 9, 8)),
    penaltyAmountOutstanding = Some(100),
    penaltyAmountPaid = Some(13.44),
    penaltyAmountPosted = 113.44,
    LPP1LRDays = None,
    LPP1HRDays = None,
    LPP2Days = None,
    LPP1HRCalculationAmount = None,
    LPP1LRCalculationAmount = None,
    LPP2Percentage = None,
    LPP1LRPercentage = None,
    LPP1HRPercentage = None,
    penaltyChargeDueDate = Some(LocalDate.of(2022, 8, 7)),
    principalChargeLatestClearing = Some(LocalDate.of(2022, 10, 1)),
    metadata = LPPDetailsMetadata(),
    penaltyAmountAccruing = BigDecimal(0),
    principalChargeMainTransaction = VATReturnCharge,
    vatOutstandingAmount = Some(BigDecimal(123.45)),
    supplement = false
  )

  val getPenaltyDetailsOnePenalty: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(sampleLPP1)))),
    breathingSpace = None
  )

  val getPenaltyDetailsTwoPenalties: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(sampleLPP2, sampleLPP1)))),
    breathingSpace = None
  )

  val getPenaltyDetailsTwoPenaltiesNoCommunicationsDate: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        Some(
          Seq(
            sampleLPP2.copy(communicationsDate = None),
            sampleLPP1.copy(communicationsDate = None)
          )))),
    breathingSpace = None
  )

  val getPenaltyDetailsTwoPenaltiesWithAppeal: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(Some(Seq(
        sampleLPP2.copy(appealInformation = Some(Seq(AppealInformationType(
          appealStatus = Some(AppealStatusEnum.Under_Appeal),
          appealLevel = Some(AppealLevelEnum.HMRC),
          appealDescription = Some("Some value")
        )))),
        sampleLPP1
      )))),
    breathingSpace = None
  )

  val getPenaltyDetailsTwoPenaltiesLPP2Accruing: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(Some(Seq(
        sampleLPP2.copy(
          penaltyStatus = LPPPenaltyStatusEnum.Accruing,
          penaltyChargeReference = None,
          principalChargeLatestClearing = None,
          penaltyAmountPosted = 0,
          penaltyAmountPaid = None,
          penaltyAmountOutstanding = None,
          penaltyAmountAccruing = 144.21,
          communicationsDate = None
        ),
        sampleLPP1
      )))),
    breathingSpace = None
  )

  val getPenaltyDetailsTwoPenaltiesVATNotPaid: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(Some(Seq(
        sampleLPP2.copy(
          penaltyStatus = LPPPenaltyStatusEnum.Accruing,
          penaltyChargeReference = None,
          principalChargeLatestClearing = None,
          penaltyAmountPosted = 0,
          penaltyAmountPaid = None,
          penaltyAmountOutstanding = None,
          penaltyAmountAccruing = 144.21,
          communicationsDate = None
        ),
        sampleLPP1.copy(
          principalChargeLatestClearing = None
        )
      )))),
    breathingSpace = None
  )

  def makeLPP(category: LPPPenaltyCategoryEnum.Value, penaltyChargeRef: String, appealInfo: Option[Seq[AppealInformationType]]): LPPDetails =
    sampleLPP1.copy(
      penaltyCategory = category,
      penaltyChargeReference = Some(penaltyChargeRef),
      appealInformation = appealInfo
    )

  def makeFirstLPP(appealInfo: Option[Seq[AppealInformationType]]): LPPDetails =
    makeLPP(LPPPenaltyCategoryEnum.FirstPenalty, "1234567891", appealInfo)
  def makeSecondLPP(appealInfo: Option[Seq[AppealInformationType]]): LPPDetails =
    makeLPP(LPPPenaltyCategoryEnum.SecondPenalty, "1234567892", appealInfo)

  def makePenaltyDetailsWithLpp(lpp: Option[Seq[LPPDetails]]): GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(LatePaymentPenalty(details = lpp)),
    breathingSpace = None
  )
}
