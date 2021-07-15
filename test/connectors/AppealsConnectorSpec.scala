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
import models.appeals.{AppealSubmission, CrimeAppealInformation}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status.OK
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class AppealsConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  class Setup {
    val connector = new AppealsConnector(
      mockHttpClient,
      appConfig
    )(ExecutionContext.Implicits.global)

    reset(mockHttpClient)
  }

  "submitAppeal" should {
    "return the response of the call" in new Setup {
      when(mockHttpClient.POST[AppealSubmission, HttpResponse](
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )(ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "OK")))
      val modelToSend: AppealSubmission = AppealSubmission(
        submittedBy = "client",
        penaltyId = "1234567890",
        reasonableExcuse = "ENUM_PEGA_LIST",
        honestyDeclaration = true,
        appealInformation = CrimeAppealInformation(
          `type` = "crime",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssue = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = None,
          causeOfLateSubmissionAgent = None
        )
      )
      val result = await(connector.submitAppeal(modelToSend, "HMRC-MTD-VAT~VRN~123456789")(HeaderCarrier()))
      result.status shouldBe OK
      result.body shouldBe "OK"
    }
  }
}
