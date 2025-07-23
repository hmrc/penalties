/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import base.SpecBase
import models.hipPenaltyDetails.{PenaltyDetails => HIPPenaltyDetails}
import models.getPenaltyDetails.GetPenaltyDetails
import models.hipPenaltyDetails.lateSubmission._
import models.hipPenaltyDetails.latePayment._
import models.hipPenaltyDetails.appealInfo._
import models.hipPenaltyDetails.breathingSpace.BreathingSpace
import models.hipPenaltyDetails.Totalisations

import java.time.{LocalDate, Instant}

class PenaltyDetailsConverterSpec extends SpecBase {

  "PenaltyDetailsConverter" should {
    
    "convert HIP PenaltyDetails to regular GetPenaltyDetails" in {
      val hipPenaltyDetails = HIPPenaltyDetails(
        processingDate = Instant.now(),
        totalisations = Some(Totalisations(
          lspTotalValue = Some(100.00),
          penalisedPrincipalTotal = Some(200.00),
          lppPostedTotal = Some(50.00),
          lppEstimatedTotal = Some(25.00),
          totalAccountOverdue = Some(300.00),
          totalAccountPostedInterest = Some(10.00),
          totalAccountAccruingInterest = Some(5.00)
        )),
        lateSubmissionPenalty = Some(LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 2,
            inactivePenaltyPoints = 1,
            regimeThreshold = 10,
            penaltyChargeAmount = 200.00,
            pocAchievementDate = Some(LocalDate.of(2022, 1, 1))
          ),
          details = Seq(
            LSPDetails(
              penaltyNumber = "123456789",
              penaltyOrder = Some("01"),
              penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.of(2022, 1, 1),
              penaltyExpiryDate = LocalDate.of(2022, 12, 31),
              communicationsDate = Some(LocalDate.of(2022, 1, 15)),
              fapIndicator = Some("X"),
              lateSubmissions = Some(Seq(
                models.hipPenaltyDetails.lateSubmission.LateSubmission(
                  lateSubmissionID = "001",
                  incomeSource = Some("IT"),
                  taxPeriod = Some("23AA"),
                  taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                  taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
                  taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
                  returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
                  taxReturnStatus = Some(TaxReturnStatusEnum.Fulfilled)
                )
              )),
              expiryReason = Some(ExpiryReasonEnum.NaturalExpiration),
              appealInformation = Some(Seq(
                AppealInformationType(
                  appealStatus = Some(AppealStatusEnum.Under_Appeal),
                  appealLevel = Some(AppealLevelEnum.HMRC),
                  appealDescription = Some("Test appeal")
                )
              )),
              chargeDueDate = Some(LocalDate.of(2022, 12, 31)),
              chargeOutstandingAmount = Some(200.00),
              chargeAmount = Some(200.00),
              triggeringProcess = Some("TEST"),
              chargeReference = Some("CHARGE123")
            )
          )
        )),
        latePaymentPenalty = Some(LatePaymentPenalty(
          lppDetails = Some(Seq(
            LPPDetails(
              principalChargeReference = "PRINCIPAL123",
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              penaltyStatus = Some(LPPPenaltyStatusEnum.Posted),
              penaltyAmountAccruing = 0.00,
              penaltyAmountPosted = 50.00,
              penaltyAmountPaid = Some(25.00),
              penaltyAmountOutstanding = Some(25.00),
              lpp1LRCalculationAmt = Some(25.00),
              lpp1LRDays = Some("30"),
              lpp1LRPercentage = Some(2.00),
              lpp1HRCalculationAmt = Some(25.00),
              lpp1HRDays = Some("30"),
              lpp1HRPercentage = Some(4.00),
              lpp2Days = Some("30"),
              lpp2Percentage = Some(4.00),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              communicationsDate = Some(LocalDate.of(2022, 1, 15)),
              penaltyChargeReference = Some("LPP123"),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 12, 31)),
              appealInformation = Some(Seq(
                AppealInformationType(
                  appealStatus = Some(AppealStatusEnum.Upheld),
                  appealLevel = Some(AppealLevelEnum.HMRC),
                  appealDescription = Some("LPP appeal")
                )
              )),
              principalChargeDocNumber = Some("DOC123"),
              principalChargeMainTr = "4700",
              principalChargeSubTr = Some("1174"),
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 12, 31),
              principalChargeDueDate = LocalDate.of(2022, 12, 31),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 12, 31)),
              timeToPay = Some(Seq(
                models.hipPenaltyDetails.latePayment.TimeToPay(
                  ttpStartDate = Some(LocalDate.of(2022, 1, 1)),
                  ttpEndDate = Some(LocalDate.of(2022, 12, 31))
                )
              ))
            )
          )),
          manualLPPIndicator = false
        )),
        breathingSpace = Some(Seq(
          BreathingSpace(
            bsStartDate = LocalDate.of(2022, 1, 1),
            bsEndDate = LocalDate.of(2022, 12, 31)
          )
        ))
      )

      val result = PenaltyDetailsConverter.convertHIPToGetPenaltyDetails(hipPenaltyDetails)

      result shouldBe a[GetPenaltyDetails]
      result.totalisations shouldBe defined
      result.lateSubmissionPenalty shouldBe defined
      result.latePaymentPenalty shouldBe defined
      result.breathingSpace shouldBe defined
    }

    "convert LateSubmission with incomeSource correctly" in {
      val hipLateSubmission = models.hipPenaltyDetails.lateSubmission.LateSubmission(
        lateSubmissionID = "001",
        incomeSource = Some("IT"),
        taxPeriod = Some("23AA"),
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
        taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
        returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
        taxReturnStatus = Some(TaxReturnStatusEnum.Fulfilled)
      )

      val result = PenaltyDetailsConverter.convertHIPToGetPenaltyDetails(
        HIPPenaltyDetails(
          processingDate = Instant.now(),
          totalisations = None,
          lateSubmissionPenalty = Some(LateSubmissionPenalty(
            summary = LSPSummary(0, 0, 10, 0.00, None),
            details = Seq(
              LSPDetails(
                penaltyNumber = "123",
                penaltyOrder = None,
                penaltyCategory = None,
                penaltyStatus = LSPPenaltyStatusEnum.Active,
                penaltyCreationDate = LocalDate.now(),
                penaltyExpiryDate = LocalDate.now(),
                communicationsDate = None,
                fapIndicator = None,
                lateSubmissions = Some(Seq(hipLateSubmission)),
                expiryReason = None,
                appealInformation = None,
                chargeDueDate = None,
                chargeOutstandingAmount = None,
                chargeAmount = None,
                triggeringProcess = None,
                chargeReference = None
              )
            )
          )),
          latePaymentPenalty = None,
          breathingSpace = None
        )
      )

      val convertedLateSubmission = result.lateSubmissionPenalty.flatMap(_.details.headOption).flatMap(_.lateSubmissions).flatMap(_.headOption)
      convertedLateSubmission shouldBe defined
      convertedLateSubmission.get.incomeSource shouldBe Some("IT")
    }

    "handle upheld appeals correctly by setting penalty status to inactive" in {
      val hipPenaltyDetails = HIPPenaltyDetails(
        processingDate = Instant.now(),
        totalisations = None,
        lateSubmissionPenalty = Some(LateSubmissionPenalty(
          summary = LSPSummary(0, 1, 10, 0.00, None),
          details = Seq(
            LSPDetails(
              penaltyNumber = "123",
              penaltyOrder = None,
              penaltyCategory = None,
              penaltyStatus = LSPPenaltyStatusEnum.Active, // This should become Inactive due to upheld appeal
              penaltyCreationDate = LocalDate.now(),
              penaltyExpiryDate = LocalDate.now(),
              communicationsDate = None,
              fapIndicator = None,
              lateSubmissions = None,
              expiryReason = None,
              appealInformation = Some(Seq(
                AppealInformationType(
                  appealStatus = Some(AppealStatusEnum.Upheld),
                  appealLevel = Some(AppealLevelEnum.HMRC),
                  appealDescription = Some("Upheld appeal")
                )
              )),
              chargeDueDate = None,
              chargeOutstandingAmount = None,
              chargeAmount = None,
              triggeringProcess = None,
              chargeReference = None
            )
          )
        )),
        latePaymentPenalty = None,
        breathingSpace = None
      )

      val result = PenaltyDetailsConverter.convertHIPToGetPenaltyDetails(hipPenaltyDetails)
      val convertedDetail = result.lateSubmissionPenalty.flatMap(_.details.headOption)
      
      convertedDetail shouldBe defined
      convertedDetail.get.penaltyStatus shouldBe models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive
    }

    "calculate corrected LSP summary counts correctly" in {
      val hipPenaltyDetails = HIPPenaltyDetails(
        processingDate = Instant.now(),
        totalisations = None,
        lateSubmissionPenalty = Some(LateSubmissionPenalty(
          summary = LSPSummary(5, 3, 10, 0.00, None), // These should be recalculated
          details = Seq(
            LSPDetails(
              penaltyNumber = "123",
              penaltyOrder = None,
              penaltyCategory = None,
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.now(),
              penaltyExpiryDate = LocalDate.now(),
              communicationsDate = None,
              fapIndicator = None,
              lateSubmissions = None,
              expiryReason = None,
              appealInformation = Some(Seq(
                AppealInformationType(
                  appealStatus = Some(AppealStatusEnum.Upheld),
                  appealLevel = Some(AppealLevelEnum.HMRC),
                  appealDescription = Some("Upheld appeal")
                )
              )),
              chargeDueDate = None,
              chargeOutstandingAmount = None,
              chargeAmount = None,
              triggeringProcess = None,
              chargeReference = None
            ),
            LSPDetails(
              penaltyNumber = "124",
              penaltyOrder = None,
              penaltyCategory = None,
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.now(),
              penaltyExpiryDate = LocalDate.now(),
              communicationsDate = None,
              fapIndicator = None,
              lateSubmissions = None,
              expiryReason = None,
              appealInformation = None,
              chargeDueDate = None,
              chargeOutstandingAmount = None,
              chargeAmount = None,
              triggeringProcess = None,
              chargeReference = None
            )
          )
        )),
        latePaymentPenalty = None,
        breathingSpace = None
      )

      val result = PenaltyDetailsConverter.convertHIPToGetPenaltyDetails(hipPenaltyDetails)
      val convertedSummary = result.lateSubmissionPenalty.map(_.summary)
      
      convertedSummary shouldBe defined
      convertedSummary.get.activePenaltyPoints shouldBe 1 // One active (the second one)
      convertedSummary.get.inactivePenaltyPoints shouldBe 1 // One inactive (the first one due to upheld appeal)
    }
  }
} 