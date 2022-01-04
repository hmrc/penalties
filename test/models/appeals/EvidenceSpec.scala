/*
 * Copyright 2022 HM Revenue & Customs
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

package models.appeals

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class EvidenceSpec extends AnyWordSpec with Matchers {
  val evidenceModelAsJsonNoReference: JsValue = Json.parse(
    """
      |{
      |   "noOfUploadedFiles": 1
      |}
      |""".stripMargin)

  val evidenceModelAsJsonWithReference: JsValue = Json.parse(
    """
      |{
      |   "noOfUploadedFiles": 1,
      |   "referenceId": "ref1"
      |}
      |""".stripMargin)

  val evidenceModelWithNoReference: Evidence = Evidence(1, None)
  val evidenceModelWithReference: Evidence = Evidence(1, Some("ref1"))

  "be writable to JSON (with reference)" in {
    val result = Json.toJson(evidenceModelWithReference)(Evidence.format)
    result shouldBe evidenceModelAsJsonWithReference
  }

  "be readable from JSON (with reference)" in {
    val result = Json.fromJson(evidenceModelAsJsonWithReference)(Evidence.format)
    result.isSuccess shouldBe true
    result.get shouldBe evidenceModelWithReference
  }

  "be writable to JSON (without reference)" in {
    val result = Json.toJson(evidenceModelWithNoReference)(Evidence.format)
    result shouldBe evidenceModelAsJsonNoReference
  }

  "be readable from JSON (without reference)" in {
    val result = Json.fromJson(evidenceModelAsJsonNoReference)(Evidence.format)
    result.isSuccess shouldBe true
    result.get shouldBe evidenceModelWithNoReference
  }
}
