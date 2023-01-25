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

package connectors

import base.{LogCapturing, SpecBase}
import config.featureSwitches.{CallPEGA, FeatureSwitching}
import connectors.parsers.AppealsParser.{AppealSubmissionResponse, UnexpectedFailure}
import models.appeals.{AgentDetails, AppealSubmission, CrimeAppealInformation}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class PEGAConnectorSpec extends SpecBase with FeatureSwitching with LogCapturing {
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq("CorrelationId" -> "id"))
  implicit val config: Configuration = appConfig.config

  class Setup {
    val connector = new PEGAConnector(
      mockHttpClient,
      appConfig
    )(ExecutionContext.Implicits.global)

    reset(mockHttpClient)
  }

  "submitAppeal with headers" should {
    "return the response of the call - including extra headers" in new Setup {
      enableFeatureSwitch(CallPEGA)
      val argumentCaptorOtherHeaders: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      when(mockHttpClient.POST[AppealSubmission, AppealSubmissionResponse](
        Matchers.any(),
        Matchers.any(),
        argumentCaptorOtherHeaders.capture()
      )(Matchers.any(),
        Matchers.any(),
        Matchers.any(),
        Matchers.any()))
        .thenReturn(Future.successful(Right(appealResponseModel)))
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
        appealInformation = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          reportedIssueToPolice = "yes",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      )
      val result: AppealSubmissionResponse = await(connector.submitAppeal(modelToSend,
        "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "1234567890", correlationId = "id"))
      result shouldBe Right(appealResponseModel)

      argumentCaptorOtherHeaders.getValue.find(_._1 == "Authorization").get._2 shouldBe "Bearer placeholder"
      argumentCaptorOtherHeaders.getValue.find(_._1 == "CorrelationId").get._2 shouldBe "id"
      argumentCaptorOtherHeaders.getValue.find(_._1 == "Environment").get._2 shouldBe "environmentValue"
    }

    "return the response of the call for LPP" in new Setup {
      when(mockHttpClient.POST[AppealSubmission, AppealSubmissionResponse](
        Matchers.any(),
        Matchers.any(),
        Matchers.any()
      )(Matchers.any(),
        Matchers.any(),
        Matchers.any(),
        Matchers.any()))
        .thenReturn(Future.successful(Right(appealResponseModel)))
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
        appealInformation = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          reportedIssueToPolice = "yes",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      )
      val result: AppealSubmissionResponse = await(connector.submitAppeal(modelToSend,
        "HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyNumber = "1234567890", correlationId = "id"))
      result shouldBe Right(appealResponseModel)
    }

    "returns a 4xx response for a UpstreamErrorResponse(4xx) exception" in new Setup {
      when(mockHttpClient.POST[AppealSubmission, AppealSubmissionResponse](
        Matchers.any(),
        Matchers.any(),
        Matchers.any()
      )(Matchers.any(),
        Matchers.any(),
        Matchers.any(),
        Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("", BAD_REQUEST)))
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
        appealInformation = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          reportedIssueToPolice = "yes",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      )
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: AppealSubmissionResponse = await(connector.submitAppeal(modelToSend,
            "HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyNumber = "1234567890", correlationId = "id"))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1808_API.toString)) shouldBe true
          result shouldBe Left(UnexpectedFailure(BAD_REQUEST, ""))
        }
      }
    }

    "returns a 5xx response for a UpstreamErrorResponse(5xx) exception" in new Setup {
      when(mockHttpClient.POST[AppealSubmission, AppealSubmissionResponse](
        Matchers.any(),
        Matchers.any(),
        Matchers.any()
      )(Matchers.any(),
        Matchers.any(),
        Matchers.any(),
        Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("", INTERNAL_SERVER_ERROR)))
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
        appealInformation = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          reportedIssueToPolice = "yes",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      )
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: AppealSubmissionResponse = await(connector.submitAppeal(modelToSend,
            "HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyNumber = "1234567890", correlationId = "id"))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1808_API.toString)) shouldBe true
          result shouldBe Left(UnexpectedFailure(INTERNAL_SERVER_ERROR, ""))
        }
      }
    }

    "returns a 500 response for an unknown exception" in new Setup {
      when(mockHttpClient.POST[AppealSubmission, AppealSubmissionResponse](
        Matchers.any(),
        Matchers.any(),
        Matchers.any()
      )(Matchers.any(),
        Matchers.any(),
        Matchers.any(),
        Matchers.any()))
        .thenReturn(Future.failed(new Exception("failed")))
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
        appealInformation = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          reportedIssueToPolice = "yes",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      )
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: AppealSubmissionResponse = await(connector.submitAppeal(modelToSend,
            "HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyNumber = "1234567890", correlationId = "id"))
          logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1808_API.toString)) shouldBe true
          result shouldBe Left(UnexpectedFailure(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information."))
        }
      }
    }
  }

  "headerForEIS" should {
    "return a HeaderCarrier with the correct headers" in new Setup {
      val result: HeaderCarrier = connector.headersForEIS("id", "token", "env")
      result.otherHeaders.toMap.get("Environment").get shouldBe "env"
      result.otherHeaders.toMap.get("CorrelationId").get shouldBe "id"
      result.otherHeaders.toMap.get(AUTHORIZATION).get shouldBe "Bearer token"
    }
  }
}
