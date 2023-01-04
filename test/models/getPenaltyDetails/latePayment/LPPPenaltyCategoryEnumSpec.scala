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

package models.getPenaltyDetails.latePayment

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class LPPPenaltyCategoryEnumSpec extends SpecBase {

  "be writable to JSON for 'First Penalty' (LPP1)" in {
    val result = Json.toJson(LPPPenaltyCategoryEnum.FirstPenalty)
    result shouldBe JsString("LPP1")
  }

  "be writable to JSON for 'Second Penalty' (LPP2)" in {
    val result = Json.toJson(LPPPenaltyCategoryEnum.SecondPenalty)
    result shouldBe JsString("LPP2")
  }

  "be readable from JSON for 'First Penalty' (LPP1)" in {
    val result = Json.fromJson(JsString("LPP1"))(LPPPenaltyCategoryEnum.format)
    result.get shouldBe LPPPenaltyCategoryEnum.FirstPenalty
  }

  "be readable from JSON for 'Second Penalty' (LPP2)" in {
    val result = Json.fromJson(JsString("LPP2"))(LPPPenaltyCategoryEnum.format)
    result.get shouldBe LPPPenaltyCategoryEnum.SecondPenalty
  }

  "return JsError when the enum is not readable" in {
    val result = Json.fromJson(JsString("unknown"))(LPPPenaltyCategoryEnum.format)
    result.isError shouldBe true
  }
}
