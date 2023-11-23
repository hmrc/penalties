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

package services.auditing

import base.SpecBase
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, LineItemDetails, MainTransactionEnum}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.lateSubmission._
import services.APIService
import java.time.LocalDate

class APIServiceSpec extends SpecBase {
  class Setup {
    val service = new APIService()
  }

  "getNumberOfEstimatedPenalties" should {

    "return total number of estimated penalties" when {
      "penalty details has accruing LPPs" in new Setup {
        val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
          lateSubmissionPenalty = None,
          totalisations = None,
          latePaymentPenalty = Some(
            LatePaymentPenalty(
              details = Some(
                Seq(
                  LPPDetails(
                    penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                    principalChargeReference = "123456789",
                    penaltyChargeReference = Some("123456789"),
                    penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                    appealInformation = None,
                    principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                    principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                    principalChargeDueDate = LocalDate.of(2022, 1, 1),
                    communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyAmountOutstanding = None,
                    penaltyAmountPaid = None,
                    penaltyAmountPosted = 0,
                    LPP1LRDays = None,
                    LPP1HRDays = None,
                    LPP2Days = None,
                    LPP1HRCalculationAmount = None,
                    LPP1LRCalculationAmount = None,
                    LPP2Percentage = None,
                    LPP1LRPercentage = None,
                    LPP1HRPercentage = None,
                    penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
                    principalChargeLatestClearing = None,
                    metadata = LPPDetailsMetadata(),
                    penaltyAmountAccruing = BigDecimal(10.21),
                    principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                    vatOutstandingAmount = Some(BigDecimal(123.45))
                  ),
                  LPPDetails(
                    penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                    principalChargeReference = "123456788",
                    penaltyChargeReference = Some("123456788"),
                    penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                    appealInformation = None,
                    principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                    principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                    principalChargeDueDate = LocalDate.of(2022, 1, 1),
                    communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyAmountOutstanding = None,
                    penaltyAmountPaid = None,
                    penaltyAmountPosted = 0,
                    LPP1LRDays = None,
                    LPP1HRDays = None,
                    LPP2Days = None,
                    LPP1HRCalculationAmount = None,
                    LPP1LRCalculationAmount = None,
                    LPP2Percentage = None,
                    LPP1LRPercentage = None,
                    LPP1HRPercentage = None,
                    penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
                    principalChargeLatestClearing = None,
                    metadata = LPPDetailsMetadata(),
                    penaltyAmountAccruing = BigDecimal(10.21),
                    principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                    vatOutstandingAmount = Some(BigDecimal(123.45))
                  )
                )
              )
            )
          ),
          breathingSpace = None
        )
        service.getNumberOfEstimatedPenalties(penaltyDetails) shouldBe 2
      }
    }

