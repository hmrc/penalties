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

package models

import models.communication.{Communication, CommunicationTypeEnum}
import models.financial.Financial
import models.penalty.PenaltyPeriod
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import models.submission.{Submission, SubmissionStatusEnum}
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ETMPPayloadSpec extends WordSpec with Matchers {

  val sampleDate: LocalDateTime = LocalDateTime.of(2021, 4, 23, 18, 25, 43).plus(511, ChronoUnit.MILLIS)

  val etmpPayloadAsJson: JsValue = Json.parse(
    """
        {
      |	"pointsTotal": 1,
      |	"lateSubmissions": 1,
      |	"adjustmentPointsTotal": 1,
      |	"fixedPenaltyAmount": 200,
      |	"penaltyAmountsTotal": 400.00,
      |	"penaltyPointsThreshold": 4,
      |	"penaltyPoints": [
      |		{
      |			"type": "financial",
      |			"number": "2",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": {
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			},
      |			"communications": [
      |				{
      |					"type": "secureMessage",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			],
      |     "financial": {
      |        "amountDue": 400.00,
      |        "dueDate": "2021-04-23T18:25:43.511"
      |     }
      |		},
      |		{
      |			"type": "point",
      |			"number": "1",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": {
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			},
      |			"communications": [
      |				{
      |					"type": "letter",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			]
      |		}
      |	]
      |}
      |""".stripMargin)

  val etmpPayloadModel: ETMPPayload = ETMPPayload(
    pointsTotal = 1,
    lateSubmissions = 1,
    adjustmentPointsTotal = 1,
    fixedPenaltyAmount = 200.00,
    penaltyAmountsTotal = 400.00,
    penaltyPointsThreshold = 4,
    penaltyPoints = Seq(
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Financial,
        number = "2",
        dateCreated = sampleDate,
        dateExpired = Some(sampleDate),
        status = PointStatusEnum.Active,
        period = PenaltyPeriod(
          startDate = sampleDate,
          endDate = sampleDate,
          submission = Submission(
            dueDate = sampleDate,
            submittedDate = Some(sampleDate),
            status = SubmissionStatusEnum.Submitted
          )
        ),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.secureMessage,
            dateSent = sampleDate,
            documentId = "1234567890"
          )
        ),
        financial = Some(Financial(
          amountDue = 400.00,
          dueDate = sampleDate
        ))
      ),
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Point,
        number = "1",
        dateCreated = sampleDate,
        dateExpired = Some(sampleDate),
        status = PointStatusEnum.Active,
        period = PenaltyPeriod(
          startDate = sampleDate,
          endDate = sampleDate,
          submission = Submission(
            dueDate = sampleDate,
            submittedDate = Some(sampleDate),
            status = SubmissionStatusEnum.Submitted
          )
        ),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.letter,
            dateSent = sampleDate,
            documentId = "1234567890")
        ),
        financial = None
      )
    )
  )

  "ETMPPayload" should {
    "be writable to JSON" in {
      val result = Json.toJson(etmpPayloadModel)
      result shouldBe etmpPayloadAsJson
    }

    "be readable from JSON" in {
      val result = Json.fromJson(etmpPayloadAsJson)(ETMPPayload.format)
      result.isSuccess shouldBe true
      result.get shouldBe etmpPayloadModel
    }
  }
}
