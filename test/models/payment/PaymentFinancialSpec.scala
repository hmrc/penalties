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

import models.financial.Financial
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime

class FinancialSpec extends AnyWordSpec with Matchers {
  val FinancialJson: JsValue = Json.parse(
    """
      |{
      | "amountDue": 420.10,
      | "outstandingAmountDue": 0.00,
      | "dueDate": "2020-01-01T13:00:00.123"
      |}
      |""".stripMargin)

  val FinancialWithCrystalizedAndEstimatedInterestJson: JsValue = Json.parse(
    """
      |{
      | "amountDue": 420.10,
      | "outstandingAmountDue": 0.00,
      | "dueDate": "2020-01-01T13:00:00.123",
      | "estimatedInterest": 10.00,
      | "crystalizedInterest": 10.00
      |}
      |""".stripMargin)

  val FinancialModel: Financial = Financial(
    amountDue = 420.10,
    outstandingAmountDue = 0.00,
    dueDate = LocalDateTime.parse("2020-01-01T13:00:00.123")
  )

  val FinancialModelWithCrystalizedAndEstimatedInterest: Financial = Financial(
    amountDue = 420.10,
    outstandingAmountDue = 0.00,
    dueDate = LocalDateTime.parse("2020-01-01T13:00:00.123"),
    estimatedInterest = Some(10.00),
    crystalizedInterest = Some(10.00)
  )

  "be readable from JSON" in {
    val result = Json.fromJson(FinancialJson)(Financial.format)
    result.isSuccess shouldBe true
    result.get shouldBe FinancialModel
  }

  "be writable to JSON" in {
    val result = Json.toJson(FinancialModel)
    result shouldBe FinancialJson
  }

  "be readable from JSON with interest present" in {
    val result = Json.fromJson(FinancialWithCrystalizedAndEstimatedInterestJson)(Financial.format)
    result.isSuccess shouldBe true
    result.get shouldBe FinancialModelWithCrystalizedAndEstimatedInterest
  }

  "be writable to JSON with interest present" in {
    val result = Json.toJson(FinancialModelWithCrystalizedAndEstimatedInterest)
    result shouldBe FinancialWithCrystalizedAndEstimatedInterestJson
  }
}
