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

package services

import base.SpecBase
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, LineItemDetails, MainTransactionEnum}
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}

import java.time.LocalDate

class PenaltiesFrontendServiceSpec extends SpecBase {
  val penaltiesFrontendService: PenaltiesFrontendService = new PenaltiesFrontendService()

  "combineAPIData" should {
    "update both first and second penalty separately (no TTP)" in {
      val penaltyDetailsWithFirstAndSecondPenalty = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = None,
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(99.9),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = None,
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(99.9),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
              )
            )
          )
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnSecondLPP))))),
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))))
        )),
        totalisation = None
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(BigDecimal(99.9)),
              penaltyAmountPaid = None,
              penaltyAmountPosted = None,
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnCharge), outstandingAmount = None
              ),
              penaltyAmountAccruing = BigDecimal(99.9),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(BigDecimal(99.9)),
              penaltyAmountPaid = None,
              penaltyAmountPosted = None,
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnCharge), outstandingAmount = None
              ),
              penaltyAmountAccruing = BigDecimal(99.9),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithFirstAndSecondPenalty, financialDetails)
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

    "update only the first penalty if no second penalty present" in {
      val penaltyDetailsWithFirstPenalty = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                penaltyAmountPosted = Some(1101.44),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
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
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("1234567890"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))))
        )),
        totalisation = None
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              penaltyAmountPosted = Some(1101.44),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
                outstandingAmount = Some(123.45),
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

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithFirstPenalty, financialDetails)
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

    "update only the second penalty if no first penalty present" in {
      val penaltyDetailsWithSecondPenalty = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                penaltyAmountPosted = Some(1101.44),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
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
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("1234567890"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnSecondLPP)))))
        )),
        totalisation = None
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              penaltyAmountPosted = Some(1101.44),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnSecondLPP),
                outstandingAmount = Some(123.45),
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

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithSecondPenalty, financialDetails)
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

    "update two different principal charge penalties" in {
      val penaltyDetailsWithDifferentPrincipals = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567891",
                penaltyChargeReference = Some("1234567891"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                penaltyAmountPosted = Some(1101.44),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
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
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                penaltyAmountPosted = Some(1101.44),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
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
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(
          DocumentDetails(
            chargeReferenceNumber = Some("1234567891"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.OfficersAssessmentFirstLPP))))),
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))))
        )),
        totalisation = None
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567891",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              penaltyAmountPosted = Some(1101.44),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.OfficersAssessmentFirstLPP),
                outstandingAmount = Some(123.45),
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
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              penaltyAmountPosted = Some(1101.44),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
                outstandingAmount = Some(123.45),
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

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithDifferentPrincipals, financialDetails)
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

    "combine the financial details totalisations - if totalisations already present" in {
      val penaltyDetails = GetPenaltyDetails(
        totalisations = Some(
          Totalisations(
            LSPTotalValue = Some(123.45),
            penalisedPrincipalTotal = Some(321.45),
            LPPPostedTotal = None,
            LPPEstimatedTotal = None,
            totalAccountOverdue = None,
            totalAccountPostedInterest = None,
            totalAccountAccruingInterest = None
          )
        ),
        lateSubmissionPenalty = None,
        latePaymentPenalty = None,
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = None,
        totalisation = Some(FinancialDetailsTotalisation(
          regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(123.45))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = Some(43.21)))
        ))
      )

      val expectedResult: Totalisations = Totalisations(
        LSPTotalValue = Some(123.45),
        penalisedPrincipalTotal = Some(321.45),
        LPPPostedTotal = None,
        LPPEstimatedTotal = None,
        totalAccountOverdue = Some(123.45),
        totalAccountPostedInterest = Some(12.34),
        totalAccountAccruingInterest = Some(43.21)
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetails, financialDetails)
      result.totalisations.isDefined shouldBe true
      result.totalisations.get shouldBe expectedResult
    }

    "combine the financial details totalisations - if totalisations NOT already present" in {
      val penaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = None,
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = None,
        totalisation = Some(FinancialDetailsTotalisation(
          regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(123.45))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = None))
        ))
      )

      val expectedResult: Totalisations = Totalisations(
        LSPTotalValue = None,
        penalisedPrincipalTotal = None,
        LPPPostedTotal = None,
        LPPEstimatedTotal = None,
        totalAccountOverdue = Some(123.45),
        totalAccountPostedInterest = Some(12.34),
        totalAccountAccruingInterest = None
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetails, financialDetails)
      result.totalisations.isDefined shouldBe true
      result.totalisations.get shouldBe expectedResult
    }

    "not combine the financial data when the LPP has been appealed successfully" in {
      val penaltyDetailsWithSecondPenalty = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Upheld), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = Some(1101.44),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = Some(LocalDate.of(2022, 10, 30)),
                metadata = LPPDetailsMetadata(
                  timeToPay = None
                ),
                penaltyAmountAccruing = BigDecimal(0),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
              )
            )
          )
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = None,
        totalisation = None
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Upheld), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = None,
              penaltyAmountPosted = Some(1101.44),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 10, 30)),
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnCharge),
                outstandingAmount = None,
                timeToPay = None
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithSecondPenalty, financialDetails)
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }
  }
}
