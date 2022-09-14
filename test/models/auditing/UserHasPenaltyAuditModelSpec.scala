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

package models.auditing

import base.{LogCapturing, SpecBase}
import models.getPenaltyDetails._
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPDetailsMetadata, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty, TimeToPay}
import models.getPenaltyDetails.lateSubmission._
import utils.Logger

import java.time.LocalDate

class UserHasPenaltyAuditModelSpec extends SpecBase with LogCapturing {
  val basicGetPenaltyDetailsModel: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = None
  )

  val getPenaltyDetailsModelWithVATDue: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None)),
    lateSubmissionPenalty = Some(LateSubmissionPenalty(
      summary = LSPSummary(
        activePenaltyPoints = 2,
        inactivePenaltyPoints = 0,
        regimeThreshold = 1,
        penaltyChargeAmount = 200,
        PoCAchievementDate = LocalDate.of(2022, 1, 1)
      ),
      details = Seq(
        LSPDetails(
          penaltyNumber = "12345678",
          penaltyOrder = "2",
          penaltyCategory = LSPPenaltyCategoryEnum.Charge,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
          chargeOutstandingAmount = Some(200),
          chargeAmount = Some(200)
        ),
        LSPDetails(
          penaltyNumber = "12345677",
          penaltyOrder = "1",
          penaltyCategory = LSPPenaltyCategoryEnum.Charge,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
          chargeOutstandingAmount = Some(200),
          chargeAmount = Some(200)
        )
      )
    ))
  )

  val getPenaltyDetailsModelWithLSPPs: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None)),
    lateSubmissionPenalty = Some(LateSubmissionPenalty(
      summary = LSPSummary(
        activePenaltyPoints = 2,
        inactivePenaltyPoints = 0,
        regimeThreshold = 5,
        penaltyChargeAmount = 200,
        PoCAchievementDate = LocalDate.of(2022, 1, 1)
      ),
      details = Seq(
        LSPDetails(
          penaltyNumber = "12345678",
          penaltyOrder = "2",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        ),
        LSPDetails(
          penaltyNumber = "12345677",
          penaltyOrder = "1",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        )
      )
    ))
  )

  val getPenaltyDetailsModelWithActiveAndInactiveLSPPs: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None)),
    lateSubmissionPenalty = Some(LateSubmissionPenalty(
      summary = LSPSummary(
        activePenaltyPoints = 2,
        inactivePenaltyPoints = 1,
        regimeThreshold = 5,
        penaltyChargeAmount = 200,
        PoCAchievementDate = LocalDate.of(2022, 1, 1)
      ),
      details = Seq(
        LSPDetails(
          penaltyNumber = "12345679",
          penaltyOrder = "3",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        ),
        LSPDetails(
          penaltyNumber = "12345678",
          penaltyOrder = "2",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        ),
        LSPDetails(
          penaltyNumber = "12345677",
          penaltyOrder = "1",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Inactive,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        )
      )
    ))
  )

  val getPenaltyDetailsModelWithLSPPsUnderAppeal: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None)),
    lateSubmissionPenalty = Some(LateSubmissionPenalty(
      summary = LSPSummary(
        activePenaltyPoints = 2,
        inactivePenaltyPoints = 0,
        regimeThreshold = 5,
        penaltyChargeAmount = 200,
        PoCAchievementDate = LocalDate.of(2022, 1, 1)
      ),
      details = Seq(
        LSPDetails(
          penaltyNumber = "12345678",
          penaltyOrder = "2",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = Some(Seq(
            AppealInformationType(appealStatus = Some(AppealStatusEnum.Under_Appeal), appealLevel = Some(AppealLevelEnum.HMRC)))),
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        ),
        LSPDetails(
          penaltyNumber = "12345677",
          penaltyOrder = "1",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = Some(Seq(
            AppealInformationType(appealStatus = Some(AppealStatusEnum.Under_Appeal), appealLevel = Some(AppealLevelEnum.HMRC)))),
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        )
      )
    ))
  )

  val getPenaltyDetailsModelWithLSPPsAppealed: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None)),
    lateSubmissionPenalty = Some(LateSubmissionPenalty(
      summary = LSPSummary(
        activePenaltyPoints = 0,
        inactivePenaltyPoints = 2,
        regimeThreshold = 5,
        penaltyChargeAmount = 200,
        PoCAchievementDate = LocalDate.of(2022, 1, 1)
      ),
      details = Seq(
        LSPDetails(
          penaltyNumber = "12345678",
          penaltyOrder = "2",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = Some(Seq(
            AppealInformationType(appealStatus = Some(AppealStatusEnum.Upheld), appealLevel = Some(AppealLevelEnum.HMRC)))),
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        ),
        LSPDetails(
          penaltyNumber = "12345677",
          penaltyOrder = "1",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = Some(Seq(
            AppealInformationType(appealStatus = Some(AppealStatusEnum.Upheld), appealLevel = Some(AppealLevelEnum.HMRC)))
          ),
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        )
      )
    ))
  )

  val getPenaltyDetailsModelWithLSPUnpaidAndRemoved: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None)),
    lateSubmissionPenalty = Some(LateSubmissionPenalty(
      summary = LSPSummary(
        activePenaltyPoints = 2,
        inactivePenaltyPoints = 0,
        regimeThreshold = 2,
        penaltyChargeAmount = 200,
        PoCAchievementDate = LocalDate.of(2022, 1, 1)
      ),
      details = Seq(
        LSPDetails(
          penaltyNumber = "12345678",
          penaltyOrder = "3",
          penaltyCategory = LSPPenaltyCategoryEnum.Charge,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
          chargeOutstandingAmount = Some(200),
          chargeAmount = Some(200)
        ),
        LSPDetails(
          penaltyNumber = "12345678",
          penaltyOrder = "2",
          penaltyCategory = LSPPenaltyCategoryEnum.Charge,
          penaltyStatus = LSPPenaltyStatusEnum.Inactive,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = Some(
            Seq(
              AppealInformationType(appealStatus = Some(AppealStatusEnum.Upheld), appealLevel = Some(AppealLevelEnum.HMRC))
            )
          ),
          chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
          chargeOutstandingAmount = Some(200),
          chargeAmount = Some(200)
        ),
        LSPDetails(
          penaltyNumber = "12345677",
          penaltyOrder = "1",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = LocalDate.of(2022, 1, 1),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = None,
          chargeOutstandingAmount = None,
          chargeAmount = None
        )
      )
    ))
  )

  val getPenaltyDetailsModelWithLPPsPaid: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None
    )),
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(0),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(0),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            )
          )
        )
      )
    )
  )

  val getPenaltyDetailsModelWithLPPsUnpaid: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None
    )),
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(144.21),
              penaltyAmountPaid = Some(0),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              )
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(144.21),
              penaltyAmountPaid = Some(0),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              )
            )
          )
        )
      )
    )
  )

  val getPenaltyDetailsModelWithLPPsPartiallyPaid: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None
    )),
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(10),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              )
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(10),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              )
            )
          )
        )
      )
    )
  )

  val getPenaltyDetailsModelWithLPPsUnderReview: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None
    )),
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(0),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(
                Seq(
                  AppealInformationType(appealStatus = Some(AppealStatusEnum.Under_Appeal), appealLevel = Some(AppealLevelEnum.HMRC))
                )
              ),
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(10),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            )
          )
        )
      )
    )
  )

  val getPenaltyDetailsModelWithLPPsAccepted: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LPIPostedTotal = Some(120),
      LPIEstimatedTotal = Some(130),
      LSPTotalValue = None
    )),
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(0),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(
                Seq(
                  AppealInformationType(appealStatus = Some(AppealStatusEnum.Upheld), appealLevel = Some(AppealLevelEnum.HMRC))
                )
              ),
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(10),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            )
          )
        )
      )
    )
  )

  val getPenaltyDetailsModelWithLPPsPaidAndUnpaidAndPartiallyPaid: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = None,
      LPPPostedTotal = Some(150),
      LPPEstimatedTotal = Some(50),
      LPIPostedTotal = Some(10),
      LPIEstimatedTotal = Some(10),
      LSPTotalValue = None
    )),
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(0),
              penaltyAmountPaid = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(200),
              penaltyAmountPaid = Some(344.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = Some(10),
              LPP1LRCalculationAmount = Some(10),
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(200),
              penaltyAmountPaid = Some(0),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = Some(10),
              LPP1LRCalculationAmount = Some(10),
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            )
          )
        )
      )
    )
  )

  val basicModel: UserHasPenaltyAuditModel =
    UserHasPenaltyAuditModel(basicGetPenaltyDetailsModel, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val basicAgentModel: UserHasPenaltyAuditModel =
    UserHasPenaltyAuditModel(basicGetPenaltyDetailsModel, "1234", "VRN", Some("ARN123"))(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithInterest: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithVATDue, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLSPPs: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLSPPs, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithActiveAndInactiveLSPPs: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithActiveAndInactiveLSPPs, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLSPPsUnderReview: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLSPPsUnderAppeal, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLSPPsAppealed: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLSPPsAppealed, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLSPUnpaidAndRemoved: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLSPUnpaidAndRemoved, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsPaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsPaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsUnpaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsUnpaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsPartiallyPaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsPartiallyPaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsUnderReview: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsUnderReview, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsAccepted: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsAccepted, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsPaidAndUnpaidAndPartiallyPaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val basicModelWithUserAgent: String => UserHasPenaltyAuditModel =
    (userAgent: String) => UserHasPenaltyAuditModel(basicGetPenaltyDetailsModel, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> userAgent))

  "UserHasPenaltyAuditModel" should {
    "have the correct audit type" in {
      basicModel.auditType shouldBe "PenaltyUserHasPenalty"
    }

    "have the correct transaction name" in {
      basicModel.transactionName shouldBe "penalties-user-has-penalty"
    }

    "show the correct audit details" when {
      "the correct basic detail information is present" in {
        (basicModel.detail \ "taxIdentifier").validate[String].get shouldBe "1234"
        (basicModel.detail \ "identifierType").validate[String].get shouldBe "VRN"
        (basicModel.detail \ "callingService").validate[String].get shouldBe "penalties-frontend"
      }

      "the service is BTA" in {
        (basicModelWithUserAgent("business-account").detail \ "taxIdentifier").validate[String].get shouldBe "1234"
        (basicModelWithUserAgent("business-account").detail \ "identifierType").validate[String].get shouldBe "VRN"
        (basicModelWithUserAgent("business-account").detail \ "callingService").validate[String].get shouldBe "BTA"
      }

      "the service is VATVC" in {
        (basicModelWithUserAgent("vat-through-software").detail \ "taxIdentifier").validate[String].get shouldBe "1234"
        (basicModelWithUserAgent("vat-through-software").detail \ "identifierType").validate[String].get shouldBe "VRN"
        (basicModelWithUserAgent("vat-through-software").detail \ "callingService").validate[String].get shouldBe "VATVC"
      }

      "the user is an agent" in {
        (basicAgentModel.detail \ "agentReferenceNumber").isDefined
        (basicAgentModel.detail \ "agentReferenceNumber").validate[String].get shouldBe "ARN123"
        (basicAgentModel.detail \ "userType").isDefined
        (basicAgentModel.detail \ "userType").validate[String].get shouldBe "Agent"
      }

      "the user is a trader" in {
        (basicModel.detail \ "userType").isDefined
        (basicModel.detail \ "userType").validate[String].get shouldBe "Trader"
      }

      "the user has parent charges, interest due and financial penalties" in {
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalTaxDue").validate[BigDecimal].get shouldBe 2000.12
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalInterestDue").validate[Int].get shouldBe 250
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalFinancialPenaltyDue").validate[Int].get shouldBe 400
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalDue").validate[BigDecimal].get shouldBe 2650.12
      }

      "the user has LSPPs (no appeals)" in {
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LSPPs (with appeals)" in {
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 2
      }

      "the user has LSPPs (with reviewed appeals)" in {
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 0
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LSPs that are paid and unpaid" in {
        (auditModelWithLSPUnpaidAndRemoved.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 2
        (auditModelWithLSPUnpaidAndRemoved.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithLSPUnpaidAndRemoved.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 1
        (auditModelWithLSPUnpaidAndRemoved.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LSPs" in {
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 1
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 2
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has inactive LSPs" in {
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "inactivePoints").validate[Int].get shouldBe 1
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LPPs (all paid)" in {
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LPPs (all partially paid)" in {
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPartiallyPaidPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LPPs (all unpaid)" in {
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has a combination of unpaid and paid and partially paid LPPs" in {
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPartiallyPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 3
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "totalFinancialPenaltyDue").validate[Int].get shouldBe 400
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "totalInterestDue").validate[Int].get shouldBe 20
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "totalDue").validate[Int].get shouldBe 420
      }

      "the user has LPPs (with appeals)" in {
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 1
      }

      "the user has LPPs (with reviewed appeals)" in {
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }
    }

    "set the callingService to blank string and log error when the User-Agent can not be matched" in {
      withCaptureOfLoggingFrom(Logger.logger) {
        logs => {
          (basicModelWithUserAgent("").detail \ "callingService").validate[String].get shouldBe ""
          logs.exists(_.getMessage.equals("[UserHasPenaltyAuditModel] - could not distinguish referer for audit")) shouldBe true
        }
      }
    }
  }
}
