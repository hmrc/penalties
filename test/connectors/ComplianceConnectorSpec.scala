/*
 * Copyright 2021 HM Revenue & Customs
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
import config.AppConfig
import connectors.parsers.ComplianceParser._
import models.compliance.{CompliancePayload, ComplianceStatusEnum, ObligationDetail, ObligationIdentification}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpClient}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class ComplianceConnectorSpec extends SpecBase {
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
        identification = ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        ),
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
      when(mockAppConfig.getComplianceData(ArgumentMatchers.eq("123456789"), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn("/123456789")
      when(mockAppConfig.desEnvironment).thenReturn("env")
      when(mockAppConfig.desBearerToken).thenReturn("Bearer 12345")
      val hcArgumentCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      when(mockHttpClient.GET[CompliancePayloadResponse](ArgumentMatchers.eq("/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          hcArgumentCaptor.capture(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(CompliancePayloadSuccessResponse(compliancePayloadAsModel))))
      val result: CompliancePayloadResponse =
        await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[CompliancePayloadSuccessResponse] shouldBe CompliancePayloadSuccessResponse(compliancePayloadAsModel)
      hcArgumentCaptor.getValue.authorization shouldBe Some(Authorization("Bearer 12345"))
      hcArgumentCaptor.getValue.extraHeaders.find(_._1 == "Environment").get shouldBe ("Environment" -> "env")
    }

    "return a Left response" when {
      "the call returns a OK response however the body is not parsable as a model" in new Setup {
        when(mockAppConfig.getComplianceData(ArgumentMatchers.eq("123456789"), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.desEnvironment).thenReturn("env")
        when(mockAppConfig.desBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](ArgumentMatchers.eq("/123456789"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadMalformed)))
        val result: CompliancePayloadResponse =
          await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns a Not Found status" in new Setup {
        when(mockAppConfig.getComplianceData(ArgumentMatchers.eq("123456789"), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.desEnvironment).thenReturn("env")
        when(mockAppConfig.desBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](ArgumentMatchers.eq("/123456789"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadNoData)))
        val result: CompliancePayloadResponse =
          await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns a ISE" in new Setup {
        when(mockAppConfig.getComplianceData(ArgumentMatchers.eq("123456789"), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.desEnvironment).thenReturn("env")
        when(mockAppConfig.desBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](ArgumentMatchers.eq("/123456789"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadFailureResponse(INTERNAL_SERVER_ERROR))))
        val result: CompliancePayloadResponse =
          await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns an unmatched response" in new Setup {
        when(mockAppConfig.getComplianceData(ArgumentMatchers.eq("123456789"), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn("/123456789")
        when(mockAppConfig.desEnvironment).thenReturn("env")
        when(mockAppConfig.desBearerToken).thenReturn("Bearer 12345")
        when(mockHttpClient.GET[CompliancePayloadResponse](ArgumentMatchers.eq("/123456789"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadFailureResponse(SERVICE_UNAVAILABLE))))
        val result: CompliancePayloadResponse =
          await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31")(HeaderCarrier()))
        result.isLeft shouldBe true
      }
    }
  }
}
