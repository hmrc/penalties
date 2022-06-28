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

import models.v3.ChargeTypeEnum
import models.v3.getFinancialDetails.GetFinancialDetails
import models.v3.getPenaltyDetails.GetPenaltyDetails
import models.v3.getPenaltyDetails.latePayment.{LPPDetailsMetadata, LPPPenaltyCategoryEnum, LatePaymentPenalty}

import javax.inject.Inject

class PenaltiesFrontendService @Inject()() {
  def combineAPIData(penaltyDetails: GetPenaltyDetails, financialDetails: GetFinancialDetails): GetPenaltyDetails = {
    val newLPPDetails = penaltyDetails.latePaymentPenalty.flatMap(
      _.details.map(_.map(
        oldLPPDetails => {
          val isAdditional = oldLPPDetails.penaltyCategory.equals(LPPPenaltyCategoryEnum.SecondPenalty)
          val firstAndMaybeSecondPenalty = financialDetails.financialDetails.filter(_.chargeReference.exists(_.equals(oldLPPDetails.principalChargeReference)))
          val penaltyToCopy = firstAndMaybeSecondPenalty.find(lpp => {
              if(isAdditional) ChargeTypeEnum.secondCharges.contains(lpp.mainTransaction.get)
              else ChargeTypeEnum.firstCharges.contains(lpp.mainTransaction.get)
            }
          )
          oldLPPDetails.copy(
            metadata = LPPDetailsMetadata(
              mainTransaction = Some(penaltyToCopy.get.mainTransaction.get),
              outstandingAmount = Some(penaltyToCopy.get.outstandingAmount.get)
            )
          )
        }
      ))
    )
    penaltyDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(newLPPDetails)))
  }
}
