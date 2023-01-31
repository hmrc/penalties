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

class LSPPenaltyCategoryEnumSpec extends SpecBase {
  "be writable to JSON for 'Point' (P)" in {
    val result = Json.toJson(LSPPenaltyCategoryEnum.Point)
    result shouldBe JsString("P")
  }

  "be writable to JSON for 'Threshold' (T)" in {
    val result = Json.toJson(LSPPenaltyCategoryEnum.Threshold)
    result shouldBe JsString("T")
  }

  "be writable to JSON for 'Charge' (C)" in {
    val result = Json.toJson(LSPPenaltyCategoryEnum.Charge)
    result shouldBe JsString("C")
  }

  "be readable from JSON for 'Point' (P)" in {
    val result = Json.fromJson(JsString("P"))(LSPPenaltyCategoryEnum.format)
    result.get shouldBe LSPPenaltyCategoryEnum.Point
  }

  "be readable from JSON for 'Threshold' (T)" in {
    val result = Json.fromJson(JsString("T"))(LSPPenaltyCategoryEnum.format)
    result.get shouldBe LSPPenaltyCategoryEnum.Threshold
  }

  "be readable from JSON for 'Charge' (C)" in {
    val result = Json.fromJson(JsString("C"))(LSPPenaltyCategoryEnum.format)
    result.get shouldBe LSPPenaltyCategoryEnum.Charge
  }

  "be readable from JSON for 'Point' (P) when Category is blank" in {
    val result = Json.fromJson(JsString(" "))(LSPPenaltyCategoryEnum.format)
    result.get shouldBe LSPPenaltyCategoryEnum.Point
  }

  "return JsError when the enum is not readable" in {
    val result = Json.fromJson(JsString("unknown"))(LSPPenaltyCategoryEnum.format)
    result.isError shouldBe true
  }
}
