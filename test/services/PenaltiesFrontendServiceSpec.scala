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

package services

import base.SpecBase
import models.getFinancialDetails.{FinancialDetails, FinancialDetailsMetadata, FinancialItem, FinancialItemMetadata, GetFinancialDetails}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPDetailsMetadata, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty, TimeToPay}
import models.mainTransaction.MainTransactionEnum

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
                penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = LocalDate.of(2022, 10, 30),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata()
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = LocalDate.of(2022, 10, 30),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata()
              )
            )
          )
        ))
      )

      val financialDetails = GetFinancialDetails(
        documentDetails = Seq.empty,
        financialDetails = Seq(
          FinancialDetails(
            documentId = "DOC1234",
            taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
            items = Seq(
              FinancialItem(
                dueDate = Some(LocalDate.of(2018, 8, 13)),
                clearingDate = Some(LocalDate.of(2018, 8, 13)),
                metadata = FinancialItemMetadata(
                  subItem = Some("001"),
                  amount = Some(10000),
                  clearingReason = Some("01"),
                  outgoingPaymentMethod = Some("outgoing payment"),
                  paymentLock = Some("paymentLock"),
                  clearingLock = Some("clearingLock"),
                  interestLock = Some("interestLock"),
                  dunningLock = Some("dunningLock"),
                  returnFlag = Some(true),
                  paymentReference = Some("Ab12453535"),
                  paymentAmount = Some(10000),
                  paymentMethod = Some("Payment"),
                  paymentLot = Some("081203010024"),
                  paymentLotItem = Some("000001"),
                  clearingSAPDocument = Some("3350000253"),
                  codingInitiationDate = Some(LocalDate.of(2021, 1, 11)),
                  statisticalDocument = Some("S"),
                  DDCollectionInProgress = Some(true),
                  returnReason = Some("ABCA"),
                  promisetoPay = Some("Y")
                )
              )
            ),
            originalAmount = Some(123.45),
            outstandingAmount = Some(123.45),
            mainTransaction = Some(MainTransactionEnum.VATReturnSecondLPP),
            chargeReference = Some("1234567890"),
            metadata = FinancialDetailsMetadata(
              taxYear = "2022",
              chargeType = Some("1234"),
              mainType = Some("1234"),
              periodKey = Some("123"),
              periodKeyDescription = Some("foobar"),
              businessPartner = Some("123"),
              contractAccountCategory = Some("1"),
              contractAccount = Some("1"),
              contractObjectType = Some("1"),
              contractObject = Some("1"),
              sapDocumentNumber = Some("1"),
              sapDocumentNumberItem = Some("1"),
              subTransaction = Some("1"),
              clearedAmount = Some(123.45),
              accruedInterest = Some(123.45)
            )
          ),
          FinancialDetails(
            documentId = "DOC1234",
            taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
            items = Seq(
              FinancialItem(
                dueDate = Some(LocalDate.of(2018, 8, 13)),
                clearingDate = Some(LocalDate.of(2018, 8, 13)),
                metadata = FinancialItemMetadata(
                  subItem = Some("001"),
                  amount = Some(10000),
                  clearingReason = Some("01"),
                  outgoingPaymentMethod = Some("outgoing payment"),
                  paymentLock = Some("paymentLock"),
                  clearingLock = Some("clearingLock"),
                  interestLock = Some("interestLock"),
                  dunningLock = Some("dunningLock"),
                  returnFlag = Some(true),
                  paymentReference = Some("Ab12453535"),
                  paymentAmount = Some(10000),
                  paymentMethod = Some("Payment"),
                  paymentLot = Some("081203010024"),
                  paymentLotItem = Some("000001"),
                  clearingSAPDocument = Some("3350000253"),
                  codingInitiationDate = Some(LocalDate.of(2021, 1, 11)),
                  statisticalDocument = Some("S"),
                  DDCollectionInProgress = Some(true),
                  returnReason = Some("ABCA"),
                  promisetoPay = Some("Y")
                )
              )
            ),
            originalAmount = Some(123.45),
            outstandingAmount = Some(123.45),
            mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
            chargeReference = Some("1234567890"),
            metadata = FinancialDetailsMetadata(
              taxYear = "2022",
              chargeType = Some("1234"),
              mainType = Some("1234"),
              periodKey = Some("123"),
              periodKeyDescription = Some("foobar"),
              businessPartner = Some("123"),
              contractAccountCategory = Some("1"),
              contractAccount = Some("1"),
              contractObjectType = Some("1"),
              contractObject = Some("1"),
              sapDocumentNumber = Some("1"),
              sapDocumentNumberItem = Some("1"),
              subTransaction = Some("1"),
              clearedAmount = Some(123.45),
              accruedInterest = Some(123.45)
            )
          )
        )
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnSecondLPP), outstandingAmount = Some(123.45)
              )
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP), outstandingAmount = Some(123.45)
              )
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
                penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = LocalDate.of(2022, 10, 30),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(
                  timeToPay = Some(Seq(TimeToPay(
                    TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                    TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                  )))
                )
              )
            )
          )
        ))
      )

      val financialDetails = GetFinancialDetails(
        documentDetails = Seq.empty,
        financialDetails = Seq(
          FinancialDetails(
            documentId = "DOC1234",
            taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
            items = Seq(
              FinancialItem(
                dueDate = Some(LocalDate.of(2018, 8, 13)),
                clearingDate = Some(LocalDate.of(2018, 8, 13)),
                metadata = FinancialItemMetadata(
                  subItem = Some("001"),
                  amount = Some(10000),
                  clearingReason = Some("01"),
                  outgoingPaymentMethod = Some("outgoing payment"),
                  paymentLock = Some("paymentLock"),
                  clearingLock = Some("clearingLock"),
                  interestLock = Some("interestLock"),
                  dunningLock = Some("dunningLock"),
                  returnFlag = Some(true),
                  paymentReference = Some("Ab12453535"),
                  paymentAmount = Some(10000),
                  paymentMethod = Some("Payment"),
                  paymentLot = Some("081203010024"),
                  paymentLotItem = Some("000001"),
                  clearingSAPDocument = Some("3350000253"),
                  codingInitiationDate = Some(LocalDate.of(2021, 1, 11)),
                  statisticalDocument = Some("S"),
                  DDCollectionInProgress = Some(true),
                  returnReason = Some("ABCA"),
                  promisetoPay = Some("Y")
                )
              )
            ),
            originalAmount = Some(123.45),
            outstandingAmount = Some(123.45),
            mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
            chargeReference = Some("1234567890"),
            metadata = FinancialDetailsMetadata(
              taxYear = "2022",
              chargeType = Some("1234"),
              mainType = Some("1234"),
              periodKey = Some("123"),
              periodKeyDescription = Some("foobar"),
              businessPartner = Some("123"),
              contractAccountCategory = Some("1"),
              contractAccount = Some("1"),
              contractObjectType = Some("1"),
              contractObject = Some("1"),
              sapDocumentNumber = Some("1"),
              sapDocumentNumberItem = Some("1"),
              subTransaction = Some("1"),
              clearedAmount = Some(123.45),
              accruedInterest = Some(123.45)
            )
          )
        )
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
                outstandingAmount = Some(123.45),
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              )
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
                penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = LocalDate.of(2022, 10, 30),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(
                  timeToPay = Some(Seq(TimeToPay(
                    TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                    TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                  )))
                )
              )
            )
          )
        ))
      )

      val financialDetails = GetFinancialDetails(
        documentDetails = Seq.empty,
        financialDetails = Seq(
          FinancialDetails(
            documentId = "DOC1234",
            taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
            items = Seq(
              FinancialItem(
                dueDate = Some(LocalDate.of(2018, 8, 13)),
                clearingDate = Some(LocalDate.of(2018, 8, 13)),
                metadata = FinancialItemMetadata(
                  subItem = Some("001"),
                  amount = Some(10000),
                  clearingReason = Some("01"),
                  outgoingPaymentMethod = Some("outgoing payment"),
                  paymentLock = Some("paymentLock"),
                  clearingLock = Some("clearingLock"),
                  interestLock = Some("interestLock"),
                  dunningLock = Some("dunningLock"),
                  returnFlag = Some(true),
                  paymentReference = Some("Ab12453535"),
                  paymentAmount = Some(10000),
                  paymentMethod = Some("Payment"),
                  paymentLot = Some("081203010024"),
                  paymentLotItem = Some("000001"),
                  clearingSAPDocument = Some("3350000253"),
                  codingInitiationDate = Some(LocalDate.of(2021, 1, 11)),
                  statisticalDocument = Some("S"),
                  DDCollectionInProgress = Some(true),
                  returnReason = Some("ABCA"),
                  promisetoPay = Some("Y")
                )
              )
            ),
            originalAmount = Some(123.45),
            outstandingAmount = Some(123.45),
            mainTransaction = Some(MainTransactionEnum.VATReturnSecondLPP),
            chargeReference = Some("1234567890"),
            metadata = FinancialDetailsMetadata(
              taxYear = "2022",
              chargeType = Some("1234"),
              mainType = Some("1234"),
              periodKey = Some("123"),
              periodKeyDescription = Some("foobar"),
              businessPartner = Some("123"),
              contractAccountCategory = Some("1"),
              contractAccount = Some("1"),
              contractObjectType = Some("1"),
              contractObject = Some("1"),
              sapDocumentNumber = Some("1"),
              sapDocumentNumberItem = Some("1"),
              subTransaction = Some("1"),
              clearedAmount = Some(123.45),
              accruedInterest = Some(123.45)
            )
          )
        )
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnSecondLPP),
                outstandingAmount = Some(123.45),
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              )
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
                penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = LocalDate.of(2022, 10, 30),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(
                  timeToPay = Some(Seq(TimeToPay(
                    TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                    TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                  )))
                )
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = LocalDate.of(2022, 10, 30),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(
                  timeToPay = Some(Seq(TimeToPay(
                    TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                    TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                  )))
                )
              )
            )
          )
        ))
      )

      val financialDetails = GetFinancialDetails(
        documentDetails = Seq.empty,
        financialDetails = Seq(
          FinancialDetails(
            documentId = "DOC1234",
            taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
            items = Seq(
              FinancialItem(
                dueDate = Some(LocalDate.of(2018, 8, 13)),
                clearingDate = Some(LocalDate.of(2018, 8, 13)),
                metadata = FinancialItemMetadata(
                  subItem = Some("001"),
                  amount = Some(10000),
                  clearingReason = Some("01"),
                  outgoingPaymentMethod = Some("outgoing payment"),
                  paymentLock = Some("paymentLock"),
                  clearingLock = Some("clearingLock"),
                  interestLock = Some("interestLock"),
                  dunningLock = Some("dunningLock"),
                  returnFlag = Some(true),
                  paymentReference = Some("Ab12453535"),
                  paymentAmount = Some(10000),
                  paymentMethod = Some("Payment"),
                  paymentLot = Some("081203010024"),
                  paymentLotItem = Some("000001"),
                  clearingSAPDocument = Some("3350000253"),
                  codingInitiationDate = Some(LocalDate.of(2021, 1, 11)),
                  statisticalDocument = Some("S"),
                  DDCollectionInProgress = Some(true),
                  returnReason = Some("ABCA"),
                  promisetoPay = Some("Y")
                )
              )
            ),
            originalAmount = Some(123.45),
            outstandingAmount = Some(123.45),
            mainTransaction = Some(MainTransactionEnum.OfficersAssessmentFirstLPP),
            chargeReference = Some("1234567891"),
            metadata = FinancialDetailsMetadata(
              taxYear = "2022",
              chargeType = Some("1234"),
              mainType = Some("1234"),
              periodKey = Some("123"),
              periodKeyDescription = Some("foobar"),
              businessPartner = Some("123"),
              contractAccountCategory = Some("1"),
              contractAccount = Some("1"),
              contractObjectType = Some("1"),
              contractObject = Some("1"),
              sapDocumentNumber = Some("1"),
              sapDocumentNumberItem = Some("1"),
              subTransaction = Some("1"),
              clearedAmount = Some(123.45),
              accruedInterest = Some(123.45)
            )
          ),
          FinancialDetails(
            documentId = "DOC1234",
            taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
            items = Seq(
              FinancialItem(
                dueDate = Some(LocalDate.of(2018, 8, 13)),
                clearingDate = Some(LocalDate.of(2018, 8, 13)),
                metadata = FinancialItemMetadata(
                  subItem = Some("001"),
                  amount = Some(10000),
                  clearingReason = Some("01"),
                  outgoingPaymentMethod = Some("outgoing payment"),
                  paymentLock = Some("paymentLock"),
                  clearingLock = Some("clearingLock"),
                  interestLock = Some("interestLock"),
                  dunningLock = Some("dunningLock"),
                  returnFlag = Some(true),
                  paymentReference = Some("Ab12453535"),
                  paymentAmount = Some(10000),
                  paymentMethod = Some("Payment"),
                  paymentLot = Some("081203010024"),
                  paymentLotItem = Some("000001"),
                  clearingSAPDocument = Some("3350000253"),
                  codingInitiationDate = Some(LocalDate.of(2021, 1, 11)),
                  statisticalDocument = Some("S"),
                  DDCollectionInProgress = Some(true),
                  returnReason = Some("ABCA"),
                  promisetoPay = Some("Y")
                )
              )
            ),
            originalAmount = Some(123.45),
            outstandingAmount = Some(123.45),
            mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
            chargeReference = Some("1234567890"),
            metadata = FinancialDetailsMetadata(
              taxYear = "2022",
              chargeType = Some("1234"),
              mainType = Some("1234"),
              periodKey = Some("123"),
              periodKeyDescription = Some("foobar"),
              businessPartner = Some("123"),
              contractAccountCategory = Some("1"),
              contractAccount = Some("1"),
              contractObjectType = Some("1"),
              contractObject = Some("1"),
              sapDocumentNumber = Some("1"),
              sapDocumentNumberItem = Some("1"),
              subTransaction = Some("1"),
              clearedAmount = Some(123.45),
              accruedInterest = Some(123.45)
            )
          )
        )
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567891",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.OfficersAssessmentFirstLPP),
                outstandingAmount = Some(123.45),
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              )
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
                outstandingAmount = Some(123.45),
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              )
            )
          )
        )
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithDifferentPrincipals, financialDetails)
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }
  }
}
