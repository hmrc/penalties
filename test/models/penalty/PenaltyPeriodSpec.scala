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

package models.penalty

import models.submission.{Submission, SubmissionStatusEnum}
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PenaltyPeriodSpec extends WordSpec with Matchers {
  val penaltyPeriodModelAsJson: JsValue = Json.parse(
    """
      |{
      | "startDate": "2019-01-31T23:59:59.998",
      | "endDate": "2019-01-31T23:59:59.999",
      | "submission": {
      |    "dueDate": "2019-05-31T23:59:59.999",
      |    "submittedDate": "2019-05-30T23:59:59.999",
      |    "status": "SUBMITTED"
      | }
      |}
      |
      |""".stripMargin)

  val penaltyPeriodModel: PenaltyPeriod = PenaltyPeriod(
    startDate = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(998, ChronoUnit.MILLIS),
    endDate = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS),
    submission = Submission(
      dueDate = LocalDateTime.of(2019, 5, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS),
      submittedDate = Some(LocalDateTime.of(2019, 5, 30, 23, 59, 59).plus(999, ChronoUnit.MILLIS)),
      status = SubmissionStatusEnum.Submitted
    )
  )
  "PenaltyPeriod" should {
    "be writeable to JSON" in {
      val result = Json.toJson(penaltyPeriodModel)
      result shouldBe penaltyPeriodModelAsJson
    }

    "be readable from JSON" in {
      val result = Json.fromJson(penaltyPeriodModelAsJson)(PenaltyPeriod.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPeriodModel
    }
  }
}
