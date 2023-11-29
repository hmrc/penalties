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

import base.{LogCapturing, SpecBase}
import config.AppConfig
import config.featureSwitches.SanitiseFileName
import connectors.PEGAConnector
import connectors.parsers.AppealsParser
import connectors.parsers.AppealsParser.UnexpectedFailure
import models.appeals._
import models.getFinancialDetails.MainTransactionEnum
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment._
import models.notification._
import models.upload._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger
import utils.UUIDGenerator

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class AppealServiceSpec extends SpecBase with LogCapturing {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq("CorrelationId" -> "id"))
  val mockAppealsConnector: PEGAConnector = mock(classOf[PEGAConnector])
  val correlationId: String = "correlationId"
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val mockUUIDGenerator: UUIDGenerator = mock(classOf[UUIDGenerator])
  implicit val config: Configuration = mockAppConfig.config

  class Setup {
    val service = new AppealService(
      mockAppealsConnector, mockAppConfig, mockUUIDGenerator
    )
    reset(mockAppealsConnector)
    reset(mockAppConfig)
    reset(mockUUIDGenerator)
    when(mockAppConfig.isEnabled(Matchers.eq(SanitiseFileName))).thenReturn(false)
    when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
    when(mockAppConfig.SDESNotificationInfoType).thenReturn("S18")
    when(mockAppConfig.SDESNotificationFileRecipient).thenReturn("123456789012")
    when(mockAppConfig.maximumFilenameLength).thenReturn(150)
  }

  "submitAppeal" should {
    val modelToPassToServer: AppealSubmission = AppealSubmission(
      taxRegime = "VAT",
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

    "return the response from the connector i.e. act as a pass-through function" in new Setup {
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(appealResponseModel)))

      val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] = await(
        service.submitAppeal(modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId))
      result shouldBe Right(appealResponseModel)
    }

    "return the response from the connector on error i.e. act as a pass-through function" in new Setup {
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.successful(
        Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))))

      val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] = await(service.submitAppeal(
        modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId))
      result shouldBe Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))
    }

    "throw an exception when the connector throws an exception" in new Setup {
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("Something went wrong")))

      val result: Exception = intercept[Exception](await(service.submitAppeal(
        modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)))
      result.getMessage shouldBe "Something went wrong"
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
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = "file1",
              fileMimeType = "text/plain",
              uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
              checksum = "check123456789",
              size = 1
            )),
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
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
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = "file1",
              fileMimeType = "text/plain",
              uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
              checksum = "check123456789",
              size = 1
            )),
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
              "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
            ))
          ),
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = None,
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
              "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
            ))
          ),
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = "file3",
              fileMimeType = "text/plain",
              uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
              checksum = "check123456789",
              size = 1
            )),
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
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
        withCaptureOfLoggingFrom(logger) {
          logs => {
            val result = service.createSDESNotifications(Some(uploads), caseID = "PR-1234")
            result shouldBe expectedResult
            logs.exists(_.getMessage == "[AppealService][createSDESNotifications] - There are 3 uploads but" +
              s" only 2 uploads have upload details defined (possible missing files for case ID: PR-1234)") shouldBe true
          }
        }

      }

      "notifications are sent and then should sanitise the file names (when feature switch is enabled)" in new Setup {
        when(mockAppConfig.isEnabled(Matchers.eq(SanitiseFileName))).thenReturn(true)
        when(mockAppConfig.getMimeType(Matchers.eq("text.plain"))).thenReturn(Some(".txt"))
        when(mockAppConfig.checksumAlgorithmForFileNotifications).thenReturn("SHA-256")
        val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
        val uploads = Seq(
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = "file 1 / * 3  something‘ ’ “ ” <4 x>y_z |\" \\",
              fileMimeType = "text/plain",
              uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
              checksum = "check123456789",
              size = 1
            )),
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
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
    s"truncate file name" when {
      val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
      val longFilename = Random.alphanumeric.take(160).mkString
      "filename is above maximumFilenameLength and includes file extension" in new Setup {
        val uploads = Seq(
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = longFilename + ".txt",
              fileMimeType = "text/plain",
              uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
              checksum = "check123456789",
              size = 1
            )),
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
              "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
            ))
          )
        )
        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result: Seq[SDESNotification] = service.createSDESNotifications(Some(uploads), caseID = "PR-5678")
        val resultFileName: String = result.head.file.name
        resultFileName.length shouldBe mockAppConfig.maximumFilenameLength + 4
        resultFileName.contains(".txt") shouldBe true
      }

      "reduce a filename to maximum character length based on maximumFilenameLength when there is no file extension" in new Setup {
        val uploads = Seq(
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = longFilename,
              fileMimeType = "text/plain",
              uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
              checksum = "check123456789",
              size = 1
            )),
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
              "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
            ))
          )
        )
        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result: Seq[SDESNotification] = service.createSDESNotifications(Some(uploads), caseID = "PR-5678")
        val resultFileName: String = result.head.file.name
        resultFileName.length shouldBe mockAppConfig.maximumFilenameLength
      }

      "correctly remove file extension when filename includes periods" in new Setup {
        val longFilename: String = Random.alphanumeric.take(60).mkString
        val uploads = Seq(
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = longFilename + "." + longFilename + ".." + longFilename + ".txt",
              fileMimeType = "text/plain",
              uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
              checksum = "check123456789",
              size = 1
            )),
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
              "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
            ))
          )
        )
        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result: Seq[SDESNotification] = service.createSDESNotifications(Some(uploads), caseID = "PR-5678")
        val resultFileName: String = result.head.file.name
        resultFileName.length shouldBe mockAppConfig.maximumFilenameLength + 4
        resultFileName.contains(".txt") shouldBe true
        resultFileName.count(_ == '.') shouldBe 4
      }
    }
  }

  "getMultiplePenaltyData" should {
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
      principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
      vatOutstandingAmount = Some(BigDecimal(123.45))
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
      principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
      vatOutstandingAmount = Some(BigDecimal(123.45))
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
      latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
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

    s"return None" when {
      "there is only one penalty under this principal charge" in new Setup {
        val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(getPenaltyDetailsOnePenalty, "1234567891")
        result shouldBe None
      }

      "either penalty under the principal charge has appeal in any state" in new Setup {
        val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(getPenaltyDetailsTwoPenaltiesWithAppeal, "1234567891")
        result shouldBe None
      }

      "either penalty is accruing" in new Setup {
        val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(getPenaltyDetailsTwoPenaltiesLPP2Accruing, "1234567891")
        result shouldBe None
      }

      "the VAT has not been paid" in new Setup {
        val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(getPenaltyDetailsTwoPenaltiesVATNotPaid, "1234567891")
        result shouldBe None
      }
    }

    s"return Some" when {
      "there is two penalties under this principal charge and they are both POSTED and VAT has been paid" in new Setup {
        val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(getPenaltyDetailsTwoPenalties, "1234567892")
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

      s"there is two penalties under this principal charge and they are both POSTED and VAT has been paid" +
        s" (defaulting the comms date if not present)" in new Setup {
        when(mockAppConfig.getTimeMachineDateTime).thenReturn(LocalDateTime.now)
        val result: Option[MultiplePenaltiesData] = service.findMultiplePenalties(getPenaltyDetailsTwoPenaltiesNoCommunicationsDate, "1234567891")
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
  }
}
