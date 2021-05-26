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

class ReturnSpec extends AnyWordSpec with Matchers {
  val returnModelAsJsonNoStatus: JsValue = Json.parse(
    """
      |{
      | "startDate": "2019-01-31T23:59:59.999",
      | "endDate": "2019-01-31T23:59:59.999",
      | "dueDate": "2019-01-31T23:59:59.999"
      |}
      |""".stripMargin)

  val returnModelAsJsonWithStatus: JsValue = Json.parse(
    """
      |{
      | "startDate": "2019-01-31T23:59:59.999",
      | "endDate": "2019-01-31T23:59:59.999",
      | "dueDate": "2019-01-31T23:59:59.999",
      | "status": "SUBMITTED"
      |}
      |""".stripMargin)

  val date = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS)

  val returnModelNoStatus: Return = Return(date, date, date)
  val returnModelWithStatus: Return = Return(date, date, date, Some(ReturnStatusEnum.submitted))

  "returnNoStatus" should {
    "be writeable to JSON" in {
      val result = Json.toJson(returnModelNoStatus)
      result shouldBe returnModelAsJsonNoStatus
    }

    "be readable from JSON" in {
      val result = Json.fromJson(returnModelAsJsonNoStatus)(Return.format)
      result.isSuccess shouldBe true
      result.get shouldBe returnModelNoStatus
    }
  }

  "returnWithStatus" should {
    "be writeable to JSON" in {
      val result = Json.toJson(returnModelWithStatus)
      result shouldBe returnModelAsJsonWithStatus
    }

    "be readable from JSON" in {
      val result = Json.fromJson(returnModelAsJsonWithStatus)(Return.format)
      result.isSuccess shouldBe true
      result.get shouldBe returnModelWithStatus
    }
  }
}
