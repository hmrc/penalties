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
import config.AppConfig
import connectors.parsers.ComplianceParser._
import models.compliance.{CompliancePayload, ComplianceStatusEnum, ObligationDetail, ObligationIdentification}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class ComplianceConnectorSpec extends SpecBase with LogCapturing {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val testStartDate: LocalDateTime = LocalDateTime.of(
    2021,1,1,1,0,0)
  val testEndDate: LocalDateTime = LocalDateTime.of(
    2021,1,8,1,0,0)

  val date1: LocalDateTime = LocalDateTime.of(
    2022, 1, 1, 1, 1, 0)
  val date2: LocalDateTime = LocalDateTime.of(
    2024, 1, 1, 1, 1, 0)

  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)

    val connector = new ComplianceConnector(mockHttpClient, mockAppConfig)
  }

  "getComplianceData" should {
    "should return a model - when the call succeeds and the body can be parsed" in new Setup {
      val compliancePayloadAsModel: CompliancePayload = CompliancePayload(
        identification = Some(ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        )),
        obligationDetails = Seq(
          ObligationDetail(
            status = ComplianceStatusEnum.open,
            inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceDateReceived = None,
            inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
            periodKey = "#001"
          ),
          ObligationDetail(
            status = ComplianceStatusEnum.fulfilled,
            inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceDateReceived = Some(LocalDate.of(1920, 2, 29)),
            inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
            periodKey = "#001"
          )
        )
      )
      when(mockAppConfig.getComplianceData(Matchers.eq("123456789"), Matchers.any(), Matchers.any()))
        .thenReturn("/123456789")
      when(mockAppConfig.eisEnvironment).thenReturn("env")
      when(mockAppConfig.desBearerToken).thenReturn("12345")
      val headersArgumentCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      when(mockHttpClient.GET[CompliancePayloadResponse](Matchers.eq("/123456789"),
        Matchers.any(),
        headersArgumentCaptor.capture())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Right(CompliancePayloadSuccessResponse(compliancePayloadAsModel))))
      val result: CompliancePayloadResponse =
        await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
      result.isRight shouldBe true
      result.toOption.get.asInstanceOf[CompliancePayloadSuccessResponse] shouldBe CompliancePayloadSuccessResponse(compliancePayloadAsModel)
      headersArgumentCaptor.getValue.find(_._1 == "Authorization").get._2 shouldBe "Bearer 12345"
      headersArgumentCaptor.getValue.find(_._1 == "Environment").get._2 shouldBe "env"
    }

    "return a Left response" when {
      "the call returns a OK response however the body is not parsable as a model" in new Setup {
        when(mockAppConfig.getComplianceData(Matchers.eq("123456789"), Matchers.any(), Matchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.eisEnvironment).thenReturn("env")
        when(mockAppConfig.eiOutboundBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](Matchers.eq("/123456789"),
          Matchers.any(),
          Matchers.any())
          (Matchers.any(),
            Matchers.any(),
            Matchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadMalformed)))
        val result: CompliancePayloadResponse =
          await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns a Not Found status" in new Setup {
        when(mockAppConfig.getComplianceData(Matchers.eq("123456789"), Matchers.any(), Matchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.eisEnvironment).thenReturn("env")
        when(mockAppConfig.eiOutboundBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](Matchers.eq("/123456789"),
          Matchers.any(),
          Matchers.any())
          (Matchers.any(),
            Matchers.any(),
            Matchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadNoData)))
        val result: CompliancePayloadResponse =
          await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns a ISE" in new Setup {
        when(mockAppConfig.getComplianceData(Matchers.eq("123456789"), Matchers.any(), Matchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.eisEnvironment).thenReturn("env")
        when(mockAppConfig.eiOutboundBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](Matchers.eq("/123456789"),
          Matchers.any(),
          Matchers.any())
          (Matchers.any(),
            Matchers.any(),
            Matchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadFailureResponse(INTERNAL_SERVER_ERROR))))
        val result: CompliancePayloadResponse =
          await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns an unmatched response" in new Setup {
        when(mockAppConfig.getComplianceData(Matchers.eq("123456789"), Matchers.any(), Matchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.eisEnvironment).thenReturn("env")
        when(mockAppConfig.eiOutboundBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](Matchers.eq("/123456789"),
          Matchers.any(),
          Matchers.any())
          (Matchers.any(),
            Matchers.any(),
            Matchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadFailureResponse(SERVICE_UNAVAILABLE))))
        val result: CompliancePayloadResponse =
          await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns a UpstreamErrorResponse(4xx) exception" in new Setup {
        when(mockAppConfig.getComplianceData(Matchers.eq("123456789"), Matchers.any(), Matchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.eisEnvironment).thenReturn("env")
        when(mockAppConfig.eiOutboundBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](Matchers.eq("/123456789"),
          Matchers.any(),
          Matchers.any())
          (Matchers.any(),
            Matchers.any(),
            Matchers.any()))
          .thenReturn(Future.failed(UpstreamErrorResponse.apply("", BAD_REQUEST)))
        withCaptureOfLoggingFrom(logger) {
          logs => {
            val result: CompliancePayloadResponse =
              await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1330_API.toString)) shouldBe true
            result.isLeft shouldBe true
          }
        }
      }

      "the call returns a UpstreamErrorResponse(5xx) exception" in new Setup {
        when(mockAppConfig.getComplianceData(Matchers.eq("123456789"), Matchers.any(), Matchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.eisEnvironment).thenReturn("env")
        when(mockAppConfig.eiOutboundBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](Matchers.eq("/123456789"),
          Matchers.any(),
          Matchers.any())
          (Matchers.any(),
            Matchers.any(),
            Matchers.any()))
          .thenReturn(Future.failed(UpstreamErrorResponse.apply("", INTERNAL_SERVER_ERROR)))
        withCaptureOfLoggingFrom(logger) {
          logs => {
            val result: CompliancePayloadResponse =
              await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1330_API.toString)) shouldBe true
            result.isLeft shouldBe true
          }
        }
      }

      "the call returns an exception" in new Setup {
        when(mockAppConfig.getComplianceData(Matchers.eq("123456789"), Matchers.any(), Matchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.eisEnvironment).thenReturn("env")
        when(mockAppConfig.eiOutboundBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](Matchers.eq("/123456789"),
          Matchers.any(),
          Matchers.any())
          (Matchers.any(),
            Matchers.any(),
            Matchers.any()))
          .thenReturn(Future.failed(new Exception("failed")))
        withCaptureOfLoggingFrom(logger) {
          logs => {
            val result: CompliancePayloadResponse =
              await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
            logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1330_API.toString)) shouldBe true
            result.isLeft shouldBe true
          }
        }
      }
    }
  }
}
