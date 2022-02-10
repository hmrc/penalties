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

  val etmpPayloadAsJsonWithMultiplePenaltyPeriod: JsValue = Json.parse(
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
      |			},
      |     {
      |				"startDate": "2021-05-23T18:25:43.511",
      |				"endDate": "2021-05-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-05-23T18:25:43.511",
      |					"submittedDate": "2021-05-23T18:25:43.511",
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


  def mockResponseForStubETMPPayload(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/etmp/mtd-vat/$enrolmentKey"))
      .willReturn(
        aResponse()
          .withBody(body.fold(etmpPayloadAsJson.toString())(identity))
          .withStatus(status)))
  }

  def mockResponseForStubETMPPayloadWithMultiplePenaltyPeriod(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/etmp/mtd-vat/$enrolmentKey"))
      .willReturn(
        aResponse()
          .withBody(body.fold(etmpPayloadAsJsonWithMultiplePenaltyPeriod.toString())(identity))
          .withStatus(status)))
  }

  def mockResponseForETMPPayload(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/$enrolmentKey"))
      .willReturn(
        aResponse()
          .withBody(body.fold(etmpPayloadAsJson.toString())(identity))
          .withStatus(status)))
  }

  def mockResponseForGetPenaltyDetails(status: Int, vatcUrl: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalty/details/$vatcUrl"))
    .willReturn(
      aResponse()
        .withBody(body.fold(getPenaltyDetailsWithLSPandLPPAsJson.toString())(identity))
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
}
