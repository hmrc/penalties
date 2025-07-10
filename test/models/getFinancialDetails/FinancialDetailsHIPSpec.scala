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

package models.getFinancialDetails

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

class FinancialDetailsHIPSpec extends SpecBase {

  val jsonHipResponseWithoutSuccessResponse: JsValue = Json.parse(
    """
      |{
      |  "processingDate": "2023-11-28T10:15:10Z",
      |  "financialData": {
      |    "totalisation": {
      |      "regimeTotalisation": {
      |        "totalAccountOverdue": 1000
      |      },
      |      "interestTotalisations": {
      |        "totalAccountPostedInterest": 123.45,
      |        "totalAccountAccruingInterest": 23.45
      |      }
      |    },
      |    "documentDetails": [
      |      {
      |        "chargeReferenceNumber": "1234567890",
      |        "documentOutstandingAmount": 123.45,
      |        "documentTotalAmount": 100,
      |        "lineItemDetails": [
      |          {
      |            "mainTransaction": "4703"
      |          }
      |        ],
      |        "issueDate": "2023-01-01"
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin
  )

  "be readable from JSON" in {
    val result = Json.fromJson(jsonHipResponse)(FinancialDetailsHIP.reads)
    result.isSuccess shouldBe true
    result.get shouldBe financialDetailsHip
  }

  "be writable to JSON, removing the HIP 'success' wrapper" in {
    val result = Json.toJson(financialDetailsHip)(FinancialDetailsHIP.writes)
    result shouldBe jsonHipResponseWithoutSuccessResponse
  }
}
