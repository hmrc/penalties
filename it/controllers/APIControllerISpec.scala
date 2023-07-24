/*
 * Copyright 2023 HM Revenue & Customs
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
import config.featureSwitches.{CallAPI1811ETMP, CallAPI1812ETMP, FeatureSwitching}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import scala.jdk.CollectionConverters._

class APIControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {
  val controller: APIController = injector.instanceOf[APIController]

  "getSummaryDataForVRN" should {
    val getPenaltyDetailsJson: JsValue = Json.parse(
      """
        |{
        | "totalisations": {
        |   "LSPTotalValue": 200,
        |   "penalisedPrincipalTotal": 2000,
        |   "LPPPostedTotal": 165.25,
        |   "LPPEstimatedTotal": 15.26
        | },
        | "lateSubmissionPenalty": {
        |   "summary": {
        |     "activePenaltyPoints": 2,
        |     "inactivePenaltyPoints": 0,
        |     "regimeThreshold": 5,
        |     "penaltyChargeAmount": 200.00,
        |     "PoCAchievementDate": "2022-01-01"
        |   },
        |   "details": []
        | },
        | "latePaymentPenalty": {
        |     "details": [
        |       {
        |          "penaltyCategory": "LPP2",
        |          "penaltyStatus": "A",
        |          "penaltyAmountPosted": 0,
        |          "LPP1LRCalculationAmount": 123.45,
        |          "LPP1LRDays": "15",
        |          "LPP1LRPercentage": 2.00,
        |          "LPP1HRCalculationAmount": 123.45,
        |          "LPP1HRDays": "31",
        |          "LPP1HRPercentage": 2.00,
        |          "LPP2Days": "31",
        |          "LPP2Percentage": 4.00,
        |          "penaltyChargeCreationDate": "2022-10-30",
        |          "communicationsDate": "2022-10-30",
        |          "penaltyAmountAccruing": 246.9,
        |          "principalChargeMainTransaction" : "4700",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-10-30",
        |          "principalChargeBillingTo": "2022-10-30",
        |          "principalChargeMainTransaction": "4700",
        |          "principalChargeDueDate": "2022-10-30"
        |       },
        |       {
        |          "penaltyCategory": "LPP2",
        |          "penaltyStatus": "A",
        |          "penaltyAmountPosted": 0,
        |          "penaltyAmountAccruing": 123.45,
        |          "LPP1LRCalculationAmount": 123.45,
        |          "LPP1LRDays": "15",
        |          "LPP1LRPercentage": 2.00,
        |          "LPP1HRCalculationAmount": 123.45,
        |          "LPP1HRDays": "31",
        |          "LPP1HRPercentage": 2.00,
        |          "LPP2Days": "31",
        |          "LPP2Percentage": 4.00,
        |          "penaltyChargeCreationDate": "2022-10-30",
        |          "communicationsDate": "2022-10-30",
        |          "penaltyAmountAccruing": 0.00,
        |          "principalChargeMainTransaction" : "4700",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-10-30",
        |          "principalChargeBillingTo": "2022-10-30",
        |          "principalChargeMainTransaction": "4700",
        |          "principalChargeDueDate": "2022-10-30"
        |       },
        |       {
        |          "penaltyCategory": "LPP1",
        |          "penaltyStatus": "P",
        |          "penaltyAmountPaid": 0,
        |          "penaltyAmountPosted": 144.0,
        |          "penaltyAmountOutstanding": 144.00,
        |          "penaltyAmountAccruing": 0,
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
        |          "penaltyAmountAccruing": 0.00,
        |          "principalChargeMainTransaction" : "4700",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-10-30",
        |          "principalChargeBillingTo": "2022-10-30",
        |          "principalChargeMainTransaction": "4700",
        |          "principalChargeDueDate": "2022-10-30"
        |       },
        |       {
        |          "penaltyCategory": "LPP1",
        |          "penaltyStatus": "P",
        |          "penaltyAmountPaid": 0,
        |          "penaltyAmountPosted": 144.00,
        |          "penaltyAmountOutstanding": 144.00,
        |          "penaltyAmountAccruing": 0,
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
        |          "penaltyAmountAccruing": 0,
        |          "principalChargeMainTransaction" : "4700",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-10-30",
        |          "principalChargeBillingTo": "2022-10-30",
        |          "principalChargeMainTransaction": "4700",
        |          "principalChargeDueDate": "2022-10-30"
        |       }
        |   ]
        | }
        |}
        |""".stripMargin)

    s"return OK (${Status.OK})" when {
      "the get penalty details call succeeds" in {
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get())
        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.parse(
          """
            |{
            |  "noOfPoints": 2,
            |  "noOfEstimatedPenalties": 2,
            |  "noOfCrystalisedPenalties": 2,
            |  "estimatedPenaltyAmount": 246.9,
            |  "crystalisedPenaltyAmountDue": 288,
            |  "hasAnyPenaltyData": true
            |}
            |""".stripMargin
        )
      }
    }

    s"return BAD_REQUEST (${Status.BAD_REQUEST})" when {
      "the user supplies an invalid VRN" in {
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789123456789").get())
        result.status shouldBe BAD_REQUEST
      }
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR})" when {
      "the get penalty details call fails" in {
        mockStubResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, "123456789", body = Some(""))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get())
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND})" when {
      "the get penalty details call returns 404" in {
        mockStubResponseForGetPenaltyDetails(Status.NOT_FOUND, "123456789", body = Some(""))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get())
        result.status shouldBe NOT_FOUND
      }
    }

    s"return NO_CONTENT (${Status.NO_CONTENT})" when {
      "the get penalty details call returns 404 (with NO_DATA_FOUND in body)" in {
        val notFoundResponseBody: String =
          """
            |{
            |  "failures": [
            |    {
            |      "code": "NO_DATA_FOUND",
            |      "reason": "The remote endpoint has indicated that no penalty data found for provided ID number."
            |    }
            |  ]
            |}
            |""".stripMargin
        mockStubResponseForGetPenaltyDetails(Status.NOT_FOUND, "123456789", body = Some(notFoundResponseBody))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get())
        result.status shouldBe NO_CONTENT
      }

      "the get penalty details call returns 200 with an empty body" in {
        val emptyResponse: String = "{}"
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(emptyResponse))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get())
        result.status shouldBe NO_CONTENT
      }
    }
  }

  "getFinancialDetails" should {
    s"return OK (${Status.OK})" when {
      "the get Financial Details call succeeds" in {
        val sampleAPI1811Response = Json.parse(
          """
            |{
            | "getFinancialData" : {
            | "financialDetails": {
            |  "totalisation": {
            |    "regimeTotalisation": {
            |      "totalAccountOverdue": 1000.0,
            |      "totalAccountNotYetDue": 250.0,
            |      "totalAccountCredit": 40.0,
            |      "totalAccountBalance": 1210
            |    },
            |    "targetedSearch_SelectionCriteriaTotalisation": {
            |      "totalOverdue": 100.0,
            |      "totalNotYetDue": 0.0,
            |      "totalBalance": 100.0,
            |      "totalCredit": 10.0,
            |      "totalCleared": 50
            |    },
            |    "additionalReceivableTotalisations": {
            |      "totalAccountPostedInterest": 12.34,
            |      "totalAccountAccruingInterest": 43.21
            |    }
            |  },
            |  "documentDetails": [
            |    {
            |      "documentNumber": "187346702498",
            |      "documentType": "TRM New Charge",
            |      "chargeReferenceNumber": "XM002610011594",
            |      "businessPartnerNumber": "100893731",
            |      "contractAccountNumber": "900726630",
            |      "contractAccountCategory": "VAT",
            |      "contractObjectNumber": "104920928302302",
            |      "contractObjectType": "ZVAT",
            |      "postingDate": "2022-01-01",
            |      "issueDate": "2022-01-01",
            |      "documentTotalAmount": "100.0",
            |      "documentClearedAmount": "100.0",
            |      "documentOutstandingAmount": "543.21",
            |      "documentLockDetails": {
            |        "lockType": "Payment",
            |        "lockStartDate": "2022-01-01",
            |        "lockEndDate": "2022-01-01"
            |      },
            |      "documentInterestTotals": {
            |        "interestPostedAmount": "13.12",
            |        "interestPostedChargeRef": "XB001286323438",
            |        "interestAccruingAmount": 12.1
            |      },
            |      "documentPenaltyTotals": [
            |        {
            |          "penaltyType": "LPP1",
            |          "penaltyStatus": "POSTED",
            |          "penaltyAmount": "10.01",
            |          "postedChargeReference": "XR00123933492"
            |        }
            |      ],
            |      "lineItemDetails": [
            |        {
            |          "itemNumber": "0001",
            |          "subItemNumber": "003",
            |          "mainTransaction": "4703",
            |          "subTransaction": "1000",
            |          "chargeDescription": "VAT Return",
            |          "periodFromDate": "2022-01-01",
            |          "periodToDate": "2022-01-31",
            |          "periodKey": "22A1",
            |          "netDueDate": "2022-02-08",
            |          "formBundleNumber": "125435934761",
            |          "statisticalKey": "1",
            |          "amount": "3420.0",
            |          "clearingDate": "2022-02-09",
            |          "clearingReason": "Payment at External Payment Collector Reported",
            |          "clearingDocument": "719283701921",
            |          "outgoingPaymentMethod": "B",
            |          "ddCollectionInProgress": "true",
            |          "lineItemLockDetails": [
            |            {
            |              "lockType": "Payment",
            |              "lockStartDate": "2022-01-01",
            |              "lockEndDate": "2022-01-01"
            |            }
            |          ],
            |          "lineItemInterestDetails": {
            |            "interestKey": "String",
            |            "currentInterestRate": "-999.999999",
            |            "interestStartDate": "1920-02-29",
            |            "interestPostedAmount": "-99999999999.99",
            |            "interestAccruingAmount": -99999999999.99
            |          }
            |        }
            |      ]
            |    }
            |  ]
            |}
            |}
            |}""".stripMargin)
        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false" +
          s"&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true" +
          s"&addPostedInterestDetails=true&addAccruingInterestDetails=true", Some(getFinancialDetailsAsJson.toString()))
        val result = await(buildClientForRequestToApp(uri = s"/penalty/financial-data/VRN/123456789/VATC?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false" +
          s"&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true" +
          s"&addPostedInterestDetails=true&addAccruingInterestDetails=true").get())
        result.status shouldBe OK
        result.json shouldBe sampleAPI1811Response
        wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList
          .exists(_.getBodyAsString.contains("Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval")) shouldBe true
      }
    }

    "return the status from EIS" when {
      "404 response received " in {
        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetails(Status.NOT_FOUND, s"VRN/123456789/VATC?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false" +
          s"&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true" +
          s"&addPostedInterestDetails=true&addAccruingInterestDetails=true")
        val result = await(buildClientForRequestToApp(uri = s"/penalty/financial-data/VRN/123456789/VATC?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false" +
          s"&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true" +
          s"&addPostedInterestDetails=true&addAccruingInterestDetails=true").get())
        result.status shouldBe NOT_FOUND
        wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList
          .exists(_.getBodyAsString.contains("Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval")) shouldBe true
      }

      "Non 200 response received " in {
        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetails(Status.BAD_REQUEST, s"VRN/123456789/VATC?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false" +
          s"&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true" +
          s"&addPostedInterestDetails=true&addAccruingInterestDetails=true", Some(""))
        val result = await(buildClientForRequestToApp(uri = s"/penalty/financial-data/VRN/123456789/VATC?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false" +
          s"&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true" +
          s"&addPostedInterestDetails=true&addAccruingInterestDetails=true").get())
        result.status shouldBe BAD_REQUEST
        wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList
          .exists(_.getBodyAsString.contains("Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval")) shouldBe true
      }
    }
  }

  "getPenaltyDetails" should {
    s"return OK (${Status.OK})" when {
      "the get Penalty Details call succeeds" in {
        val sampleAPI1812Response = Json.parse(
          """
            |{
            | "totalisations": {
            |   "LSPTotalValue": 200,
            |   "penalisedPrincipalTotal": 2000,
            |   "LPPPostedTotal": 165.25,
            |   "LPPEstimatedTotal": 15.26
            | },
            | "lateSubmissionPenalty": {
            |   "summary": {
            |     "activePenaltyPoints": 10,
            |     "inactivePenaltyPoints": 12,
            |     "regimeThreshold": 10,
            |     "penaltyChargeAmount": 684.25,
            |     "PoCAchievementDate": "2022-10-30"
            |   },
            |   "details": [
            |     {
            |       "penaltyNumber": "12345678901234",
            |       "penaltyOrder": "01",
            |       "penaltyCategory": "P",
            |       "penaltyStatus": "ACTIVE",
            |       "penaltyCreationDate": "2022-10-30",
            |       "penaltyExpiryDate": "2022-10-30",
            |       "communicationsDate": "2022-10-30",
            |       "FAPIndicator": "X",
            |       "lateSubmissions": [
            |         {
            |           "lateSubmissionID": "001",
            |           "taxPeriod":  "23AA",
            |           "taxPeriodStartDate": "2022-01-01",
            |           "taxPeriodEndDate": "2022-12-31",
            |           "taxPeriodDueDate": "2023-02-07",
            |           "returnReceiptDate": "2023-02-01",
            |           "taxReturnStatus": "Fulfilled"
            |         }
            |       ],
            |       "expiryReason": "FAP",
            |       "appealInformation": [
            |         {
            |           "appealStatus": "99",
            |           "appealLevel": "01",
            |           "appealDescription": "Some value"
            |         }
            |       ],
            |       "chargeDueDate": "2022-10-30",
            |       "chargeOutstandingAmount": 200,
            |       "chargeAmount": 200,
            |       "triggeringProcess": "P123",
            |       "chargeReference": "CHARGEREF1"
            |   }]
            | },
            | "latePaymentPenalty": {
            |     "details": [{
            |       "penaltyCategory": "LPP1",
            |       "penaltyChargeReference": "1234567890",
            |       "principalChargeReference":"1234567890",
            |       "penaltyChargeCreationDate":"2022-10-30",
            |       "penaltyStatus": "A",
            |       "appealInformation":
            |       [{
            |         "appealStatus": "99",
            |         "appealLevel": "01",
            |         "appealDescription": "Some value"
            |       }],
            |       "principalChargeBillingFrom": "2022-10-30",
            |       "principalChargeBillingTo": "2022-10-30",
            |       "principalChargeDueDate": "2022-10-30",
            |       "principalChargeDocNumber": "DOC1",
            |       "principalChargeSubTransaction": "SUB1",
            |       "communicationsDate": "2022-10-30",
            |       "penaltyAmountAccruing": 1001.45,
            |       "principalChargeMainTransaction" : "4700",
            |       "penaltyAmountOutstanding": 0,
            |       "penaltyAmountPaid": 0,
            |       "penaltyAmountPosted": 0,
            |       "LPP1LRDays": "15",
            |       "LPP1HRDays": "31",
            |       "LPP2Days": "31",
            |       "LPP1HRCalculationAmount": 99.99,
            |       "LPP1LRCalculationAmount": 99.99,
            |       "LPP2Percentage": 4.00,
            |       "LPP1LRPercentage": 2.00,
            |       "LPP1HRPercentage": 2.00,
            |       "penaltyChargeDueDate": "2022-10-30"
            |   }]
            | }
            |}
            |""".stripMargin)
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.OK, s"123456789?dateLimit=09", Some(sampleAPI1812Response.toString))
        val result = await(buildClientForRequestToApp(uri = s"/penalty-details/VAT/VRN/123456789?dateLimit=09").get())
        result.status shouldBe OK
        result.json shouldBe sampleAPI1812Response
        wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("Penalties3rdPartyPenaltyDetailsDataRetrieval")) shouldBe true
      }
    }

    "return the status from EIS" when {
      "404 response received" in {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.NOT_FOUND, s"123456789?dateLimit=09", Some(""))
        val result = await(buildClientForRequestToApp(uri = s"/penalty-details/VAT/VRN/123456789?dateLimit=09").get())
        result.status shouldBe NOT_FOUND
        wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("Penalties3rdPartyPenaltyDetailsDataRetrieval")) shouldBe true
      }

      "Non 200 response received" in {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.BAD_REQUEST, s"123456789?dateLimit=09", Some(""))
        val result = await(buildClientForRequestToApp(uri = s"/penalty-details/VAT/VRN/123456789?dateLimit=09").get())
        result.status shouldBe BAD_REQUEST
        wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("Penalties3rdPartyPenaltyDetailsDataRetrieval")) shouldBe true
      }
    }
  }
}
