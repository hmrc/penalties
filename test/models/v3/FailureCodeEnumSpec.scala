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

package models.v3

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class FailureCodeEnumSpec extends SpecBase {
  "be writable to JSON for 'NO_DATA_FOUND'" in {
    val result = Json.toJson(FailureCodeEnum.NoDataFound)
    result shouldBe JsString("NO_DATA_FOUND")
  }

  "be readable from JSON for 'NO_DATA_FOUND'" in {
    val result = Json.fromJson(JsString("NO_DATA_FOUND"))(FailureCodeEnum.format)
    result.get shouldBe FailureCodeEnum.NoDataFound
  }
}
