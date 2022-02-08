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
      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq("/VRN/123456789/VATC"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(Status.OK, "")))

      val result: HttpResponse = await(connector.getFinancialDetails("VRN/123456789/VATC")(HeaderCarrier()))
      result.status shouldBe Status.OK
    }

    s"return a 400 when the call fails" in new Setup {
      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq("/FOO/123456789/BAR"),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(Status.BAD_REQUEST, "")))

      val result: HttpResponse = await(connector.getFinancialDetails("FOO/123456789/BAR")(HeaderCarrier()))
      result.status shouldBe Status.BAD_REQUEST
    }
  }
}
