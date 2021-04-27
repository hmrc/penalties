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

package models.point

import models.financial.Financial
import models.penalty.PenaltyPeriod
import models.submission.{Submission, SubmissionStatusEnum}
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsString, JsValue, Json}

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}

class PenaltyPointSpec extends WordSpec with Matchers {

  val sampleDateTime1: LocalDateTime = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(998, ChronoUnit.MILLIS)
  val sampleDateTime2: LocalDateTime = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS)
  val sampleDateTime3: LocalDateTime = LocalDateTime.of(2019, 5, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS)
  val sampleDateTime4: LocalDateTime = LocalDateTime.of(2019, 6, 1, 23, 59, 59).plus(999, ChronoUnit.MILLIS)

  val penaltyPointAsJson = (pointType: PenaltyTypeEnum.Value, dateExpired: Option[LocalDateTime], financial: Option[Financial]) => Json.obj(
    "type" -> pointType,
    "number" -> "1",
    "dateCreated" -> "2019-01-31T23:59:59.998",
    "status" -> PointStatusEnum.Active,
    "period" -> Json.obj(
      "startDate" -> "2019-01-31T23:59:59.998",
      "endDate" -> "2019-01-31T23:59:59.999",
      "submission" -> Json.obj (
      "dueDate" -> "2019-05-31T23:59:59.999",
      "submittedDate" -> "2019-06-01T23:59:59.999",
      "status" -> SubmissionStatusEnum.Submitted
        )
    ),
    "communications" -> Seq.empty[String],
  ).deepMerge(
    dateExpired.fold(Json.obj())(obj => Json.obj("dateExpired" -> obj))
  ).deepMerge(
    financial.fold(Json.obj())(obj => Json.obj("financial" -> obj))
  )

  val penaltyPointAsPointModel: PenaltyPoint = PenaltyPoint(
    `type` = PenaltyTypeEnum.Point,
    number = "1",
    dateCreated = sampleDateTime1,
    dateExpired = Some(sampleDateTime2),
    status = PointStatusEnum.Active,
    period = PenaltyPeriod(
      startDate = sampleDateTime1,
      endDate = sampleDateTime2,
      submission = Submission(
        dueDate = sampleDateTime3,
        submittedDate = Some(sampleDateTime4),
        status = SubmissionStatusEnum.Submitted
      )
    ),
    communications = Seq.empty,
    financial = None
  )

  val penaltyPointAsPointModelWithNoDateExpired: PenaltyPoint = penaltyPointAsPointModel.copy(
    dateExpired = None
  )

  val penaltyPointAsFinancialPenaltyModel: PenaltyPoint = penaltyPointAsPointModel.copy(
    `type` = PenaltyTypeEnum.Financial,
    financial = Some(Financial(300.00, sampleDateTime3))
  )

  val penaltyPointAsFinancialPenaltyModelNoDateExpired: PenaltyPoint = penaltyPointAsPointModel.copy(
    `type` = PenaltyTypeEnum.Financial,
    dateExpired = None,
    financial = Some(Financial(300.00, sampleDateTime3))
  )

  "PenaltyPoint" should {
    s"be writeable to JSON when the point type is ${PenaltyTypeEnum.Financial}" in {
      val result = Json.toJson(penaltyPointAsFinancialPenaltyModel)
      result shouldBe penaltyPointAsJson(PenaltyTypeEnum.Financial, Some(sampleDateTime2), Some(Financial(300.00, sampleDateTime3)))
    }

    s"be writeable to JSON when the point type is ${PenaltyTypeEnum.Point}" in {
      val result = Json.toJson(penaltyPointAsPointModel)
      result shouldBe penaltyPointAsJson(PenaltyTypeEnum.Point, Some(sampleDateTime2), None)
    }

    s"be readable from JSON when the point type is ${PenaltyTypeEnum.Financial}" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Financial, Some(sampleDateTime2), Some(Financial(300.00, sampleDateTime3))
      ))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsFinancialPenaltyModel
    }

    s"be readable from JSON when the point type is ${PenaltyTypeEnum.Point}" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Point, Some(sampleDateTime2), None))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsPointModel
    }

    "be readable from JSON when there is no dateExpired KV in the JSON" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Financial, None, Some(Financial(300.00, sampleDateTime3))))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsFinancialPenaltyModelNoDateExpired
    }

    "be readable from JSON when there is no financial KV i.e. normal penalty point, in the JSON" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Point, None, None))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsPointModelWithNoDateExpired
    }
  }
}
