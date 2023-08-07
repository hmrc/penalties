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

package base

import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.lateSubmission.{ExpiryReasonEnum, LSPDetails, LSPPenaltyCategoryEnum, LSPPenaltyStatusEnum, LateSubmission, TaxReturnStatusEnum}

import java.time.LocalDate

trait LSPDetailsBase {
  val lspPointDetails: LSPDetails = LSPDetails(
    penaltyNumber = "1234567890",
    penaltyOrder = Some("01"),
    penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
    penaltyStatus = LSPPenaltyStatusEnum.Active,
    penaltyCreationDate = LocalDate.now(),
    penaltyExpiryDate = LocalDate.now().plusYears(2),
    communicationsDate = Some(LocalDate.now()),
    FAPIndicator = None,
    lateSubmissions = Some(Seq(LateSubmission(
      lateSubmissionID = "001",
      taxPeriod = Some("23AA"),
      taxPeriodStartDate = Some(LocalDate.now()),
      taxPeriodEndDate = Some(LocalDate.now()),
      taxPeriodDueDate = Some(LocalDate.now()),
      returnReceiptDate = Some(LocalDate.now()),
      taxReturnStatus = Some(TaxReturnStatusEnum.Fulfilled)
    ))),
    expiryReason = None,
    appealInformation = None,
    chargeDueDate = None,
    chargeOutstandingAmount = None,
    chargeAmount = None,
    triggeringProcess = None,
    chargeReference = None
  )

  def lspPointDetailsWithAppealStatus(status: String): LSPDetails = lspPointDetails.copy(appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.withName(status)), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))), expiryReason = Some(ExpiryReasonEnum.Appeal), penaltyStatus = if (AppealStatusEnum.withName(status) == AppealStatusEnum.Upheld) LSPPenaltyStatusEnum.Inactive else lspPointDetails.penaltyStatus)

  val lspThresholdDetails: LSPDetails = lspPointDetails.copy(penaltyCategory = Some(LSPPenaltyCategoryEnum.Threshold), chargeAmount = Some(200), chargeOutstandingAmount = Some(200), chargeDueDate = Some(LocalDate.now()))

  def lspThresholdDetailsWithAppealStatus(status: String): LSPDetails = lspThresholdDetails.copy(appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.withName(status)), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))), expiryReason = Some(ExpiryReasonEnum.Appeal), penaltyStatus = if (AppealStatusEnum.withName(status) == AppealStatusEnum.Upheld) LSPPenaltyStatusEnum.Inactive else lspPointDetails.penaltyStatus)

  val lspChargeDetails: LSPDetails = lspPointDetails.copy(penaltyCategory = Some(LSPPenaltyCategoryEnum.Charge), chargeAmount = Some(200), chargeOutstandingAmount = Some(200), chargeDueDate = Some(LocalDate.now()))

  def lspChargeDetailsWithAppealStatus(status: String): LSPDetails = lspChargeDetails.copy(appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.withName(status)), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))), expiryReason = Some(ExpiryReasonEnum.Appeal), penaltyStatus = if (AppealStatusEnum.withName(status) == AppealStatusEnum.Upheld) LSPPenaltyStatusEnum.Inactive else lspPointDetails.penaltyStatus)
}
