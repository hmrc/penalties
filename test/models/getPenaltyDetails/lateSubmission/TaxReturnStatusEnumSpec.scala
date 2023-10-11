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

package models.getPenaltyDetails.lateSubmission

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class TaxReturnStatusEnumSpec extends SpecBase {

  "be readable from JSON for OPEN" in {
    val result = Json.fromJson(JsString("Open"))(TaxReturnStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe TaxReturnStatusEnum.Open
  }

  "be readable from JSON for FULFILLED" in {
    val result = Json.fromJson(JsString("Fulfilled"))(TaxReturnStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe TaxReturnStatusEnum.Fulfilled
  }

  "be readable from JSON for REVERSED" in {
    val result = Json.fromJson(JsString("Reversed"))(TaxReturnStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe TaxReturnStatusEnum.Reversed
  }

  "be writable to JSON for OPEN" in {
    val result = Json.toJson(TaxReturnStatusEnum.Open)(TaxReturnStatusEnum.format)
    result shouldBe JsString("Open")
  }

  "be writable to JSON for FULFILLED" in {
    val result = Json.toJson(TaxReturnStatusEnum.Fulfilled)(TaxReturnStatusEnum.format)
    result shouldBe JsString("Fulfilled")
  }

  "be writable to JSON for REVERSED" in {
    val result = Json.toJson(TaxReturnStatusEnum.Reversed)(TaxReturnStatusEnum.format)
    result shouldBe JsString("Reversed")
  }

  "return a JSError for an unrecognised value" in {
    val result = Json.fromJson(JsString("INVALID"))(TaxReturnStatusEnum.format)
    result.isError shouldBe true
  }

}
