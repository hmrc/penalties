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

import models.getFinancialDetails.MainTransactionEnum
import models.hipPenaltyDetails.{PenaltyDetails => HIPPenaltyDetails}
import models.getPenaltyDetails.GetPenaltyDetails

object PenaltyDetailsConverter {

  def convertHIPToGetPenaltyDetails(hipPenaltyDetails: HIPPenaltyDetails): GetPenaltyDetails = {
    GetPenaltyDetails(
      totalisations = convertTotalisations(hipPenaltyDetails.totalisations),
      lateSubmissionPenalty = convertLateSubmissionPenalty(hipPenaltyDetails.lateSubmissionPenalty),
      latePaymentPenalty = convertLatePaymentPenalty(hipPenaltyDetails.latePaymentPenalty),
      breathingSpace = convertBreathingSpace(hipPenaltyDetails.breathingSpace)
    )
  }

  private def convertTotalisations(hipTotalisations: Option[models.hipPenaltyDetails.Totalisations]): Option[models.getPenaltyDetails.Totalisations] = {
    hipTotalisations.map { hipTot =>
      models.getPenaltyDetails.Totalisations(
        LSPTotalValue = hipTot.lspTotalValue,
        penalisedPrincipalTotal = hipTot.penalisedPrincipalTotal,
        LPPPostedTotal = hipTot.lppPostedTotal,
        LPPEstimatedTotal = hipTot.lppEstimatedTotal,
        totalAccountOverdue = hipTot.totalAccountOverdue,
        totalAccountPostedInterest = hipTot.totalAccountPostedInterest,
        totalAccountAccruingInterest = hipTot.totalAccountAccruingInterest
      )
    }
  }

  private def convertLateSubmissionPenalty(hipLSP: Option[models.hipPenaltyDetails.lateSubmission.LateSubmissionPenalty]): Option[models.getPenaltyDetails.lateSubmission.LateSubmissionPenalty] = {
    hipLSP.map { hipLSPContainer =>
      val regularDetails = convertLSPDetails(hipLSPContainer.details)
      val correctedSummary = calculateCorrectedLSPSummary(hipLSPContainer.summary, regularDetails)
      models.getPenaltyDetails.lateSubmission.LateSubmissionPenalty(correctedSummary, regularDetails)
    }
  }

  private def convertLSPDetails(hipDetails: Seq[models.hipPenaltyDetails.lateSubmission.LSPDetails]): Seq[models.getPenaltyDetails.lateSubmission.LSPDetails] = {
    hipDetails.map { hipDetail =>
      val hasUpheldAppeal = hasUpheldAppealInDetails(hipDetail)
      val correctedPenaltyStatus = if (hasUpheldAppeal) {
        models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive
      } else {
        convertPenaltyStatus(hipDetail.penaltyStatus)
      }

      models.getPenaltyDetails.lateSubmission.LSPDetails(
        penaltyNumber = hipDetail.penaltyNumber,
        penaltyOrder = hipDetail.penaltyOrder,
        penaltyCategory = hipDetail.penaltyCategory.map(convertPenaltyCategory),
        penaltyStatus = correctedPenaltyStatus,
        penaltyCreationDate = hipDetail.penaltyCreationDate,
        penaltyExpiryDate = hipDetail.penaltyExpiryDate,
        communicationsDate = hipDetail.communicationsDate,
        FAPIndicator = hipDetail.fapIndicator,
        lateSubmissions = hipDetail.lateSubmissions.map(_.map(convertLateSubmission)),
        expiryReason = hipDetail.expiryReason.map(convertExpiryReason),
        appealInformation = hipDetail.appealInformation.map(_.map(convertAppealInformation)),
        chargeDueDate = hipDetail.chargeDueDate,
        chargeOutstandingAmount = hipDetail.chargeOutstandingAmount,
        chargeAmount = hipDetail.chargeAmount,
        triggeringProcess = hipDetail.triggeringProcess,
        chargeReference = hipDetail.chargeReference
      )
    }
  }

  private def convertLateSubmission(hipSub: models.hipPenaltyDetails.lateSubmission.LateSubmission): models.getPenaltyDetails.lateSubmission.LateSubmission = {
    models.getPenaltyDetails.lateSubmission.LateSubmission(
      lateSubmissionID = hipSub.lateSubmissionID,
      incomeSource = hipSub.incomeSource,
      taxPeriod = hipSub.taxPeriod,
      taxPeriodStartDate = hipSub.taxPeriodStartDate,
      taxPeriodEndDate = hipSub.taxPeriodEndDate,
      taxPeriodDueDate = hipSub.taxPeriodDueDate,
      returnReceiptDate = hipSub.returnReceiptDate,
      taxReturnStatus = hipSub.taxReturnStatus.map(convertTaxReturnStatus)
    )
  }

