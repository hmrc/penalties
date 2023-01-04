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

package models.compliance

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class ObligationIdentificationSpec extends AnyWordSpec with Matchers {
  val identificationWithNoIncomeSourceTypeAsJson: JsValue = Json.parse(
    """
      |{
      | "referenceNumber": "123456789",
      | "referenceType": "VRN"
      |}
      |""".stripMargin)

  val identificationWithIncomeSourceTypeAsJson: JsValue = Json.parse(
    """
      |{
      | "incomeSourceType": "ITSA",
      | "referenceNumber": "123456789",
      | "referenceType": "VRN"
      |}
      |""".stripMargin)

  val identificationWithNoIncomeSourceTypeAsModel: ObligationIdentification = ObligationIdentification(
    incomeSourceType = None,
    referenceNumber = "123456789",
    referenceType = "VRN"
  )

  val identificationWithIncomeSourceTypeAsModel: ObligationIdentification = ObligationIdentification(
    incomeSourceType = Some("ITSA"),
    referenceNumber = "123456789",
    referenceType = "VRN"
  )

  "ObligationIdentification" should {
    "correctly parse the model from JSON for models with no income source type" in {
      val result = Json.fromJson(identificationWithNoIncomeSourceTypeAsJson)(ObligationIdentification.format)
      result.isSuccess shouldBe true
      result.get shouldBe identificationWithNoIncomeSourceTypeAsModel
    }

    "correctly parse the model from JSON for models with income source type" in {
      val result = Json.fromJson(identificationWithIncomeSourceTypeAsJson)(ObligationIdentification.format)
      result.isSuccess shouldBe true
      result.get shouldBe identificationWithIncomeSourceTypeAsModel
    }

    "correctly parse the model to JSON for models with no income source type" in {
      val result = Json.toJson(identificationWithNoIncomeSourceTypeAsModel)(ObligationIdentification.format)
      result shouldBe identificationWithNoIncomeSourceTypeAsJson
    }

    "correctly parse the model to JSON for models with income source type" in {
      val result = Json.toJson(identificationWithIncomeSourceTypeAsModel)(ObligationIdentification.format)
      result shouldBe identificationWithIncomeSourceTypeAsJson
    }
  }
}
