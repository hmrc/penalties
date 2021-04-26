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

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsString, JsValue, Json}

import java.time.LocalDateTime

class SubmissionSpec extends WordSpec with Matchers {
  val sampleDate1: LocalDateTime = LocalDateTime.of(2020, 1, 1, 1, 0, 1)
  val sampleDate2: LocalDateTime = LocalDateTime.of(2020, 1, 1, 2, 0, 1)

  val submissionAsModelWithSubmittedDate: Submission = Submission(
    dueDate = sampleDate1,
    submittedDate = Some(sampleDate2),
    status = SubmissionStatusEnum.Submitted
  )

  val submissionAsModel: Submission = Submission(
    dueDate = sampleDate1,
    submittedDate = None,
    status = SubmissionStatusEnum.Submitted
  )

  val submissionWithSubmittedDateAsJson: JsValue = Json.parse(
    s"""
      |{
      |   "dueDate": "$sampleDate1",
      |   "submittedDate": "$sampleDate2",
      |   "status": ${Json.toJson(SubmissionStatusEnum.Submitted)}
      |}
      |""".stripMargin)

  val submissionAsJson: JsValue = Json.parse(
    s"""
       |{
       |   "dueDate": "$sampleDate1",
       |   "status": ${Json.toJson(SubmissionStatusEnum.Submitted)}
       |}
       |""".stripMargin)

  "Submission" should {
    "be writable to JSON" in {
      val result = Json.toJson(submissionAsModelWithSubmittedDate)
      result shouldBe submissionWithSubmittedDateAsJson
    }

    "be writable to JSON with no submitted date" in {
      val result = Json.toJson(submissionAsModel)
      result shouldBe submissionAsJson
    }

    "be readable from JSON" in {
      val result = Json.fromJson(submissionWithSubmittedDateAsJson)(Submission.format)
      result.isSuccess shouldBe true
      result.get shouldBe submissionAsModelWithSubmittedDate
    }

    "be readable from JSON with no submitted date" in {
      val result = Json.fromJson(submissionAsJson)(Submission.format)
      result.isSuccess shouldBe true
      result.get shouldBe submissionAsModel
    }
  }
}
