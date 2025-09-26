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

package models.hipPenaltyDetails.lateSubmission

import models.hipPenaltyDetails.appealInfo.AppealInformationType
import play.api.libs.json._

import java.time.LocalDate

case class LSPDetails(
    penaltyNumber: String,
    penaltyOrder: Option[String],
    penaltyCategory: Option[LSPPenaltyCategoryEnum.Value],
    penaltyStatus: LSPPenaltyStatusEnum.Value,
    penaltyCreationDate: LocalDate,
    penaltyExpiryDate: LocalDate,
    communicationsDate: Option[LocalDate],
    fapIndicator: Option[String],
    lateSubmissions: Option[Seq[LateSubmission]],
    expiryReason: Option[ExpiryReasonEnum.Value],
    appealInformation: Option[Seq[AppealInformationType]],
    chargeDueDate: Option[LocalDate],
    chargeOutstandingAmount: Option[BigDecimal],
    chargeAmount: Option[BigDecimal],
    // NOTE: these fields are required in 1812 spec but have been set to optional as they are only used by 3rd party APIs - START
    triggeringProcess: Option[String],
    chargeReference: Option[String]
    // END NOTE
)

object LSPDetails {

  implicit val reads: Reads[LSPDetails] = (json: JsValue) =>
    for {
      penaltyNumber <- (json \ "penaltyNumber").validate[String]
      penaltyOrder  <- (json \ "penaltyOrder").validateOpt[String]
      penaltyCategory <- (json \ "penaltyCategory")
        .validateOpt[LSPPenaltyCategoryEnum.Value]
      penaltyStatus <- (json \ "penaltyStatus")
        .validate[LSPPenaltyStatusEnum.Value]
      penaltyCreationDate <- (json \ "penaltyCreationDate").validate[LocalDate]
      penaltyExpiryDate   <- (json \ "penaltyExpiryDate").validate[LocalDate]
      communicationsDate  <- (json \ "communicationsDate").validateOpt[LocalDate]
      fapIndicator        <- (json \ "fapIndicator").validateOpt[String]
      lateSubmissions <- (json \ "lateSubmissions")
        .validateOpt[Seq[LateSubmission]]
      expiryReason <- (json \ "expiryReason")
        .validateOpt[ExpiryReasonEnum.Value]
      appealInformation <- (json \ "appealInformation")
        .validateOpt[Seq[AppealInformationType]]
      chargeDueDate <- (json \ "chargeDueDate").validateOpt[LocalDate]
      chargeOutstandingAmount <- (json \ "chargeOutstandingAmount")
        .validateOpt[BigDecimal]
      chargeAmount      <- (json \ "chargeAmount").validateOpt[BigDecimal]
      triggeringProcess <- (json \ "triggeringProcess").validateOpt[String]
      chargeReference   <- (json \ "chargeReference").validateOpt[String]
    } yield LSPDetails(
      penaltyNumber,
      penaltyOrder,
      penaltyCategory,
      penaltyStatus,
      penaltyCreationDate,
      penaltyExpiryDate,
      communicationsDate,
      fapIndicator,
      lateSubmissions,
      expiryReason,
      appealInformation,
      chargeDueDate,
      chargeOutstandingAmount,
      chargeAmount,
      triggeringProcess,
      chargeReference
    )

  implicit val writes: Writes[LSPDetails] = Json.writes[LSPDetails]
  implicit val format: Format[LSPDetails] = Format(reads, writes)
}
