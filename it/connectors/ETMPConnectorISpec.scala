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

import connectors.parsers.ETMPPayloadParser.{ETMPPayloadResponse, GetETMPPayloadFailureResponse,
  GetETMPPayloadMalformed, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import featureSwitches.{CallETMP, FeatureSwitching}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import scala.concurrent.ExecutionContext

class ETMPConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val connector: ETMPConnector = injector.instanceOf[ETMPConnector]
  }

  val etmpPayloadWithLPP: JsValue = Json.parse(
    """
      |{
      |	"pointsTotal": 1,
      |	"lateSubmissions": 1,
      |	"adjustmentPointsTotal": 1,
      |	"fixedPenaltyAmount": 200,
      |	"penaltyAmountsTotal": 400.00,
      |	"penaltyPointsThreshold": 4,
      |	"penaltyPoints": [
      |		{
      |			"type": "financial",
      |			"number": "2",
      |     "id": "1235",
      |     "appealStatus": "UNDER_REVIEW",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": [{
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			}],
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
      |     "id": "1234",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": [{
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			}],
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
      |    {
      |      "type": "financial",
      |      "reason": "VAT_NOT_PAID_WITHIN_30_DAYS",
      |      "id": "1234567891",
      |      "dateCreated": "2021-04-23T18:25:43.511",
      |      "status": "DUE",
      |      "appealStatus": "UNDER_REVIEW",
      |      "period": {
      |        "startDate": "2021-04-23T18:25:43.511",
      |        "endDate": "2021-04-23T18:25:43.511",
      |        "dueDate": "2021-04-23T18:25:43.511",
      |		     "paymentStatus": "DUE"
      |      },
      |      "communications": [
      |        {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |        }
      |      ],
      |      "financial": {
      |        "amountDue": 400.00,
      |        "outstandingAmountDue": 0.00,
      |        "dueDate": "2021-04-23T18:25:43.511"
      |      }
      |    }
      | ]
      |}
      |""".stripMargin)

  val etmpPayloadWithLPPAndAdditionalPenalty: JsValue = Json.parse(
    """
      |{
      |	"pointsTotal": 1,
      |	"lateSubmissions": 1,
      |	"adjustmentPointsTotal": 1,
      |	"fixedPenaltyAmount": 200,
      |	"penaltyAmountsTotal": 400.00,
      |	"penaltyPointsThreshold": 4,
      |	"penaltyPoints": [
      |		{
      |			"type": "financial",
      |			"number": "2",
      |     "id": "1235",
      |     "appealStatus": "UNDER_REVIEW",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": [{
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			}],
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
      |     "id": "1234",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": [{
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			}],
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
      |      "type": "additional",
      |      "reason": "VAT_NOT_PAID_AFTER_30_DAYS",
      |      "id": "1234567892",
      |      "dateCreated": "2021-04-23T18:25:43.511",
      |      "status": "ACTIVE",
      |      "period": {
      |        "startDate": "2021-04-23T18:25:43.511",
      |        "endDate": "2021-04-23T18:25:43.511",
      |        "dueDate": "2021-04-23T18:25:43.511",
      |		     "paymentStatus": "PAID"
      |      },
      |      "communications": [
      |        {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |        }
      |      ],
      |      "financial": {
      |        "amountDue": 123.45,
      |        "outstandingAmountDue": 0.00,
      |        "dueDate": "2021-04-23T18:25:43.511"
      |      }
      |    },
      |    {
      |      "type": "financial",
      |      "reason": "VAT_NOT_PAID_WITHIN_30_DAYS",
      |      "id": "1234567891",
      |      "dateCreated": "2021-04-23T18:25:43.511",
      |      "status": "ACTIVE",
      |      "appealStatus": "UNDER_REVIEW",
      |      "period": {
      |        "startDate": "2021-04-23T18:25:43.511",
      |        "endDate": "2021-04-23T18:25:43.511",
      |        "dueDate": "2021-04-23T18:25:43.511",
      |		     "paymentStatus": "PAID",
      |        "paymentReceivedDate": "2021-04-23T18:25:43.511"
      |      },
      |      "communications": [
      |        {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |        }
      |      ],
      |      "financial": {
      |        "amountDue": 400.00,
      |        "outstandingAmountDue": 0.00,
      |        "dueDate": "2021-04-23T18:25:43.511"
      |      }
      |    }
      | ]
      |}
      |""".stripMargin)


  "getPenaltiesDataForEnrolmentKey" should {
    "call ETMP when the feature switch is enabled and handle a successful response" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.OK, "123456789")
      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetETMPPayloadSuccessResponse].etmpPayload shouldBe etmpPayloadModel
    }

    "call ETMP when the feature switch is enabled and handle a successful response (with LPP)" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.OK, "123456789", Some(etmpPayloadWithLPP.toString()))
      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetETMPPayloadSuccessResponse].etmpPayload shouldBe etmpPayloadModelWithLPP
    }

    "call ETMP when the feature switch is enabled and handle a successful response (with LPP and additional penalty)" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.OK, "123456789", Some(etmpPayloadWithLPPAndAdditionalPenalty.toString()))
      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetETMPPayloadSuccessResponse].etmpPayload shouldBe etmpPayloadModelWithLPPAndAdditionalPenalties
    }

    "call the stub when the feature switch is disabled and handle a successful response" in new Setup {
      disableFeatureSwitch(CallETMP)
      mockResponseForStubETMPPayload(Status.OK, "123456789")
      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetETMPPayloadSuccessResponse].etmpPayload shouldBe etmpPayloadModel
    }

    s"return a $GetETMPPayloadMalformed when the JSON is malformed" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.OK, "123456789", body = Some("{}"))
      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetETMPPayloadMalformed
    }

    s"return a $GetETMPPayloadNoContent when the response status is No Content (${Status.NO_CONTENT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.NO_CONTENT, "123456789", body = Some("{}"))
      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetETMPPayloadNoContent
    }

    s"return a $GetETMPPayloadFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.INTERNAL_SERVER_ERROR, "123456789")
      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetETMPPayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)
    }

    s"return a $GetETMPPayloadFailureResponse when the response status is unmatched i.e. Gateway Timeout (${Status.GATEWAY_TIMEOUT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.GATEWAY_TIMEOUT, "123456789")
      val result: ETMPPayloadResponse = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetETMPPayloadFailureResponse(Status.GATEWAY_TIMEOUT)
    }
  }
}
