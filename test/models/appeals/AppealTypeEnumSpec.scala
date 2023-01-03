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

package models.appeals

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class AppealTypeEnumSpec extends AnyWordSpec with Matchers {

  "be writable to JSON for 'LATE_SUBMISSION'" in {
    val result = Json.toJson(AppealTypeEnum.Late_Submission)
    result shouldBe JsString("LATE_SUBMISSION")
  }

  "be writable to JSON for 'LATE_PAYMENT'" in {
    val result = Json.toJson(AppealTypeEnum.Late_Payment)
    result shouldBe JsString("LATE_PAYMENT")
  }

  "be writable to JSON for 'ADDITIONAL'" in {
    val result = Json.toJson(AppealTypeEnum.Additional)
    result shouldBe JsString("ADDITIONAL")
  }

  "be readable from JSON for 'LATE_SUBMISSION'" in {
    val result = Json.fromJson(JsString("LATE_SUBMISSION"))(AppealTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AppealTypeEnum.Late_Submission
  }

  "be writable from JSON for 'LATE_PAYMENT'" in {
    val result = Json.fromJson(JsString("LATE_PAYMENT"))(AppealTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AppealTypeEnum.Late_Payment
  }

  "be writable from JSON for 'ADDITIONAL'" in {
    val result = Json.fromJson(JsString("ADDITIONAL"))(AppealTypeEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AppealTypeEnum.Additional
  }

  "return a JSError when there is no matches for the specified value" in {
    val result = Json.fromJson(JsString("invalid"))(AppealTypeEnum.format)
    result.isError shouldBe true
  }
}
