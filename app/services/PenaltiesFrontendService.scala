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

import models.getFinancialDetails.{FinancialDetails, MainTransactionEnum}
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPDetailsMetadata, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}

import javax.inject.Inject

class PenaltiesFrontendService @Inject()() {
  def combineAPIData(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): GetPenaltyDetails = {
    val totalisationsCombined = combineTotalisations(penaltyDetails, financialDetails)
    val allLPPData = combineLPPData(penaltyDetails, financialDetails)
    if(allLPPData.isDefined) {
      totalisationsCombined.copy(latePaymentPenalty = Some(LatePaymentPenalty(allLPPData)))
    } else {
      totalisationsCombined
    }
  }

  private def combineLPPData(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): Option[Seq[LPPDetails]] = {
    penaltyDetails.latePaymentPenalty.flatMap(
      _.details.map(_.map(
        oldLPPDetails => {
          if(oldLPPDetails.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing)) {
            oldLPPDetails.copy(
              penaltyAmountOutstanding = Some(oldLPPDetails.penaltyAmountAccruing),
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(oldLPPDetails.principalChargeMainTransaction),
                timeToPay = None
              )
            )
          } else {
            val isAdditional = oldLPPDetails.penaltyCategory.equals(LPPPenaltyCategoryEnum.SecondPenalty)
            val firstAndMaybeSecondPenalty = financialDetails.documentDetails.get.filter(_.chargeReferenceNumber.exists(_.equals(oldLPPDetails.penaltyChargeReference.get)))
            val penaltyToCopy = firstAndMaybeSecondPenalty.find(lpp => {
              if (isAdditional) MainTransactionEnum.secondCharges.contains(lpp.lineItemDetails.get.head.mainTransaction.get)
              else MainTransactionEnum.firstCharges.contains(lpp.lineItemDetails.get.head.mainTransaction.get)
            }
            )
            oldLPPDetails.copy(
              metadata = LPPDetailsMetadata(
                mainTransaction = penaltyToCopy.get.lineItemDetails.get.head.mainTransaction,
                outstandingAmount = penaltyToCopy.get.documentOutstandingAmount,
                timeToPay = oldLPPDetails.metadata.timeToPay
              )
            )
          }
        }
      ))
    )
  }

  private def combineTotalisations(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): GetPenaltyDetails = {
    (financialDetails.totalisation.isDefined, penaltyDetails.totalisations.isDefined) match {
      //If there is totalisations already, add to it
      case (_, true) => {
        val newTotalisations: Option[Totalisations] = penaltyDetails.totalisations.map(
          oldTotalisations => {
            oldTotalisations.copy(
              totalAccountOverdue = financialDetails.totalisation.flatMap(_.regimeTotalisations.flatMap(_.totalAccountOverdue)),
              totalAccountPostedInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountPostedInterest)),
              totalAccountAccruingInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountAccruingInterest))
            )
          }
        )
        penaltyDetails.copy(totalisations = newTotalisations)
      }
      case (true, false) => {
        //If there is no totalisations already, create a new object
        val totalisations: Totalisations = new Totalisations(
          totalAccountOverdue = financialDetails.totalisation.flatMap(_.regimeTotalisations.flatMap(_.totalAccountOverdue)),
          totalAccountPostedInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountPostedInterest)),
          totalAccountAccruingInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountAccruingInterest)),
          LSPTotalValue = None,
          penalisedPrincipalTotal = None,
          LPPPostedTotal = None,
          LPPEstimatedTotal = None
        )
        penaltyDetails.copy(totalisations = Some(totalisations))
      }
      case _ => {
        //No totalisations at all, don't do any processing on totalisation field
        penaltyDetails
      }
    }
  }
}
