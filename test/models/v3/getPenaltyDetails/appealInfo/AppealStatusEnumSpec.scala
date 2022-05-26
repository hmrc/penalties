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

package models.v3.getPenaltyDetails.appealInfo

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class AppealStatusEnumSpec extends SpecBase {

  "AppealStatusEnum" should {
    "be writable to JSON for UNDER_APPEAL" in {
      val result = Json.toJson(AppealStatusEnum.Under_Appeal)(AppealStatusEnum.format)
      result shouldBe JsString("A")
    }

    "be writable to JSON for UPHELD" in {
      val result = Json.toJson(AppealStatusEnum.Upheld)(AppealStatusEnum.format)
      result shouldBe JsString("B")
    }

    "be writable to JSON for REJECTED" in {
      val result = Json.toJson(AppealStatusEnum.Rejected)(AppealStatusEnum.format)
      result shouldBe JsString("C")
    }

    "be writable to JSON for UNAPPEALABLE" in {
      val result = Json.toJson(AppealStatusEnum.Unappealable)(AppealStatusEnum.format)
      result shouldBe JsString("99")
    }

    "be readable from JSON for UNDER_APPEAL" in{
      val result = Json.fromJson(JsString("A"))(AppealStatusEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe AppealStatusEnum.Under_Appeal
    }

    "be readable from JSON for UPHELD" in{
      val result = Json.fromJson(JsString("B"))(AppealStatusEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe AppealStatusEnum.Upheld
    }

    "be readable from JSON for REJECTED" in{
      val result = Json.fromJson(JsString("C"))(AppealStatusEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe AppealStatusEnum.Rejected
    }

    "be readable from JSON for UNAPPEALABLE" in{
      val result = Json.fromJson(JsString("99"))(AppealStatusEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe AppealStatusEnum.Unappealable
    }

    "return JsError when the enum is not recognised" in {
      val result = Json.fromJson(JsString("error"))(AppealStatusEnum.format)
      result.isError shouldBe true
    }
  }

}
