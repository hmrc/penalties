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

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsString, Json}

class PointStatusEnumSpec extends WordSpec with Matchers {
  "be writable to JSON for 'ACTIVE'" in {
    val result = Json.toJson(PointStatusEnum.Active)
    result shouldBe JsString("ACTIVE")
  }

  "be writable to JSON for 'REJECTED'" in {
    val result = Json.toJson(PointStatusEnum.Rejected)
    result shouldBe JsString("REJECTED")
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

  "return a JSError when there is no matches for the specified value" in {
    val result = Json.fromJson(JsString("invalid"))(PointStatusEnum.format)
    result.isError shouldBe true
  }
}
