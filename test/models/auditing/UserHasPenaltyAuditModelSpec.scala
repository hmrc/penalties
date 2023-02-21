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

package models.auditing

import base.{LogCapturing, SpecBase}
import models.getFinancialDetails.MainTransactionEnum
import models.getPenaltyDetails._
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.lateSubmission._
import org.mockito.Mockito.{mock, when}
import utils.{DateHelper, Logger}

import java.time.LocalDate

class UserHasPenaltyAuditModelSpec extends SpecBase with LogCapturing {
  val mockDateHelper: DateHelper = mock(classOf[DateHelper])
  when(mockDateHelper.dateNow()).thenReturn(LocalDate.of(2022, 1, 1))

  val basicGetPenaltyDetailsModel: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = None,
    breathingSpace = None
  )

  val getPenaltyDetailsModelWithVATDue: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
    )),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
    )),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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

  val getPenaltyDetailsModelWithPartiallyPaidLSPs: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
    )),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = None,
          chargeOutstandingAmount = Some(100),
          chargeAmount = Some(200)
        ),
        LSPDetails(
          penaltyNumber = "12345677",
          penaltyOrder = "1",
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = None,
          chargeOutstandingAmount = Some(0),
          chargeAmount = Some(200)
        )
      )
    ))
  )

  val getPenaltyDetailsModelWithActiveAndInactiveLSPPs: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
    )),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
    )),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
    )),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
    )),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
          FAPIndicator = None,
          lateSubmissions = None,
          expiryReason = None,
          appealInformation = None,
          chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
          chargeOutstandingAmount = Some(0),
          chargeAmount = Some(200)
        ),
        LSPDetails(
          penaltyNumber = "12345678",
          penaltyOrder = "2",
          penaltyCategory = LSPPenaltyCategoryEnum.Threshold,
          penaltyStatus = LSPPenaltyStatusEnum.Inactive,
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
          communicationsDate = Some(LocalDate.of(2022, 1, 1)),
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
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
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
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
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(144.21),
              penaltyAmountPaid = None,
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(144.21),
              penaltyAmountPaid = None,
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )
    )
  )

  val getPenaltyDetailsModelWithLPPsUnpaidNoTTPEndDate: GetPenaltyDetails = basicGetPenaltyDetailsModel.copy(
    totalisations = Some(Totalisations(
      penalisedPrincipalTotal = Some(2000.12),
      LPPPostedTotal = None,
      LPPEstimatedTotal = None,
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
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
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(144.21),
              penaltyAmountPaid = None,
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = None
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(144.21),
              penaltyAmountPaid = None,
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = None
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
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
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(10),
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(154.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(10),
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(154.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(10.01), //Invalid scenario but proves filters out accuring penalties
              penaltyAmountPosted = None, //Invalid scenario but proves filters out accuring penalties
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(99.99),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
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
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(
                Seq(
                  AppealInformationType(appealStatus = Some(AppealStatusEnum.Under_Appeal), appealLevel = Some(AppealLevelEnum.HMRC))
                )
              ),
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(10),
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(154.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
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
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(
                Seq(
                  AppealInformationType(appealStatus = Some(AppealStatusEnum.Upheld), appealLevel = Some(AppealLevelEnum.HMRC))
                )
              ),
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(10),
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(154.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
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
      LSPTotalValue = None,
      totalAccountOverdue = None,
      totalAccountPostedInterest = None,
      totalAccountAccruingInterest = None
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
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = Some(144.21),
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(200),
              penaltyAmountPaid = Some(344.21),
              penaltyAmountPosted = Some(544.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = Some(10),
              LPP1LRCalculationAmount = Some(10),
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "123456789",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(200),
              penaltyAmountPaid = None,
              penaltyAmountPosted = Some(200),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = Some(10),
              LPP1LRCalculationAmount = Some(10),
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(),
              penaltyAmountAccruing = BigDecimal(0),

              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )
    )
  )

  val basicModel: UserHasPenaltyAuditModel =
    UserHasPenaltyAuditModel(basicGetPenaltyDetailsModel, "1234", "VRN", None, mockDateHelper)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val basicAgentModel: UserHasPenaltyAuditModel =
    UserHasPenaltyAuditModel(basicGetPenaltyDetailsModel, "1234", "VRN", Some("ARN123"), mockDateHelper)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithInterest: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithVATDue, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLSPPs: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLSPPs, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithPartiallyPaidLSPs: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithPartiallyPaidLSPs, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithActiveAndInactiveLSPPs: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithActiveAndInactiveLSPPs, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLSPPsUnderReview: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLSPPsUnderAppeal, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLSPPsAppealed: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLSPPsAppealed, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLSPUnpaidAndPaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLSPUnpaidAndRemoved, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsPaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsPaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsUnpaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsUnpaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsPartiallyPaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsPartiallyPaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsUnderReview: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsUnderReview, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsAccepted: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsAccepted, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsPaidAndUnpaidAndPartiallyPaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))

  val basicModelWithUserAgent: String => UserHasPenaltyAuditModel =
    (userAgent: String) => UserHasPenaltyAuditModel(basicGetPenaltyDetailsModel, "1234", "VRN", None, mockDateHelper)(fakeRequest.withHeaders("User-Agent" -> userAgent))

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

      "the user has parent charges and financial penalties" in {
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalTaxDue").validate[BigDecimal].get shouldBe 2000.12
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalFinancialPenaltyDue").validate[Int].get shouldBe 400
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalDue").validate[BigDecimal].get shouldBe 2400.12
      }

      "the user has LSPPs (no appeals)" in {
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPartiallyPaidLSPs").validate[Int].get shouldBe 0
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
      }

      "the user has LSPPs (with appeals)" in {
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnderReview.detail  \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 2
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPartiallyPaidLSPs").validate[Int].get shouldBe 0
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
      }

      "the user has LSPPs (with reviewed appeals)" in {
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "pointsTotal").validate[Int].get shouldBe 0
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPartiallyPaidLSPs").validate[Int].get shouldBe 0
        (auditModelWithLSPPsAppealed.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
      }

      "the user has LSPs that are paid and unpaid" in {
        (auditModelWithLSPUnpaidAndPaid.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 2
        (auditModelWithLSPUnpaidAndPaid.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithLSPUnpaidAndPaid.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "financialPenalties").validate[Int].get shouldBe 1
        (auditModelWithLSPUnpaidAndPaid.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLSPUnpaidAndPaid.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithLSPUnpaidAndPaid.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPartiallyPaidLSPs").validate[Int].get shouldBe 0
        (auditModelWithLSPUnpaidAndPaid.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 1
      }

      "the user has LSPs  that are partially paid" in {
        (auditModelWithPartiallyPaidLSPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithPartiallyPaidLSPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithPartiallyPaidLSPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithPartiallyPaidLSPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithPartiallyPaidLSPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithPartiallyPaidLSPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPartiallyPaidLSPs").validate[Int].get shouldBe 1
      }

      "the user has LSPs" in {
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 1
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "financialPenalties").validate[Int].get shouldBe 2
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 2
      }

      "the user has inactive LSPs" in {
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 5
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "inactivePoints").validate[Int].get shouldBe 1
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithActiveAndInactiveLSPPs.detail \ "penaltyInformation" \ "lateSubmissionPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
      }

      "the user has LPPs (all paid)" in {
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "timeToPayInformation" \ "isTimeToPayActive").validate[Boolean].get shouldBe false
      }

      "the user has LPPs (all partially paid)" in {
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfPartiallyPaidPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 3
        (auditModelWithLPPsPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LPPs (all unpaid)" in {
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has a combination of unpaid and paid and partially paid LPPs" in {
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfPartiallyPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 3
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "totalFinancialPenaltyDue").validate[Int].get shouldBe 400
        (auditModelWithLPPsUnpaidAndPaidAndPartiallyPaid.detail \ "penaltyInformation" \ "totalDue").validate[Int].get shouldBe 400
      }

      "the user has LPPs (with appeals)" in {
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 1
      }

      "the user has LPPs (with reviewed appeals)" in {
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "latePaymentPenaltyDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has an active TTP - With end date" in {
        when(mockDateHelper.dateNow()).thenReturn(LocalDate.of(2022, 2, 1))
        val auditModelWithLPPsUnpaid: UserHasPenaltyAuditModel = basicModel.copy(getPenaltyDetailsModelWithLPPsUnpaid, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "timeToPayInformation" \ "isTimeToPayActive").validate[Boolean].get shouldBe true
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "timeToPayInformation" \ "timeToPayStartDate").validate[LocalDate].get shouldBe LocalDate.of(2022, 1, 1)
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "timeToPayInformation" \ "timeToPayEndDate").validate[LocalDate].get shouldBe LocalDate.of(2022, 12, 31)
      }

      "the user has an active TTP - With no end date" in {
        when(mockDateHelper.dateNow()).thenReturn(LocalDate.of(2022, 2, 1))
        val auditModelWithLPPsUnpaid: UserHasPenaltyAuditModel = basicModel.copy(
          getPenaltyDetailsModelWithLPPsUnpaidNoTTPEndDate, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "timeToPayInformation" \ "isTimeToPayActive").validate[Boolean].get shouldBe true
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "timeToPayInformation" \ "timeToPayStartDate").validate[LocalDate].get shouldBe LocalDate.of(2022, 1, 1)
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "timeToPayInformation" \ "timeToPayEndDate").isDefined shouldBe false
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
