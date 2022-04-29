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

package connectors.v3.getPenaltyDetails

import base.SpecBase
import config.AppConfig
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsResponse, GetPenaltyDetailsSuccessResponse}
import models.v3.getPenaltyDetails.GetPenaltyDetails
import org.mockito.Matchers
import org.mockito.Mockito.{mock, reset, when}
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class GetPenaltyDetailsConnectorSpec extends SpecBase {
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
    latePaymentPenalty = None
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
  }
}
