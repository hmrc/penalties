/*
 * Copyright 2023 HM Revenue & Customs
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

package models.getPenaltyDetails.lateSubmission

import base.SpecBase
import play.api.libs.json.{JsResult, JsValue, Json}

import java.time.LocalDate

class LSPSummarySpec extends SpecBase {

  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      |   "activePenaltyPoints": 10,
      |   "inactivePenaltyPoints": 12,
      |   "PoCAchievementDate": "2022-01-01",
      |   "regimeThreshold": 10,
      |   "penaltyChargeAmount": 684.25
      |}
      |""".stripMargin)

  val model: LSPSummary = LSPSummary(
    activePenaltyPoints = 10,
    inactivePenaltyPoints = 12,
    regimeThreshold = 10,
    penaltyChargeAmount = 684.25,
    PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
  )

  "be readable from JSON" in {
    val result: JsResult[LSPSummary] = Json.fromJson(jsonRepresentingModel)(LSPSummary.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result: JsValue = Json.toJson(model)(LSPSummary.format)
    result shouldBe jsonRepresentingModel
  }
}
