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

package models.communication

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class CommunicationTypeEnumSpec extends AnyWordSpec with Matchers {

  "CommunicationTypeEnum" should {
    "be writable to JSON for 'secureMessage'" in {
      val result = Json.toJson(CommunicationTypeEnum.secureMessage)
      result shouldBe JsString("secureMessage")
    }

    "be writable to JSON for 'letter'" in {
      val result = Json.toJson(CommunicationTypeEnum.letter)
      result shouldBe JsString("letter")
    }

    "be readable from JSON for 'secureMessage'" in {
      val result = Json.fromJson(JsString("secureMessage"))(CommunicationTypeEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe CommunicationTypeEnum.secureMessage
    }

    "be readable from JSON for 'letter'" in {
      val result = Json.fromJson(JsString("letter"))(CommunicationTypeEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe CommunicationTypeEnum.letter
    }

    "return a JSError when there is no matches for the specified value" in {
      val result = Json.fromJson(JsString("invalid"))(CommunicationTypeEnum.format)
      result.isError shouldBe true
    }
  }
}
