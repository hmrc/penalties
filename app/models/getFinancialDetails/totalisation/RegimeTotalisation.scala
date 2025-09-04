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

package models.getFinancialDetails.totalisation

import play.api.libs.json.{JsValue, Json, Reads, Writes}

case class RegimeTotalisation(
    totalAccountOverdue: Option[BigDecimal]
)

object RegimeTotalisation {

  implicit val writes: Writes[RegimeTotalisation] = Json.writes[RegimeTotalisation]

  implicit val reads: Reads[RegimeTotalisation] = (json: JsValue) =>
    for {
      totalAccountOverdue <- (json \ "totalAccountOverdue").validateOpt[BigDecimal]
    } yield new RegimeTotalisation(totalAccountOverdue)
}
