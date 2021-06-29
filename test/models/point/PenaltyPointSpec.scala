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

import models.appeals.AppealStatusEnum

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import models.financial.Financial
import models.penalty.PenaltyPeriod
import models.submission.{Submission, SubmissionStatusEnum}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, Json}

class PenaltyPointSpec extends AnyWordSpec with Matchers {

  val sampleDateTime1: LocalDateTime = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(998, ChronoUnit.MILLIS)
  val sampleDateTime2: LocalDateTime = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS)
  val sampleDateTime3: LocalDateTime = LocalDateTime.of(2019, 5, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS)
  val sampleDateTime4: LocalDateTime = LocalDateTime.of(2019, 6, 1, 23, 59, 59).plus(999, ChronoUnit.MILLIS)

  val penaltyPointAsJson: (PenaltyTypeEnum.Value, Option[LocalDateTime], Option[Financial], Boolean, PointStatusEnum.Value,
    Option[AppealStatusEnum.Value]) => JsObject = (pointType: PenaltyTypeEnum.Value, dateExpired: Option[LocalDateTime], financial: Option[Financial],
                            withPeriod: Boolean, status: PointStatusEnum.Value, appealStatus: Option[AppealStatusEnum.Value]) => {
    val base = Json.obj(
      "type" -> pointType,
      "number" -> "1",
      "id" -> "123456789",
      "dateCreated" -> "2019-01-31T23:59:59.998",
      "status" -> status,
      "communications" -> Seq.empty[String],
    ).deepMerge(
      dateExpired.fold(Json.obj())(obj => Json.obj("dateExpired" -> obj))
    ).deepMerge(
      financial.fold(Json.obj())(obj => Json.obj("financial" -> obj))
    ).deepMerge(
      appealStatus.fold(Json.obj())(obj => Json.obj("appealStatus" -> obj.toString))
    )
    if(withPeriod) {
      base.deepMerge(Json.obj("period" -> Json.obj(
            "startDate" -> "2019-01-31T23:59:59.998",
            "endDate" -> "2019-01-31T23:59:59.999",
            "submission" -> Json.obj (
              "dueDate" -> "2019-05-31T23:59:59.999",
              "submittedDate" -> "2019-06-01T23:59:59.999",
              "status" -> SubmissionStatusEnum.Submitted
            )
          )))
    } else {
      base
    }

  }

  val penaltyPointAsPointModel: PenaltyPoint = PenaltyPoint(
    `type` = PenaltyTypeEnum.Point,
    number = "1",
    id = "123456789",
    dateCreated = sampleDateTime1,
    appealStatus = None,
    dateExpired = Some(sampleDateTime2),
    status = PointStatusEnum.Active,
    reason = None,
    period = Some(PenaltyPeriod(
      startDate = sampleDateTime1,
      endDate = sampleDateTime2,
      submission = Submission(
        dueDate = sampleDateTime3,
        submittedDate = Some(sampleDateTime4),
        status = SubmissionStatusEnum.Submitted
      )
    )),
    communications = Seq.empty,
    financial = None
  )

  val penaltyPointAsPointModelWithAppeal: PenaltyPoint = penaltyPointAsPointModel.copy(appealStatus = Some(AppealStatusEnum.Under_Review))

  val penaltyPointAsPointModelWithNoDateExpired: PenaltyPoint = penaltyPointAsPointModel.copy(
    dateExpired = None
  )

  val addedPenaltyPointAsPointModelWithNoPeriod: PenaltyPoint = penaltyPointAsPointModel.copy(
    period = None,
    dateExpired = None,
    status = PointStatusEnum.Added
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
      result shouldBe penaltyPointAsJson(PenaltyTypeEnum.Financial, Some(sampleDateTime2), Some(Financial(300.00, sampleDateTime3)), true, PointStatusEnum.Active, None)
    }

    s"be writeable to JSON when the point type is ${PenaltyTypeEnum.Point}" in {
      val result = Json.toJson(penaltyPointAsPointModel)
      result shouldBe penaltyPointAsJson(PenaltyTypeEnum.Point, Some(sampleDateTime2), None, true, PointStatusEnum.Active, None)
    }

    "be writable to JSON when no period exists i.e. user has an added point" in {
      val result = Json.toJson(addedPenaltyPointAsPointModelWithNoPeriod)
      result shouldBe penaltyPointAsJson(PenaltyTypeEnum.Point, None, None, false, PointStatusEnum.Added, None)
    }

    s"be readable from JSON when the point type is ${PenaltyTypeEnum.Financial}" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Financial, Some(sampleDateTime2), Some(Financial(300.00, sampleDateTime3)), true, PointStatusEnum.Active, None
      ))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsFinancialPenaltyModel
    }

    s"be readable from JSON when the point type is ${PenaltyTypeEnum.Point}" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Point, Some(sampleDateTime2), None, true, PointStatusEnum.Active, None))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsPointModel
    }

    "be readable from JSON when there is no dateExpired KV in the JSON" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Financial, None, Some(Financial(300.00, sampleDateTime3)), true, PointStatusEnum.Active, None))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsFinancialPenaltyModelNoDateExpired
    }

    "be readable from JSON when there is no appealStatus KV in the JSON" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Point, Some(sampleDateTime2), None, true, PointStatusEnum.Active, None))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsPointModel
    }

    "be readable from JSON when there is an appealStatus KV in the JSON" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Point, Some(sampleDateTime2), None, true, PointStatusEnum.Active,
        Some(AppealStatusEnum.Under_Review)))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsPointModelWithAppeal
    }

    "be readable from JSON when there is no financial KV i.e. normal penalty point, in the JSON" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Point, None, None, true, PointStatusEnum.Active, None))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe penaltyPointAsPointModelWithNoDateExpired
    }

    "be readable from JSON when no period exists i.e. user has an added point" in {
      val result = Json.fromJson(penaltyPointAsJson(PenaltyTypeEnum.Point, None, None, false, PointStatusEnum.Added, None))(PenaltyPoint.format)
      result.isSuccess shouldBe true
      result.get shouldBe addedPenaltyPointAsPointModelWithNoPeriod
    }
  }
}
