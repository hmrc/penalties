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

package services

import config.featureSwitches.FeatureSwitching
import connectors.getPenaltyDetails.{HIPPenaltyDetailsConnector, RegimePenaltyDetailsConnector}
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser._
import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser._
import models.AgnosticEnrolmentKey
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import config.featureSwitches.CallAPI1812HIP

case class LoggingContext(callingClass: String, function: String, enrolmentKey: String)

class PenaltyDetailsService @Inject() (getPenaltyDetailsConnector: RegimePenaltyDetailsConnector,
                                       hipPenaltyDetailsConnector: HIPPenaltyDetailsConnector,
                                       filterService: RegimeFilterService)(implicit ec: ExecutionContext, val config: Configuration)
    extends FeatureSwitching {

  def getPenaltyDetails(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    if (isEnabled(CallAPI1812HIP)) {
      val startOfLogMsg: String = s"[PenaltyDetailsService][getPenaltyDetails][${enrolmentKey.regime.value}]"
      hipPenaltyDetailsConnector.getPenaltyDetails(enrolmentKey).map { hipResponse =>
        hipResponse.fold(
          failure => convertHIPFailureToRegular(failure, enrolmentKey),
          success => {
            implicit val loggingContext: LoggingContext = LoggingContext(
              callingClass = "PenaltiesDetailsService",
              function = "handleConnectorResponse", 
              enrolmentKey = enrolmentKey.toString
            )
            val convertedPenaltyDetails = convertHIPToGetPenaltyDetails(success.asInstanceOf[HIPPenaltyDetailsSuccessResponse].penaltyDetails)
            val penaltiesWithAppealStatusFiltered = filterService.filterPenaltiesWith9xAppealStatus(convertedPenaltyDetails)
            val filteredPenaltyDetails = filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesWithAppealStatusFiltered)
            Right(GetPenaltyDetailsSuccessResponse(filteredPenaltyDetails))
          }
        )
      }
    } else {
      val startOfLogMsg: String = s"[PenaltyDetailsService][getPenaltyDetails][${enrolmentKey.regime.value}]"
      getPenaltyDetailsConnector.getPenaltyDetails(enrolmentKey).map {
        handleConnectorResponse(_)(startOfLogMsg, enrolmentKey)
      }
    }
  }

  private def convertHIPFailureToRegular(failure: HIPPenaltyDetailsFailure, enrolmentKey: AgnosticEnrolmentKey): GetPenaltyDetailsResponse = {
    val startOfLogMsg: String = s"[PenaltyDetailsService][getPenaltyDetails][${enrolmentKey.regime.value}]"
    failure match {
      case HIPPenaltyDetailsNoContent => 
        logger.info(s"$startOfLogMsg - Got a 404 response and no data was found for GetPenaltyDetails call")
        Left(GetPenaltyDetailsNoContent)
      case HIPPenaltyDetailsMalformed => 
        logger.info(s"$startOfLogMsg - Failed to parse HTTP response into HIP model for $enrolmentKey")
        Left(GetPenaltyDetailsMalformed)
      case HIPPenaltyDetailsFailureResponse(status) => 
        logger.error(s"$startOfLogMsg - Unknown status returned from HIP connector for $enrolmentKey")
        Left(GetPenaltyDetailsFailureResponse(status))
    }
  }

  private def convertHIPToGetPenaltyDetails(hipPenaltyDetails: models.hipPenaltyDetails.PenaltyDetails): models.getPenaltyDetails.GetPenaltyDetails = {
    val regularTotalisations = hipPenaltyDetails.totalisations.map { hipTot =>
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
    
    val regularLSP = hipPenaltyDetails.lateSubmissionPenalty.map { hipLSP =>
      val regularDetails = hipLSP.details.map { hipDetail =>
        val hasUpheldAppeal = hipDetail.appealInformation.exists(_.exists(appeal => 
          appeal.appealStatus.exists(status => 
            status == models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Upheld ||
            status == models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldPointAlreadyRemoved ||
            status == models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldChargeAlreadyReversed
          )
        ))
        
        val correctedPenaltyStatus = if (hasUpheldAppeal) {
          models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive
        } else {
          hipDetail.penaltyStatus match {
            case models.hipPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Active => models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Active
            case models.hipPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive => models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive
          }
        }
        
        models.getPenaltyDetails.lateSubmission.LSPDetails(
          penaltyNumber = hipDetail.penaltyNumber,
          penaltyOrder = hipDetail.penaltyOrder,
          penaltyCategory = hipDetail.penaltyCategory.map {
            case models.hipPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Point => models.getPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Point
            case models.hipPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Threshold => models.getPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Threshold
            case models.hipPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Charge => models.getPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Charge
          },
          penaltyStatus = correctedPenaltyStatus,
          penaltyCreationDate = hipDetail.penaltyCreationDate,
          penaltyExpiryDate = hipDetail.penaltyExpiryDate,
          communicationsDate = hipDetail.communicationsDate,
          FAPIndicator = hipDetail.fapIndicator,
          lateSubmissions = hipDetail.lateSubmissions.map(_.map { hipSub =>
            models.getPenaltyDetails.lateSubmission.LateSubmission(
              lateSubmissionID = hipSub.lateSubmissionID,
              taxPeriod = hipSub.taxPeriod,
              taxPeriodStartDate = hipSub.taxPeriodStartDate,
              taxPeriodEndDate = hipSub.taxPeriodEndDate,
              taxPeriodDueDate = hipSub.taxPeriodDueDate,
              returnReceiptDate = hipSub.returnReceiptDate,
              taxReturnStatus = hipSub.taxReturnStatus.map {
                case models.hipPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Fulfilled => models.getPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Fulfilled
                case models.hipPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Open => models.getPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Open
              }
            )
          }),
          expiryReason = hipDetail.expiryReason.map {
            case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Appeal => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Appeal
            case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.SubmissionOnTime => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.SubmissionOnTime
            case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Compliance => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Compliance
            case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.NaturalExpiration => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.NaturalExpiration
            case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Adjustment => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Adjustment
            case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Reversal => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Reversal
            case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Manual => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Manual
            case models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Reset => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.Reset
            case _ => models.getPenaltyDetails.lateSubmission.ExpiryReasonEnum.NaturalExpiration 
          },
          appealInformation = hipDetail.appealInformation.map(_.map { hipAppeal =>
            models.getPenaltyDetails.appealInfo.AppealInformationType(
              appealStatus = hipAppeal.appealStatus.map {
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Under_Appeal => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Under_Appeal
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Upheld => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Upheld
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Rejected => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Rejected
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedChargeAlreadyReversed => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedChargeAlreadyReversed
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldPointAlreadyRemoved => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldPointAlreadyRemoved
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldChargeAlreadyReversed => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldChargeAlreadyReversed
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedPointAlreadyRemoved => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedPointAlreadyRemoved
                case _ => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable
              },
              appealLevel = hipAppeal.appealLevel.map {
                case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.HMRC => models.getPenaltyDetails.appealInfo.AppealLevelEnum.HMRC
                case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.TribunalOrSecond => models.getPenaltyDetails.appealInfo.AppealLevelEnum.TribunalOrSecond
                case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.Tribunal => models.getPenaltyDetails.appealInfo.AppealLevelEnum.Tribunal
                case _ => models.getPenaltyDetails.appealInfo.AppealLevelEnum.HMRC
              },
              appealDescription = hipAppeal.appealDescription
            )
          }),
          chargeDueDate = hipDetail.chargeDueDate,
          chargeOutstandingAmount = hipDetail.chargeOutstandingAmount,
          chargeAmount = hipDetail.chargeAmount,
          triggeringProcess = hipDetail.triggeringProcess,
          chargeReference = hipDetail.chargeReference
        )
      }
      
      // Use the counts from regularDetails if they exist, otherwise use the HIP summary values
      val actualActivePenaltyPoints = if (regularDetails.nonEmpty) {
        regularDetails.count(detail => 
          detail.penaltyStatus == models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Active
        )
      } else {
        hipLSP.summary.activePenaltyPoints
      }
      
      val actualInactivePenaltyPoints = if (regularDetails.nonEmpty) {
        regularDetails.count(detail => 
          detail.penaltyStatus == models.getPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Inactive
        )
      } else {
        hipLSP.summary.inactivePenaltyPoints
      }
      
      val correctedSummary = models.getPenaltyDetails.lateSubmission.LSPSummary(
        activePenaltyPoints = actualActivePenaltyPoints,
        inactivePenaltyPoints = actualInactivePenaltyPoints,
        regimeThreshold = hipLSP.summary.regimeThreshold,
        penaltyChargeAmount = hipLSP.summary.penaltyChargeAmount,
        PoCAchievementDate = hipLSP.summary.pocAchievementDate
      )
      
      models.getPenaltyDetails.lateSubmission.LateSubmissionPenalty(correctedSummary, regularDetails)
    }
    
    val regularLPP = hipPenaltyDetails.latePaymentPenalty.map { hipLPPContainer =>
      val regularLPPDetails = hipLPPContainer.lppDetails.map(_.map { hipLPP =>
        models.getPenaltyDetails.latePayment.LPPDetails(
          principalChargeReference = hipLPP.principalChargeReference,
          penaltyCategory = hipLPP.penaltyCategory match {
            case models.hipPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.FirstPenalty => models.getPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.FirstPenalty
            case models.hipPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.SecondPenalty => models.getPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.SecondPenalty
            case models.hipPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.ManualLPP => models.getPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.ManualLPP
          },
          penaltyStatus = hipLPP.penaltyStatus.getOrElse(models.hipPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Posted) match {
            case models.hipPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Accruing => models.getPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Accruing
            case models.hipPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Posted => models.getPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Posted
          },
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
          principalChargeMainTransaction = hipLPP.principalChargeMainTr match {
            case models.hipPenaltyDetails.MainTransactionEnum.VATReturnCharge => models.getFinancialDetails.MainTransactionEnum.VATReturnCharge
            case models.hipPenaltyDetails.MainTransactionEnum.VATReturnFirstLPP => models.getFinancialDetails.MainTransactionEnum.VATReturnFirstLPP
            case models.hipPenaltyDetails.MainTransactionEnum.VATReturnSecondLPP => models.getFinancialDetails.MainTransactionEnum.VATReturnSecondLPP
            case models.hipPenaltyDetails.MainTransactionEnum.CentralAssessment => models.getFinancialDetails.MainTransactionEnum.CentralAssessment
            case models.hipPenaltyDetails.MainTransactionEnum.CentralAssessmentFirstLPP => models.getFinancialDetails.MainTransactionEnum.CentralAssessmentFirstLPP
            case models.hipPenaltyDetails.MainTransactionEnum.CentralAssessmentSecondLPP => models.getFinancialDetails.MainTransactionEnum.CentralAssessmentSecondLPP
            case models.hipPenaltyDetails.MainTransactionEnum.OfficersAssessment => models.getFinancialDetails.MainTransactionEnum.OfficersAssessment
            case models.hipPenaltyDetails.MainTransactionEnum.OfficersAssessmentFirstLPP => models.getFinancialDetails.MainTransactionEnum.OfficersAssessmentFirstLPP
            case models.hipPenaltyDetails.MainTransactionEnum.OfficersAssessmentSecondLPP => models.getFinancialDetails.MainTransactionEnum.OfficersAssessmentSecondLPP
            case models.hipPenaltyDetails.MainTransactionEnum.ErrorCorrection => models.getFinancialDetails.MainTransactionEnum.ErrorCorrection
            case models.hipPenaltyDetails.MainTransactionEnum.ErrorCorrectionFirstLPP => models.getFinancialDetails.MainTransactionEnum.ErrorCorrectionFirstLPP
            case models.hipPenaltyDetails.MainTransactionEnum.ErrorCorrectionSecondLPP => models.getFinancialDetails.MainTransactionEnum.ErrorCorrectionSecondLPP
            case models.hipPenaltyDetails.MainTransactionEnum.AdditionalAssessment => models.getFinancialDetails.MainTransactionEnum.AdditionalAssessment
            case models.hipPenaltyDetails.MainTransactionEnum.AdditionalAssessmentFirstLPP => models.getFinancialDetails.MainTransactionEnum.AdditionalAssessmentFirstLPP
            case models.hipPenaltyDetails.MainTransactionEnum.AdditionalAssessmentSecondLPP => models.getFinancialDetails.MainTransactionEnum.AdditionalAssessmentSecondLPP
            case models.hipPenaltyDetails.MainTransactionEnum.ProtectiveAssessment => models.getFinancialDetails.MainTransactionEnum.ProtectiveAssessment
            case models.hipPenaltyDetails.MainTransactionEnum.ProtectiveAssessmentFirstLPP => models.getFinancialDetails.MainTransactionEnum.ProtectiveAssessmentFirstLPP
            case models.hipPenaltyDetails.MainTransactionEnum.ProtectiveAssessmentSecondLPP => models.getFinancialDetails.MainTransactionEnum.ProtectiveAssessmentSecondLPP
            case models.hipPenaltyDetails.MainTransactionEnum.POAReturnCharge => models.getFinancialDetails.MainTransactionEnum.POAReturnCharge
            case models.hipPenaltyDetails.MainTransactionEnum.POAReturnChargeFirstLPP => models.getFinancialDetails.MainTransactionEnum.POAReturnChargeFirstLPP
            case models.hipPenaltyDetails.MainTransactionEnum.POAReturnChargeSecondLPP => models.getFinancialDetails.MainTransactionEnum.POAReturnChargeSecondLPP
            case models.hipPenaltyDetails.MainTransactionEnum.AAReturnCharge => models.getFinancialDetails.MainTransactionEnum.AAReturnCharge
            case models.hipPenaltyDetails.MainTransactionEnum.AAReturnChargeFirstLPP => models.getFinancialDetails.MainTransactionEnum.AAReturnChargeFirstLPP
            case models.hipPenaltyDetails.MainTransactionEnum.AAReturnChargeSecondLPP => models.getFinancialDetails.MainTransactionEnum.AAReturnChargeSecondLPP
            case models.hipPenaltyDetails.MainTransactionEnum.VATOverpaymentFromTax => models.getFinancialDetails.MainTransactionEnum.VATOverpaymentForTax
            case models.hipPenaltyDetails.MainTransactionEnum.ManualLPP => models.getFinancialDetails.MainTransactionEnum.ManualLPP
          },
          principalChargeBillingFrom = hipLPP.principalChargeBillingFrom,
          principalChargeBillingTo = hipLPP.principalChargeBillingTo,
          principalChargeDueDate = hipLPP.principalChargeDueDate,
          principalChargeLatestClearing = hipLPP.principalChargeLatestClearing,
          vatOutstandingAmount = None,
          penaltyChargeDueDate = hipLPP.penaltyChargeDueDate,
          appealInformation = hipLPP.appealInformation.map(_.map { hipAppeal =>
            models.getPenaltyDetails.appealInfo.AppealInformationType(
              appealStatus = hipAppeal.appealStatus.map {
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Under_Appeal => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Under_Appeal
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Upheld => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Upheld
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Rejected => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Rejected
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedChargeAlreadyReversed => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedChargeAlreadyReversed
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldPointAlreadyRemoved => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldPointAlreadyRemoved
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldChargeAlreadyReversed => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealUpheldChargeAlreadyReversed
                case models.hipPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedPointAlreadyRemoved => models.getPenaltyDetails.appealInfo.AppealStatusEnum.AppealRejectedPointAlreadyRemoved
                case _ => models.getPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable 
              },
              appealLevel = hipAppeal.appealLevel.map {
                case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.HMRC => models.getPenaltyDetails.appealInfo.AppealLevelEnum.HMRC
                case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.TribunalOrSecond => models.getPenaltyDetails.appealInfo.AppealLevelEnum.TribunalOrSecond
                case models.hipPenaltyDetails.appealInfo.AppealLevelEnum.Tribunal => models.getPenaltyDetails.appealInfo.AppealLevelEnum.Tribunal
                case _ => models.getPenaltyDetails.appealInfo.AppealLevelEnum.HMRC 
              },
              appealDescription = hipAppeal.appealDescription
            )
          }),
          metadata = models.getPenaltyDetails.latePayment.LPPDetailsMetadata(
            principalChargeSubTransaction = hipLPP.principalChargeSubTr,
            principalChargeDocNumber = hipLPP.principalChargeDocNumber,
            timeToPay = hipLPP.timeToPay.map(_.map { ttp =>
              models.getPenaltyDetails.latePayment.TimeToPay(
                TTPStartDate = ttp.ttpStartDate,
                TTPEndDate = ttp.ttpEndDate
              )
            })
          )
        )
      })
      
      models.getPenaltyDetails.latePayment.LatePaymentPenalty(
        details = regularLPPDetails,
        ManualLPPIndicator = Some(hipLPPContainer.manualLPPIndicator)
      )
    }
    // TODO this is where HIP gets converted to IF structure. Need to apply this to the API call
    models.getPenaltyDetails.GetPenaltyDetails(
      totalisations = regularTotalisations,
      lateSubmissionPenalty = regularLSP,
      latePaymentPenalty = regularLPP,
      breathingSpace = hipPenaltyDetails.breathingSpace.map(_.map { hipBS =>
        models.getPenaltyDetails.breathingSpace.BreathingSpace(
          BSStartDate = hipBS.bsStartDate,
          BSEndDate = hipBS.bsEndDate
        )
      })
    )
  }

  def getDataFromPenaltyService(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    val startOfLogMsg: String = s"[PenaltyDetailsService][getDataFromPenaltyService][${enrolmentKey.regime.value}]"

    getPenaltyDetailsConnector.getPenaltyDetails(enrolmentKey).map {
      handleConnectorResponse(_)(startOfLogMsg, enrolmentKey)
    }
  }

  private def handleConnectorResponse(connectorResponse: GetPenaltyDetailsResponse)(implicit
      startOfLogMsg: String,
      enrolmentKeyInfo: AgnosticEnrolmentKey): GetPenaltyDetailsResponse =
    connectorResponse match {
      case res @ Right(_ @GetPenaltyDetailsSuccessResponse(penaltyDetails)) =>
        implicit val loggingContext: LoggingContext = LoggingContext(
          callingClass = "PenaltiesDetailsService",
          function = "handleConnectorResponse",
          enrolmentKey = enrolmentKeyInfo.toString
        )

        logger.info(s"$startOfLogMsg - Got a success response from the connector. Parsed model")
        val penaltiesWithAppealStatusFiltered = filterService.filterPenaltiesWith9xAppealStatus(penaltyDetails)
        val filteredPenaltyDetails = filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesWithAppealStatusFiltered)
        Right(GetPenaltyDetailsSuccessResponse(filteredPenaltyDetails))
      case res @ Left(GetPenaltyDetailsNoContent) =>
        logger.info(s"$startOfLogMsg - Got a 404 response and no data was found for GetPenaltyDetails call")
        res
      case res @ Left(GetPenaltyDetailsMalformed) =>
        logger.info(s"$startOfLogMsg - Failed to parse HTTP response into model for $enrolmentKeyInfo")
        res
      case res @ Left(GetPenaltyDetailsFailureResponse(_)) =>
        logger.error(s"$startOfLogMsg - Unknown status returned from connector for $enrolmentKeyInfo")
        res
    }
}

