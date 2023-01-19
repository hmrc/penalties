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

import models.getPenaltyDetails.appealInfo.AppealInformationType
import play.api.libs.json.{JsValue, Json, Reads, Writes}

import java.time.LocalDate

case class LSPDetails(
                       penaltyNumber: String,
                       penaltyOrder: String,
                       penaltyCategory: LSPPenaltyCategoryEnum.Value,
                       penaltyStatus: LSPPenaltyStatusEnum.Value,
                       penaltyCreationDate: LocalDate,
                       penaltyExpiryDate: LocalDate,
                       communicationsDate: Option[LocalDate],
                       FAPIndicator: Option[String],
                       lateSubmissions: Option[Seq[LateSubmission]],
                       expiryReason: Option[ExpiryReasonEnum.Value],
                       appealInformation: Option[Seq[AppealInformationType]],
                       chargeDueDate: Option[LocalDate],
                       chargeOutstandingAmount: Option[BigDecimal],
                       chargeAmount: Option[BigDecimal]
                     )

object LSPDetails {
  implicit val reads: Reads[LSPDetails] = Json.reads[LSPDetails]

  private val writes: Writes[LSPDetails] = Json.writes[LSPDetails]

  implicit val customWrites: Writes[LSPDetails] = new Writes[LSPDetails] {
    override def writes(lspDetails: LSPDetails): JsValue = {
      if(lspDetails.expiryReason.contains(ExpiryReasonEnum.Empty)) {
        val lspDetailsWithoutEmptyExpiryReason = lspDetails.copy(expiryReason = None)
        Json.toJson(lspDetailsWithoutEmptyExpiryReason)(LSPDetails.writes)
      } else {
        Json.toJson(lspDetails)(LSPDetails.writes)
      }
    }
  }
}
