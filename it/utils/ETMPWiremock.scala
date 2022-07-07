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
  val etmpPayloadAsJson: JsValue = Json.parse(
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
      |	]
      |}
      |""".stripMargin)

  val getPenaltyDetailsWithLSPandLPPAsJson: JsValue = Json.parse(
    """
      |{
      | "lateSubmissionPenalty": {
      |   "summary": {
      |     "activePenaltyPoints": 1,
      |     "inactivePenaltyPoints": 2,
      |     "regimeThreshold": 3,
      |     "POCAchievementDate": "2022-01-01",
      |     "penaltyChargeAmount": 123.45
      |   },
      |   "details": [{
      |    	"penaltyNumber": "1234ABCD",
      |	    "penaltyOrder": "1",
      |	    "penaltyCategory": "P",
      |	    "penaltyStatus": "ACTIVE",
      |	    "penaltyCreationDate": "2022-01-01",
      |	    "penaltyExpiryDate": "2024-01-01",
      |	    "communicationsDate": "2022-01-01",
      |     "appealStatus": "1",
      |	    "appealLevel": "1",
      |	    "chargeReference": "foobar",
      |	    "chargeAmount": 123.45,
      |	    "chargeOutstandingAmount": 123.45,
      |	    "chargeDueDate": "2022-01-01",
      |     "lateSubmissions": [{
      |       "lateSubmissionID": "ID123",
      |       "taxPeriod": "1",
      |       "taxReturnStatus": "2",
      |       "taxPeriodStartDate": "2022-01-01",
      |       "taxPeriodEndDate": "2022-03-31",
      |       "taxPeriodDueDate": "2022-05-07",
      |       "returnReceiptDate": "2022-04-01"
      |     }]
      |   }]
      |  },
      |  "latePaymentPenalty": {
      |   "details": [{
      |	    "penaltyNumber": "1234ABCD",
      |	    "penaltyCategory": "LPP1",
      |   	"penaltyStatus": "P",
      |	    "penaltyAmountAccruing": 123.45,
      |	    "penaltyAmountPosted": 123.45,
      |	    "penaltyChargeCreationDate": "2022-01-01",
      |	    "penaltyChargeDueDate": "2022-02-01",
      |	    "communicationsDate": "2022-01-01",
      |	    "appealLevel": "1",
      |	    "appealStatus": "1",
      |	    "penaltyChargeReference": "CHARGE123456",
      |     "principalChargeDueDate": "2022-03-01",
      |     "principalChargeReference": "CHARGING12345"
      |   }]
      |  }
      |}
      |""".stripMargin)

  val getPenaltyDetailsWithLSPandLPPAsJsonv3: JsValue = Json.parse(
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
      |     "penaltyChargeAmount": 684.25
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
      |       "penaltyChargeDueDate": "2022-10-30"
      |   }]
      | }
      |}
      |""".stripMargin)

    val getPenaltyDetailsWithNoPointsAsJsonv3: JsValue = Json.parse(
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
        |     "penaltyChargeAmount": 0.00
        |   },
        |   "details": []
        |   },
        |   "latePaymentPenalty":{
        |      "details":[]
        | }
        |}
        |""".stripMargin)

  val getFinancialDetailsAsJson: JsValue = Json.parse(
    """
      |{
      | "documentDetails": [
      | {
      |   "taxYear": "2022",
      |   "documentId": "DOC1234",
      |   "documentDate": "2022-01-01",
      |   "documentText": "1234",
      |   "documentDueDate": "2022-01-01",
      |   "documentDescription": "1234",
      |   "formBundleNumber": "1234",
      |   "totalAmount": 123.45,
      |   "documentOutstandingAmount": 123.45,
      |   "lastClearingDate": "2022-01-01",
      |   "lastClearingReason": "1234",
      |   "lastClearedAmount": 123.45,
      |   "statisticalFlag": true,
      |   "informationCode": "1",
      |   "paymentLot": "1",
      |   "paymentLotItem": "1",
      |   "accruingInterestAmount": 123.45,
      |   "interestRate": 123.45,
      |   "interestFromDate": "2022-01-01",
      |   "interestEndDate": "2022-01-01",
      |   "latePaymentInterestID": "1234",
      |   "latePaymentInterestAmount": 123.45,
      |   "lpiWithDunningBlock": 123.45,
      |   "interestOutstandingAmount": 123.45,
      |   "accruingPenaltyLPP1": "1234"
      | }
      | ],
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
      |   "chargeReference": "1",
      |   "mainTransaction": "1",
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
      |     "statisticalDocument": "G",
      |     "returnReason": "ABCA",
      |     "DDCollectionInProgress": "Y",
      |     "promisetoPay": "Y"
      |   }]
      | }
      | ]
      |}
      |""".stripMargin)

  val getFinancialDetailsAsJsonv3: JsValue = Json.parse(
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


  def mockResponseForStubETMPPayload(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/etmp/mtd-vat/$enrolmentKey"))
      .willReturn(
        aResponse()
          .withBody(body.fold(etmpPayloadAsJson.toString())(identity))
          .withStatus(status)))
  }

  def mockResponseForETMPPayload(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/$enrolmentKey"))
      .willReturn(
        aResponse()
          .withBody(body.fold(etmpPayloadAsJson.toString())(identity))
          .withStatus(status)))
  }

  def mockResponseForGetPenaltyDetails(status: Int, vrn: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/penalty/details/VATC/VRN/$vrn"))
    .willReturn(
      aResponse()
        .withBody(body.fold(getPenaltyDetailsWithLSPandLPPAsJson.toString())(identity))
        .withStatus(status)
    ))
  }

  def mockResponseForGetPenaltyDetailsv3(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalty/details/VATC/VRN/$vatcUrl"))
      .willReturn(
        aResponse()
          .withBody(body.fold(getPenaltyDetailsWithLSPandLPPAsJsonv3.toString())(identity))
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

  def mockResponseForGetFinancialDetailsv3(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalty/financial-data/$vatcUrl"))
    .willReturn(
      aResponse()
        .withBody(body.fold(getFinancialDetailsAsJsonv3.toString())(identity))
        .withStatus(status)
    ))
  }
}
