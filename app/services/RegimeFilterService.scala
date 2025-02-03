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

import config.AppConfig
import models.EnrolmentKey
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.AppealStatusEnum
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import models.getPenaltyDetails.lateSubmission.{LSPDetails, LateSubmissionPenalty}
import play.api.libs.json.{JsString, JsValue, Json}
import utils.Logger.logger

import javax.inject.Inject
import scala.util.Try

class RegimeFilterService @Inject()()(implicit appConfig: AppConfig) {

  def tryJsonParseOrJsString(body: String): JsValue = {
    Try(Json.parse(body)).getOrElse(JsString(body))
  }

  def filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails: GetPenaltyDetails)(implicit loggingContext: LoggingContext): GetPenaltyDetails = {
    if (penaltiesDetails.latePaymentPenalty.nonEmpty) {
      val filteredLPPs: Option[Seq[LPPDetails]] = findEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails)
      val numberOfFilteredLPPs = countNumberOfFilteredLPPs(filteredLPPs, penaltiesDetails)
      if (filteredLPPs.nonEmpty && filteredLPPs.get.nonEmpty && numberOfFilteredLPPs >= 0) {
        logger.info(s"[RegimeFilterService][filterEstimatedLPP1DuringPeriodOfFamiliarisation] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] -" +
          s" Filtered ${numberOfFilteredLPPs} LPP1(s) from payload for ${loggingContext.enrolmentKey}")
        penaltiesDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(filteredLPPs, penaltiesDetails.latePaymentPenalty.get.ManualLPPIndicator)))
      } else {
        logger.info(s"[RegimeFilterService][filterEstimatedLPP1DuringPeriodOfFamiliarisation] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] - Filtered all LPP1s from payload for ${loggingContext.enrolmentKey}")
        penaltiesDetails.copy(latePaymentPenalty = None)
      }
    } else {
      logger.info(s"[RegimeFilterService][filterEstimatedLPP1DuringPeriodOfFamiliarisation] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] - No LPPs to filter for ${loggingContext.enrolmentKey}")
      penaltiesDetails
    }
  }

  private def findEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails: GetPenaltyDetails): Option[Seq[LPPDetails]] = {
    penaltiesDetails.latePaymentPenalty.flatMap(
      _.details.map(latePaymentPenalties => latePaymentPenalties.filterNot(lpp => {
        lpp.penaltyCategory.equals(LPPPenaltyCategoryEnum.FirstPenalty) &&
          lpp.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing) &&
          appConfig.withinLPP1FilterWindow(lpp.principalChargeDueDate)
      })
      )
    )
  }

  def filterPenaltiesWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails)(implicit logingContext: LoggingContext): GetPenaltyDetails = {
    val filteredLSPs: Option[Seq[LSPDetails]] = filterLSPWith9xAppealStatus(penaltiesDetails)
    val numberOfFilteredLSPs: Int = countNumberOfFilteredLSPs(filteredLSPs, penaltiesDetails)
    val filteredLPPs: Option[Seq[LPPDetails]] = findLPPWith9xAppealStatus(penaltiesDetails)
    val numberOfFilteredLPPs: Int = countNumberOfFilteredLPPs(filteredLPPs, penaltiesDetails)

    penaltiesDetails.copy(
      lateSubmissionPenalty = prepareLateSubmissionPenaltiesAfterFilter(penaltiesDetails, filteredLSPs, numberOfFilteredLSPs),
      latePaymentPenalty = prepareLatePaymentPenaltiesAfterFilter(penaltiesDetails, filteredLPPs, numberOfFilteredLPPs)
    )
  }

  private def prepareLateSubmissionPenaltiesAfterFilter(penaltiesDetails: GetPenaltyDetails, filteredLSPs: Option[Seq[LSPDetails]], noOfFilteredLSPs: Int)(implicit loggingContext: LoggingContext): Option[LateSubmissionPenalty] = {
    if (filteredLSPs.nonEmpty && filteredLSPs.get.nonEmpty && noOfFilteredLSPs > 0) {
      logger.info(s"[RegimeFilterService][filterPenaltiesWith9xAppealStatus] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] -" +
        s" Filtered $noOfFilteredLSPs LSP(s) from payload for ${loggingContext.enrolmentKey}")
      val summary = penaltiesDetails.lateSubmissionPenalty.map(lateSubmissionPenalty => lateSubmissionPenalty.summary)
      Some(LateSubmissionPenalty(summary = summary.get, details = filteredLSPs.getOrElse(Seq.empty[LSPDetails])))
    } else if (noOfFilteredLSPs == 0) {
      logger.info(s"[RegimeFilterService][prepareLateSubmissionPenaltiesAfterFilter] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] -" +
        s" No LSPs to filter from payload for ${loggingContext.enrolmentKey}")
      penaltiesDetails.lateSubmissionPenalty
    } else {
      logger.info(s"[RegimeFilterService][prepareLateSubmissionPenaltiesAfterFilter] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] - Filtered all LSPs from payload for ${loggingContext.enrolmentKey}")
      None
    }
  }

  private def prepareLatePaymentPenaltiesAfterFilter(penaltiesDetails: GetPenaltyDetails, filteredLPPs: Option[Seq[LPPDetails]], noOfFilteredLPPs: Int)(implicit loggingContext: LoggingContext): Option[LatePaymentPenalty] = {
    if (filteredLPPs.nonEmpty && filteredLPPs.get.nonEmpty && noOfFilteredLPPs > 0) {
      logger.info(s"[RegimeFilterService][filterPenaltiesWith9xAppealStatus] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] -" +
        s" Filtered ${noOfFilteredLPPs} LPP(s) from payload for ${loggingContext.enrolmentKey}")
      Some(LatePaymentPenalty(filteredLPPs))
    } else if (noOfFilteredLPPs == 0) {
      logger.info(s"[RegimeFilterService][prepareLatePaymentPenaltiesAfterFilter] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] -" +
        s" No LPPs to filter from payload for ${loggingContext.enrolmentKey}")
      penaltiesDetails.latePaymentPenalty
    } else {
      logger.info(s"[RegimeFilterService][prepareLatePaymentPenaltiesAfterFilter] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] - Filtered all LPPs from payload for ${loggingContext.enrolmentKey}")
      None
    }
  }

  private def findLPPWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails): Option[Seq[LPPDetails]] = {
    penaltiesDetails.latePaymentPenalty.flatMap(
      _.details.map(latePaymentPenalties => latePaymentPenalties.filterNot(lpp => {
        lpp.appealInformation.nonEmpty && lpp.appealInformation.get.exists(appealInfo => appealInfo.appealStatus.nonEmpty && (appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealRejectedChargeAlreadyReversed) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealUpheldChargeAlreadyReversed) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealRejectedPointAlreadyRemoved) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealUpheldPointAlreadyRemoved)))
      })
      )
    )
  }

  private def filterLSPWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails): Option[Seq[LSPDetails]] = {
    penaltiesDetails.lateSubmissionPenalty.map(_.details.filterNot(lsp =>
      lsp.appealInformation.nonEmpty && lsp.appealInformation.get.exists(appealInfo => appealInfo.appealStatus.nonEmpty && (appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealRejectedChargeAlreadyReversed) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealUpheldChargeAlreadyReversed) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealRejectedPointAlreadyRemoved) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealUpheldPointAlreadyRemoved)))))
  }


  private def countNumberOfFilteredLPPs(filteredLPPs: Option[Seq[LPPDetails]], penaltiesDetails: GetPenaltyDetails): Int = {
    if (penaltiesDetails.latePaymentPenalty.nonEmpty) {
      penaltiesDetails.latePaymentPenalty.get.details.get.size - filteredLPPs.get.size
    } else {
      0
    }
  }

  private def countNumberOfFilteredLSPs(filteredLSPs: Option[Seq[LSPDetails]], penaltiesDetails: GetPenaltyDetails): Int = {
    if (penaltiesDetails.lateSubmissionPenalty.nonEmpty) {
      penaltiesDetails.lateSubmissionPenalty.get.details.size - filteredLSPs.get.size
    } else {
      0
    }
  }
}