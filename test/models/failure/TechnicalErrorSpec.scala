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

package models.failure

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

class TechnicalErrorSpec extends SpecBase {

  val model: TechnicalError = TechnicalError("code", "message", "logId")
  val json: JsValue         = Json.parse("""{"code":"code", "message":"message", "logId":"logId"}""")

  "TechnicalError" should {
    "be writable to JSON" in {
      val result = Json.toJson(model)
      result shouldBe json
    }
    "be readable from JSON" in {
      val result = Json.fromJson(json)(TechnicalError.format)
      result.get shouldBe model
    }
  }
}
