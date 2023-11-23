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

import models.getFinancialDetails.MainTransactionEnum
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment._

import java.time.LocalDate

trait LPPDetailsBase {
  val lpp1PrincipalChargeDueToday: LPPDetails = LPPDetails(
    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
    principalChargeReference = "1234567890",
    penaltyChargeReference = Some("123456789"),
    penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
    penaltyStatus = LPPPenaltyStatusEnum.Accruing,
    appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
    principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
    principalChargeBillingTo = LocalDate.of(2022, 10, 30),
    principalChargeDueDate = LocalDate.now(),
    communicationsDate = Some(LocalDate.of(2022, 10, 30)),
    penaltyAmountOutstanding = None,
    penaltyAmountPaid = None,
    penaltyAmountPosted = 0,
    LPP1LRDays = Some("15"),
    LPP1HRDays = Some("31"),
    LPP2Days = None,
    LPP1HRCalculationAmount = Some(99.99),
    LPP1LRCalculationAmount = Some(99.99),
    LPP2Percentage = None,
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
    penaltyAmountAccruing = BigDecimal(144.21),
    principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
    vatOutstandingAmount = Some(BigDecimal(123.45))
  )

  val lpp2: LPPDetails = LPPDetails(
    penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
    principalChargeReference = "1234567890",
    penaltyChargeReference = None,
    penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
    penaltyStatus = LPPPenaltyStatusEnum.Accruing,
    appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
    principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
    principalChargeBillingTo = LocalDate.of(2022, 10, 30),
    principalChargeDueDate = LocalDate.now(),
    communicationsDate = Some(LocalDate.of(2022, 10, 30)),
    penaltyAmountOutstanding = None,
    penaltyAmountPaid = None,
    penaltyAmountPosted = 0,
    LPP1LRDays = None,
    LPP1HRDays = None,
    LPP2Days = Some("31"),
    LPP1HRCalculationAmount = None,
    LPP1LRCalculationAmount = None,
    LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
    LPP1LRPercentage = None,
    LPP1HRPercentage = None,
    penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
    principalChargeLatestClearing = None,
    metadata = LPPDetailsMetadata(
      timeToPay = Some(Seq(TimeToPay(
        TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
        TTPEndDate = Some(LocalDate.of(2022, 12, 31))
      )))
    ),
    penaltyAmountAccruing = BigDecimal(144.21),
    principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
    vatOutstandingAmount = Some(BigDecimal(123.45))
  )

  val lpp1PrincipalChargeDueYesterday: LPPDetails = lpp1PrincipalChargeDueToday.copy(principalChargeDueDate = LocalDate.now().minusDays(1))
  val lpp1PrincipalChargeDueYesterdayPosted: LPPDetails = lpp1PrincipalChargeDueToday.copy(principalChargeDueDate = LocalDate.now().minusDays(1), penaltyStatus = LPPPenaltyStatusEnum.Posted)
  val lpp1PrincipalChargeDueTomorrow: LPPDetails = lpp1PrincipalChargeDueToday.copy(principalChargeDueDate = LocalDate.now().plusDays(1))

  def lpp2WithAppealStatus(status: String): LPPDetails = lpp2.copy(appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.withName(status)), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))))

  def lpp1PrincipalChargeDueTodayAppealStatus(status: String): LPPDetails = lpp1PrincipalChargeDueToday.copy(appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.withName(status)), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))))
}
