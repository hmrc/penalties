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

package connectors

import base.SpecBase
import connectors.parsers.AppealsParser.AppealSubmissionResponse
import featureSwitches.{CallPEGA, FeatureSwitching}
import models.appeals.{AppealResponseModel, AppealSubmission, CrimeAppealInformation}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.Mockito._
import play.api.http.Status.OK
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class AppealsConnectorSpec extends SpecBase with FeatureSwitching {
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  implicit val hc: HeaderCarrier = HeaderCarrier().copy(otherHeaders = Seq(("Authorization","auth1"), ("CorrelationId", "1234"), ("Environment", "env")))

  class Setup {
    val connector = new AppealsConnector(
      mockHttpClient,
      appConfig
    )(ExecutionContext.Implicits.global)

    reset(mockHttpClient)
  }

  "submitAppeal with headers" should {
    "return the response of the call - sending extra headers when calling PEGA" in new Setup {
      enableFeatureSwitch(CallPEGA)
      val argumentCaptorOtherHeaders: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      when(mockHttpClient.POST[AppealSubmission, AppealSubmissionResponse](
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        argumentCaptorOtherHeaders.capture()
      )(ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(appealResponseModel)))
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentReferenceNo = Some("AGENT1"),
        appealInformation = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssueToPolice = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      )
      val result = await(connector.submitAppeal(modelToSend, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyId = "1234567890")(hc))
      result shouldBe Right(appealResponseModel)
      argumentCaptorOtherHeaders.getValue.find(_._1 == "Authorization").get._2 shouldBe "auth1"
      argumentCaptorOtherHeaders.getValue.find(_._1 == "CorrelationId").get._2 shouldBe "1234"
      argumentCaptorOtherHeaders.getValue.find(_._1 == "Environment").get._2 shouldBe "env"
    }

    "return the response of the call - sending no extra headers when NOT calling PEGA" in new Setup {
      disableFeatureSwitch(CallPEGA)
      val argumentCaptorOtherHeaders: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      when(mockHttpClient.POST[AppealSubmission, AppealSubmissionResponse](
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        argumentCaptorOtherHeaders.capture()
      )(ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(appealResponseModel)))
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentReferenceNo = Some("AGENT1"),
        appealInformation = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssueToPolice = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      )
      val result: AppealSubmissionResponse = await(connector.submitAppeal(modelToSend, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyId = "1234567890")(hc))
      result shouldBe Right(appealResponseModel)
      argumentCaptorOtherHeaders.getValue.exists(_._1 == "Authorization") shouldBe false
      argumentCaptorOtherHeaders.getValue.exists(_._1 == "CorrelationId") shouldBe false
      argumentCaptorOtherHeaders.getValue.exists(_._1 == "Environment") shouldBe false
    }

    "return the response of the call for LPP" in new Setup {
      when(mockHttpClient.POST[AppealSubmission, AppealSubmissionResponse](
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )(ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(appealResponseModel)))
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentReferenceNo = Some("AGENT1"),
        appealInformation = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssueToPolice = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      )
      val result: AppealSubmissionResponse = await(connector.submitAppeal(modelToSend, "HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyId = "1234567890")(HeaderCarrier()))
      result shouldBe Right(appealResponseModel)
    }
  }
}
