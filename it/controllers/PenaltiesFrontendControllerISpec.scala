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

import java.time.LocalDate

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
        |     "activePenaltyPoints": 0,
        |     "inactivePenaltyPoints": 0,
        |     "regimeThreshold": 5,
        |     "penaltyChargeAmount": 200.00
        |   },
        |   "details": []
        | },
        | "latePaymentPenalty": {
        |     "details": [
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

    val getFinancialDetailsJson: JsValue = Json.parse(
      """
        |{
        | "documentDetails": [],
        | "financialDetails": [
        | {
        |   "taxYear": "2022",
        |   "documentId": "DOC1234",
        |   "chargeType": "1234",
        |   "mainType": "1234",
        |   "periodKey": "123",
        |   "periodKeyDescription": "foobar",
        |   "taxPeriodFrom": "2022-01-01",
        |   "taxPeriodTo": "2022-03-31",
        |   "businessPartner": "123",
        |   "contractAccountCategory": "1",
        |   "contractAccount": "1",
        |   "contractObjectType": "1",
        |   "contractObject": "1",
        |   "sapDocumentNumber": "1",
        |   "sapDocumentNumberItem": "1",
        |   "chargeReference": "1234567890",
        |   "mainTransaction": "4703",
        |   "subTransaction": "1",
        |   "originalAmount": 123.45,
        |   "outstandingAmount": 123.45,
        |   "clearedAmount": 123.45,
        |   "accruedInterest": 123.45,
        |   "items": [{
        |     "subItem": "001",
        |     "dueDate": "2018-08-13",
        |     "amount": 10000,
        |     "clearingDate": "2018-08-13",
        |     "clearingReason": "01",
        |     "outgoingPaymentMethod": "outgoing payment",
        |     "paymentLock": "paymentLock",
        |     "clearingLock": "clearingLock",
        |     "interestLock": "interestLock",
        |     "dunningLock": "dunningLock",
        |     "returnFlag": true,
        |     "paymentReference": "Ab12453535",
        |     "paymentAmount": 10000,
        |     "paymentMethod": "Payment",
        |     "paymentLot": "081203010024",
        |     "paymentLotItem": "000001",
        |     "clearingSAPDocument": "3350000253",
        |     "codingInitiationDate": "2021-01-11",
        |     "statisticalDocument": "S",
        |     "returnReason": "ABCA",
        |     "DDCollectionInProgress": true,
        |     "promisetoPay": "Y"
        |   }]
        | }
        | ]
        |}
        |""".stripMargin)

    val combinedPenaltyAndFinancialData: JsValue = Json.parse(
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
        |     "activePenaltyPoints": 0,
        |     "inactivePenaltyPoints": 0,
        |     "regimeThreshold": 5,
        |     "penaltyChargeAmount": 200.00
        |   },
        |   "details": []
        | },
        | "latePaymentPenalty": {
        |     "details": [
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
        |          "principalChargeDueDate": "2022-10-30",
        |          "mainTransaction": "4703",
        |          "outstandingAmount": 123.45
        |       }
        |   ]
        | }
        |}
        |""".stripMargin
    )

    s"return OK (${Status.OK})" when {

      "the get penalty details call succeeds and the get financial details call succeeds (combining the data together)" in {
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
        mockStubResponseForGetFinancialDetails(Status.OK,
          s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
          s"&onlyOpenItems=false&includeStatistical=true&includeLocks=false" +
          s"&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", Some(getFinancialDetailsJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
        result.status shouldBe OK
        Json.parse(result.body) shouldBe combinedPenaltyAndFinancialData
      }

      s"the get penalty details call succeeds and the get financial details call returns NO_CONTENT (${Status.NO_CONTENT}) (returning penalty details unaltered)" in {
        val getPenaltyDetailsNoLPPJson: JsValue = Json.parse(
          """
            |{
            | "lateSubmissionPenalty": {
            |   "summary": {
            |     "activePenaltyPoints": 0,
            |     "inactivePenaltyPoints": 0,
            |     "regimeThreshold": 5,
            |     "penaltyChargeAmount": 200.00
            |   },
            |   "details": []
            | },
            | "latePaymentPenalty": {
            |
            | }
            |}
            |""".stripMargin)
        val noDataFoundBody =
          """
            |{
            | "failures": [
            |   {
            |     "code": "NO_DATA_FOUND",
            |     "reason": "This is a reason"
            |   }
            | ]
            |}
            |""".stripMargin
        mockStubResponseForGetFinancialDetails(Status.NOT_FOUND,
          s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
            s"&onlyOpenItems=false&includeStatistical=true&includeLocks=false" +
            s"&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", Some(noDataFoundBody))
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsNoLPPJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
        result.status shouldBe OK
        Json.parse(result.body) shouldBe getPenaltyDetailsNoLPPJson
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND})" when {
      "the user supplies an invalid VRN" in {
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789123456789").get)
        result.status shouldBe NOT_FOUND
      }
    }

    s"return NO_CONTENT (${Status.NO_CONTENT})" when {
      "the get penalty details call returns 404 with NO_DATA_FOUND in body" in {
        val noDataFoundBody =
          """
            |{
            | "failures": [
            |   {
            |     "code": "NO_DATA_FOUND",
            |     "reason": "This is a reason"
            |   }
            | ]
            |}
            |""".stripMargin
        mockStubResponseForGetPenaltyDetails(Status.NOT_FOUND, "123456789", body = Some(noDataFoundBody))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
        result.status shouldBe NO_CONTENT
      }

      "the get financial details call returns 404 with NO_DATA_FOUND in body" in {
        val noDataFoundBody =
          """
            |{
            | "failures": [
            |   {
            |     "code": "NO_DATA_FOUND",
            |     "reason": "This is a reason"
            |   }
            | ]
            |}
            |""".stripMargin
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
        mockStubResponseForGetFinancialDetails(Status.NOT_FOUND,
          s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
            s"&onlyOpenItems=false&includeStatistical=true&includeLocks=false" +
            s"&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", Some(noDataFoundBody))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
        result.status shouldBe NO_CONTENT
      }
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR})" when {
      "the get penalty details call fails" in {
        mockStubResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, "123456789", body = Some(""))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "the get financial details call fails" in {
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
        mockStubResponseForGetFinancialDetails(Status.INTERNAL_SERVER_ERROR,
          s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
            s"&onlyOpenItems=false&includeStatistical=true&includeLocks=false" +
            s"&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", Some(getFinancialDetailsJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "audit the response when the user has > 0 penalties" in {
      val penaltyDetailsWithLSPandLPPAndFinancialDetails: JsValue = Json.parse(
        """
          |{
          |   "totalisations":{
          |      "LSPTotalValue":200,
          |      "penalisedPrincipalTotal":2000,
          |      "LPPPostedTotal":165.25,
          |      "LPPEstimatedTotal":15.26,
          |      "LPIPostedTotal":1968.2,
          |      "LPIEstimatedTotal":7
          |   },
          |   "lateSubmissionPenalty":{
          |      "summary":{
          |         "activePenaltyPoints":10,
          |         "inactivePenaltyPoints":12,
          |         "regimeThreshold":10,
          |         "penaltyChargeAmount":684.25
          |      },
          |      "details":[
          |         {
          |            "penaltyNumber":"12345678901234",
          |            "penaltyOrder":"01",
          |            "penaltyCategory":"P",
          |            "penaltyStatus":"ACTIVE",
          |            "penaltyCreationDate":"2022-10-30",
          |            "penaltyExpiryDate":"2022-10-30",
          |            "communicationsDate":"2022-10-30",
          |            "FAPIndicator":"X",
          |            "lateSubmissions":[
          |               {
          |                  "taxPeriodStartDate":"2022-01-01",
          |                  "taxPeriodEndDate":"2022-12-31",
          |                  "taxPeriodDueDate":"2023-02-07",
          |                  "returnReceiptDate":"2023-02-01",
          |                  "taxReturnStatus":"Fulfilled"
          |               }
          |            ],
          |            "expiryReason":"FAP",
          |            "appealInformation":[
          |               {
          |                  "appealStatus":"99",
          |                  "appealLevel":"01"
          |               }
          |            ],
          |            "chargeDueDate":"2022-10-30",
          |            "chargeOutstandingAmount":200,
          |            "chargeAmount":200
          |         }
          |      ]
          |   },
          |   "latePaymentPenalty":{
          |      "details":[
          |         {
          |            "principalChargeDueDate":"2022-10-30",
          |            "principalChargeBillingTo":"2022-10-30",
          |            "penaltyAmountPaid":1001.45,
          |            "outstandingAmount":123.45,
          |            "LPP1LRPercentage":2,
          |            "LPP1HRDays":"31",
          |            "penaltyChargeDueDate":"2022-10-30",
          |            "communicationsDate":"2022-10-30",
          |            "LPP2Days":"31",
          |            "penaltyChargeCreationDate":"2022-10-30",
          |            "LPP1HRPercentage":2,
          |            "LPP1LRDays":"15",
          |            "LPP1HRCalculationAmount":99.99,
          |            "penaltyChargeReference":"1234567890",
          |            "penaltyCategory":"LPP1",
          |            "principalChargeReference":"1234567890",
          |            "penaltyStatus":"A",
          |            "principalChargeBillingFrom":"2022-10-30",
          |            "mainTransaction":"4703",
          |            "LPP2Percentage":4,
          |            "appealInformation":[
          |               {
          |                  "appealStatus":"99",
          |                  "appealLevel":"01"
          |               }
          |            ],
          |            "LPP1LRCalculationAmount":99.99,
          |            "penaltyAmountOutstanding":99.99
          |         }
          |      ]
          |   }
          |}
          |""".stripMargin)
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789")
      mockStubResponseForGetFinancialDetails(Status.OK,
        s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
          s"&onlyOpenItems=false&includeStatistical=true&includeLocks=false" +
          s"&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", Some(getFinancialDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe penaltyDetailsWithLSPandLPPAndFinancialDetails
      wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe true
    }

    "NOT audit the response when the user has 0 penalties" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsWithNoPointsAsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.OK,
        s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
          s"&onlyOpenItems=false&includeStatistical=true&includeLocks=false" +
          s"&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", Some(getFinancialDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe Status.OK
      result.body shouldBe getPenaltyDetailsWithNoPointsAsJson.toString()
      wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe false
    }
  }
}
