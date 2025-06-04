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

package models.penaltyDetails.lateSubmission

import play.api.libs.json.{JsValue, Json, Reads, Writes}

import java.time.LocalDate

case class LSPSummary(
                       activePenaltyPoints: Int,
                       inactivePenaltyPoints: Int,
                       regimeThreshold: Int,
                       penaltyChargeAmount: BigDecimal,
                       PoCAchievementDate: Option[LocalDate]
                     )

object LSPSummary {
  implicit val reads: Reads[LSPSummary] = (json: JsValue) => for {
    activePenaltyPoints <- (json \ "activePenaltyPoints").validate[Int]
    inactivePenaltyPoints <- (json \ "inactivePenaltyPoints").validate[Int]
    regimeThreshold <- (json \ "regimeThreshold").validate[Int]
    penaltyChargeAmount <- (json \ "penaltyChargeAmount").validate[BigDecimal]
    PoCAchievementDate <- (json \ "pocAchievementDate").validateOpt[LocalDate]
  } yield {
    LSPSummary(activePenaltyPoints, inactivePenaltyPoints, regimeThreshold, penaltyChargeAmount, PoCAchievementDate)
  }

  implicit val writes: Writes[LSPSummary] = (o: LSPSummary) => Json.obj(
    "activePenaltyPoints" -> o.activePenaltyPoints,
    "inactivePenaltyPoints" -> o.inactivePenaltyPoints,
    "regimeThreshold" -> o.regimeThreshold,
    "penaltyChargeAmount" -> o.penaltyChargeAmount,
    "pocAchievementDate" -> o.PoCAchievementDate
  )
}
