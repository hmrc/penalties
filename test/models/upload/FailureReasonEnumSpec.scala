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

package models.upload

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class FailureReasonEnumSpec extends AnyWordSpec with Matchers {

  "FailureReasonEnum" should {

    "be writable to Json" when {

      "the upload was Quarantined" in {
        val result = Json.toJson(FailureReasonEnum.QUARANTINE)
        result shouldBe JsString("QUARANTINE")
      }

      "the upload was Rejected" in {
        val result = Json.toJson(FailureReasonEnum.REJECTED)
        result shouldBe JsString("REJECTED")
      }

      "there was some other problem with the file" in {
        val result = Json.toJson(FailureReasonEnum.UNKNOWN)
        result shouldBe JsString("UNKNOWN")
      }
    }

    "be readable from json" when {

      "the upload was Quarantined" in {
        val result = Json.fromJson(JsString("QUARANTINE"))(FailureReasonEnum.format)
        result.isSuccess shouldBe true
        result.get shouldBe FailureReasonEnum.QUARANTINE
      }

      "the upload was Rejected" in {
        val result = Json.fromJson(JsString("REJECTED"))(FailureReasonEnum.format)
        result.isSuccess shouldBe true
        result.get shouldBe FailureReasonEnum.REJECTED
      }

      "there was some other problem with the file" in {
        val result = Json.fromJson(JsString("UNKNOWN"))(FailureReasonEnum.format)
        result.isSuccess shouldBe true
        result.get shouldBe FailureReasonEnum.UNKNOWN
      }

      "return a JSError for an unrecognised value" in {
        val result = Json.fromJson(JsString("INVALID"))(FailureReasonEnum.format)
        result.isError shouldBe true
      }
    }
  }
}
