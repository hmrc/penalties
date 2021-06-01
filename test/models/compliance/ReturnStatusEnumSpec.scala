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

package models.compliance

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Json}

class ReturnStatusEnumSpec extends AnyWordSpec with Matchers {
  "be writable to JSON for 'SUBMITTED'" in {
    val result = Json.toJson(ReturnStatusEnum.submitted)
    result shouldBe JsString("SUBMITTED")
  }

  "return a JSError when there is no matches for the specified value" in {
    val result = Json.fromJson(JsString("invalid"))(ReturnStatusEnum.format)
    result.isError shouldBe true
  }
}
