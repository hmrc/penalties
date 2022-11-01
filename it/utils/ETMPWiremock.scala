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
      |   "LPPEstimatedTotal": 15.26,
      |   "LPIPostedTotal": 1968.2,
      |   "LPIEstimatedTotal": 7
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
      |       "FAPIndicator": "X",
      |       "lateSubmissions": [
      |         {
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
      |           "appealLevel": "01"
      |         }
      |       ],
      |       "chargeDueDate": "2022-10-30",
      |       "chargeOutstandingAmount": 200,
      |       "chargeAmount": 200
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
      |         "appealLevel": "01"
      |       }],
      |       "principalChargeBillingFrom": "2022-10-30",
      |       "principalChargeBillingTo": "2022-10-30",
      |       "principalChargeDueDate": "2022-10-30",
      |       "communicationsDate": "2022-10-30",
      |       "penaltyAmountOutstanding": 99.99,
      |       "penaltyAmountPaid": 1001.45,
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
      | }
      |}
      |""".stripMargin)

  val getPenaltyDetailsWithNoPointsAsJson: JsValue = Json.parse(
      """
        |{
        | "totalisations": {
        |   "LSPTotalValue": 0,
        |   "penalisedPrincipalTotal": 0,
        |   "LPPPostedTotal": 0.00,
        |   "LPIPostedTotal": 0.00
        | },
        | "lateSubmissionPenalty": {
        |   "summary": {
        |     "activePenaltyPoints": 0,
        |     "inactivePenaltyPoints": 0,
        |     "regimeThreshold": 0,
        |     "penaltyChargeAmount": 0.00,
        |     "PoCAchievementDate": "2022-01-01"
        |   },
        |   "details": []
        |   },
        |   "latePaymentPenalty":{
        |      "details":[]
        | }
        |}
        |""".stripMargin)


  val getFinancialDetailsAsJsonOld: JsValue = Json.parse(
    """
      |{
      | "documentDetails": [
      | {
      |   "taxYear": "2022",
      |   "documentId": "0002",
      |   "documentDate": "2022-10-30",
      |   "documentText": "Document Text",
      |   "documentDueDate": "2022-10-30",
      |   "documentDescription": "Document Description",
      |   "formBundleNumber": "1234",
      |   "totalAmount": 123.45,
      |   "documentOutstandingAmount": 543.21,
      |   "lastClearingDate": "2022-10-30",
      |   "lastClearingReason": "last Clearing Reason",
      |   "lastClearedAmount": 123.45,
      |   "statisticalFlag": true,
      |   "informationCode": "A",
      |   "paymentLot": "81203010024",
      |   "paymentLotItem": "000001",
      |   "accruingInterestAmount": 123.45,
      |   "interestRate": 543.21,
      |   "interestFromDate": "2022-10-30",
      |   "interestEndDate": "2022-10-30",
      |   "latePaymentInterestID": "1234567890123456",
      |   "latePaymentInterestAmount": 123.45,
      |   "lpiWithDunningBlock": 543.21,
      |   "interestOutstandingAmount": 543.21,
      |   "accruingPenaltyLPP1": "Interest Rate"
      | }
      | ],
      | "financialDetails": [
      | {
      |   "taxYear": "2022",
      |   "documentId": "0001",
      |   "chargeType": "PAYE",
      |   "mainType": "2100",
      |   "periodKey": "13RL",
      |   "periodKeyDescription": "abcde",
      |   "taxPeriodFrom": "2022-10-30",
      |   "taxPeriodTo": "2022-10-30",
      |   "businessPartner": "6622334455",
      |   "contractAccountCategory": "02",
      |   "contractAccount": "X",
      |   "contractObjectType": "ABCD",
      |   "contractObject": "00000003000000002757",
      |   "sapDocumentNumber": "1040000872",
      |   "sapDocumentNumberItem": "XM00",
      |   "chargeReference": "XM002610011594",
      |   "mainTransaction": "4703",
      |   "subTransaction": "5678",
      |   "originalAmount": 123.45,
      |   "outstandingAmount": 543.21,
      |   "clearedAmount": 123.45,
      |   "accruedInterest": 543.21,
      |   "items": [{
      |     "subItem": "001",
      |     "dueDate": "2022-10-30",
      |     "amount": 123.45,
      |     "clearingDate": "2022-10-30",
      |     "clearingReason": "01",
      |     "outgoingPaymentMethod": "outgoing Payment",
      |     "paymentLock": "paymentLock",
      |     "clearingLock": "clearingLock",
      |     "interestLock": "interestLock",
      |     "dunningLock": "dunningLock",
      |     "returnFlag": true,
      |     "paymentReference": "Ab12453535",
      |     "paymentAmount": 543.21,
      |     "paymentMethod": "Payment",
      |     "paymentLot": "81203010024",
      |     "paymentLotItem": "000001",
      |     "clearingSAPDocument": "3350000253",
      |     "codingInitiationDate": "2022-10-30",
      |     "statisticalDocument": "S",
      |     "returnReason": "ABCA",
      |     "DDCollectionInProgress": true,
      |     "promisetoPay": "promisetoPay"
      |   }]
      | }
      | ]
      |}
      |""".stripMargin
  )

  val getFinancialDetailsAsJson: JsValue = Json.parse(
    """
      {
      |  "totalisation": {
      |    "regimeTotalisation": {
      |      "totalAccountOverdue": "1000.0,",
      |      "totalAccountNotYetDue": "250.0,",
      |      "totalAccountCredit": "40.0,",
      |      "totalAccountBalance": 1210
      |    },
      |    "targetedSearch_SelectionCriteriaTotalisation": {
      |      "totalOverdue": "100.0,",
      |      "totalNotYetDue": "0.0,",
      |      "totalBalance": "100.0,",
      |      "totalCredit": "10.0,",
      |      "totalCleared": 50
      |    },
      |    "additionalReceivableTotalisations": {
      |      "totalAccountPostedInterest": "-99999999999.99,",
      |      "totalAccountAccruingInterest": -99999999999.99
      |    }
      |  },
      |  "documentDetails": [
      |    {
      |      "documentNumber": "187346702498,",
      |      "documentType": "TRM New Charge,",
      |      "chargeReferenceNumber": "XP001286394838,",
      |      "businessPartnerNumber": "100893731,",
      |      "contractAccountNumber": "900726630,",
      |      "contractAccountCategory": "VAT,",
      |      "contractObjectNumber": "104920928302302,",
      |      "contractObjectType": "ZVAT,",
      |      "postingDate": "2022-01-01,",
      |      "issueDate": "2022-01-01,",
      |      "documentTotalAmount": "100.0,",
      |      "documentClearedAmount": "100.0,",
      |      "documentOutstandingAmount": "0.0,",
      |      "documentLockDetails": {
      |        "lockType": "Payment,",
      |        "lockStartDate": "2022-01-01,",
      |        "lockEndDate": "2022-01-01"
      |      },
      |      "documentInterestTotals": {
      |        "interestPostedAmount": "13.12,",
      |        "interestPostedChargeRef": "XB001286323438,",
      |        "interestAccruingAmount": 12.1
      |      },
      |      "documentPenaltyTotals": [
      |        {
      |          "penaltyType": "LPP1,",
      |          "penaltyStatus": "POSTED,",
      |          "penaltyAmount": "10.01,",
      |          "postedChargeReference": "XR00123933492"
      |        }
      |      ],
      |      "lineItemDetails": [
      |        {
      |          "itemNumber": "0001,",
      |          "subItemNumber": "003,",
      |          "mainTransaction": "4576,",
      |          "subTransaction": "1000,",
      |          "chargeDescription": "VAT Return,",
      |          "periodFromDate": "2022-01-01,",
      |          "periodToDate": "2022-01-31,",
      |          "periodKey": "22A1,",
      |          "netDueDate": "2022-02-08,",
      |          "formBundleNumber": "125435934761,",
      |          "statisticalKey": "1,",
      |          "amount": "3420.0,",
      |          "clearingDate": "2022-02-09,",
      |          "clearingReason": "Payment at External Payment Collector Reported,",
      |          "clearingDocument": "719283701921,",
      |          "outgoingPaymentMethod": "B,",
      |          "ddCollectionInProgress": "true,",
      |          "lineItemLockDetails": [
      |            {
      |              "lockType": "Payment,",
      |              "lockStartDate": "2022-01-01,",
      |              "lockEndDate": "2022-01-01"
      |            }
      |          ],
      |          "lineItemInterestDetails": {
      |            "interestKey": "String,",
      |            "currentInterestRate": "-999.999999,",
      |            "interestStartDate": "1920-02-29,",
      |            "interestPostedAmount": "-99999999999.99,",
      |            "interestAccruingAmount": -99999999999.99
      |          }
      |        }
      |      ]
      |    }
      |  ]
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
          .withBody(body.fold(getFinancialDetailsAsJson.toString())(identity))
          .withStatus(status)
      ))
  }

  def mockResponseForGetFinancialDetails(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalty/financial-data/$vatcUrl"))
    .willReturn(
      aResponse()
        .withBody(body.fold(getFinancialDetailsAsJson.toString())(identity))
        .withStatus(status)
    ))
  }

  //TODO: remove when we have updated mapping
  def mockStubResponseForGetFinancialDetailsOld(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/penalty/financial-data/$vatcUrl"))
      .willReturn(
        aResponse()
          .withBody(body.fold(getFinancialDetailsAsJsonOld.toString())(identity))
          .withStatus(status)
      ))
  }

  //TODO: remove when we have updated mapping
  def mockResponseForGetFinancialDetailsOld(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalty/financial-data/$vatcUrl"))
      .willReturn(
        aResponse()
          .withBody(body.fold(getFinancialDetailsAsJsonOld.toString())(identity))
          .withStatus(status)
      ))
  }
}