    "return zero estimated penalties" when {
      "penalty details has no accruing LPPs" in new Setup {
        val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
          lateSubmissionPenalty = None,
          totalisations = None,
          latePaymentPenalty = Some(
            LatePaymentPenalty(
              details = Some(
                Seq(
                  LPPDetails(
                    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                    principalChargeReference = "123456789",
                    penaltyChargeReference = Some("123456789"),
                    penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyStatus = LPPPenaltyStatusEnum.Posted,
                    appealInformation = None,
                    principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                    principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                    principalChargeDueDate = LocalDate.of(2022, 1, 1),
                    communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyAmountOutstanding = Some(10.21),
                    penaltyAmountPaid = Some(10.21),
                    penaltyAmountPosted = 20.42,
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
                    principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                    vatOutstandingAmount = Some(BigDecimal(123.45))
                  ),
                  LPPDetails(
                    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                    principalChargeReference = "123456788",
                    penaltyChargeReference = Some("123456788"),
                    penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyStatus = LPPPenaltyStatusEnum.Posted,
                    appealInformation = None,
                    principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                    principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                    principalChargeDueDate = LocalDate.of(2022, 1, 1),
                    communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyAmountOutstanding = Some(10.21),
                    penaltyAmountPaid = Some(10.21),
                    penaltyAmountPosted = 20.42,
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
                    principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                    vatOutstandingAmount = Some(BigDecimal(123.45))
                  )
                )
              )
            )
          ),
          breathingSpace = None
        )
        service.getNumberOfEstimatedPenalties(penaltyDetails) shouldBe 0
      }
    }
  }

  "findEstimatedPenaltiesAmount" should {

    "return the outstanding amount of LPPs" in new Setup {
      val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        lateSubmissionPenalty = None,
        totalisations = None,
        latePaymentPenalty = Some(
          LatePaymentPenalty(
            Some(
              Seq(
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                  principalChargeReference = "123456788",
                  penaltyChargeReference = Some("123456789"),
                  penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                  penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                  appealInformation = None,
                  principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                  principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                  principalChargeDueDate = LocalDate.of(2022, 1, 1),
                  communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                  penaltyAmountOutstanding = None,
                  penaltyAmountPaid = None,
                  penaltyAmountPosted = 0,
                  LPP1LRDays = None,
                  LPP1HRDays = None,
                  LPP2Days = None,
                  LPP1HRCalculationAmount = None,
                  LPP1LRCalculationAmount = None,
                  LPP2Percentage = None,
                  LPP1LRPercentage = None,
                  LPP1HRPercentage = None,
                  penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
                  principalChargeLatestClearing = None,
                  metadata = LPPDetailsMetadata(),
                  penaltyAmountAccruing = BigDecimal(10.22),
                  principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                  vatOutstandingAmount = Some(BigDecimal(123.45))
                ),
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                  principalChargeReference = "123456788",
                  penaltyChargeReference = Some("123456788"),
                  penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                  penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                  appealInformation = None,
                  principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                  principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                  principalChargeDueDate = LocalDate.of(2022, 1, 1),
                  communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                  penaltyAmountOutstanding = None,
                  penaltyAmountPaid = None,
                  penaltyAmountPosted = 0,
                  LPP1LRDays = None,
                  LPP1HRDays = None,
                  LPP2Days = None,
                  LPP1HRCalculationAmount = None,
                  LPP1LRCalculationAmount = None,
                  LPP2Percentage = None,
                  LPP1LRPercentage = None,
                  LPP1HRPercentage = None,
                  penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
                  principalChargeLatestClearing = None,
                  metadata = LPPDetailsMetadata(),
                  penaltyAmountAccruing = BigDecimal(10.21),
                  principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                  vatOutstandingAmount = Some(BigDecimal(123.45))
                )
              )
            )
          )
        ),
        breathingSpace = None
      )
      val result: BigDecimal = service.findEstimatedPenaltiesAmount(penaltyDetails)
      result shouldBe BigDecimal(20.43)
    }

    "return 0 if no LPPs exist" in new Setup {
      val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        lateSubmissionPenalty = None,
        totalisations = None,
        latePaymentPenalty = None,
        breathingSpace = None
      )
      val result: BigDecimal = service.findEstimatedPenaltiesAmount(penaltyDetails)
      result shouldBe BigDecimal(0)
    }

    "return 0 if no accruing LPPs exist" in new Setup {
      val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        lateSubmissionPenalty = None,
        totalisations = None,
        latePaymentPenalty = Some(
          LatePaymentPenalty(
            Some(
              Seq(
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                  principalChargeReference = "123456788",
                  penaltyChargeReference = Some("123456789"),
                  penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                  penaltyStatus = LPPPenaltyStatusEnum.Posted,
                  appealInformation = None,
                  principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                  principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                  principalChargeDueDate = LocalDate.of(2022, 1, 1),
                  communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                  penaltyAmountOutstanding = Some(10.21),
                  penaltyAmountPaid = Some(10.21),
                  penaltyAmountPosted = 20.42,
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
                  principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                  vatOutstandingAmount = Some(BigDecimal(123.45))
                )
              )
            )
          )
        ),
        breathingSpace = None
      )
      val result: BigDecimal = service.findEstimatedPenaltiesAmount(penaltyDetails)
      result shouldBe BigDecimal(0)
    }
  }

  "checkIfHasAnyPenaltyData" should {

    "return true" when {
      "penalty details has LPP and LSP" in new Setup {
        val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
          lateSubmissionPenalty = Some(
            LateSubmissionPenalty(
              summary = LSPSummary(
                activePenaltyPoints = 1,
                inactivePenaltyPoints = 0,
                regimeThreshold = 2,
                penaltyChargeAmount = 200,
                PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
              ),
              details = Seq(
                LSPDetails(
                  penaltyNumber = "12345678",
                  penaltyOrder = Some("1"),
                  penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
                  penaltyStatus = LSPPenaltyStatusEnum.Active,
                  penaltyCreationDate = LocalDate.of(2022, 1, 1),
                  penaltyExpiryDate = LocalDate.of(2022, 1, 1),
                  communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                  FAPIndicator = None,
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
            )
          ),
          totalisations = None,
          latePaymentPenalty = Some(
            LatePaymentPenalty(
              details = Some(
                Seq(
                  LPPDetails(
                    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                    principalChargeReference = "123456789",
                    penaltyChargeReference = Some("123456789"),
                    penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyStatus = LPPPenaltyStatusEnum.Posted,
                    appealInformation = None,
                    principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                    principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                    principalChargeDueDate = LocalDate.of(2022, 1, 1),
                    communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyAmountOutstanding = Some(10.21),
                    penaltyAmountPaid = Some(10.21),
                    penaltyAmountPosted = 20.42,
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
                    principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                    vatOutstandingAmount = Some(BigDecimal(123.45))
                  )
                )
              )
            )
          ),
          breathingSpace = None
        )
        service.checkIfHasAnyPenaltyData(penaltyDetails) shouldBe true
      }

      "penalty details has NO LPP but has LSP" in new Setup {
        val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
          lateSubmissionPenalty = Some(
            LateSubmissionPenalty(
              summary = LSPSummary(
                activePenaltyPoints = 1,
                inactivePenaltyPoints = 0,
                regimeThreshold = 2,
                penaltyChargeAmount = 200,
                PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
              ),
              details = Seq(
                LSPDetails(
                  penaltyNumber = "12345678",
                  penaltyOrder = Some("1"),
                  penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
                  penaltyStatus = LSPPenaltyStatusEnum.Active,
                  penaltyCreationDate = LocalDate.of(2022, 1, 1),
                  penaltyExpiryDate = LocalDate.of(2022, 1, 1),
                  communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                  FAPIndicator = None,
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
            )
          ),
          totalisations = None,
          latePaymentPenalty = None,
          breathingSpace = None
        )
        service.checkIfHasAnyPenaltyData(penaltyDetails) shouldBe true
      }

      "penalty details has LPP but has NO LSP" in new Setup {
        val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
          lateSubmissionPenalty = None,
          totalisations = None,
          latePaymentPenalty = Some(
            LatePaymentPenalty(
              details = Some(
                Seq(
                  LPPDetails(
                    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                    principalChargeReference = "123456789",
                    penaltyChargeReference = Some("123456789"),
                    penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyStatus = LPPPenaltyStatusEnum.Posted,
                    appealInformation = None,
                    principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                    principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                    principalChargeDueDate = LocalDate.of(2022, 1, 1),
                    communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                    penaltyAmountOutstanding = None,
                    penaltyAmountPaid = Some(144.21),
                    penaltyAmountPosted = 144.21,
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
                    principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                    vatOutstandingAmount = Some(BigDecimal(123.45))
                  )
                )
              )
            )
          ),
          breathingSpace = None
        )
        service.checkIfHasAnyPenaltyData(penaltyDetails) shouldBe true
      }
    }

    "return false" when {
      "penalty details has NO LPP and NO LSP" in new Setup {
        val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
          totalisations = None, lateSubmissionPenalty = None, latePaymentPenalty = None, breathingSpace = None
        )
        service.checkIfHasAnyPenaltyData(penaltyDetails) shouldBe false
      }
    }
  }

  "getNumberOfCrystallisedPenalties" should {

    val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
      lateSubmissionPenalty = None,
      totalisations = None,
      latePaymentPenalty = Some(
        LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "123456789",
                penaltyChargeReference = Some("123456789"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = None,
                principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                principalChargeDueDate = LocalDate.of(2022, 1, 1),
                communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 0,
                LPP1LRDays = None,
                LPP1HRDays = None,
                LPP2Days = None,
                LPP1HRCalculationAmount = None,
                LPP1LRCalculationAmount = None,
                LPP2Percentage = None,
                LPP1LRPercentage = None,
                LPP1HRPercentage = None,
                penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(10.21),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "123456789",
                penaltyChargeReference = Some("123456788"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = None,
                principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                principalChargeDueDate = LocalDate.of(2022, 1, 1),
                communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyAmountOutstanding = Some(10.21),
                penaltyAmountPaid = Some(10.21),
                penaltyAmountPosted = 20.42,
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
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              )
            )
          )
        )
      ),
      breathingSpace = None
    )

    "return the correct amount of due penalties in a payload" in new Setup {
      val result = service.getNumberOfCrystallisedPenalties(penaltyDetails, None)
      result shouldBe 1
    }

    "return the correct amount of due penalties in a payload when FinancialDetails are included" in new Setup {
      val financialDetails = FinancialDetails(
        None,
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("xyz1234"),
          documentOutstandingAmount = Some(100.00),
          documentTotalAmount = Some(200.00),
          lineItemDetails = Some(Seq(LineItemDetails(
            mainTransaction = Some(MainTransactionEnum.ManualLPP)
          ))),
          issueDate = Some(LocalDate.of(2023,1,1))
        )))
      )
      val result = service.getNumberOfCrystallisedPenalties(penaltyDetails, Some(financialDetails))
      result shouldBe 2
    }

    val penaltyDetailsWithNoDuePenalties: GetPenaltyDetails = GetPenaltyDetails(
      lateSubmissionPenalty = None,
      totalisations = None,
      latePaymentPenalty = Some(
        LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "123456789",
                penaltyChargeReference = Some("123456789"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = None,
                principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                principalChargeDueDate = LocalDate.of(2022, 1, 1),
                communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 0,
                LPP1LRDays = None,
                LPP1HRDays = None,
                LPP2Days = None,
                LPP1HRCalculationAmount = None,
                LPP1LRCalculationAmount = None,
                LPP2Percentage = None,
                LPP1LRPercentage = None,
                LPP1HRPercentage = None,
                penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(10.21),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "123456789",
                penaltyChargeReference = Some("123456788"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = None,
                principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                principalChargeDueDate = LocalDate.of(2022, 1, 1),
                communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyAmountOutstanding = Some(0),
                penaltyAmountPaid = Some(10.21),
                penaltyAmountPosted = 10.21,
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
                penaltyAmountAccruing = BigDecimal(10.21),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              )
            )
          )
        )
      ),
      breathingSpace = None
    )

    "return 0 when a payload has no due penalties" in new Setup {
      val result: Int = service.getNumberOfCrystallisedPenalties(penaltyDetailsWithNoDuePenalties, None)
      result shouldBe 0
    }

    "return 0 when a payload has no due penalties, in penalty details and financial details" in new Setup {
      val financialDetails = FinancialDetails(
        None,
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("xyz1234"),
          documentOutstandingAmount = Some(0.00),
          documentTotalAmount = Some(200.00),
          lineItemDetails = None,
          issueDate = Some(LocalDate.of(2023,1,1))
        )))
      )
      val result: Int = service.getNumberOfCrystallisedPenalties(penaltyDetailsWithNoDuePenalties, Some(financialDetails))
      result shouldBe 0
    }

  }

  "getCrystallisedPenaltyTotal" should {

    val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
      lateSubmissionPenalty = Some(
        LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 1,
            inactivePenaltyPoints = 0,
            regimeThreshold = 2,
            penaltyChargeAmount = 200,
            PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
          ),
          details = Seq(
            LSPDetails(
              penaltyNumber = "12345678",
              penaltyOrder = Some("2"),
              penaltyCategory = Some(LSPPenaltyCategoryEnum.Threshold),
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.of(2022, 1, 1),
              penaltyExpiryDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              FAPIndicator = None,
              lateSubmissions = None,
              expiryReason = None,
              appealInformation = None,
              chargeDueDate = None,
              chargeOutstandingAmount = Some(200),
              chargeAmount = None,
              triggeringProcess = None,
              chargeReference = None
            ),
            LSPDetails(
              penaltyNumber = "12345678",
              penaltyOrder = Some("1"),
              penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.of(2022, 1, 1),
              penaltyExpiryDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              FAPIndicator = None,
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
        )
      ),
      totalisations = None,
      latePaymentPenalty = Some(
        LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "123456789",
                penaltyChargeReference = Some("123456789"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = None,
                principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                principalChargeDueDate = LocalDate.of(2022, 1, 1),
                communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyAmountOutstanding = Some(10.21),
                penaltyAmountPaid = Some(10.21),
                penaltyAmountPosted = 20.42,
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
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              )
            )
          )
        )
      ),
      breathingSpace = None
    )

    "return the correct total of due penalties in a payload" in new Setup {
      val result: BigDecimal = service.getCrystallisedPenaltyTotal(penaltyDetails, None)
      result shouldBe BigDecimal(210.21)
    }

    "return the correct total of due penalties in a payload when financial details are included" in new Setup {
      val financialDetails: FinancialDetails = FinancialDetails(
        None,
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("xyz1234"),
          documentOutstandingAmount = Some(100.00),
          documentTotalAmount = Some(200.00),
          lineItemDetails = Some(Seq(LineItemDetails(
            mainTransaction = Some(MainTransactionEnum.ManualLPP)
          ))),
          issueDate = Some(LocalDate.of(2023,1,1))
        )))
      )
      val result: BigDecimal = service.getCrystallisedPenaltyTotal(penaltyDetails, Some(financialDetails))
      result shouldBe BigDecimal(310.21)
    }

    val penaltyDetailsWithNoPenalties: GetPenaltyDetails = GetPenaltyDetails(
      lateSubmissionPenalty = Some(
        LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 1,
            inactivePenaltyPoints = 0,
            regimeThreshold = 2,
            penaltyChargeAmount = 200,
            PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
          ),
          details = Seq(
            LSPDetails(
              penaltyNumber = "12345678",
              penaltyOrder = Some("2"),
              penaltyCategory = Some(LSPPenaltyCategoryEnum.Threshold),
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.of(2022, 1, 1),
              penaltyExpiryDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              FAPIndicator = None,
              lateSubmissions = None,
              expiryReason = None,
              appealInformation = None,
              chargeDueDate = None,
              chargeOutstandingAmount = Some(0),
              chargeAmount = None,
              triggeringProcess = None,
              chargeReference = None
            ),
            LSPDetails(
              penaltyNumber = "12345678",
              penaltyOrder = Some("1"),
              penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.of(2022, 1, 1),
              penaltyExpiryDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              FAPIndicator = None,
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
        )
      ),
      totalisations = None,
      latePaymentPenalty = Some(
        LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "123456789",
                penaltyChargeReference = Some("123456789"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = None,
                principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                principalChargeDueDate = LocalDate.of(2022, 1, 1),
                communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyAmountOutstanding = Some(0),
                penaltyAmountPaid = Some(10.21),
                penaltyAmountPosted = 10.21,
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
                penaltyAmountAccruing = BigDecimal(10.21),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              )
            )
          )
        )
      ),
      breathingSpace = None
    )

    "return 0 when the payload has no due penalties" in new Setup {
      val result: BigDecimal = service.getCrystallisedPenaltyTotal(penaltyDetailsWithNoPenalties, None)
      result shouldBe BigDecimal(0)
    }

    "return 0 when the payload has no due penalties, in penalties details and financial details" in new Setup {
      val financialDetails = FinancialDetails(
        None,
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("xyz1234"),
          documentOutstandingAmount = Some(0.00),
          documentTotalAmount = Some(200.00),
          lineItemDetails = None,
          issueDate = Some(LocalDate.of(2023,1,1))
        )))
      )
      val result: BigDecimal = service.getCrystallisedPenaltyTotal(penaltyDetailsWithNoPenalties, Some(financialDetails))
      result shouldBe BigDecimal(0)
    }
  }
}
