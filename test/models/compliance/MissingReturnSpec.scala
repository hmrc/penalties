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

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class MissingReturnSpec extends AnyWordSpec with Matchers {
  val missingReturnJson: JsValue = Json.parse(
    """
      |{
      | "startDate": "2019-01-31T23:59:59.999",
      | "endDate": "2019-01-31T23:59:59.999"
      |}
      |""".stripMargin)

  val date = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS)

  val missingReturn: MissingReturn = MissingReturn(date, date)

  "missingReturn" should {
    "be writeable to JSON" in {
      val result = Json.toJson(missingReturn)
      result shouldBe missingReturnJson
    }

    "be readable from JSON" in {
      val result = Json.fromJson(missingReturnJson)(MissingReturn.format)
      result.isSuccess shouldBe true
      result.get shouldBe missingReturn
    }
  }
}
