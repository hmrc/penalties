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

package models.payment

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime

class PaymentPeriodSpec extends AnyWordSpec with Matchers {
  val paymentPeriodJson: JsValue = Json.parse(
    """
      |{
      | "startDate": "2020-01-01T13:00:00.091",
      | "endDate": "2020-01-31T13:00:00.091",
      | "dueDate": "2020-03-07T13:00:00.091",
      | "paymentStatus": "PAID"
      |}
      |""".stripMargin)

  val paymentPeriodAsModel: PaymentPeriod = PaymentPeriod(
    startDate = LocalDateTime.parse("2020-01-01T13:00:00.091"),
    endDate = LocalDateTime.parse("2020-01-31T13:00:00.091"),
    dueDate = LocalDateTime.parse("2020-03-07T13:00:00.091"),
    paymentStatus = PaymentStatusEnum.Paid
  )

  "be writable to JSON" in {
    val result = Json.toJson(paymentPeriodAsModel)
    result shouldBe paymentPeriodJson
  }

  s"be readable from JSON" in {
    val result = Json.fromJson(paymentPeriodJson)(PaymentPeriod.format)
    result.isSuccess shouldBe true
    result.get shouldBe paymentPeriodAsModel
  }
}
