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

package models.financial

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class FinancialSpec extends AnyWordSpec with Matchers {
  val financialModelAsJson: JsValue = Json.parse(
    """
      |{
      | "amountDue": 400.12,
      | "outstandingAmountDue": 400.12,
      | "dueDate": "2019-01-31T23:59:59.999"
      |}
      |
      |""".stripMargin)

  val financialModelWithInterestAsJson: JsValue = Json.parse(
    """
      |{
      | "amountDue": 400.12,
      | "outstandingAmountDue": 400.12,
      | "dueDate": "2019-01-31T23:59:59.999",
      | "estimatedInterest": 10.11,
      | "crystalizedInterest": 12.13
      |}
      |
      |""".stripMargin)

  val financialModelWithLPPDataAsJson: JsValue = Json.parse(
    """
      |{
      | "amountDue": 400.12,
      | "outstandingAmountDue": 400.12,
      | "dueDate": "2019-01-31T23:59:59.999",
      | "outstandingAmountDay15": 100.53,
      | "outstandingAmountDay31": 210.32,
      | "percentageOfOutstandingAmtCharged": 2
      |}
      |
      |""".stripMargin)

  val financialModelWithAllDataAsJson: JsValue = Json.parse(
    """
      |{
      | "amountDue": 400.12,
      | "outstandingAmountDue": 400.12,
      | "dueDate": "2019-01-31T23:59:59.999",
      | "outstandingAmountDay15": 100.53,
      | "outstandingAmountDay31": 210.32,
      | "percentageOfOutstandingAmtCharged": 2,
      | "estimatedInterest": 10.11,
      | "crystalizedInterest": 12.13
      |}
      |
      |""".stripMargin)

  val financialModel: Financial = Financial(
    amountDue = 400.12,
    outstandingAmountDue = 400.12,
    dueDate = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS)
  )

  val financialModelWithInterest: Financial = Financial(
    amountDue = 400.12,
    outstandingAmountDue = 400.12,
    dueDate = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS),
    crystalizedInterest = Some(12.13),
    estimatedInterest = Some(10.11)
  )

  val financialModelWithLPPData: Financial = Financial(
    amountDue = 400.12,
    outstandingAmountDue = 400.12,
    dueDate = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS),
    outstandingAmountDay15 = Some(100.53),
    outstandingAmountDay31 = Some(210.32),
    percentageOfOutstandingAmtCharged = Some(2)
  )

  val financialModelWithAllData: Financial = Financial(
    amountDue = 400.12,
    outstandingAmountDue = 400.12,
    dueDate = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(999, ChronoUnit.MILLIS),
    outstandingAmountDay15 = Some(100.53),
    outstandingAmountDay31 = Some(210.32),
    percentageOfOutstandingAmtCharged = Some(2),
    crystalizedInterest = Some(12.13),
    estimatedInterest = Some(10.11)
  )

  "Financial" should {
    "be writeable to JSON" in {
      val result = Json.toJson(financialModel)
      result shouldBe financialModelAsJson
    }

    "be readable from JSON" in {
      val result = Json.fromJson(financialModelAsJson)(Financial.format)
      result.isSuccess shouldBe true
      result.get shouldBe financialModel
    }

    "be writeable to JSON (with only interest fields)" in {
      val result = Json.toJson(financialModelWithInterest)
      result shouldBe financialModelWithInterestAsJson
    }

    "be readable from JSON (with only interest fields)" in {
      val result = Json.fromJson(financialModelWithInterestAsJson)(Financial.format)
      result.isSuccess shouldBe true
      result.get shouldBe financialModelWithInterest
    }

    "be writeable to JSON (with only LPP fields)" in {
      val result = Json.toJson(financialModelWithLPPData)
      result shouldBe financialModelWithLPPDataAsJson
    }

    "be readable from JSON (with only LPP fields)" in {
      val result = Json.fromJson(financialModelWithLPPDataAsJson)(Financial.format)
      result.isSuccess shouldBe true
      result.get shouldBe financialModelWithLPPData
    }

    "be writeable to JSON (with all fields)" in {
      val result = Json.toJson(financialModelWithAllData)
      result shouldBe financialModelWithAllDataAsJson
    }

    "be readable from JSON (with all fields)" in {
      val result = Json.fromJson(financialModelWithAllDataAsJson)(Financial.format)
      result.isSuccess shouldBe true
      result.get shouldBe financialModelWithAllData
    }
  }
}
