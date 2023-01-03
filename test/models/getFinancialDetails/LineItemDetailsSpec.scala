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

class LineItemDetailsSpec extends SpecBase {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      | "mainTransaction":"4703"
      |}
      |""".stripMargin)

  val model: LineItemDetails = LineItemDetails(
    Some(MainTransactionEnum.VATReturnFirstLPP)
  )

  "be readable from JSON" in {
    val result = Json.fromJson(modelAsJson)(LineItemDetails.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result = Json.toJson(model)(LineItemDetails.format)
    result shouldBe modelAsJson
  }
}
