/*
 * Copyright 2025 HM Revenue & Customs
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
import models.{Regime, IdType, Id}
import java.time.Instant

trait HIPPenaltiesWiremock {
    
  val mockInstant = Instant.parse("2025-04-24T12:00:00Z")
val getHIPPenaltyDetailsWithLSPAndLPPAsJson: JsValue = Json.parse(
  s"""
     |{
     |  "success": {
     |    "processingDate": "$mockInstant",
     |    "penaltyData": {
     |      "totalisations": {
     |        "lspTotalValue": 200,
     |        "penalisedPrincipalTotal": 2000,
     |        "lppPostedTotal": 165.25,
     |        "lppEstimatedTotal": 15.26
     |      },
     |      "lsp": {
     |        "lspSummary": {
     |          "activePenaltyPoints": 10,
     |          "inactivePenaltyPoints": 12,
     |          "regimeThreshold": 10,
     |          "penaltyChargeAmount": 684.25,
     |          "pocAchievementDate": "2022-01-01"
     |        },
     |        "lspDetails": [
     |          {
     |            "penaltyNumber": "12345678901234",
     |            "penaltyOrder": "01",
     |            "penaltyCategory": "P",
     |            "penaltyStatus": "ACTIVE",
     |            "penaltyCreationDate": "2022-10-30",
     |            "penaltyExpiryDate": "2022-10-30",
     |            "communicationsDate": "2022-10-30",
     |            "lateSubmissions": [
     |              {
     |                "lateSubmissionID": "001",
     |                "incomeSource": "IT",
     |                "taxPeriod": "23AA",
     |                "taxPeriodStartDate": "2022-01-01",
     |                "taxPeriodEndDate": "2022-12-31",
     |                "taxPeriodDueDate": "2023-02-07",
     |                "returnReceiptDate": "2023-02-01",
     |                "taxReturnStatus": "Fulfilled"
     |              }
     |            ],
     |            "appealInformation": [
     |              {
     |                "appealStatus": "99",
     |                "appealDescription": "Some value"
     |              }
     |            ],
     |            "chargeDueDate": "2022-10-30",
     |            "chargeOutstandingAmount": 200,
     |            "chargeAmount": 200,
     |            "triggeringProcess": "P123",
     |            "chargeReference": "CHARGEREF1"
     |          }
     |        ]
     |      },
     |      "lpp": {
     |        "manualLPPIndicator": false,
     |        "lppDetails": [
     |          {
     |            "penaltyCategory": "LPP1",
     |            "penaltyChargeReference": "1234567890",
     |            "principalChargeReference": "1234567890",
     |            "penaltyChargeCreationDate": "2022-10-30",
     |            "penaltyStatus": "A",
     |            "penaltyChargeAmount": 99.99,
     |            "penaltyAmountPosted": 0,
     |            "penaltyAmountOutstanding": null,
     |            "penaltyAmountPaid": null,
     |            "penaltyAmountAccruing": 99.99,
     |            "principalChargeMainTr": "4700",
     |            "principalChargeBillingFrom": "2022-10-30",
     |            "principalChargeBillingTo": "2022-10-30",
     |            "principalChargeDueDate": "2022-10-30",
     |            "lpp1LRDays": "15",
     |            "lpp1HRDays": "31",
     |            "lpp2Days": "31",
     |            "lpp1HRCalculationAmt": 99.99,
     |            "lpp1LRCalculationAmt": 99.99,
     |            "lpp2Percentage": 4.00,
     |            "lpp1LRPercentage": 2.00,
     |            "lpp1HRPercentage": 2.00,
     |            "communicationsDate": "2022-10-30",
     |            "penaltyChargeDueDate": "2022-10-30",
     |            "appealInformation": [
     |              {
     |                "appealStatus": "99",
     |                "appealLevel": "01",
     |                "appealDescription": "Some value"
     |              }
     |            ],
     |            "principalChargeLatestClearing": "2027-07-20",
     |            "vatOutstandingAmount": null,
     |            "timeToPay": [
     |              {
     |                "ttpStartDate": "2022-01-01",
     |                "ttpEndDate": "2022-12-31"
     |              }
     |             ],
     |            "principalChargeDocNumber": "DOC1",
     |            "principalChargeSubTr": "SUB1"
     |          }
     |        ]
     |      },
     |      "breathingSpace": [
     |        {
     |          "bsStartDate": "2023-01-01",
     |          "bsEndDate": "2023-12-31"
     |        }
     |      ]
     |    }
     |  }
     |}
     |""".stripMargin)

  val getHIPPenaltyDetailsWithIncomeSourceNoneAsJson: JsValue = Json.parse(
    s"""
       |{
       |  "success": {
       |    "processingDate": "$mockInstant",
       |    "penaltyData": {
       |      "totalisations": {
       |        "lspTotalValue": 100,
       |        "penalisedPrincipalTotal": 1000,
       |        "lppPostedTotal": 0,
       |        "lppEstimatedTotal": 0
       |      },
       |      "lsp": {
       |        "lspSummary": {
       |          "activePenaltyPoints": 1,
       |          "inactivePenaltyPoints": 0,
       |          "regimeThreshold": 5,
       |          "penaltyChargeAmount": 100,
       |          "pocAchievementDate": "2022-01-01"
       |        },
       |        "lspDetails": [
       |          {
       |            "penaltyNumber": "123456787",
       |            "penaltyOrder": "01",
       |            "penaltyCategory": "P",
       |            "penaltyStatus": "ACTIVE",
       |            "penaltyCreationDate": "2022-04-01",
       |            "penaltyExpiryDate": "2022-04-01",
       |            "communicationsDate": "2022-05-08",
       |            "lateSubmissions": [
       |              {
       |                "lateSubmissionID": "001",
       |                "incomeSource": null,
       |                "taxPeriod": "23AA",
       |                "taxPeriodStartDate": "2022-01-01",
       |                "taxPeriodEndDate": "2022-12-31",
       |                "taxPeriodDueDate": "2023-02-07",
       |                "returnReceiptDate": "2023-02-01",
       |                "taxReturnStatus": "Fulfilled"
       |              }
       |            ],
       |            "appealInformation": [],
       |            "chargeDueDate": "2022-04-01",
       |            "chargeOutstandingAmount": 100,
       |            "chargeAmount": 100,
       |            "triggeringProcess": "P123",
       |            "chargeReference": "CHARGEREF1"
       |          }
       |        ]
       |      },
       |      "lpp": {
       |        "manualLPPIndicator": false,
       |        "lppDetails": []
       |      },
       |      "breathingSpace": []
       |    }
       |  }
       |}
       |""".stripMargin)

  def mockResponseForHIPPenaltyDetails(status: Int, apiRegime: Regime, idType: IdType, id: Id, dateLimit: Option[String] = None, body: Option[String] = None): StubMapping = {
    val dateLimitParam = dateLimit.map(d => s"&dateLimit=$d").getOrElse("")
    stubFor(get(urlEqualTo(s"/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=${apiRegime.value}&idType=${idType.value}&idNumber=${id.value}$dateLimitParam"))
      .willReturn(
        aResponse()
          .withBody(body.fold(getHIPPenaltyDetailsWithLSPAndLPPAsJson.toString())(identity))
          .withStatus(status)
      ))
  }

  def mockResponseForHIPPenaltyDetailsWithIncomeSourceNone(status: Int, apiRegime: Regime, idType: IdType, id: Id, dateLimit: Option[String] = None): StubMapping = {
    val dateLimitParam = dateLimit.map(d => s"&dateLimit=$d").getOrElse("")
    stubFor(get(urlEqualTo(s"/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=${apiRegime.value}&idType=${idType.value}&idNumber=${id.value}$dateLimitParam"))
      .willReturn(
        aResponse()
          .withBody(getHIPPenaltyDetailsWithIncomeSourceNoneAsJson.toString())
          .withStatus(status)
      ))
  }
}