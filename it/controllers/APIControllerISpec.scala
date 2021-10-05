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
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class APIControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val controller: APIController = injector.instanceOf[APIController]

  val etmpPayloadAsJsonWithEstimatedLPP: JsValue = Json.parse(
    """
        {
      |	"pointsTotal": 2,
      |	"lateSubmissions": 2,
      |	"adjustmentPointsTotal": 1,
      |	"fixedPenaltyAmount": 200,
      |	"penaltyAmountsTotal": 400.00,
      |	"penaltyPointsThreshold": 4,
      |	"penaltyPoints": [
      |		{
      |			"type": "financial",
      |			"number": "3",
      |     "appealStatus": "UNDER_REVIEW",
      |     "id": "123456791",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "DUE",
      |			"period": {
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"status": "OVERDUE"
      |				}
      |			},
      |			"communications": [
      |				{
      |					"type": "secureMessage",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			],
      |     "financial": {
      |        "amountDue": 400.00,
      |        "outstandingAmountDue": 400.00,
      |        "dueDate": "2021-04-23T18:25:43.511"
      |     }
      |		},
      |  {
      |			"type": "financial",
      |			"number": "2",
      |     "appealStatus": "UNDER_REVIEW",
      |     "id": "123456790",
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
      |					"type": "secureMessage",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			],
      |     "financial": {
      |        "amountDue": 400.00,
      |        "outstandingAmountDue": 400.00,
      |        "dueDate": "2021-04-23T18:25:43.511"
      |     }
      |		},
      |		{
      |			"type": "point",
      |			"number": "1",
      |     "id": "123456789",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |     "reason": "reason",
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
      |	],
      | "latePaymentPenalties": [
      |     {
      |       "type": "financial",
      |       "reason": "VAT_NOT_PAID_AFTER_30_DAYS",
      |       "id": "1234567893",
      |       "dateCreated": "2021-04-23T18:25:43.511",
      |       "status": "DUE",
      |       "period": {
      |         "startDate": "2021-04-23T18:25:43.511",
      |         "endDate": "2021-04-23T18:25:43.511",
      |         "dueDate": "2021-04-23T18:25:43.511",
      |	        "paymentStatus": "PAID"
      |       },
      |       "communications": [
      |         {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |         }
      |       ],
      |       "financial": {
      |         "amountDue": 400.00,
      |         "outstandingAmountDue": 2.00,
      |         "dueDate": "2021-04-23T18:25:43.511"
      |       }
      |     },
      |     {
      |       "type": "additional",
      |       "reason": "VAT_NOT_PAID_AFTER_30_DAYS",
      |       "id": "1234567892",
      |       "dateCreated": "2021-04-23T18:25:43.511",
      |       "status": "ESTIMATED",
      |       "period": {
      |         "startDate": "2021-04-23T18:25:43.511",
      |         "endDate": "2021-04-23T18:25:43.511",
      |         "dueDate": "2021-04-23T18:25:43.511",
      |	        "paymentStatus": "PAID"
      |       },
      |       "communications": [
      |         {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |         }
      |       ],
      |       "financial": {
      |         "amountDue": 23.45,
      |         "outstandingAmountDue": 12.00,
      |         "dueDate": "2021-04-23T18:25:43.511"
      |       }
      |     },
      |     {
      |       "type": "financial",
      |       "reason": "VAT_NOT_PAID_WITHIN_30_DAYS",
      |       "id": "1234567891",
      |       "dateCreated": "2021-04-23T18:25:43.511",
      |       "status": "ACTIVE",
      |       "period": {
      |         "startDate": "2021-04-23T18:25:43.511",
      |         "endDate": "2021-04-23T18:25:43.511",
      |         "dueDate": "2021-04-23T18:25:43.511",
      |	        "paymentStatus": "PAID"
      |       },
      |       "communications": [
      |       {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |        }
      |       ],
      |       "financial": {
      |         "amountDue": 400.00,
      |         "outstandingAmountDue": 2.00,
      |         "dueDate": "2021-04-23T18:25:43.511"
      |       }
      |    }
      |]
      |}
      |""".stripMargin)

  "getSummaryDataForVRN" should {
    s"return OK (${Status.OK})" when {
      "the ETMP call succeeds" in {
        mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", body = Some(etmpPayloadAsJsonWithEstimatedLPP.toString()))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.parse(
          """
            |{
            |  "noOfPoints": 2,
            |  "noOfEstimatedPenalties": 1,
            |  "noOfCrystalisedPenalties": 2,
            |  "estimatedPenaltyAmount": 12,
            |  "crystalisedPenaltyAmountDue": 800,
            |  "hasAnyPenaltyData": true
            |}
            |""".stripMargin
        )
      }
    }

    s"return BAD_REQUEST (${Status.BAD_REQUEST})" when {
      "the user supplies an invalid VRN" in {
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789123456789").get)
        result.status shouldBe BAD_REQUEST
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND})" when {
      "the ETMP call fails" in {
        mockResponseForStubETMPPayload(Status.INTERNAL_SERVER_ERROR, "HMRC-MTD-VAT~VRN~123456789", body = Some(""))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
        result.status shouldBe NOT_FOUND
      }

      "the ETMP call returns nothing" in {
        mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", body = Some("{}"))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
        result.status shouldBe NOT_FOUND
      }
    }
  }
}
