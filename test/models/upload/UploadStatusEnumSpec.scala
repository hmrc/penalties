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

class UploadStatusEnumSpec extends AnyWordSpec with Matchers {

  "UploadStatusEnum" should {

    "be writable to Json" when {

      "we are waiting for a call back from UpScan" in {
        val result = Json.toJson(UploadStatusEnum.WAITING)
        result shouldBe JsString("WAITING")
      }

      "the upload succeeded" in {
        val result = Json.toJson(UploadStatusEnum.READY)
        result shouldBe JsString("READY")
      }

      "the upload failed" in {
        val result = Json.toJson(UploadStatusEnum.FAILED)
        result shouldBe JsString("FAILED")
      }
    }

    "be readable from json" when {
      "we are waiting for a call back from UpScan" in {
        val result = Json.fromJson(JsString("WAITING"))(UploadStatusEnum.format)
        result.isSuccess shouldBe true
        result.get shouldBe UploadStatusEnum.WAITING
      }

      "the upload succeeded" in {
        val result = Json.fromJson(JsString("READY"))(UploadStatusEnum.format)
        result.isSuccess shouldBe true
        result.get shouldBe UploadStatusEnum.READY
      }

      "the upload failed" in {
        val result = Json.fromJson(JsString("FAILED"))(UploadStatusEnum.format)
        result.isSuccess shouldBe true
        result.get shouldBe UploadStatusEnum.FAILED
      }

      "return a JSError for an unrecognised value" in {
        val result = Json.fromJson(JsString("INVALID"))(UploadStatusEnum.format)
        result.isError shouldBe true
      }
    }
  }
}
