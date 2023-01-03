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

package models.compliance

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class ComplianceStatusEnumSpec extends AnyWordSpec with Matchers {

  "ComplianceStatusEnum" should {
    "be writable to JSON for 'O'" in {
      val result = Json.toJson(ComplianceStatusEnum.open)
      result shouldBe JsString("O")
    }

    "be writable to JSON for 'F'" in {
      val result = Json.toJson(ComplianceStatusEnum.fulfilled)
      result shouldBe JsString("F")
    }

    "be readable from JSON for 'O'" in {
      val result = Json.fromJson(JsString("O"))(ComplianceStatusEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ComplianceStatusEnum.open
    }

    "be readable from JSON for 'F'" in {
      val result = Json.fromJson(JsString("F"))(ComplianceStatusEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ComplianceStatusEnum.fulfilled
    }

    "return a JSError when there is no matches for the specified value" in {
      val result = Json.fromJson(JsString("invalid"))(ComplianceStatusEnum.format)
      result.isError shouldBe true
    }
  }

}
