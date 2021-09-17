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
import models.financial.Financial
import models.payment
import models.payment.{LatePaymentPenalty, PaymentPeriod, PaymentStatusEnum}
import models.reason.PaymentPenaltyReasonEnum
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, Json}
import java.time.LocalDateTime

class PaymentPointSpec extends AnyWordSpec with Matchers {
  val paymentPointJson: Boolean => JsObject = (withAppealStatus: Boolean) => Json.obj(
    "type" -> "financial",
    "id" -> "123456789",
    "reason" -> "VAT_NOT_PAID_WITHIN_30_DAYS",
    "dateCreated" -> "2020-01-01T01:00:00.123",
    "status" -> "ACTIVE",
    "period" -> Json.obj(
      "startDate" -> "2020-01-01T01:00:00.123",
      "endDate" -> "2020-01-01T01:00:00.123",
      "dueDate" -> "2020-01-01T01:00:00.123",
      "paymentStatus" -> "PAID"
    ),
    "communications" -> Seq.empty[String],
    "financial" -> Json.obj(
      "amountDue" -> 400.00,
      "outstandingAmountDue" -> 0.00,
      "dueDate" -> "2020-01-01T01:00:00.123"
    )
  ).deepMerge(
    if(withAppealStatus) Json.obj("appealStatus" -> AppealStatusEnum.Under_Review) else Json.obj()
  )

  val paymentPointModelWithAppeal: LatePaymentPenalty = payment.LatePaymentPenalty(
    `type` = PenaltyTypeEnum.Financial,
    id = "123456789",
    reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
    dateCreated = LocalDateTime.parse("2020-01-01T01:00:00.123"),
    status = PointStatusEnum.Active,
    appealStatus = Some(AppealStatusEnum.Under_Review),
    period = PaymentPeriod(
      startDate = LocalDateTime.parse("2020-01-01T01:00:00.123"),
      endDate = LocalDateTime.parse("2020-01-01T01:00:00.123"),
      dueDate = LocalDateTime.parse("2020-01-01T01:00:00.123"),
      paymentStatus = PaymentStatusEnum.Paid
    ),
    communications = Seq.empty,
    financial = Financial(
      amountDue = 400.00,
      outstandingAmountDue = 0.00,
      dueDate = LocalDateTime.parse("2020-01-01T01:00:00.123")
    )
  )

  val paymentPointModelWithNoAppeal: LatePaymentPenalty = paymentPointModelWithAppeal.copy(appealStatus = None)

  "be writable to JSON with no appeal status" in {
    val result = Json.toJson(paymentPointModelWithNoAppeal)
    result shouldBe paymentPointJson(false)
  }

  "be writable to JSON with an appeal status" in {
    val result = Json.toJson(paymentPointModelWithAppeal)
    result shouldBe paymentPointJson(true)
  }

  "be readable from JSON with no appeal status" in {
    val result = Json.fromJson(paymentPointJson(false))(LatePaymentPenalty.format)
    result.isSuccess shouldBe true
    result.get shouldBe paymentPointModelWithNoAppeal
  }

  "be readable from JSON with an appeal status" in {
    val result = Json.fromJson(paymentPointJson(true))(LatePaymentPenalty.format)
    result.isSuccess shouldBe true
    result.get shouldBe paymentPointModelWithAppeal
  }
}
