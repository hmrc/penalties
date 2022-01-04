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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class Financial(
                      amountDue: BigDecimal,
                      outstandingAmountDue: BigDecimal,
                      dueDate: LocalDateTime,
                      outstandingAmountDay15: Option[BigDecimal] = None,
                      outstandingAmountDay31: Option[BigDecimal] = None,
                      percentageOfOutstandingAmtCharged: Option[BigDecimal] = None,
                      estimatedInterest: Option[BigDecimal] = None,
                      crystalizedInterest: Option[BigDecimal] = None
                    )

object Financial {
  implicit val format: OFormat[Financial] = Json.format[Financial]
}
