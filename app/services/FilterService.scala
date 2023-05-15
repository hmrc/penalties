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

import config.AppConfig
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.AppealStatusEnum
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import models.getPenaltyDetails.lateSubmission.{LSPDetails, LateSubmissionPenalty}
import play.api.libs.json.{JsString, JsValue, Json}
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class FilterService @Inject()()(implicit ec: ExecutionContext, appConfig: AppConfig) {

  def tryJsonParseOrJsSting(body: String): JsValue = {
    Try(Json.parse(body)).getOrElse(JsString(body))
  }

  def filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails: GetPenaltyDetails, callingClass: String, function: String, vrn: String): GetPenaltyDetails = {
    if (penaltiesDetails.latePaymentPenalty.nonEmpty) {
      val filtered: Option[Seq[LPPDetails]] = filterEstimatedLPP1(penaltiesDetails)
      val numberOfFiltered = numberOfFilteredLPPs(filtered, penaltiesDetails)
      if (filtered.nonEmpty && filtered.get.nonEmpty && numberOfFiltered >= 0) {
        logger.info(s"[FilterService][filterEstimatedLPP1DuringPeriodOfFamiliarisation] Filtering for [$callingClass][$function] -" +
          s" Filtered ${numberOfFiltered} LPP1(s) from payload for VRN: $vrn")
        penaltiesDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(filtered)))
      } else {
        logger.info(s"[FilterService][filterEstimatedLPP1DuringPeriodOfFamiliarisation] Filtering for [$callingClass][$function] - Filtered all LPP1s from payload for VRN: $vrn")
        penaltiesDetails.copy(latePaymentPenalty = None)
      }
    } else {
      logger.info(s"[FilterService][filterEstimatedLPP1DuringPeriodOfFamiliarisation] Filtering for [$callingClass][$function] - No LPPs to filter for VRN: $vrn")
      penaltiesDetails
    }
  }

  private def filterEstimatedLPP1(penaltiesDetails: GetPenaltyDetails): Option[Seq[LPPDetails]] = {
    penaltiesDetails.latePaymentPenalty.flatMap(
      _.details.map(latePaymentPenalties => latePaymentPenalties.filterNot(lpp => {
        lpp.penaltyCategory.equals(LPPPenaltyCategoryEnum.FirstPenalty) &&
          lpp.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing) &&
          appConfig.withinLPP1FilterWindow(lpp.principalChargeDueDate)
      })
      )
    )
  }

  def filterPenaltiesWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails)(implicit callingClass: String, function: String, vrn: String): GetPenaltyDetails = {
    val filteredLSPs: Option[Seq[LSPDetails]] = if (penaltiesDetails.lateSubmissionPenalty.nonEmpty) filterLSPWith9xAppealStatus(penaltiesDetails) else None
    val noOfFilteredLSPs:Int = numberOfFilteredLSPs(filteredLSPs, penaltiesDetails)
    val filteredLPPs: Option[Seq[LPPDetails]] = if (penaltiesDetails.latePaymentPenalty.nonEmpty) filterLPPWith9xAppealStatus(penaltiesDetails) else None
    val noOfFilteredLPPs: Int = numberOfFilteredLPPs(filteredLPPs, penaltiesDetails)

    (noOfFilteredLSPs, noOfFilteredLPPs) match {
      case (x , 0) if x > 0 => penaltiesDetails.copy(lateSubmissionPenalty = prepareLateSubmissionPenaltiesAfterFilter(penaltiesDetails, filteredLSPs, noOfFilteredLSPs, vrn, callingClass, function))
      case (0, x) if x > 0 =>
        penaltiesDetails.copy(latePaymentPenalty = prepareLatePaymentPenaltiesAfterFilter(filteredLPPs, noOfFilteredLPPs, vrn, callingClass, function))
      case (x, y) if x > 0 && y > 0 =>
        val newLasteSubmissions: Option[LateSubmissionPenalty] = prepareLateSubmissionPenaltiesAfterFilter(penaltiesDetails, filteredLSPs, noOfFilteredLSPs, vrn, callingClass, function)
        val newLastePaymentPenalties: Option[LatePaymentPenalty] = prepareLatePaymentPenaltiesAfterFilter(filteredLPPs, noOfFilteredLPPs, vrn, callingClass, function)
        penaltiesDetails.copy(lateSubmissionPenalty = newLasteSubmissions, latePaymentPenalty = newLastePaymentPenalties)
      case _ =>
        logger.info(s"[FilterService][filterPenaltiesWith9xAppealStatus] Filtering for [$callingClass][$function] - No LSPs or LPPs to filter for VRN: $vrn")
        penaltiesDetails
    }
  }

  private def prepareLateSubmissionPenaltiesAfterFilter(penaltiesDetails: GetPenaltyDetails, filteredLSPs: Option[Seq[LSPDetails]], noOfFilteredLSPs: Int, vrn: String, callingClass: String, function: String) = {
    if (filteredLSPs.nonEmpty && filteredLSPs.get.nonEmpty && noOfFilteredLSPs >= 0) {
      logger.info(s"[FilterService][filterPenaltiesWith9xAppealStatus] Filtering for [$callingClass][$function] -" +
        s" Filtered ${noOfFilteredLSPs} LSP(s) from payload for VRN: $vrn")
      val summary = penaltiesDetails.lateSubmissionPenalty.map(lateSubmissionPenalty => lateSubmissionPenalty.summary)
      Some(LateSubmissionPenalty(summary = summary.get, details = filteredLSPs.getOrElse(Seq.empty[LSPDetails])))
    } else {
      logger.info(s"[FilterService][filterPenaltiesWith9xAppealStatus] Filtering for [$callingClass][$function] - Filtered all LSPs from payload for VRN: $vrn")
      None
    }
  }

  private def prepareLatePaymentPenaltiesAfterFilter(filteredLPPs: Option[Seq[LPPDetails]], noOfFilteredLPPs: Int, vrn: String, callingClass: String, function: String) = {
    if (filteredLPPs.nonEmpty && filteredLPPs.get.nonEmpty && noOfFilteredLPPs >= 0) {
      logger.info(s"[FilterService][filterPenaltiesWith9xAppealStatus] Filtering for [$callingClass][$function] -" +
        s" Filtered ${noOfFilteredLPPs} LPP(s) from payload for VRN: $vrn")
      Some(LatePaymentPenalty(filteredLPPs))
    } else {
      logger.info(s"[FilterService][filterPenaltiesWith9xAppealStatus] Filtering for [$callingClass][$function] - Filtered all LPPs from payload for VRN: $vrn")
      None
    }
  }

  private def filterLPPWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails): Option[Seq[LPPDetails]] = {
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


  private def numberOfFilteredLPPs(filteredLPPs: Option[Seq[LPPDetails]], penaltiesDetails: GetPenaltyDetails): Int = {
    if (penaltiesDetails.latePaymentPenalty.nonEmpty) {
      penaltiesDetails.latePaymentPenalty.get.details.get.size - filteredLPPs.get.size
    } else {
      0
    }
  }

  private def numberOfFilteredLSPs(filteredLSPs: Option[Seq[LSPDetails]], penaltiesDetails: GetPenaltyDetails): Int = {
    if (penaltiesDetails.lateSubmissionPenalty.nonEmpty) {
      penaltiesDetails.lateSubmissionPenalty.get.details.size - filteredLSPs.get.size
    } else {
      0
    }
  }
}
