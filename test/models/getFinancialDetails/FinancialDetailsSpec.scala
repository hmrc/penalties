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
      |    "processingDate": "2025-09-03T12:22:40Z",
      |    "financialData": {
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
      |              "lineItemInterestDetails": {
      |                "interestKey": "365/SAIN (LPI)",
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
      |              "lineItemInterestDetails": {
      |                "interestKey": "365/SAIN (LPI)",
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
      |              "lineItemInterestDetails": {
      |                "interestKey": "365/SAIN (LPI)",
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
      |              "lineItemInterestDetails": {
      |                "interestKey": "365/SAIN (LPI)",
      |                "interestStartDate": "2025-04-06"
      |              }
      |            }
      |          ]
      |        },
      |        {
      |          "documentNumber": "522000000064",
      |          "documentType": "ITSA- POA 1",
      |          "chargeReferenceNumber": "XB002610234503",
      |          "businessPartnerNumber": "0100468984",
      |          "contractAccountNumber": "000960031075",
      |          "contractAccountCategory": "Income Tax",
      |          "contractObjectNumber": "00000160000000022665",
      |          "contractObjectType": "ITSA",
      |          "postingDate": "2025-08-07",
      |          "documentTotalAmount": 2527.35,
      |          "documentOutstandingAmount": 2527.35,
      |          "lineItemDetails": [
      |            {
      |              "itemNumber": "0001",
      |              "subItemNumber": "000",
      |              "mainTransaction": "4920",
      |              "subTransaction": "1005",
      |              "chargeDescription": "ITSA England & NI",
      |              "periodFromDate": "2024-04-06",
      |              "periodToDate": "2025-04-05",
      |              "periodKey": "25M0",
      |              "netDueDate": "2025-01-31",
      |              "formBundleNumber": "122000001950",
      |              "amount": 1743,
      |              "lineItemInterestDetails": {
      |                "interestKey": "365/SAIN (LPI)",
      |                "interestStartDate": "2025-02-01"
      |              }
      |            },
      |            {
      |              "itemNumber": "0002",
      |              "subItemNumber": "000",
      |              "mainTransaction": "4920",
      |              "subTransaction": "1010",
      |              "chargeDescription": "NIC4-GB",
      |              "periodFromDate": "2024-04-06",
      |              "periodToDate": "2025-04-05",
      |              "periodKey": "25M0",
      |              "netDueDate": "2025-01-31",
      |              "formBundleNumber": "122000001950",
      |              "amount": 784.35,
      |              "lineItemInterestDetails": {
      |                "interestKey": "365/SAIN (LPI)",
      |                "interestStartDate": "2025-02-01"
      |              }
      |            }
      |          ]
      |        },
      |        {
      |          "documentNumber": "523000000065",
      |          "documentType": "ITSA - POA 2",
      |          "chargeReferenceNumber": "XD002610234504",
      |          "businessPartnerNumber": "0100468984",
      |          "contractAccountNumber": "000960031075",
      |          "contractAccountCategory": "Income Tax",
      |          "contractObjectNumber": "00000160000000022665",
      |          "contractObjectType": "ITSA",
      |          "postingDate": "2025-08-07",
      |          "documentTotalAmount": 2527.35,
      |          "documentOutstandingAmount": 2527.35,
      |          "lineItemDetails": [
      |            {
      |              "itemNumber": "0001",
      |              "subItemNumber": "000",
      |              "mainTransaction": "4930",
      |              "subTransaction": "1005",
      |              "chargeDescription": "ITSA England & NI",
      |              "periodFromDate": "2024-04-06",
      |              "periodToDate": "2025-04-05",
      |              "periodKey": "25M0",
      |              "netDueDate": "2025-07-31",
      |              "formBundleNumber": "122000001950",
      |              "amount": 1743,
      |              "lineItemInterestDetails": {
      |                "interestKey": "365/SAIN (LPI)",
      |                "interestStartDate": "2025-08-01"
      |              }
      |            },
      |            {
      |              "itemNumber": "0002",
      |              "subItemNumber": "000",
      |              "mainTransaction": "4930",
      |              "subTransaction": "1010",
      |              "chargeDescription": "NIC4-GB",
      |              "periodFromDate": "2024-04-06",
      |              "periodToDate": "2025-04-05",
      |              "periodKey": "25M0",
      |              "netDueDate": "2025-07-31",
      |              "formBundleNumber": "122000001950",
      |              "amount": 784.35,
      |              "lineItemInterestDetails": {
      |                "interestKey": "365/SAIN (LPI)",
      |                "interestStartDate": "2025-08-01"
      |              }
      |            }
      |          ]
      |        }
      |      ],
      |      "totalon": 1
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