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

package models.getPenaltyDetails.appealInfo

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class AppealLevelEnumSpec extends SpecBase {
  "AppealLevelEnum" should {
    "be writable to JSON for appeal level '01'" in {
      val result = Json.toJson(AppealLevelEnum.HMRC)(AppealLevelEnum.format)
      result shouldBe JsString("01")
    }

    "be writable to JSON for appeal level '02'" in {
      val result = Json.toJson(AppealLevelEnum.Tribunal)(AppealLevelEnum.format)
      result shouldBe JsString("02")
    }

    "be readable from JSON for appeal level '01'" in{
      val result = Json.fromJson(JsString("01"))(AppealLevelEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe AppealLevelEnum.HMRC
    }

    "be readable from JSON for appeal level '02'" in{
      val result = Json.fromJson(JsString("02"))(AppealLevelEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe AppealLevelEnum.Tribunal
    }

    "return JsError when the enum is not recognised" in {
      val result = Json.fromJson(JsString("error"))(AppealLevelEnum.format)
      result.isError shouldBe true
    }
  }

}
