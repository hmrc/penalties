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

class PenaltyTypeEnumSpec extends WordSpec with Matchers {
  "be writable to JSON for 'point'" in {
    val result = Json.toJson(PenaltyTypeEnum.Point)
    result shouldBe JsString("point")
  }

  "be writable to JSON for 'financial'" in {
    val result = Json.toJson(PenaltyTypeEnum.Financial)
    result shouldBe JsString("financial")
  }

  "be readable from JSON for 'point'" in {
    val result = Json.fromJson(JsString("point"))(PenaltyTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PenaltyTypeEnum.Point
  }

  "be readable from JSON for 'financial'" in {
    val result = Json.fromJson(JsString("financial"))(PenaltyTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe PenaltyTypeEnum.Financial
  }

  "return a JSError when there is no matches for the specified value" in {
    val result = Json.fromJson(JsString("invalid"))(PenaltyTypeEnum.format)
    result.isError shouldBe true
  }
}
