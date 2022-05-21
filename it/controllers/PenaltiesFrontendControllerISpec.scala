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

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo}

import scala.collection.JavaConverters._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class PenaltiesFrontendControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock{
  val controller: PenaltiesFrontendController = injector.instanceOf[PenaltiesFrontendController]

  val etmpPayloadAsJsonWithNoPoints: JsValue = Json.parse(
    """
      |{
      |	"pointsTotal": 0,
      |	"lateSubmissions": 0,
      |	"adjustmentPointsTotal": 0,
      |	"fixedPenaltyAmount": 0,
      |	"penaltyAmountsTotal": 0,
      |	"penaltyPointsThreshold": 4,
      |	"penaltyPoints": []
      |}
      |""".stripMargin)

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
      |     "id": "1235",
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
      |	]
      |}
      |""".stripMargin)

  val etmpPayloadWithMultipleLSPInSameCalenderMonth: JsValue = Json.parse(
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
      |			"type": "financial",
      |			"number": "2",
      |     "id": "1235",
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
      |			"type": "financial",
      |			"number": "1",
      |     "id": "1234",
      |			"dateCreated": "2021-01-01T18:25:43.511",
      |			"dateExpired": "2021-01-01T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": [{
      |				"startDate": "2021-01-01T18:25:43.511",
      |				"endDate": "2021-01-15T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-05-07T18:25:43.511",
      |					"submittedDate": "2021-05-12T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			},
      |   {
      |				"startDate": "2021-01-16T18:25:43.511",
      |				"endDate": "2021-01-31T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-05-23T18:25:43.511",
      |					"submittedDate": "2021-05-25T18:25:43.511",
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
      |	]
      |}
      |""".stripMargin)

  val etmpPayloadWithMultipleLSPPInSameCalenderMonth: JsValue = Json.parse(
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
      |     "id": "1235",
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
      |     "id": "1234",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |     "period": [{
      |				"startDate": "2021-01-01T18:25:43.511",
      |				"endDate": "2021-01-15T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-05-07T18:25:43.511",
      |					"submittedDate": "2021-05-12T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			},
      |     {
      |				"startDate": "2021-01-16T18:25:43.511",
      |				"endDate": "2021-01-31T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-05-23T18:25:43.511",
      |					"submittedDate": "2021-05-25T18:25:43.511",
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
      |	]
      |}
      |""".stripMargin)

  "getPenaltiesData" should {
    "call stub data when 1812 feature is disabled" must {
      s"call out to ETMP and return OK (${Status.OK}) when successful" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789")
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe etmpPayloadAsJson.toString()
      }

      s"call out to ETMP and return OK (${Status.OK}) when successful for LPP" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some(Json.toJson(etmpPayloadModelWithLPP).toString))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe Json.toJson(etmpPayloadModelWithLPP).toString
      }

      s"call out to ETMP and return OK (${Status.OK}) when successful for LPP with additional penalties" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some(Json.toJson(etmpPayloadModelWithLPPAndAdditionalPenalties).toString))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe Json.toJson(etmpPayloadModelWithLPPAndAdditionalPenalties).toString
      }

      s"call out to ETMP and return OK (${Status.OK}) when successful with VAT overview section present" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some(Json.toJson(etmpPayloadModelWithVATOverview).toString))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe Json.toJson(etmpPayloadModelWithVATOverview).toString
      }

      s"call out to ETMP and return OK (${Status.OK}) when there is added points i.e. no period" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some(etmpPayloadAsJsonAddedPoint.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe etmpPayloadAsJsonAddedPoint.toString()
      }

      s"call out to ETMP and return OK (${Status.OK}) when there are multiple LSP periods in same calendar month" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some(etmpPayloadWithMultipleLSPInSameCalenderMonth.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe etmpPayloadWithMultipleLSPInSameCalenderMonth.toString()
      }

      s"call out to ETMP and return OK (${Status.OK}) when there are multiple LSPP periods in same calendar month" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some(etmpPayloadWithMultipleLSPPInSameCalenderMonth.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe etmpPayloadWithMultipleLSPPInSameCalenderMonth.toString()
      }

      s"call out to ETMP and return a Not Found (${Status.NOT_FOUND}) when NoContent is returned from the connector" in {
        mockResponseForStubETMPPayload(Status.NO_CONTENT, "123456789", body = Some(""))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.NOT_FOUND
      }

      s"call out to ETMP and return a ISE (${Status.INTERNAL_SERVER_ERROR}) when an issue has occurred i.e. invalid json response" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some("{}"))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.INTERNAL_SERVER_ERROR
        result.body shouldBe "Something went wrong."
      }

      "audit the response when the user has > 0 penalties" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789")
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe etmpPayloadAsJson.toString()
        wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe true
      }

      "NOT audit the response when the user has 0 penalties" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some(etmpPayloadAsJsonWithNoPoints.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789?newApiModel=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe etmpPayloadAsJsonWithNoPoints.toString()
        wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe false
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
      "the get penalty details call succeeds" in {
        mockStubResponseForGetPenaltyDetailsv3(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789?newApiModel=true").get)
        result.status shouldBe OK
      }
    }

    s"return BAD_REQUEST (${Status.NOT_FOUND})" when {
      "the user supplies an invalid VRN" in {
        mockStubResponseForGetPenaltyDetailsv3(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789123456789?newApiModel=true").get)
        result.status shouldBe NOT_FOUND
      }
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR})" when {
      "the get penalty details call fails" in {
        mockStubResponseForGetPenaltyDetailsv3(Status.INTERNAL_SERVER_ERROR, "123456789", body = Some(""))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789?newApiModel=true").get)
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "audit the response when the user has > 0 penalties" in {
      mockStubResponseForGetPenaltyDetailsv3(Status.OK, "123456789")
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789?newApiModel=true").get())
      result.status shouldBe Status.OK
      result.body shouldBe getPenaltyDetailsWithLSPandLPPAsJsonv3.toString()
      wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe true
    }

    "NOT audit the response when the user has 0 penalties" in {
      mockStubResponseForGetPenaltyDetailsv3(Status.OK, "123456789", body = Some(getPenaltyDetailsWithNoPointsAsJsonv3.toString()))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789?newApiModel=true").get())
      result.status shouldBe Status.OK
      result.body shouldBe getPenaltyDetailsWithNoPointsAsJsonv3.toString()
      wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe false
    }
  }
}
