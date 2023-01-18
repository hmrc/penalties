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
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime

class UploadDetailsSpec extends AnyWordSpec with Matchers {
  val uploadDetailsAsJson: JsValue = Json.parse(
    """
      |{
      | "fileName": "file1.txt",
      | "fileMimeType": "text/plain",
      | "uploadTimestamp": "2018-04-24T09:30:00",
      | "checksum": "check123456789",
      | "size": 1
      |}
      |""".stripMargin)

  val uploadDetailsModel: UploadDetails = UploadDetails(
    fileName = "file1.txt",
    fileMimeType = "text/plain",
    uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30, 0),
    checksum = "check123456789",
    size = 1
  )

  "UploadDetailsSpec" should {
    "be readable from JSON" in {
      val result = Json.fromJson(uploadDetailsAsJson)(UploadDetails.format)
      result.isSuccess shouldBe true
      result.get shouldBe uploadDetailsModel
    }

    "be writable to JSON" in {
      val result = Json.toJson(uploadDetailsModel)(UploadDetails.format)
      result shouldBe uploadDetailsAsJson
    }
  }
}
