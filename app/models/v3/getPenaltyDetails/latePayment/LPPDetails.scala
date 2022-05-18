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

package models.v3.getPenaltyDetails.latePayment

import models.v3.getPenaltyDetails.AppealInformation
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class LPPDetails(
                      penaltyCategory: LPPPenaltyCategoryEnum.Value,
                      penaltyChargeReference: Option[String],
                      principalChargeReference: String,
                      penaltyChargeCreationDate: LocalDate,
                      penaltyStatus: LPPPenaltyStatusEnum.Value,
                      appealInformation: Option[Seq[AppealInformation]],
                      principalChargeBillingFrom: LocalDate,
                      principalChargeBillingTo: LocalDate,
                      principalChargeDueDate: LocalDate,
                      communicationsDate: LocalDate,
                      penaltyAmountOutstanding: Option[BigDecimal],
                      penaltyAmountPaid: Option[BigDecimal],
                      LPP1LRDays: Option[String],
                      LPP1HRDays: Option[String],
                      LPP2Days: Option[String],
                      LPP1HRCalculationAmount: Option[BigDecimal],
                      LPP1LRCalculationAmount: Option[BigDecimal],
                      LPP2Percentage: Option[BigDecimal],
                      LPP1LRPercentage: Option[BigDecimal],
                      LPP1HRPercentage: Option[BigDecimal],
                      penaltyChargeDueDate: LocalDate,
                      principalChargeLatestClearing: Option[LocalDate]
                     )

object LPPDetails {
  implicit val format: OFormat[LPPDetails] = Json.format[LPPDetails]
}