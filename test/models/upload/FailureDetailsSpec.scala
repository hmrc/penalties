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

class FailureDetailsSpec extends AnyWordSpec with Matchers {
  val failureDetailsAsJson: JsValue = Json.parse(
    """
      |{
      | "failureReason": "REJECTED",
      | "message": "this file was rejected"
      |}
      |""".stripMargin
  )

  val failureDetailsModel: FailureDetails = FailureDetails(
    failureReason = FailureReasonEnum.REJECTED,
    message = "this file was rejected"
  )

  "FailureDetailsSpec" should {
    "be readable from JSON" in {
      val result = Json.fromJson(failureDetailsAsJson)(FailureDetails.format)
      result.isSuccess shouldBe true
      result.get shouldBe failureDetailsModel
    }

    "be writable to JSON" in {
      val result = Json.toJson(failureDetailsModel)(FailureDetails.format)
      result shouldBe failureDetailsAsJson
    }
  }
}
