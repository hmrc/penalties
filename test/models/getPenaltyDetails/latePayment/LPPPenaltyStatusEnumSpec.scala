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
import play.api.libs.json._

class LPPPenaltyStatusEnumSpec extends SpecBase {
  "be writable to JSON for 'Accruing' (A)" in {
    val result = Json.toJson(LPPPenaltyStatusEnum.Accruing)
    result shouldBe JsString("A")
  }

  "be writable to JSON for 'Posted' (P)" in {
    val result = Json.toJson(LPPPenaltyStatusEnum.Posted)
    result shouldBe JsString("P")
  }

  "be readable from JSON for 'Accruing'" in {
    val result = Json.fromJson(JsString("A"))(LPPPenaltyStatusEnum.format)
    result.get shouldBe LPPPenaltyStatusEnum.Accruing
  }

  "be readable from JSON for 'Posted'" in {
    val result = Json.fromJson(JsString("P"))(LPPPenaltyStatusEnum.format)
    result.get shouldBe LPPPenaltyStatusEnum.Posted
  }

  "return JsError when the enum is not readable" in {
    val result = Json.fromJson(JsString("unknown"))(LPPPenaltyStatusEnum.format)
    result.isError shouldBe true
  }
}
