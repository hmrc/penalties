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

package utils

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}

trait ETMPWiremock {
  val getPenaltyDetailsWithLSPAndLPPAsJson: JsValue = Json.parse(
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
      |     "PoCAchievementDate": "2022-01-01"
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
      |       "appealInformation": [
      |         {
      |           "appealStatus": "99",
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
      |       "principalChargeMainTransaction": "4700",
      |       "principalChargeDocNumber": "DOC1",
      |       "principalChargeSubTransaction": "SUB1",
      |       "principalChargeDueDate": "2022-10-30",
      |       "communicationsDate": "2022-10-30",
      |       "penaltyAmountPosted": 0,
      |       "penaltyAmountAccruing": 99.99,
      |       "LPP1LRDays": "15",
      |       "LPP1HRDays": "31",
      |       "LPP2Days": "31",
      |       "LPP1HRCalculationAmount": 99.99,
      |       "LPP1LRCalculationAmount": 99.99,
      |       "LPP2Percentage": 4.00,
      |       "LPP1LRPercentage": 2.00,
      |       "LPP1HRPercentage": 2.00,
      |       "penaltyChargeDueDate": "2022-10-30",
      |       "timeToPay": [
      |             {
      |               "TTPStartDate": "2022-01-01",
      |               "TTPEndDate": "2022-12-31"
      |             }
      |          ]
      |   }]
      | },
      | "breathingSpace": [
      |   {
      |     "BSStartDate": "2023-01-01",
      |     "BSEndDate": "2023-12-31"
      |   }
      | ]
      |}
      |""".stripMargin)

  val getFinancialDetailsWithoutTotalisationsAsJson: JsValue = Json.parse(
    """
      |{
      | "getFinancialData": {
      |   "financialDetails":{
      |     "documentDetails": [
      |     {
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
      |      }]
      |    }
      |  ]
      |}
      |}
      |}
      |""".stripMargin
  )

  val getFinancialDetailsTotalisationsAsJson: JsValue = Json.parse(
    """
      |{
      | "getFinancialData": {
      |   "financialDetails":{
      |     "totalisation": {
      |       "regimeTotalisation": {
      |         "totalAccountOverdue": 1000.0,
      |         "totalAccountNotYetDue": 250.0,
      |         "totalAccountCredit": 40.0,
      |         "totalAccountBalance": 1210
      |       },
      |       "targetedSearch_SelectionCriteriaTotalisation": {
      |         "totalOverdue": 100.0,
      |         "totalNotYetDue": 0.0,
      |         "totalBalance": 100.0,
      |         "totalCredit": 10.0,
      |         "totalCleared": 50
      |       },
      |       "additionalReceivableTotalisations": {
      |         "totalAccountPostedInterest": 12.34,
      |         "totalAccountAccruingInterest": 43.21
      |       }
      |     }
      |}
      |}
      |}
      |""".stripMargin
  )

  val getFinancialDetailsAsJson: JsValue = Json.parse(
    """
      |{
      | "getFinancialData": {
      |   "financialDetails":{
      |     "totalisation": {
      |       "regimeTotalisation": {
      |         "totalAccountOverdue": 1000.0,
      |         "totalAccountNotYetDue": 250.0,
      |         "totalAccountCredit": 40.0,
      |         "totalAccountBalance": 1210
      |       },
      |       "targetedSearch_SelectionCriteriaTotalisation": {
      |         "totalOverdue": 100.0,
      |         "totalNotYetDue": 0.0,
      |         "totalBalance": 100.0,
      |         "totalCredit": 10.0,
      |         "totalCleared": 50
      |       },
      |       "additionalReceivableTotalisations": {
      |         "totalAccountPostedInterest": 12.34,
      |         "totalAccountAccruingInterest": 43.21
      |       }
      |     },
      |     "documentDetails": [
      |     {
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
      |      }]
      |    }
      |  ]
      |}
      |}
      |}
      |""".stripMargin
  )

  def mockStubResponseForGetPenaltyDetails(status: Int, vrn: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/penalty/details/VATC/VRN/$vrn"))
    .willReturn(
      aResponse()
        .withBody(body.fold(getPenaltyDetailsWithLSPAndLPPAsJson.toString())(identity))
        .withStatus(status)
    ))
  }

  def mockResponseForGetPenaltyDetails(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalty/details/VATC/VRN/$vatcUrl"))
      .willReturn(
        aResponse()
          .withBody(body.fold(getPenaltyDetailsWithLSPAndLPPAsJson.toString())(identity))
          .withStatus(status)
      ))
  }

  def mockStubResponseForGetFinancialDetails(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/penalty/financial-data/$vatcUrl"))
      .willReturn(
        aResponse()
          .withBody(body.fold(getFinancialDetailsWithoutTotalisationsAsJson.toString())(identity))
          .withStatus(status)
      ))
  }

  def mockResponseForGetFinancialDetails(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalty/financial-data/$vatcUrl"))
      .willReturn(
        aResponse()
          .withBody(body.fold(getFinancialDetailsWithoutTotalisationsAsJson.toString())(identity))
          .withStatus(status)
      ))
  }
}
