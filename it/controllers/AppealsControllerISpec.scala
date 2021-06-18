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
import utils.{AppealWiremock, ETMPWiremock, IntegrationSpecCommonBase}
import play.api.test.Helpers._

class AppealsControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock with AppealWiremock {
  val controller: AppealsController = injector.instanceOf[AppealsController]

  val appealJson: JsValue = Json.parse(
    """
      |{
      |  "type": "LATE_SUBMISSION",
      |  "startDate": "2021-04-23T18:25:43.511",
      |  "endDate": "2021-04-23T18:25:43.511",
      |  "dueDate": "2021-04-23T18:25:43.511",
      |  "dateCommunicationSent": "2021-04-23T18:25:43.511"
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

  "submitAppeal" should {
    "call the connector and send the appeal data received in the request body - returns OK when successful for crime" in {
      mockResponseForAppealSubmissionStub(OK)
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "submittedBy": "client",
          |    "penaltyId": "1234567890",
          |    "reasonableExcuse": "crime",
          |    "honestyDeclaration": true,
          |    "appealInformation": {
          |						"type": "crime",
          |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
          |            "reportedIssue": true,
          |						 "statement": "This is a statement",
          |            "lateAppeal": false
          |		}
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for fire or flood" in {
      mockResponseForAppealSubmissionStub(OK)
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "submittedBy": "client",
          |    "penaltyId": "1234567890",
          |    "reasonableExcuse": "fireOrFlood",
          |    "honestyDeclaration": true,
          |    "appealInformation": {
          |          "type": "fireOrFlood",
          |          "dateOfEvent": "2021-04-23T18:25:43.511Z",
          |          "statement": "This is a statement",
          |          "lateAppeal": false
          |    }
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for loss of staff" in {
      mockResponseForAppealSubmissionStub(OK)
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "submittedBy": "client",
          |    "penaltyId": "1234567890",
          |    "reasonableExcuse": "lossOfStaff",
          |    "honestyDeclaration": true,
          |    "appealInformation": {
          |						"type": "lossOfStaff",
          |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
          |						 "statement": "This is a statement",
          |            "lateAppeal": false
          |		}
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for technical issues" in {
      mockResponseForAppealSubmissionStub(OK)
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "submittedBy": "client",
          |    "penaltyId": "1234567890",
          |    "reasonableExcuse": "technicalIssues",
          |    "honestyDeclaration": true,
          |    "appealInformation": {
          |						"type": "technicalIssues",
          |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
          |            "endDateOfEvent": "2021-04-24T18:25:43.511Z",
          |						 "statement": "This is a statement",
          |            "lateAppeal": false
          |		}
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for health" when {
      "there has been no hospital stay" in {
        mockResponseForAppealSubmissionStub(OK)
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "submittedBy": "client",
            |    "penaltyId": "1234567890",
            |    "reasonableExcuse": "health",
            |    "honestyDeclaration": true,
            |    "appealInformation": {
            |						 "type": "health",
            |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "hospitalStayInvolved": false,
            |            "eventOngoing": false,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "there is an ongoing hospital stay" in {
        mockResponseForAppealSubmissionStub(OK)
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "submittedBy": "client",
            |    "penaltyId": "1234567890",
            |    "reasonableExcuse": "health",
            |    "honestyDeclaration": true,
            |    "appealInformation": {
            |						 "type": "health",
            |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "hospitalStayInvolved": true,
            |            "eventOngoing": true,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "there has been a hospital stay" in {
        mockResponseForAppealSubmissionStub(OK)
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "submittedBy": "client",
            |    "penaltyId": "1234567890",
            |    "reasonableExcuse": "health",
            |    "honestyDeclaration": true,
            |    "appealInformation": {
            |						 "type": "health",
            |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "endDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "hospitalStayInvolved": true,
            |            "eventOngoing": false,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }
    }

    "return BAD_REQUEST (400)" when {
      "no JSON body is in the request" in {
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
          ""
        ))
        result.status shouldBe BAD_REQUEST
      }

      "JSON body is present but it can not be parsed to a model" in {
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
          Json.parse("{}")
        ))
        result.status shouldBe BAD_REQUEST
      }
    }

    "return ISE (500)" when {
      "the call to ETMP/stub fails" in {
        mockResponseForAppealSubmissionStub(GATEWAY_TIMEOUT)
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "submittedBy": "client",
            |    "penaltyId": "1234567890",
            |    "reasonableExcuse": "crime",
            |    "honestyDeclaration": true,
            |    "appealInformation": {
            |						"type": "crime",
            |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "reportedIssue": true,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal").post(
          jsonToSubmit
        ))
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