  private def convertLatePaymentPenalty(hipLPP: Option[models.hipPenaltyDetails.latePayment.LatePaymentPenalty]): Option[models.getPenaltyDetails.latePayment.LatePaymentPenalty] = {
    hipLPP.map { hipLPPContainer =>
      val regularLPPDetails = hipLPPContainer.lppDetails.map(_.map(convertLPPDetails))
      models.getPenaltyDetails.latePayment.LatePaymentPenalty(
        details = regularLPPDetails,
        ManualLPPIndicator = Some(hipLPPContainer.manualLPPIndicator)
      )
    }
  }

  private def convertLPPDetails(hipLPP: models.hipPenaltyDetails.latePayment.LPPDetails): models.getPenaltyDetails.latePayment.LPPDetails = {
    models.getPenaltyDetails.latePayment.LPPDetails(
      principalChargeReference = hipLPP.principalChargeReference,
      penaltyCategory = convertLPPPenaltyCategory(hipLPP.penaltyCategory),
      penaltyStatus = convertLPPPenaltyStatus(hipLPP.penaltyStatus),
      penaltyAmountAccruing = hipLPP.penaltyAmountAccruing,
      penaltyAmountPosted = hipLPP.penaltyAmountPosted,
      penaltyAmountPaid = hipLPP.penaltyAmountPaid,
      penaltyAmountOutstanding = hipLPP.penaltyAmountOutstanding,
      LPP1LRCalculationAmount = hipLPP.lpp1LRCalculationAmt,
      LPP1LRDays = hipLPP.lpp1LRDays,
      LPP1LRPercentage = hipLPP.lpp1LRPercentage,
      LPP1HRCalculationAmount = hipLPP.lpp1HRCalculationAmt,
      LPP1HRDays = hipLPP.lpp1HRDays,
      LPP1HRPercentage = hipLPP.lpp1HRPercentage,
      LPP2Days = hipLPP.lpp2Days,
      LPP2Percentage = hipLPP.lpp2Percentage,
      penaltyChargeCreationDate = hipLPP.penaltyChargeCreationDate,
      communicationsDate = hipLPP.communicationsDate,
      penaltyChargeReference = hipLPP.penaltyChargeReference,
      principalChargeMainTransaction = MainTransactionEnum.WithValue(hipLPP.principalChargeMainTr),
      principalChargeBillingFrom = hipLPP.principalChargeBillingFrom,
      principalChargeBillingTo = hipLPP.principalChargeBillingTo,
      principalChargeDueDate = hipLPP.principalChargeDueDate,
      principalChargeLatestClearing = hipLPP.principalChargeLatestClearing,
      vatOutstandingAmount = None,
      penaltyChargeDueDate = hipLPP.penaltyChargeDueDate,
      appealInformation = hipLPP.appealInformation.map(_.map(convertAppealInformation)),
      metadata = models.getPenaltyDetails.latePayment.LPPDetailsMetadata(
        principalChargeSubTransaction = hipLPP.principalChargeSubTr,
        principalChargeDocNumber = hipLPP.principalChargeDocNumber,
        timeToPay = hipLPP.timeToPay.map(_.map(convertTimeToPay))
      )
    )
  }

  private def convertBreathingSpace(hipBS: Option[Seq[models.hipPenaltyDetails.breathingSpace.BreathingSpace]]): Option[Seq[models.getPenaltyDetails.breathingSpace.BreathingSpace]] = {
    hipBS.map(_.map { hipBSItem =>
      models.getPenaltyDetails.breathingSpace.BreathingSpace(
        BSStartDate = hipBSItem.bsStartDate,
        BSEndDate = hipBSItem.bsEndDate
      )
    })
  }

  // Helper conversion methods
  private def convertPenaltyStatus(hipStatus: models.hipPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Value): models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Value = {
    hipStatus match {
      case models.hipPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Active => models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Active
      case models.hipPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive => models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive
    }
  }

  private def convertPenaltyCategory(hipCategory: models.hipPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Value): models.getPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Value = {
    hipCategory match {
      case models.hipPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Point => models.getPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Point
      case models.hipPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Threshold => models.getPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Threshold
      case models.hipPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Charge => models.getPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Charge
    }
  }

  private def convertTaxReturnStatus(hipStatus: models.hipPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Value): models.getPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Value = {
    hipStatus match {
      case models.hipPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Fulfilled => models.getPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Fulfilled
      case models.hipPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Open => models.getPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Open
      case models.hipPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Reversed => models.getPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Reversed
    }
  }

  private def convertExpiryReason(hipReason: models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Value): models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Value = {
    hipReason match {
      case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Appeal => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Appeal
      case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.SubmissionOnTime => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.SubmissionOnTime
      case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Compliance => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Compliance
      case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.NaturalExpiration => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.NaturalExpiration
      case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Adjustment => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Adjustment
      case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Reversal => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Reversal
      case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Manual => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Manual
      case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Reset => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Reset
      case _ => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.NaturalExpiration
    }
  }

