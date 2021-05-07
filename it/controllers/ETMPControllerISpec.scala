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

import config.AppConfig
import featureSwitches.{CallETMP, FeatureSwitch, FeatureSwitching}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class ETMPControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val controller = injector.instanceOf[ETMPController]

  val etmpPayloadAsJsonAddedPoint: JsValue = Json.parse(
    """
        {
      |	"pointsTotal": 1,
      |	"lateSubmissions": 1,
      |	"adjustmentPointsTotal": 1,
      |	"fixedPenaltyAmount": 200,
      |	"penaltyAmountsTotal": 400.00,
      |	"penaltyPointsThreshold": 4,
      |	"penaltyPoints": [
      |		{
      |			"type": "point",
      |			"number": "2",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ADDED",
      |			"communications": [
      |				{
      |					"type": "secureMessage",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			]
      |		},
      |		{
      |			"type": "point",
      |			"number": "1",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": {
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			},
      |			"communications": [
      |				{
      |					"type": "letter",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			]
      |		}
      |	]
      |}
      |""".stripMargin)

  "getPenaltiesData" should {
    s"call out to ETMP and return OK (${Status.OK}) when successful" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789")
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789").get())
      result.status shouldBe Status.OK
      result.body shouldBe etmpPayloadAsJson.toString()
    }

    s"call out to ETMP and return OK (${Status.OK}) when there is added points i.e. no period" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some(etmpPayloadAsJsonAddedPoint.toString()))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789").get())
      result.status shouldBe Status.OK
      result.body shouldBe etmpPayloadAsJsonAddedPoint.toString()
    }

    s"call out to ETMP and return a Not Found (${Status.NOT_FOUND}) when NoContent is returned from the connector" in {
      mockResponseForStubETMPPayload(Status.NO_CONTENT, "123456789", body = Some(""))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789").get())
      result.status shouldBe Status.NOT_FOUND
    }

    s"call out to ETMP and return a ISE (${Status.INTERNAL_SERVER_ERROR}) when an issue has occurred i.e. invalid json response" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some("{}"))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789").get())
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
      result.body shouldBe "Something went wrong."
    }
  }
}
