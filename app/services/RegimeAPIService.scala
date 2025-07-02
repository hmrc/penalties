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

import javax.inject.{Inject, Singleton}
import models.getFinancialDetails.FinancialDetails
import models.getFinancialDetails.MainTransactionEnum.ManualLPP
import models.penaltyDetails.PenaltyDetails
import models.penaltyDetails.latePayment.{LPPDetails, LPPPenaltyStatusEnum}

@Singleton
class RegimeAPIService @Inject()() {
  def getNumberOfEstimatedPenalties(penaltyDetails: PenaltyDetails): Int = {
    penaltyDetails.latePaymentPenalty.flatMap(_.lppDetails.map(_.count(_.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing)))).getOrElse(0)
  }

  def findEstimatedPenaltiesAmount(penaltyDetails: PenaltyDetails): BigDecimal = {
    penaltyDetails.latePaymentPenalty.flatMap(
      _.lppDetails.map(
        _.filter(_.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing)).map(_.penaltyAmountAccruing).sum
      )
      ).getOrElse(0)
  }

  def checkIfHasAnyPenaltyData(penaltyDetails: PenaltyDetails): Boolean ={
    penaltyDetails.latePaymentPenalty.exists(_.lppDetails.exists(_.nonEmpty)) || penaltyDetails.lateSubmissionPenalty.exists(_.details.nonEmpty)
  }

  def getNumberOfCrystallisedPenalties(penaltyDetails: PenaltyDetails, financialDetails: Option[FinancialDetails]): Int = {
    val numOfDueLSPs: Int = penaltyDetails.lateSubmissionPenalty.map(
      _.details.map(
        penalty => penalty.chargeOutstandingAmount.getOrElse(BigDecimal(0)))).map(_.count(_ > BigDecimal(0)))
      .getOrElse(0)
    val lppDetails: Seq[LPPDetails] = penaltyDetails.latePaymentPenalty.flatMap(_.lppDetails).getOrElse(Seq.empty)
    val postedLPPs = lppDetails.filterNot(penalty => penalty.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing))
    val outstandingPostedLPPs = postedLPPs.filter(_.penaltyAmountOutstanding.getOrElse(BigDecimal(0)) > BigDecimal(0))
    val numOfDueLPPs = outstandingPostedLPPs.size
    val numOfManualLPPs: Int = if(financialDetails.isDefined) countManualLPPs(financialDetails.get) else 0
    numOfDueLSPs + numOfDueLPPs + numOfManualLPPs
  }

  def getCrystallisedPenaltyTotal(penaltyDetails: PenaltyDetails, financialDetails: Option[FinancialDetails]): BigDecimal = {
    val crystallisedLSPAmountDue: BigDecimal = penaltyDetails.lateSubmissionPenalty.map(
      _.details.map(
        _.chargeOutstandingAmount.getOrElse(BigDecimal(0))).sum
    ).getOrElse(BigDecimal(0))
    val lppDetails: Seq[LPPDetails] = penaltyDetails.latePaymentPenalty.flatMap(_.lppDetails).getOrElse(Seq.empty)
    val postedLPPs = lppDetails.filterNot(penalty => penalty.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing))
    val crystallisedLPPAmountDue = postedLPPs.map(_.penaltyAmountOutstanding.getOrElse(BigDecimal(0))).sum
    val manualLPPDue: BigDecimal = if(financialDetails.isDefined) manualLPPTotals(financialDetails.get) else BigDecimal(0)
    crystallisedLSPAmountDue + crystallisedLPPAmountDue + manualLPPDue
  }

  private def countManualLPPs(financialDetails: FinancialDetails): Int = {
    financialDetails.documentDetails.map(_.count(_.lineItemDetails.exists(_.exists(_.mainTransaction.contains(ManualLPP))))).getOrElse(0)
  }

  private def manualLPPTotals(financialDetails: FinancialDetails): BigDecimal = {
    val manualLPPs = financialDetails.documentDetails.map(_.filter(_.lineItemDetails.exists(_.exists(_.mainTransaction.contains(ManualLPP)))))
    manualLPPs.get.map(_.documentOutstandingAmount.getOrElse(BigDecimal(0))).sum
  }
}
