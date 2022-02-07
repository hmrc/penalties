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

package models.v2.lateSubmissionPenalty

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class LSPSummarySpec extends SpecBase {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      | "activePenaltyPoints": 1,
      | "inactivePenaltyPoints": 2,
      | "regimeThreshold": 3,
      | "POCAchievementDate": "2022-01-01",
      | "penaltyChargeAmount": 123.45
      |}
      |""".stripMargin)

  val model: LSPSummary = LSPSummary(
    activePenaltyPoints = 1,
    inactivePenaltyPoints = 2,
    regimeThreshold = 3,
    POCAchievementDate = LocalDate.of(2022, 1, 1),
    penaltyChargeAmount = 123.45
  )

  "be writable to JSON" in {
    val result = Json.toJson(model)(LSPSummary.format)
    result shouldBe modelAsJson
  }

  "be readable from JSON" in {
    val result = Json.fromJson(modelAsJson)(LSPSummary.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }
}
