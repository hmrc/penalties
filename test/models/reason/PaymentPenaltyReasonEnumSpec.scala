/*
 * Copyright 2021 HM Revenue & Customs
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

package models.reason

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class PaymentPenaltyReasonEnumSpec extends AnyWordSpec with Matchers {
  "be writable to JSON for 'VAT_NOT_PAID_AFTER_30_DAYS'" in {
    val result = Json.toJson(PaymentPenaltyReasonEnum.VAT_NOT_PAID_AFTER_30_DAYS)
    result shouldBe JsString("VAT_NOT_PAID_AFTER_30_DAYS")
  }

  "be writable to JSON for 'VAT_NOT_PAID_WITHIN_15_DAYS'" in {
    val result = Json.toJson(PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_15_DAYS)
    result shouldBe JsString("VAT_NOT_PAID_WITHIN_15_DAYS")
  }

  "be writable to JSON for 'VAT_NOT_PAID_WITHIN_30_DAYS'" in {
    val result = Json.toJson(PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS)
    result shouldBe JsString("VAT_NOT_PAID_WITHIN_30_DAYS")
  }

  "be readable from JSON for 'VAT_NOT_PAID_AFTER_30_DAYS'" in {
    val result = Json.fromJson(JsString("VAT_NOT_PAID_AFTER_30_DAYS"))(PaymentPenaltyReasonEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PaymentPenaltyReasonEnum.VAT_NOT_PAID_AFTER_30_DAYS
  }

  "be readable from JSON for 'VAT_NOT_PAID_WITHIN_30_DAYS'" in {
    val result = Json.fromJson(JsString("VAT_NOT_PAID_WITHIN_30_DAYS"))(PaymentPenaltyReasonEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS
  }

  "be readable from JSON for 'VAT_NOT_PAID_WITHIN_15_DAYS'" in {
    val result = Json.fromJson(JsString("VAT_NOT_PAID_WITHIN_15_DAYS"))(PaymentPenaltyReasonEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_15_DAYS
  }
}

