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

import base.SpecBase
import config.AppConfig
import config.featureSwitches.SanitiseFileName
import connectors.PEGAConnector
import connectors.parsers.AppealsParser
import connectors.parsers.AppealsParser.UnexpectedFailure
import models.appeals._
import models.notification._
import models.upload._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.UUIDGenerator

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class AppealServiceSpec extends SpecBase {
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

      "notifications are sent and then should sanitise the file names (when feature switch is enabled)" in new Setup {
        when(mockAppConfig.isEnabled(Matchers.eq(SanitiseFileName))).thenReturn(true)
        val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
        val uploads = Seq(
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = "file 1  !3 !something £4 x-y_z.txt",
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
              name = "file_1__3__something__4_x-y_z.txt",
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
  }
}
