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

package models.getFinancialDetails

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class FinancialDetailsSpec extends SpecBase {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      |   "documentDetails":[
      |      {
      |         "chargeReferenceNumber":"1234567890",
      |         "documentOutstandingAmount":123.45,
      |         "lineItemDetails":[
      |            {
      |               "mainTransaction":"4703"
      |            }
      |         ],
      |         "documentTotalAmount": 100.0,
      |         "issueDate": "2023-01-01"
      |      }
      |   ]
      |}
      |""".stripMargin)

  val model: FinancialDetails = FinancialDetails(
    documentDetails = Some(
      Seq(
        DocumentDetails(
          chargeReferenceNumber = Some("1234567890"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(
            Seq(
              LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP))
            )
          ),
          documentTotalAmount = Some(100.00),
          issueDate = Some(LocalDate.of(2023, 1, 1))
        )
      )
    ),
    totalisation = None
  )

  "be readable from JSON" in {
    val result = Json.fromJson(modelAsJson)(FinancialDetails.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result = Json.toJson(model)(FinancialDetails.format)
    result shouldBe modelAsJson
  }




  val testResponseJson = Json.parse(
    """
      |{
      |  "success": {
      |    "processingDate": "2025-09-04T08:09:34Z",
      |    "financialData": {
      |      "totalisation": {
      |        "regimeTotalisation": {
      |          "totalAccountOverdue": 5272.64,
      |          "totalAccountNotYetDue": 0,
      |          "totalAccountCredit": 0,
      |          "totalAccountBalance": 5272.64
      |        },
      |        "targetedSearch_SelectionCriteriaTotalisation": {
      |          "totalOverdue": 217.94,
      |          "totalNotYetDue": 0,
      |          "totalBalance": 217.94,
      |          "totalCredit": 0,
      |          "totalCleared": 0
      |        },
      |        "additionalReceivableTotalisations": [
      |          {
      |            "totalAccountPostedInterest": 0,
      |            "totalAccountAccruingInterest": 0
      |          }
      |        ]
      |      },
      |      "documentDetails": [
      |        {
      |          "documentNumber": "221000000198",
      |          "documentType": "New Charge",
      |          "chargeReferenceNumber": "XD002616075855",
      |          "businessPartnerNumber": "0100468984",
      |          "contractAccountNumber": "000960031075",
      |          "contractAccountCategory": "Income Tax",
      |          "contractObjectNumber": "00000160000000022665",
      |          "contractObjectType": "ITSA",
      |          "postingDate": "2025-03-17",
      |          "issueDate": "2025-03-19",
      |          "documentTotalAmount": 8.59,
      |          "documentOutstandingAmount": 8.59,
      |          "lineItemDetails": [
      |            {
      |              "itemNumber": "0001",
      |              "subItemNumber": "000",
      |              "mainTransaction": "4029",
      |              "subTransaction": "1095",
      |              "chargeDescription": "TG PEN NIC NEW GB",
      |              "periodFromDate": "2023-04-06",
      |              "periodToDate": "2024-04-05",
      |              "periodKey": "23P0",
      |              "netDueDate": "2025-04-18",
      |              "amount": 2.86,
      |              "lineItemLockDetails": [
      |                {
      |                  "lockType": "DUNNING",
      |                  "lockStartDate": "0001-01-01",
      |                  "lockEndDate": "9999-12-31"
      |                }
      |              ],
      |              "lineItemInterestDetails": {
      |                "interestKey": "365\/SAIN (LPI)",
      |                "interestStartDate": "2025-04-19"
      |              }
      |            },
      |            {
      |              "itemNumber": "0002",
      |              "subItemNumber": "000",
      |              "mainTransaction": "4029",
      |              "subTransaction": "1090",
      |              "chargeDescription": "TG PEN",
      |              "periodFromDate": "2023-04-06",
      |              "periodToDate": "2024-04-05",
      |              "periodKey": "23P0",
      |              "netDueDate": "2025-04-18",
      |              "amount": 5.73,
      |              "lineItemLockDetails": [
      |                {
      |                  "lockType": "DUNNING",
      |                  "lockStartDate": "0001-01-01",
      |                  "lockEndDate": "9999-12-31"
      |                }
      |              ],
      |              "lineItemInterestDetails": {
      |                "interestKey": "365\/SAIN (LPI)",
      |                "interestStartDate": "2025-04-19"
      |              }
      |            }
      |          ]
      |        },
      |        {
      |          "documentNumber": "241000000174",
      |          "documentType": "New Charge",
      |          "chargeReferenceNumber": "XW002616075853",
      |          "businessPartnerNumber": "0100468984",
      |          "contractAccountNumber": "000960031075",
      |          "contractAccountCategory": "Income Tax",
      |          "contractObjectNumber": "00000160000000022665",
      |          "contractObjectType": "ITSA",
      |          "postingDate": "2025-03-04",
      |          "issueDate": "2025-03-06",
      |          "documentTotalAmount": 209.35,
      |          "documentOutstandingAmount": 209.35,
      |          "lineItemDetails": [
      |            {
      |              "itemNumber": "0001",
      |              "subItemNumber": "000",
      |              "mainTransaction": "4028",
      |              "subTransaction": "1095",
      |              "chargeDescription": "TG PEN NIC NEW GB",
      |              "periodFromDate": "2023-04-06",
      |              "periodToDate": "2024-04-05",
      |              "periodKey": "23P0",
      |              "netDueDate": "2025-04-05",
      |              "amount": 69.91,
      |              "lineItemLockDetails": [
      |                {
      |                  "lockType": "DUNNING",
      |                  "lockStartDate": "0001-01-01",
      |                  "lockEndDate": "9999-12-31"
      |                }
      |              ],
      |              "lineItemInterestDetails": {
      |                "interestKey": "365\/SAIN (LPI)",
      |                "interestStartDate": "2025-04-06"
      |              }
      |            },
      |            {
      |              "itemNumber": "0002",
      |              "subItemNumber": "000",
      |              "mainTransaction": "4028",
      |              "subTransaction": "1090",
      |              "chargeDescription": "TG PEN",
      |              "periodFromDate": "2023-04-06",
      |              "periodToDate": "2024-04-05",
      |              "periodKey": "23P0",
      |              "netDueDate": "2025-04-05",
      |              "amount": 139.44,
      |              "lineItemLockDetails": [
      |                {
      |                  "lockType": "DUNNING",
      |                  "lockStartDate": "0001-01-01",
      |                  "lockEndDate": "9999-12-31"
      |                }
      |              ],
      |              "lineItemInterestDetails": {
      |                "interestKey": "365\/SAIN (LPI)",
      |                "interestStartDate": "2025-04-06"
      |              }
      |            }
      |          ]
      |        }
      |      ]
      |    }
      |  }
      |}
      |""".stripMargin)

  "turn json to HIPFDetails" in {
    val result = testResponseJson.validate[FinancialDetailsHIP]
    println("result -> " + result)

    result.isSuccess shouldBe true
  }
}