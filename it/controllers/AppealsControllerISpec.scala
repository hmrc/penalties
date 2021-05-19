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

package controllers

import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.await
import utils.{ETMPWiremock, IntegrationSpecCommonBase}
import play.api.test.Helpers._

class AppealsControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val controller: AppealsController = injector.instanceOf[AppealsController]

  val appealJson: JsValue = Json.parse(
    """
      |{
      |  "type": "LATE_SUBMISSION",
      |  "startDate": "2021-04-23T18:25:43.511",
      |  "endDate": "2021-04-23T18:25:43.511",
      |  "dueDate": "2021-04-23T18:25:43.511"
      |}
      |""".stripMargin)

  "getAppealsDataForLateSubmissionPenalty" should {
    "call ETMP and compare the penalty ID provided and the penalty ID in the payload - return OK if there is a match" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789")
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-submissions?penaltyId=1234&enrolmentKey=123456789").get())
      result.status shouldBe Status.OK
      result.body shouldBe appealJson.toString()
    }

    "return NOT_FOUND when the penalty ID given does not match the penalty ID in the payload" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789")
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-submissions?penaltyId=0001&enrolmentKey=123456789").get())
      result.status shouldBe Status.NOT_FOUND
    }

    "return an ISE when the call to ETMP fails" in {
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-submissions?penaltyId=0001&enrolmentKey=123456789").get())
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "getReasonableExcuses" should {
    "return all active reasonable excuses" in {
      val jsonExpectedToReturn: JsValue = Json.parse(
        """
          |{
          |  "excuses": [
          |    {
          |      "type": "bereavement",
          |      "descriptionKey": "reasonableExcuses.bereavementReason"
          |    },
          |    {
          |      "type": "crime",
          |      "descriptionKey": "reasonableExcuses.crimeReason"
          |    },
          |    {
          |      "type": "fireOrFlood",
          |      "descriptionKey": "reasonableExcuses.fireOrFloodReason"
          |    },
          |    {
          |      "type": "health",
          |      "descriptionKey": "reasonableExcuses.healthReason"
          |    },
          |    {
          |      "type": "lossOfStaff",
          |      "descriptionKey": "reasonableExcuses.lossOfStaffReason"
          |    },
          |    {
          |      "type": "technicalIssues",
          |      "descriptionKey": "reasonableExcuses.technicalIssuesReason"
          |    },
          |    {
          |      "type": "other",
          |      "descriptionKey": "reasonableExcuses.otherReason"
          |    }
          |  ]
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/reasonable-excuses").get())
      result.status shouldBe OK
      Json.parse(result.body) shouldBe jsonExpectedToReturn
    }
  }
}
