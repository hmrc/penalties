/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class OverviewElementSpec extends AnyWordSpec with Matchers {
  val overviewElementAsJson: JsValue = Json.parse(
    """
      |{
      | "type": "VAT",
      | "amount": 100.00,
      | "estimatedInterest": 10.00,
      | "crystalizedInterest": 10.00
      |}
      |
      |""".stripMargin)

  val overviewElementNoEstimatedInterestAsJson: JsValue = Json.parse(
    """
      |{
      | "type": "VAT",
      | "amount": 100.00,
      | "crystalizedInterest": 10.00
      |}
      |
      |""".stripMargin)

  val overviewElementNoCrystalizedInterestAsJson: JsValue = Json.parse(
    """
      |{
      | "type": "VAT",
      | "amount": 100.00,
      | "estimatedInterest": 10.00
      |}
      |
      |""".stripMargin)

  val overviewElementModel: OverviewElement = OverviewElement(
    `type` = AmountTypeEnum.VAT,
    amount = 100.00,
    estimatedInterest = Some(10.00),
    crystalizedInterest = Some(10.00)
  )

  val overviewElementNoEstimatedInterestModel: OverviewElement = OverviewElement(
    `type` = AmountTypeEnum.VAT,
    amount = 100.00,
    crystalizedInterest = Some(10.00)
  )

  val overviewElementNoCrystalizedInterestModel: OverviewElement = OverviewElement(
    `type` = AmountTypeEnum.VAT,
    amount = 100.00,
    estimatedInterest = Some(10.00)
  )

  "OverviewElement" should {
    "be writable to JSON" in {
      val result = Json.toJson(overviewElementModel)(OverviewElement.format)
      result shouldBe overviewElementAsJson
    }

    "be readable from JSON" in {
      val result = Json.fromJson(overviewElementAsJson)(OverviewElement.format)
      result.isSuccess shouldBe true
      result.get shouldBe overviewElementModel
    }

    "be writable to JSON without the estimatedInterest" in {
      val result = Json.toJson(overviewElementNoEstimatedInterestModel)(OverviewElement.format)
      result shouldBe overviewElementNoEstimatedInterestAsJson
    }

    "be readable from JSON without the estimatedInterest" in {
      val result = Json.fromJson(overviewElementNoEstimatedInterestAsJson)(OverviewElement.format)
      result.isSuccess shouldBe true
      result.get shouldBe overviewElementNoEstimatedInterestModel
    }

    "be writable to JSON without the crystalizedInterest" in {
      val result = Json.toJson(overviewElementNoCrystalizedInterestModel)(OverviewElement.format)
      result shouldBe overviewElementNoCrystalizedInterestAsJson
    }

    "be readable from JSON without the crystalizedInterest" in {
      val result = Json.fromJson(overviewElementNoCrystalizedInterestAsJson)(OverviewElement.format)
      result.isSuccess shouldBe true
      result.get shouldBe overviewElementNoCrystalizedInterestModel
    }
  }
}
