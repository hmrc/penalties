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

package models.submission

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class SubmissionStatusEnumSpec extends AnyWordSpec with Matchers {
  "be writable to JSON for 'SUBMITTED'" in {
    val result = Json.toJson(SubmissionStatusEnum.Submitted)
    result shouldBe JsString("SUBMITTED")
  }

  "be writable to JSON for 'OVERDUE'" in {
    val result = Json.toJson(SubmissionStatusEnum.Overdue)
    result shouldBe JsString("OVERDUE")
  }

  "be writable to JSON for 'TAX_TRIBUNAL'" in {
    val result = Json.toJson(SubmissionStatusEnum.Tax_Tribunal)
    result shouldBe JsString("TAX_TRIBUNAL")
  }

  "be writable to JSON for 'UNDER_REVIEW'" in {
    val result = Json.toJson(SubmissionStatusEnum.Under_Review)
    result shouldBe JsString("UNDER_REVIEW")
  }

  "be readable from JSON for 'SUBMITTED'" in {
    val result = Json.fromJson(JsString("SUBMITTED"))(SubmissionStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe SubmissionStatusEnum.Submitted
  }

  "be readable from JSON for 'OVERDUE'" in {
    val result = Json.fromJson(JsString("OVERDUE"))(SubmissionStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe SubmissionStatusEnum.Overdue
  }

  "be readable from JSON for 'TAX_TRIBUNAL'" in {
    val result = Json.fromJson(JsString("TAX_TRIBUNAL"))(SubmissionStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe SubmissionStatusEnum.Tax_Tribunal
  }

  "be readable from JSON for 'UNDER_REVIEW'" in {
    val result = Json.fromJson(JsString("UNDER_REVIEW"))(SubmissionStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe SubmissionStatusEnum.Under_Review
  }

  "return a JSError when there is no matches for the specified value" in {
    val result = Json.fromJson(JsString("invalid"))(SubmissionStatusEnum.format)
    result.isError shouldBe true
  }
}
