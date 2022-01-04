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

package services

import connectors.parsers.ETMPPayloadParser.{GetETMPPayloadFailureResponse, GetETMPPayloadMalformed, GetETMPPayloadNoContent}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class ETMPServiceISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val service: ETMPService = injector.instanceOf[ETMPService]
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
      |				"period" : [{
      |					"startDate" : "2023-01-01T18:25:43.511Z",
      |					"submission" : {
      |						"dueDate" : "2023-05-07T18:25:43.511Z",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-05-12T18:25:43.511Z"
      |					},
      |					"endDate" : "2023-03-31T18:25:43.511Z"
      |				}],
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
      |				"reason" : "VAT_NOT_PAID_WITHIN_30_DAYS",
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

  val twoLPPInSamePeriod: JsValue = Json.parse(
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
      |				"dateCreated" : "2022-05-08T18:25:43.511Z",
      |				"dateExpired" : "2024-05-08T18:25:43.511Z",
      |				"status" : "ACTIVE",
      |				"period" : [{
      |					"startDate" : "2022-01-01T18:25:43.511Z",
      |					"submission" : {
      |						"dueDate" : "2022-05-07T18:25:43.511Z",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2022-05-12T18:25:43.511Z"
      |					},
      |					"endDate" : "2022-03-31T18:25:43.511Z"
      |				}],
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
      |  {
      |				"type" : "financial",
      |				"id" : "1234567902",
      |				"reason" : "VAT_NOT_PAID_WITHIN_30_DAYS",
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
      |			},
      |			{
      |				"type" : "financial",
      |				"id" : "1234567901",
      |				"reason" : "VAT_NOT_PAID_WITHIN_30_DAYS",
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
      |				"period" : [{
      |					"startDate" : "2023-04-01T18:25:43.511Z",
      |					"submission" : {
      |						"dueDate" : "2023-08-07T18:25:43.511Z",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-08-12T18:25:43.511Z"
      |					},
      |					"endDate" : "2023-06-30T18:25:43.511Z"
      |				}],
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

  val multipleLSPInSameCalenderMonth: JsValue = Json.parse(
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
      |				"type" : "financial",
      |				"number" : "1",
      |				"id" : "1234567891",
      |				"dateCreated" : "2023-08-08T18:25:43.511Z",
      |				"dateExpired" : "2025-08-08T18:25:43.511Z",
      |				"status" : "ACTIVE",
      |				"period" : [{
      |					"startDate" : "2023-04-01T18:25:43.511Z",
      |					"submission" : {
      |						"dueDate" : "2023-08-07T18:25:43.511Z",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-08-12T18:25:43.511Z"
      |					},
      |					"endDate" : "2023-04-16T18:25:43.511Z"
      |				},
      |       {
      |					"startDate" : "2023-04-15T18:25:43.511Z",
      |					"submission" : {
      |						"dueDate" : "2023-08-23T18:25:43.511Z",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-08-25T18:25:43.511Z"
      |					},
      |					"endDate" : "2023-04-31T18:25:43.511Z"
      |				}],
      |				"communications" : [
      |					{
      |						"type" : "letter",
      |						"documentId" : "1234567890",
      |						"dateSent" : "2021-08-08T18:25:43.511Z"
      |					}
      |				]
      |			}
      |		],
      |		"latePaymentPenalties" : []
      |	}
      |""".stripMargin)

  val multipleLSPPInSameCalenderMonth: JsValue = Json.parse(
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
      |				"period" : [{
      |					"startDate" : "2023-05-01T18:25:43.511Z",
      |					"submission" : {
      |						"dueDate" : "2023-09-07T18:25:43.511Z",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-09-12T18:25:43.511Z"
      |					},
      |					"endDate" : "2023-05-16T18:25:43.511Z"
      |				},
      |       {
      |					"startDate" : "2023-05-15T18:25:43.511Z",
      |					"submission" : {
      |						"dueDate" : "2023-09-23T18:25:43.511Z",
      |						"status" : "SUBMITTED",
      |						"submittedDate" : "2023-09-25T18:25:43.511Z"
      |					},
      |					"endDate" : "2023-05-31T18:25:43.511Z"
      |				}],
      |				"communications" : [
      |					{
      |						"type" : "letter",
      |						"documentId" : "1234567890",
      |						"dateSent" : "2021-08-08T18:25:43.511Z"
      |					}
      |				]
      |			}
      |		],
      |		"latePaymentPenalties" : []
      |	}
      |""".stripMargin)

  "getPenaltyDataFromETMPForEnrolment" should {
    s"call the connector and return a tuple - first is the $Some result and second is the parser result - successful result" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789")
      val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe true
      result._2.isRight shouldBe true
      result._1.get shouldBe etmpPayloadModel
    }

    s"call the connector and return a $None" when {
      s"the response is No Content (${Status.NO_CONTENT}) - second tuple value: $GetETMPPayloadNoContent" in {
        mockResponseForStubETMPPayload(Status.NO_CONTENT, "123456789")
        val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
        result._1.isDefined shouldBe false
        result._2.isLeft shouldBe true
        result._2.left.get shouldBe GetETMPPayloadNoContent
      }

      s"the response body is not well formed - second tuple value: $GetETMPPayloadMalformed" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some("""{ "this": "is amazing json" }"""))
        val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
        result._1.isDefined shouldBe false
        result._2.isLeft shouldBe true
        result._2.left.get shouldBe GetETMPPayloadMalformed
      }

      s"an unknown response is returned from the connector - second tuple value: $GetETMPPayloadFailureResponse" in {
        mockResponseForStubETMPPayload(Status.IM_A_TEAPOT, "123456789")
        val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
        result._1.isDefined shouldBe false
        result._2.isLeft shouldBe true
        result._2.left.get shouldBe GetETMPPayloadFailureResponse(Status.IM_A_TEAPOT)
      }
    }
  }
  
  "isMultiplePenaltiesInSamePeriod" should {
    "return true" when {
      "there is a LPP in the same period as the LSP" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lspAndLPPInSamePeriod.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567891", "123456789", isLPP = false)
        await(result) shouldBe true
      }

      "there is a LSP in the same period as the LPP" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lspAndLPPInSamePeriod.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567901", "123456789", isLPP = true)
        await(result) shouldBe true
      }

      "there is another LPP in the same period as another LPP" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(twoLPPInSamePeriod.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567902", "123456789", isLPP = true)
        await(result) shouldBe true
      }
    }

    "return false" when {
      "the penalty is a LPP not LSP - LSP penalty ID provided for LPP check" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lspAndLPPInSamePeriod.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567891", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "the penalty is a LSP not LPP - LPP penalty ID provided for LSP check" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lspAndLPPInSamePeriod.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567901", "123456789", isLPP = false)
        await(result) shouldBe false
      }

      "the penalty is not in the payload" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lspAndLPPInSamePeriod.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "there is no matching period in the payload - when called with LPP" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lppAndLSPInDifferentPeriod.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567901", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "there is no matching period in the payload - when called with LSP" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(lppAndLSPInDifferentPeriod.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567891", "123456789", isLPP = false)
        await(result) shouldBe false
      }

      "the call to retrieve penalty data fails" in {
        mockResponseForStubETMPPayload(Status.IM_A_TEAPOT, "123456789")
        val result = service.isMultiplePenaltiesInSamePeriod("1234", "123456789", isLPP = true)
        await(result) shouldBe false
      }


      "there is multiple LSP in the same calendar month" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(multipleLSPInSameCalenderMonth.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567902", "123456789", isLPP = false)
        await(result) shouldBe false
      }

      "there is multiple LSPP in the same calendar month" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", Some(multipleLSPPInSameCalenderMonth.toString()))
        val result = service.isMultiplePenaltiesInSamePeriod("1234567902", "123456789", isLPP = false)
        await(result) shouldBe false
      }
    }
  }
}
