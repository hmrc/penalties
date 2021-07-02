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

package models.appeals

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class AppealStatusEnumSpec extends AnyWordSpec with Matchers {

  "be readable from JSON for 'Under_Review'" in {
    val result = Json.fromJson(JsString("UNDER_REVIEW"))(AppealStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AppealStatusEnum.Under_Review
  }

  "be writable to JSON for 'Under_Review'" in {
    val result = Json.toJson(AppealStatusEnum.Under_Review)
    result shouldBe JsString("UNDER_REVIEW")
  }

  "be readable from JSON for 'Accepted'" in {
    val result = Json.fromJson(JsString("ACCEPTED"))(AppealStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AppealStatusEnum.Accepted
  }

  "be writable to JSON for 'Accepted'" in {
    val result = Json.toJson(AppealStatusEnum.Accepted)
    result shouldBe JsString("ACCEPTED")
  }

  "be readable from JSON for 'Rejected'" in {
    val result = Json.fromJson(JsString("REJECTED"))(AppealStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AppealStatusEnum.Rejected
  }

  "be writable to JSON for 'Rejected'" in {
    val result = Json.toJson(AppealStatusEnum.Rejected)
    result shouldBe JsString("REJECTED")
  }

  "be readable from JSON for 'Reinstated'" in {
    val result = Json.fromJson(JsString("REINSTATED"))(AppealStatusEnum.format)
    result.isSuccess shouldBe true
    result.get shouldBe AppealStatusEnum.Reinstated
  }

  "be writable to JSON for 'Reinstated'" in {
    val result = Json.toJson(AppealStatusEnum.Reinstated)
    result shouldBe JsString("REINSTATED")

    "be readable from JSON for 'Tribunal_Rejected'" in {
      val result = Json.fromJson(JsString("TRIBUNAL_REJECTED"))(AppealStatusEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe AppealStatusEnum.Tribunal_Rejected
    }

    "be writable to JSON for 'Tribunal_Rejected'" in {
      val result = Json.toJson(AppealStatusEnum.Tribunal_Rejected)
      result shouldBe JsString("TRIBUNAL_REJECTED")
    }
  }
}
