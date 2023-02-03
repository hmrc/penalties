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

package connectors.getPenaltyDetails

import base.{LogCapturing, SpecBase}
import config.AppConfig
import config.featureSwitches.AddReceiptDateHeaderToAPI1812
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsResponse, GetPenaltyDetailsSuccessResponse}
import models.getPenaltyDetails.GetPenaltyDetails
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.{ArgumentCaptor, Matchers}
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import scala.concurrent.{ExecutionContext, Future}

class GetPenaltyDetailsConnectorSpec extends SpecBase with LogCapturing {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])

  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)

    val connector = new GetPenaltyDetailsConnector(mockHttpClient, mockAppConfig)
    when(mockAppConfig.getPenaltyDetailsUrl).thenReturn("/penalty/details/VATC/VRN/")
    when(mockAppConfig.eisEnvironment).thenReturn("env")
    when(mockAppConfig.eiOutboundBearerToken).thenReturn("token")
  }

  val mockGetPenaltyDetailsModelAPI1812: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = None,
    breathingSpace = None
  )

  "getPenaltiesDetails" should {
    "return a 200 when the call succeeds" in new Setup {
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsModelAPI1812))))

      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isRight shouldBe true
    }

    "send the 'ReceiptDate' header when the feature switch is enabled" in new Setup {
      val argumentCaptorForHeaders = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        argumentCaptorForHeaders.capture())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsModelAPI1812))))
      when(mockAppConfig.isEnabled(Matchers.eq(AddReceiptDateHeaderToAPI1812)))
        .thenReturn(true)
      val connectorForTest = new GetPenaltyDetailsConnector(mockHttpClient, mockAppConfig)
      val result: GetPenaltyDetailsResponse = await(connectorForTest.getPenaltyDetails("123456789"))
      result.isRight shouldBe true
      argumentCaptorForHeaders.getValue.exists(_._1 == "ReceiptDate") shouldBe true
    }

    "don't send the 'ReceiptDate' header when the feature switch is disabled" in new Setup {
      val argumentCaptorForHeaders = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        argumentCaptorForHeaders.capture())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsModelAPI1812))))
      when(mockAppConfig.isEnabled(Matchers.eq(AddReceiptDateHeaderToAPI1812)))
        .thenReturn(false)
      val connectorForTest = new GetPenaltyDetailsConnector(mockHttpClient, mockAppConfig)
      val result: GetPenaltyDetailsResponse = await(connectorForTest.getPenaltyDetails("123456789"))
      result.isRight shouldBe true
      argumentCaptorForHeaders.getValue.exists(_._1 == "ReceiptDate") shouldBe false
    }

    s"return a 404 when the call fails for Not Found" in new Setup {
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.NOT_FOUND))))

      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
    }

    s"return a 400 when the call fails for Bad Request" in new Setup {
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.BAD_REQUEST))))

      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
    }

    s"return a 409 when the call fails for Conflict" in new Setup {
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.CONFLICT))))

      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
    }

    s"return a 422 when the call fails for Unprocessable Entity" in new Setup {
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.UNPROCESSABLE_ENTITY))))

      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
    }

    s"return a 500 when the call fails for Internal Server Error" in new Setup {
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))

      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
    }

    s"return a 503 when the call fails" in new Setup {
      when(mockHttpClient.GET[GetPenaltyDetailsResponse](Matchers.eq("/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.SERVICE_UNAVAILABLE))))

      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
    }

    "return a 500 when the call fails due to an UpstreamErrorResponse(5xx) exception" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("", Status.INTERNAL_SERVER_ERROR)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(vrn = "123456789"))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }

    "return a 400 when the call fails due to an UpstreamErrorResponse(4xx) exception" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("", Status.BAD_REQUEST)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(vrn = "123456789"))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }

    "return a 500 when the call fails due to an unexpected exception" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(new Exception("Something weird happened")))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(vrn = "123456789")(HeaderCarrier()))
          logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }
  }

  "getPenaltyDetailsForAPI" should {
    val queryParam = "?dateLimit=09"

    "return a 200 when the call succeeds" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/penalty/details/VATC/VRN/123456789$queryParam"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(status = Status.OK, json = Json.toJson(mockGetPenaltyDetailsModelAPI1812), headers = Map.empty)))

      val result: HttpResponse = await(connector.getPenaltyDetailsForAPI(vrn = "123456789", dateLimit = Some("09"))(HeaderCarrier()))
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe Json.toJson(mockGetPenaltyDetailsModelAPI1812)
    }

    "return a 200 when the call succeeds - with only vrn" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/penalty/details/VATC/VRN/123456789"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(status = Status.OK, json = Json.toJson(mockGetPenaltyDetailsModelAPI1812), headers = Map.empty)))

      val result: HttpResponse = await(connector.getPenaltyDetailsForAPI(vrn = "123456789", dateLimit = None)(HeaderCarrier()))
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe Json.toJson(mockGetPenaltyDetailsModelAPI1812)
    }

    s"return a 403 when the call fails for Not Found (for 4xx errors)" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/penalty/details/VATC/VRN/123456789$queryParam"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("You shall not pass", Status.FORBIDDEN)))

      val result: HttpResponse = await(connector.getPenaltyDetailsForAPI(vrn = "123456789", dateLimit = Some("09"))(HeaderCarrier()))
      result.status shouldBe Status.FORBIDDEN
    }

    s"return a 500 when the call fails for Internal Server Error (for 5xx errors)" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/penalty/details/VATC/VRN/123456789$queryParam"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("Oops :(", Status.INTERNAL_SERVER_ERROR)))

      val result: HttpResponse = await(connector.getPenaltyDetailsForAPI(vrn = "123456789", dateLimit = Some("09"))(HeaderCarrier()))
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return a 500 when the call fails due to an unexpected exception" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/penalty/details/VATC/VRN/123456789$queryParam"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(new Exception("Something weird happened")))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: HttpResponse = await(connector.getPenaltyDetailsForAPI(vrn = "123456789", dateLimit = Some("09"))(HeaderCarrier()))
          logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1812_API.toString)) shouldBe true
          result.status shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
