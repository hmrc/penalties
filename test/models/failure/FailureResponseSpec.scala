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

package models.failure

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

class FailureResponseSpec extends SpecBase {
  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      | "code": "NO_DATA_FOUND",
      | "reason": "This is some reason"
      |}
      |""".stripMargin)

  val model: FailureResponse = FailureResponse(
    code = FailureCodeEnum.NoDataFound, reason = "This is some reason"
  )

  "be readable from JSON" in {
    val result = Json.fromJson[FailureResponse](jsonRepresentingModel)(FailureResponse.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result = Json.toJson(model)(FailureResponse.format)
    result shouldBe jsonRepresentingModel
  }
}
