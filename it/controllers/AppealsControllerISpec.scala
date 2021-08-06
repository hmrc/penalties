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

  val lspAndLppBodyToReturnFromETMP: JsValue = Json.parse(
    """
      |{
      |		"pointsTotal" : 3,
      |		"lateSubmissions" : 3,
      |		"fixedPenaltyAmount" : 0,
      |		"penaltyPointsThreshold" : 4,
      |		"penaltyAmountsTotal" : 0,
      |		"adjustmentPointsTotal" : 0,
      |		"penaltyPoints" : [
      |						{
      |				"communications" : [
      |					{
      |						"type" : "letter",
      |						"documentId" : "1234567890",
      |						"dateSent" : "2023-08-09T18:25:43.511"
      |					}
      |				],
      |				"type" : "point",
      |				"number" : "3",
      |				"period" : {
      |					"startDate" : "2023-07-01T18:25:43.511",
      |					"submission" : {
      |						"dueDate" : "2023-11-07T18:25:43.511",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-11-15T18:25:43.511"
      |					},
      |					"endDate" : "2023-09-30T18:25:43.511"
      |				},
      |				"status" : "ACTIVE",
      |				"dateExpired" : "2025-11-07T18:25:43.511",
      |				"dateCreated" : "2023-11-07T18:25:43.511",
      |				"id" : "1234567893"
      |			},
      |			{
      |				"communications" : [
      |					{
      |						"type" : "letter",
      |						"documentId" : "1234567890",
      |						"dateSent" : "2023-08-09T18:25:43.511"
      |					}
      |				],
      |				"type" : "point",
      |				"number" : "2",
      |				"period" : {
      |					"startDate" : "2023-04-01T18:25:43.511",
      |					"submission" : {
      |						"dueDate" : "2023-08-07T18:25:43.511",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-08-15T18:25:43.511"
      |					},
      |					"endDate" : "2023-06-30T18:25:43.511"
      |				},
      |				"status" : "ACTIVE",
      |				"dateExpired" : "2025-08-07T18:25:43.511",
      |				"dateCreated" : "2023-08-07T18:25:43.511",
      |				"id" : "1234567892"
      |			},
      |			{
      |				"type" : "point",
      |				"communications" : [
      |					{
      |						"type" : "letter",
      |						"documentId" : "1234567890",
      |						"dateSent" : "2021-05-08T18:25:43.511"
      |					}
      |				],
      |				"number" : "1",
      |				"period" : {
      |					"startDate" : "2023-01-01T18:25:43.511",
      |					"submission" : {
      |						"dueDate" : "2023-05-07T18:25:43.511",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-05-12T18:25:43.511"
      |					},
      |					"endDate" : "2023-03-31T18:25:43.511"
      |				},
      |				"status" : "ACTIVE",
      |				"dateCreated" : "2023-05-08T18:25:43.511",
      |				"dateExpired" : "2025-05-08T18:25:43.511",
      |				"id" : "1234567891"
      |			}
      |		],
      |		"latePaymentPenalties": [
      |			{
      |				"type": "financial",
      |				"id" : "1234",
      |				"reason": "",
      |				"dateCreated": "2023-01-01T18:25:43.511",
      |				"status": "DUE",
      |				"period": {
      |					"startDate": "2023-01-01T18:25:43.511",
      |					"endDate" : "2023-03-31T18:25:43.511",
      |					"dueDate" : "2023-05-07T18:25:43.511",
      |					"paymentStatus": "PAID"
      |				},
      |				"communications": [
      |					{
      |						"type" : "letter",
      |						"documentId" : "1234567890",
      |						"dateSent" : "2021-05-08T18:25:43.511"
      |					}
      |				],
      |				"financial": {
      |					"amountDue": 144.21,
      |					"outstandingAmountDue": 144.21,
      |					"dueDate": "2023-05-07T18:25:43.511"
      |				}
      |			}
      |		]
      |	}
      |""".stripMargin)

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

  val appealJsonLPP: JsValue = Json.parse(
    """
      |{
      |  "type": "LATE_PAYMENT",
      |	 "startDate": "2023-01-01T18:25:43.511",
      |	 "endDate" : "2023-03-31T18:25:43.511",
      |	 "dueDate" : "2023-05-07T18:25:43.511",
      |  "dateCommunicationSent": "2021-05-08T18:25:43.511"
      |}
      |""".stripMargin)

  val appealJsonLPPAdditional: JsValue = Json.parse(
    """
      |{
      |  "type": "ADDITIONAL",
      |	 "startDate": "2023-01-01T18:25:43.511",
      |	 "endDate" : "2023-03-31T18:25:43.511",
      |	 "dueDate" : "2023-05-07T18:25:43.511",
      |  "dateCommunicationSent": "2021-05-08T18:25:43.511"
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

  "getAppealsDataForLatePaymentPenalty" should {
    "call ETMP and compare the penalty ID provided and the penalty ID in the payload - return OK if there is a match" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lspAndLppBodyToReturnFromETMP.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-payments?penaltyId=1234&enrolmentKey=123456789&isAdditional=false").get())
      result.status shouldBe Status.OK
      result.body shouldBe appealJsonLPP.toString()
    }

    "call ETMP and compare the penalty ID provided and the penalty ID in the payload for Additional- return OK if there is a match" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lspAndLppBodyToReturnFromETMP.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-payments?penaltyId=1234&enrolmentKey=123456789&isAdditional=true").get())
      result.status shouldBe Status.OK
      result.body shouldBe appealJsonLPPAdditional.toString()
    }

    "return NOT_FOUND when the penalty ID given does not match the penalty ID in the payload" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lspAndLppBodyToReturnFromETMP.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-payments?penaltyId=0001&enrolmentKey=123456789&isAdditional=false").get())
      result.status shouldBe Status.NOT_FOUND
    }

    "return an ISE when the call to ETMP fails" in {
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-payments?penaltyId=0001&enrolmentKey=123456789&isAdditional=false").get())
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
    "call the connector and send the appeal data received in the request body - returns OK when successful for bereavement" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789")
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "submittedBy": "client",
          |    "penaltyId": "1234567890",
          |    "reasonableExcuse": "bereavement",
          |    "honestyDeclaration": true,
          |    "appealInformation": {
          |						"type": "bereavement",
          |           "dateOfEvent": "2021-04-23T18:25:43.511Z",
          |						"statement": "This is a statement",
          |           "lateAppeal": false
          |		}
          |}
          |""".stripMargin
      )
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }
    "call the connector and send the appeal data received in the request body - returns OK when successful for crime" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789")
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
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for fire or flood" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789")
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
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for loss of staff" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789")
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
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for technical issues" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789")
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
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for health" when {
      "there has been no hospital stay" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789")
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
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "there is an ongoing hospital stay" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789")
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
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "there has been a hospital stay" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789")
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
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "call the connector and send the appeal data received in the request body - returns OK when successful for LPP" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", true)
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
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }
    }

    "return BAD_REQUEST (400)" when {
      "no JSON body is in the request" in {
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789").post(
          ""
        ))
        result.status shouldBe BAD_REQUEST
      }

      "JSON body is present but it can not be parsed to a model" in {
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789").post(
          Json.parse("{}")
        ))
        result.status shouldBe BAD_REQUEST
      }
    }

    "return ISE (500)" when {
      "the call to ETMP/stub fails" in {
        mockResponseForAppealSubmissionStub(GATEWAY_TIMEOUT, "HMRC-MTD-VAT~VRN~123456789")
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
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false").post(
          jsonToSubmit
        ))
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "getIsMultiplePenaltiesInSamePeriod" should {
    val lspAndLPPInSamePeriod: JsValue = Json.parse(
      """
        |{
        |		"pointsTotal" : 1,
        |		"lateSubmissions" : 1,
        |		"adjustmentPointsTotal" : 0,
        |		"fixedPenaltyAmount" : 0,
        |		"penaltyAmountsTotal" : 0,
        |		"penaltyPointsThreshold" : 4,
        |		"penaltyPoints" : [
        |			{
        |				"type" : "point",
        |				"number" : "1",
        |				"id" : "1234567891",
        |				"dateCreated" : "2023-05-08T18:25:43.511Z",
        |				"dateExpired" : "2025-05-08T18:25:43.511Z",
        |				"status" : "ACTIVE",
        |				"period" : {
        |					"startDate" : "2023-01-01T18:25:43.511Z",
        |					"submission" : {
        |						"dueDate" : "2023-05-07T18:25:43.511Z",
        |						"status" : "SUBMITTED",
        |						"submittedDate" : "2023-05-12T18:25:43.511Z"
        |					},
        |					"endDate" : "2023-03-31T18:25:43.511Z"
        |				},
        |				"communications" : [
        |					{
        |						"type" : "letter",
        |						"documentId" : "1234567890",
        |						"dateSent" : "2021-05-08T18:25:43.511Z"
        |					}
        |				]
        |			}
        |		],
        |		"latePaymentPenalties" : [
        |			{
        |				"type" : "financial",
        |				"id" : "1234567901",
        |				"reason" : "",
        |				"dateCreated" : "2023-01-01T18:25:43.511Z",
        |				"status" : "DUE",
        |				"period" : {
        |					"startDate" : "2023-01-01T18:25:43.511Z",
        |					"endDate" : "2023-03-31T18:25:43.511Z",
        |					"dueDate" : "2023-05-07T10:00:00.010Z",
        |					"paymentStatus" : "PAID"
        |				},
        |				"communications" : [
        |					{
        |						"type" : "letter",
        |						"documentId" : "1234567890",
        |						"dateSent" : "2021-05-08T18:25:43.511Z"
        |					}
        |				],
        |				"financial" : {
        |					"amountDue" : 144.21,
        |					"outstandingAmountDue" : 144.21,
        |					"dueDate" : "2023-05-07T18:25:43.511Z"
        |				}
        |			}
        |		]
        |	}
        |""".stripMargin)

    val lppAndLSPInDifferentPeriod: JsValue = Json.parse(
      """
        |{
        |		"pointsTotal" : 1,
        |		"lateSubmissions" : 1,
        |		"adjustmentPointsTotal" : 0,
        |		"fixedPenaltyAmount" : 0,
        |		"penaltyAmountsTotal" : 0,
        |		"penaltyPointsThreshold" : 4,
        |		"penaltyPoints" : [
        |			{
        |				"type" : "point",
        |				"number" : "1",
        |				"id" : "1234567891",
        |				"dateCreated" : "2023-08-08T18:25:43.511Z",
        |				"dateExpired" : "2025-08-08T18:25:43.511Z",
        |				"status" : "ACTIVE",
        |				"period" : {
        |					"startDate" : "2023-04-01T18:25:43.511Z",
        |					"submission" : {
        |						"dueDate" : "2023-08-07T18:25:43.511Z",
        |						"status" : "SUBMITTED",
        |						"submittedDate" : "2023-08-12T18:25:43.511Z"
        |					},
        |					"endDate" : "2023-06-30T18:25:43.511Z"
        |				},
        |				"communications" : [
        |					{
        |						"type" : "letter",
        |						"documentId" : "1234567890",
        |						"dateSent" : "2021-08-08T18:25:43.511Z"
        |					}
        |				]
        |			}
        |		],
        |		"latePaymentPenalties" : [
        |			{
        |				"type" : "financial",
        |				"id" : "1234567901",
        |				"reason" : "",
        |				"dateCreated" : "2023-01-01T18:25:43.511Z",
        |				"status" : "DUE",
        |				"period" : {
        |					"startDate" : "2023-01-01T18:25:43.511Z",
        |					"endDate" : "2023-03-31T18:25:43.511Z",
        |					"dueDate" : "2023-05-07T10:00:00.010Z",
        |					"paymentStatus" : "PAID"
        |				},
        |				"communications" : [
        |					{
        |						"type" : "letter",
        |						"documentId" : "1234567890",
        |						"dateSent" : "2021-05-08T18:25:43.511Z"
        |					}
        |				],
        |				"financial" : {
        |					"amountDue" : 144.21,
        |					"outstandingAmountDue" : 144.21,
        |					"dueDate" : "2023-05-07T18:25:43.511Z"
        |				}
        |			}
        |		]
        |	}
        |""".stripMargin)
    "return OK" when {
      "the service returns true" in {
        mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", Some(lspAndLPPInSamePeriod.toString()))
        val result = await(buildClientForRequestToApp(uri = "/appeals/multiple-penalties-in-same-period?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true&penaltyId=1234567901").get())
        result.status shouldBe OK
      }
    }

    "return NO CONTENT" when {
      "the service returns false - no other periods" in {
        mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", Some(lppAndLSPInDifferentPeriod.toString()))
        val result = await(buildClientForRequestToApp(uri = "/appeals/multiple-penalties-in-same-period?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true&penaltyId=1234567901").get())
        result.status shouldBe NO_CONTENT
      }

      "the service returns false - something went wrong" in {
        mockResponseForStubETMPPayload(Status.INTERNAL_SERVER_ERROR, "HMRC-MTD-VAT~VRN~123456789", None)
        val result = await(buildClientForRequestToApp(uri = "/appeals/multiple-penalties-in-same-period?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true&penaltyId=1234567901").get())
        result.status shouldBe NO_CONTENT
      }
    }
  }
}
