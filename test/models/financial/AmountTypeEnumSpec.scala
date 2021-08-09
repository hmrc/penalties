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

package models.financial

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsString, Json}

class AmountTypeEnumSpec extends AnyWordSpec with Matchers {
  "be writable to JSON for 'VAT'" in {
    val result = Json.toJson(AmountTypeEnum.VAT)
    result shouldBe JsString("VAT")
  }

  "be writable to JSON for 'CENTRAL_ASSESSMENT'" in {
    val result = Json.toJson(AmountTypeEnum.Central_Assessment)
    result shouldBe JsString("CENTRAL_ASSESSMENT")
  }

  "be writable to JSON for 'OFFICERS_ASSESSMENT'" in {
    val result = Json.toJson(AmountTypeEnum.Officers_Assessment)
    result shouldBe JsString("OFFICERS_ASSESSMENT")
  }

  "be writable to JSON for 'ECN'" in {
    val result = Json.toJson(AmountTypeEnum.ECN)
    result shouldBe JsString("ECN")
  }

  "be readable from JSON for 'VAT'" in {
    val result = Json.fromJson(JsString("VAT"))(AmountTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AmountTypeEnum.VAT
  }

  "be readable from JSON for 'CENTRAL_ASSESSMENT'" in {
    val result = Json.fromJson(JsString("CENTRAL_ASSESSMENT"))(AmountTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AmountTypeEnum.Central_Assessment
  }

  "be readable from JSON for 'OFFICERS_ASSESSMENT'" in {
    val result = Json.fromJson(JsString("OFFICERS_ASSESSMENT"))(AmountTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AmountTypeEnum.Officers_Assessment
  }

  "be readable from JSON for 'ECN'" in {
    val result = Json.fromJson(JsString("ECN"))(AmountTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AmountTypeEnum.ECN
  }

  "return a JSError when there is no matches for the specified value" in {
    val result = Json.fromJson(JsString("invalid"))(AmountTypeEnum.format)
    result.isError shouldBe true
  }
}
