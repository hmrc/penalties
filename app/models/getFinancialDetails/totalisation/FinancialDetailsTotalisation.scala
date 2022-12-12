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

package models.getFinancialDetails.totalisation

import play.api.libs.json.{JsValue, Json, Reads, Writes}

case class FinancialDetailsTotalisation(
                                         regimeTotalisations: Option[RegimeTotalisation],
                                         interestTotalisations: Option[InterestTotalisation]
                                       )

object FinancialDetailsTotalisation {

  implicit val writes: Writes[FinancialDetailsTotalisation] = Json.writes[FinancialDetailsTotalisation]

  implicit val reads: Reads[FinancialDetailsTotalisation] = (json: JsValue) => {
    for {
      regimeTotalisations <- (json \ "regimeTotalisation").validateOpt[RegimeTotalisation]
      interestTotalisations <- (json \ "additionalReceivableTotalisations").validateOpt[InterestTotalisation]
    } yield {
      new FinancialDetailsTotalisation(regimeTotalisations, interestTotalisations)
    }
  }
}