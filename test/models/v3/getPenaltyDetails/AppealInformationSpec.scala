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

package models.v3.getPenaltyDetails

import base.SpecBase
import play.api.libs.json.{JsResult, JsValue, Json}

class AppealInformationSpec extends SpecBase {
  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      | "appealStatus": "99",
      | "appealLevel": "01"
      |}
      |""".stripMargin)

  val model: AppealInformation = AppealInformation(
    appealStatus = Some("99"), appealLevel = Some("01")
  )

  "be readable from JSON" in {
    val result: JsResult[AppealInformation] = Json.fromJson(jsonRepresentingModel)(AppealInformation.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result: JsValue = Json.toJson(model)
    result shouldBe jsonRepresentingModel
  }
}
