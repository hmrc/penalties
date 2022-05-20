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

package controllers

import featureSwitches.UseAPI1812Model
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import scala.concurrent.Future

class APIControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock {

  class Setup(isFSEnabled: Option[Boolean] = None) {
    val localApp: Application = {
      if (isFSEnabled.isDefined) {
        new GuiceApplicationBuilder()
          .configure(configForApp + (UseAPI1812Model.name -> isFSEnabled.get))
          .build()
      } else {
        app
      }
    }
    val controller: APIController = localApp.injector.instanceOf[APIController]
  }

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
      |			"period": [{
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"status": "OVERDUE"
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
      |  {
      |			"type": "financial",
      |			"number": "2",
      |     "appealStatus": "UNDER_REVIEW",
      |     "id": "123456790",
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
      |     "id": "123456789",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |     "reason": "reason",
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
    "call stub data when 1812 feature is disabled" must {
      s"return OK (${Status.OK})" when {
        "the ETMP call succeeds" in new Setup(isFSEnabled = Some(false)) {
          mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", body = Some(etmpPayloadAsJsonWithEstimatedLPP.toString()))
          val result: Future[Result] = controller.getSummaryDataForVRN("123456789")(FakeRequest())
          await(result).header.status shouldBe OK
          Json.parse(contentAsString(result)) shouldBe Json.parse(
            """
              |{
              |  "noOfPoints": 2,
              |  "noOfEstimatedPenalties": 1,
              |  "noOfCrystalisedPenalties": 2,
              |  "estimatedPenaltyAmount": 12,
              |  "crystalisedPenaltyAmountDue": 402,
              |  "hasAnyPenaltyData": true
              |}
              |""".stripMargin
          )
        }
      }

      s"return BAD_REQUEST (${Status.BAD_REQUEST})" when {
        "the user supplies an invalid VRN" in new Setup(isFSEnabled = Some(false)) {
          val result: Result = await(controller.getSummaryDataForVRN("123456789123456789")(FakeRequest()))
          result.header.status shouldBe BAD_REQUEST
        }
      }

      s"return NOT_FOUND (${Status.NOT_FOUND})" when {
        "the ETMP call fails" in new Setup(isFSEnabled = Some(false)) {
          mockResponseForStubETMPPayload(Status.INTERNAL_SERVER_ERROR, "HMRC-MTD-VAT~VRN~123456789", body = Some(""))
          val result: Result = await(controller.getSummaryDataForVRN("123456789")(FakeRequest()))
          result.header.status shouldBe NOT_FOUND
        }

        "the ETMP call returns nothing" in new Setup(isFSEnabled = Some(false)) {
          mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", body = Some("{}"))
          val result: Result = await(controller.getSummaryDataForVRN("123456789")(FakeRequest()))
          result.header.status shouldBe NOT_FOUND
        }
      }
    }

    "call API 1812 when call 1812 feature is enabled" must {
      val getPenaltyDetailsJson: JsValue = Json.parse(
        """
          |{
          | "totalisations": {
          |   "LSPTotalValue": 200,
          |   "penalisedPrincipalTotal": 2000,
          |   "LPPPostedTotal": 165.25,
          |   "LPPEstimatedTotal": 15.26,
          |   "LPIPostedTotal": 1968.2,
          |   "LPIEstimatedTotal": 7
          | },
          | "lateSubmissionPenalty": {
          |   "summary": {
          |     "activePenaltyPoints": 2,
          |     "inactivePenaltyPoints": 0,
          |     "regimeThreshold": 5,
          |     "penaltyChargeAmount": 200.00
          |   },
          |   "details": []
          | },
          | "latePaymentPenalty": {
          |     "details": [
          |       {
          |          "penaltyCategory": "LPP2",
          |          "penaltyStatus": "A",
          |          "penaltyAmountPaid": 44.21,
          |          "penaltyAmountOutstanding": 100,
          |          "LPP1LRCalculationAmount": 99.99,
          |          "LPP1LRDays": "15",
          |          "LPP1LRPercentage": 2.00,
          |          "LPP1HRCalculationAmount": 99.99,
          |          "LPP1HRDays": "31",
          |          "LPP1HRPercentage": 2.00,
          |          "LPP2Days": "31",
          |          "LPP2Percentage": 4.00,
          |          "penaltyChargeCreationDate": "2022-10-30",
          |          "communicationsDate": "2022-10-30",
          |          "penaltyChargeDueDate": "2022-10-30",
          |          "principalChargeReference": "1234567890",
          |          "principalChargeBillingFrom": "2022-10-30",
          |          "principalChargeBillingTo": "2022-10-30",
          |          "principalChargeDueDate": "2022-10-30"
          |       },
          |       {
          |          "penaltyCategory": "LPP2",
          |          "penaltyStatus": "A",
          |          "penaltyAmountPaid": 100.00,
          |          "penaltyAmountOutstanding": 23.45,
          |          "LPP1LRCalculationAmount": 99.99,
          |          "LPP1LRDays": "15",
          |          "LPP1LRPercentage": 2.00,
          |          "LPP1HRCalculationAmount": 99.99,
          |          "LPP1HRDays": "31",
          |          "LPP1HRPercentage": 2.00,
          |          "LPP2Days": "31",
          |          "LPP2Percentage": 4.00,
          |          "penaltyChargeCreationDate": "2022-10-30",
          |          "communicationsDate": "2022-10-30",
          |          "penaltyChargeDueDate": "2022-10-30",
          |          "principalChargeReference": "1234567890",
          |          "principalChargeBillingFrom": "2022-10-30",
          |          "principalChargeBillingTo": "2022-10-30",
          |          "principalChargeDueDate": "2022-10-30"
          |       },
          |       {
          |          "penaltyCategory": "LPP1",
          |          "penaltyStatus": "P",
          |          "penaltyAmountPaid": 0,
          |          "penaltyAmountOutstanding": 144.00,
          |          "LPP1LRCalculationAmount": 99.99,
          |          "LPP1LRDays": "15",
          |          "LPP1LRPercentage": 2.00,
          |          "LPP1HRCalculationAmount": 99.99,
          |          "LPP1HRDays": "31",
          |          "LPP1HRPercentage": 2.00,
          |          "LPP2Days": "31",
          |          "LPP2Percentage": 4.00,
          |          "penaltyChargeCreationDate": "2022-10-30",
          |          "communicationsDate": "2022-10-30",
          |          "penaltyChargeDueDate": "2022-10-30",
          |          "principalChargeReference": "1234567890",
          |          "principalChargeBillingFrom": "2022-10-30",
          |          "principalChargeBillingTo": "2022-10-30",
          |          "principalChargeDueDate": "2022-10-30"
          |       },
          |       {
          |          "penaltyCategory": "LPP1",
          |          "penaltyStatus": "P",
          |          "penaltyAmountPaid": 0,
          |          "penaltyAmountOutstanding": 144.00,
          |          "LPP1LRCalculationAmount": 99.99,
          |          "LPP1LRDays": "15",
          |          "LPP1LRPercentage": 2.00,
          |          "LPP1HRCalculationAmount": 99.99,
          |          "LPP1HRDays": "31",
          |          "LPP1HRPercentage": 2.00,
          |          "LPP2Days": "31",
          |          "LPP2Percentage": 4.00,
          |          "penaltyChargeCreationDate": "2022-10-30",
          |          "communicationsDate": "2022-10-30",
          |          "penaltyChargeDueDate": "2022-10-30",
          |          "principalChargeReference": "1234567890",
          |          "principalChargeBillingFrom": "2022-10-30",
          |          "principalChargeBillingTo": "2022-10-30",
          |          "principalChargeDueDate": "2022-10-30"
          |       }
          |   ]
          | }
          |}
          |""".stripMargin)

      s"return OK (${Status.OK})" when {
        "the get penalty details call succeeds" in new Setup(isFSEnabled = Some(true)) {
          mockStubResponseForGetPenaltyDetailsv3(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
          val result: Future[Result] = controller.getSummaryDataForVRN("123456789")(FakeRequest())
          await(result).header.status shouldBe OK
          Json.parse(contentAsString(result)) shouldBe Json.parse(
            """
              |{
              |  "noOfPoints": 2,
              |  "noOfEstimatedPenalties": 2,
              |  "noOfCrystalisedPenalties": 2,
              |  "estimatedPenaltyAmount": 123.45,
              |  "crystalisedPenaltyAmountDue": 288,
              |  "hasAnyPenaltyData": true
              |}
              |""".stripMargin
          )
        }
      }

      s"return BAD_REQUEST (${Status.BAD_REQUEST})" when {
        "the user supplies an invalid VRN" in new Setup(isFSEnabled = Some(true)) {
          val result: Result = await(controller.getSummaryDataForVRN("123456789123456789")(FakeRequest()))
          result.header.status shouldBe BAD_REQUEST
        }
      }

      s"return ISE (${Status.INTERNAL_SERVER_ERROR})" when {
        "the get penalty details call fails" in new Setup(isFSEnabled = Some(true)) {
          mockStubResponseForGetPenaltyDetailsv3(Status.INTERNAL_SERVER_ERROR, "123456789", body = Some(""))
          val result: Result = await(controller.getSummaryDataForVRN("123456789")(FakeRequest()))
          result.header.status shouldBe INTERNAL_SERVER_ERROR
        }
      }

      s"return NOT_FOUND (${Status.NOT_FOUND})" when {
        "the get penalty details call returns 404" in new Setup(isFSEnabled = Some(true)) {
          mockStubResponseForGetPenaltyDetailsv3(Status.NOT_FOUND, "123456789", body = Some(""))
          val result: Result = await(controller.getSummaryDataForVRN("123456789")(FakeRequest()))
          result.header.status shouldBe NOT_FOUND
        }
      }
    }
  }
}
