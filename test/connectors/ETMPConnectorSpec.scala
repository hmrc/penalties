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
import connectors.parsers.ETMPPayloadParser._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class ETMPConnectorSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])

  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)

    val connector = new ETMPConnector(mockHttpClient, mockAppConfig)
    when(mockAppConfig.getVATPenaltiesURL).thenReturn("/")
  }

  "getPenaltiesDataForEnrolmentKey" should {
    s"return a successful response i.e. GetETMPPayloadSuccess - when the call succeeds and the body can be parsed" in new Setup {
      when(mockHttpClient.GET[ETMPPayloadResponse](ArgumentMatchers.eq("/123456789"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel))))

      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789")(HeaderCarrier()))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetETMPPayloadSuccessResponse].etmpPayload shouldBe mockETMPPayloadResponseAsModel
    }

    "return a Left response" when {
      "the call returns an OK response however the body is not parsable as a model" in new Setup {
        when(mockHttpClient.GET[ETMPPayloadResponse](ArgumentMatchers.eq("/123456789"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetETMPPayloadMalformed)))

        val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns a No Content status" in new Setup {
        when(mockHttpClient.GET[ETMPPayloadResponse](ArgumentMatchers.eq("/123456789"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetETMPPayloadNoContent)))

        val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns an ISE" in new Setup {
        when(mockHttpClient.GET[ETMPPayloadResponse](ArgumentMatchers.eq("/123456789"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetETMPPayloadFailureResponse(Status.INTERNAL_SERVER_ERROR))))

        val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789")(HeaderCarrier()))
        result.isLeft shouldBe true
      }

      "the call returns an unmatched response" in new Setup {
        when(mockHttpClient.GET[ETMPPayloadResponse](ArgumentMatchers.eq("/123456789"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetETMPPayloadFailureResponse(Status.BAD_GATEWAY))))

        val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789")(HeaderCarrier()))
        result.isLeft shouldBe true
      }
    }
  }
}
