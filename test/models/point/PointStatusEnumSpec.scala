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

package models.point

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class PointStatusEnumSpec extends AnyWordSpec with Matchers {
  "be writable to JSON for 'ACTIVE'" in {
    val result = Json.toJson(PointStatusEnum.Active)
    result shouldBe JsString("ACTIVE")
  }

  "be writable to JSON for 'REJECTED'" in {
    val result = Json.toJson(PointStatusEnum.Rejected)
    result shouldBe JsString("REJECTED")
  }

  "be writable to JSON for 'ADDED'" in {
    val result = Json.toJson(PointStatusEnum.Added)
    result shouldBe JsString("ADDED")
  }

  "be writable to JSON for 'REMOVED'" in {
    val result = Json.toJson(PointStatusEnum.Removed)
    result shouldBe JsString("REMOVED")
  }

  "be writable to JSON for 'DUE'" in {
    val result = Json.toJson(PointStatusEnum.Due)
    result shouldBe JsString("DUE")
  }

  "be writable to JSON for 'PAID'" in {
    val result = Json.toJson(PointStatusEnum.Paid)
    result shouldBe JsString("PAID")
  }

  "be writable to JSON for 'ESTIMATED'" in {
    val result = Json.toJson(PointStatusEnum.Estimated)
    result shouldBe JsString("ESTIMATED")
  }

  "be readable from JSON for 'ACTIVE'" in {
    val result = Json.fromJson(JsString("ACTIVE"))(PointStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PointStatusEnum.Active
  }

  "be readable from JSON for 'REJECTED'" in {
    val result = Json.fromJson(JsString("REJECTED"))(PointStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PointStatusEnum.Rejected
  }

  "be readable from JSON for 'ADDED'" in {
    val result = Json.fromJson(JsString("ADDED"))(PointStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PointStatusEnum.Added
  }

  "be readable from JSON for 'REMOVED'" in {
    val result = Json.fromJson(JsString("REMOVED"))(PointStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PointStatusEnum.Removed
  }

  "be readable from JSON for 'DUE'" in {
    val result = Json.fromJson(JsString("DUE"))(PointStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PointStatusEnum.Due
  }

  "be readable from JSON for 'PAID'" in {
    val result = Json.fromJson(JsString("PAID"))(PointStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PointStatusEnum.Paid
  }

  "be readable from JSON for 'ESTIMATED'" in {
    val result = Json.fromJson(JsString("ESTIMATED"))(PointStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PointStatusEnum.Estimated
  }

  "return a JSError when there is no matches for the specified value" in {
    val result = Json.fromJson(JsString("invalid"))(PointStatusEnum.format)
    result.isError shouldBe true
  }
}
