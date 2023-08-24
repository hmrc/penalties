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
}