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

import java.time.LocalDateTime

import base.SpecBase
import config.AppConfig
import connectors.parsers.ComplianceParser.{CompliancePayloadResponse, GetCompliancePayloadSuccessResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class ComplianceConnectorSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val testStartDate: LocalDateTime = LocalDateTime.of(2021,1,1,1,0,0)
  val testEndDate: LocalDateTime = LocalDateTime.of(2021,1,8,1,0,0)

  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)

    val connector = new ComplianceConnector(mockHttpClient, mockAppConfig)
    when(mockAppConfig.getPastReturnURL(ArgumentMatchers.any())).thenReturn("/")
    when(mockAppConfig.getComplianceSummaryURL(ArgumentMatchers.any())).thenReturn("/")
  }

  "getPastReturnsForEnrolmentKey" should {
    "return a JsValue" in new Setup {
      when(mockHttpClient.GET[CompliancePayloadResponse](ArgumentMatchers.eq(s"/123456789?startDate=$testStartDate&endDate=$testEndDate"),
      ArgumentMatchers.any(),
      ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(Json.parse("{}")))))

      val result: CompliancePayloadResponse =
        await(connector.getPastReturnsForEnrolmentKey("123456789", testStartDate, testEndDate, "mtd-vat")(HeaderCarrier()))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse] shouldBe GetCompliancePayloadSuccessResponse(Json.parse("{}"))
    }
  }

  "getComplianceSummaryForEnrolmentKey" should {
    "return a JsValue" in new Setup {
      when(mockHttpClient.GET[CompliancePayloadResponse](ArgumentMatchers.eq("/123456789"),
      ArgumentMatchers.any(),
      ArgumentMatchers.any())
        (ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(Json.parse("{}")))))

      val result: CompliancePayloadResponse =
        await(connector.getComplianceSummaryForEnrolmentKey("123456789", "mtd-vat")(HeaderCarrier()))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse] shouldBe GetCompliancePayloadSuccessResponse(Json.parse("{}"))
    }
  }
}
