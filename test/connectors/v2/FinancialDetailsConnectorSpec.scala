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

package connectors.v2

import base.SpecBase
import config.AppConfig
import connectors.parsers.v2.GetFinancialDetailsParser.{GetFinancialDetailsFailureResponse, GetFinancialDetailsResponse, GetFinancialDetailsSuccessResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsConnectorSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])

  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)

    val connector = new FinancialDetailsConnector(mockHttpClient, mockAppConfig)
    when(mockAppConfig.getFinancialDetailsUrl).thenReturn("/")
  }

  "getFinancialDetails" should {
    "return a 200 when the call succeeds" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq("/VATC/VRN/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsModel))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("VATC/VRN/123456789")(HeaderCarrier()))
      result.isRight shouldBe true
    }

    s"return a 404 when the call fails for Not Found" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq("/FOO/BAR/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.NOT_FOUND))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789")(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 400 when the call fails for Bad Request" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq("/FOO/BAR/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.BAD_REQUEST))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789")(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 409 when the call fails for Conflict" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq("/FOO/BAR/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.CONFLICT))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789")(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 422 when the call fails for Unprocessable Entity" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq("/FOO/BAR/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.UNPROCESSABLE_ENTITY))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789")(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 500 when the call fails for Internal Server Error" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq("/FOO/BAR/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789")(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 403 when the call fails" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq("/FOO/BAR/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.FORBIDDEN))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789")(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 503 when the call fails" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq("/FOO/BAR/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.SERVICE_UNAVAILABLE))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789")(HeaderCarrier()))
      result.isLeft shouldBe true
    }
  }
}