  private def convertAppealInformation(hipAppeal: models.hipPenaltyDetails.appealInfo.AppealInformationType): models.getPenaltyDetails.appealInfo.AppealInformationType = {
    models.getPenaltyDetails.appealInfo.AppealInformationType(
      appealStatus = hipAppeal.appealStatus.map(convertAppealStatus),
      appealLevel = hipAppeal.appealLevel.map(convertAppealLevel),
      appealDescription = hipAppeal.appealDescription
    )
  }

  private def convertAppealStatus(hipStatus: models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Value): models.getPenaltyDetails.appealInfo.AppealStatusEnum.Value = {
    hipStatus match {
      case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Under_Appeal =>
          models.getPenaltyDetails.appealInfo.AppealStatusEnum.Under_Appeal
      case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Upheld => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Upheld
      case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Rejected => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Rejected
      case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable
      case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedChargeAlreadyReversed => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldChargeAlreadyReversed
      case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldPointAlreadyRemoved => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealCancelledPointAlreadyRemoved
      case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldChargeAlreadyReversed => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealCancelledChargeAlreadyReversed
      case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedPointAlreadyRemoved => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldPointAlreadyRemoved
      case _ => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable
    }
  }

  private def convertAppealLevel(hipLevel: models.hipPenaltyDetails.appealInfo.AppealLevelEnum.Value): models.getPenaltyDetails.appealInfo.AppealLevelEnum.Value = {
    hipLevel match {
      case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.HMRC => models.getPenaltyDetails.appealInfo.AppealLevelEnum.HMRC
      case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.TribunalOrSecond => models.getPenaltyDetails.appealInfo.AppealLevelEnum.TribunalOrSecond
      case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.Tribunal => models.getPenaltyDetails.appealInfo.AppealLevelEnum.Tribunal
      case _ => models.getPenaltyDetails.appealInfo.AppealLevelEnum.HMRC
    }
  }

  private def convertLPPPenaltyCategory(hipCategory: models.hipPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.Value): models.getPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.Value = {
    hipCategory match {
      case models.hipPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.FirstPenalty => models.getPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.FirstPenalty
      case models.hipPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.SecondPenalty => models.getPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.SecondPenalty
      case models.hipPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.ManualLPP => models.getPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.ManualLPP
    }
  }

  private def convertLPPPenaltyStatus(hipStatus: Option[models.hipPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Value]): models.getPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Value = {
    hipStatus.getOrElse(models.hipPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Posted) match {
      case models.hipPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Accruing => models.getPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Accruing
      case models.hipPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Posted => models.getPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Posted
    }
  }

  private def convertTimeToPay(hipTTP: models.hipPenaltyDetails.latePayment.TimeToPay): models.getPenaltyDetails.latePayment.TimeToPay = {
    models.getPenaltyDetails.latePayment.TimeToPay(
      TTPStartDate = hipTTP.ttpStartDate,
      TTPEndDate = hipTTP.ttpEndDate
    )
  }

  private def hasUpheldAppealInDetails(hipDetail: models.hipPenaltyDetails.lateSubmission.LSPDetails): Boolean = {
    hipDetail.appealInformation.exists(_.exists(appeal => 
      appeal.appealStatus.exists(status => 
        status == models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Upheld ||
        status == models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldPointAlreadyRemoved ||
        status == models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldChargeAlreadyReversed
      )
    ))
  }

  private def calculateCorrectedLSPSummary(
    hipSummary: models.hipPenaltyDetails.lateSubmission.LSPSummary,
    regularDetails: Seq[models.getPenaltyDetails.lateSubmission.LSPDetails]
  ): models.getPenaltyDetails.lateSubmission.LSPSummary = {
    val actualActivePenaltyPoints = if (regularDetails.nonEmpty) {
      regularDetails.count(_.penaltyStatus == models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Active)
    } else {
      hipSummary.activePenaltyPoints
    }
    
    val actualInactivePenaltyPoints = if (regularDetails.nonEmpty) {
      regularDetails.count(_.penaltyStatus == models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive)
    } else {
      hipSummary.inactivePenaltyPoints
    }
    
    models.getPenaltyDetails.lateSubmission.LSPSummary(
      activePenaltyPoints = actualActivePenaltyPoints,
      inactivePenaltyPoints = actualInactivePenaltyPoints,
      regimeThreshold = hipSummary.regimeThreshold,
      penaltyChargeAmount = hipSummary.penaltyChargeAmount,
      PoCAchievementDate = hipSummary.pocAchievementDate
    )
  }
} 