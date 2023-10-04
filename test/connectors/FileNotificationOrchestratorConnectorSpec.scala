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

import base.SpecBase
import config.AppConfig
import models.notification._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class FileNotificationOrchestratorConnectorSpec extends SpecBase {
   val mockHttpClient: HttpClient = mock(classOf[HttpClient])
   val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    reset(mockHttpClient)
    val connector = new FileNotificationOrchestratorConnector(mockHttpClient, mockAppConfig)
  }

  "postFileNotifications" should {
    s"return a successful response from " in new Setup {
      val model: SDESNotification = SDESNotification(
        informationType = "sample1234",
        file = SDESNotificationFile(
          recipientOrSender = "sample1",
          name = "file1.txt",
          location = "http://location.url/download",
          checksum = SDESChecksum("algorithm", "123dasd89"),
          size = 200,
          properties = Seq(SDESProperties("key", "value"))
        ),
        audit = SDESAudit(
          correlationID = "corr12345"
        )
      )
      when(mockHttpClient.POST[Seq[SDESNotification], HttpResponse](
        Matchers.any(),
        Matchers.any(),
        Matchers.any()
      )(Matchers.any(),
        Matchers.any(),
        Matchers.any(),
        Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val result: HttpResponse = await(connector.postFileNotifications(Seq(model))(HeaderCarrier()))
      result.status shouldBe OK
     }
  }
}
