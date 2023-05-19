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
import scala.util.Try

class FilterService @Inject()()(implicit appConfig: AppConfig) {

  def tryJsonParseOrJsString(body: String): JsValue = {
    Try(Json.parse(body)).getOrElse(JsString(body))
  }

  //scalastyle:off
  def filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails: GetPenaltyDetails, callingClass: String,
                                                       function: String, vrn: String): GetPenaltyDetails = {
    if (penaltiesDetails.latePaymentPenalty.nonEmpty) {
      val filteredLPPs: Option[Seq[LPPDetails]] = findEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails)
      val startOfLog = s"[FilterService][filterEstimatedLPP1DuringPeriodOfFamiliarisation][$callingClass][$function] - "
      if(filteredLPPs.isDefined) logNumberOfFilteredLPPs(filteredLPPs.get, penaltiesDetails)(startOfLog)(vrn)
      penaltiesDetails.copy(latePaymentPenalty = if(filteredLPPs.exists(_.nonEmpty)) Some(LatePaymentPenalty(filteredLPPs)) else None)
    } else {
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

  def filterPenaltiesWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails)(implicit callingClass: String, function: String, vrn: String): GetPenaltyDetails = {
    val lspsWithout9xAppealStatus: Option[Seq[LSPDetails]] = findLSPsWithout9xAppealStatus(penaltiesDetails)
    val lppsWithout9xAppealStatus: Option[Seq[LPPDetails]] = findLPPsWithout9xAppealStatus(penaltiesDetails)
    penaltiesDetails.copy(
      lateSubmissionPenalty = prepareLateSubmissionPenaltiesAfterFilter(penaltiesDetails, lspsWithout9xAppealStatus, vrn, callingClass, function),
      latePaymentPenalty = prepareLatePaymentPenaltiesAfterFilter(penaltiesDetails, lppsWithout9xAppealStatus, vrn, callingClass, function)
    )
  }

  private def prepareLateSubmissionPenaltiesAfterFilter(penaltiesDetails: GetPenaltyDetails, optLSPsWithout9xAppealStatus: Option[Seq[LSPDetails]], vrn: String, callingClass: String, function: String): Option[LateSubmissionPenalty] = {
    if (optLSPsWithout9xAppealStatus.isDefined) {
      val lspsWithout9xAppealStatus = optLSPsWithout9xAppealStatus.get
      val startOfMessage = s"[FilterService][filterPenaltiesWith9xAppealStatus][prepareLateSubmissionPenaltiesAfterFilter][$callingClass][$function] -"
      logNumberOfFilteredLSPs(lspsWithout9xAppealStatus, penaltiesDetails)(startOfMessage)(vrn)
      if(lspsWithout9xAppealStatus.nonEmpty) penaltiesDetails.lateSubmissionPenalty.map(_.copy(details = lspsWithout9xAppealStatus)) else None
    } else {
      penaltiesDetails.lateSubmissionPenalty
    }
  }

  private def prepareLatePaymentPenaltiesAfterFilter(penaltiesDetails: GetPenaltyDetails, optLPPsWithout9xAppealStatus: Option[Seq[LPPDetails]], vrn: String, callingClass: String, function: String): Option[LatePaymentPenalty] = {
    if (optLPPsWithout9xAppealStatus.isDefined) {
      val lppsWithout9xAppealStatus = optLPPsWithout9xAppealStatus.get
      val startOfMessage = s"[FilterService][filterPenaltiesWith9xAppealStatus][prepareLatePaymentPenaltiesAfterFilter][$callingClass][$function] -"
      logNumberOfFilteredLPPs(lppsWithout9xAppealStatus, penaltiesDetails)(startOfMessage)(vrn)
      if(lppsWithout9xAppealStatus.nonEmpty) penaltiesDetails.latePaymentPenalty.map(_.copy(details = optLPPsWithout9xAppealStatus)) else None
    } else {
      penaltiesDetails.latePaymentPenalty
    }
  }

  private def findLPPsWithout9xAppealStatus(penaltiesDetails: GetPenaltyDetails): Option[Seq[LPPDetails]] = {
    penaltiesDetails.latePaymentPenalty.flatMap(
      _.details.map(latePaymentPenalties => latePaymentPenalties.filterNot(lpp => {
        lpp.appealInformation.nonEmpty && lpp.appealInformation.get.exists(appealInfo => appealInfo.appealStatus.nonEmpty && (appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealRejectedChargeAlreadyReversed) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealUpheldChargeAlreadyReversed) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealRejectedPointAlreadyRemoved) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealUpheldPointAlreadyRemoved)))
      })
      )
    )
  }

  private def findLSPsWithout9xAppealStatus(penaltiesDetails: GetPenaltyDetails): Option[Seq[LSPDetails]] = {
    penaltiesDetails.lateSubmissionPenalty.map(_.details.filterNot(lsp =>
      lsp.appealInformation.nonEmpty && lsp.appealInformation.get.exists(appealInfo => appealInfo.appealStatus.nonEmpty && (appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealRejectedChargeAlreadyReversed) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealUpheldChargeAlreadyReversed) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealRejectedPointAlreadyRemoved) || appealInfo.appealStatus.get.equals(AppealStatusEnum.AppealUpheldPointAlreadyRemoved)))))
  }


  private def logNumberOfFilteredLPPs(lppsWithout9xAppealStatus: Seq[LPPDetails], penaltiesDetails: GetPenaltyDetails)(startOfMessage: String)(vrn: String): Unit = {
    val numberOfLPPsFilteredOut = {
      if (penaltiesDetails.latePaymentPenalty.exists(_.details.nonEmpty))
        penaltiesDetails.latePaymentPenalty.get.details.map(_.size).get - lppsWithout9xAppealStatus.size
      else 0
    }
    if (lppsWithout9xAppealStatus.nonEmpty) logger.info(s"$startOfMessage Filtered $numberOfLPPsFilteredOut LPP(s) from payload for VRN: $vrn")
    else logger.info(s"$startOfMessage No LPP to filter from payload for VRN: $vrn")
  }

  private def logNumberOfFilteredLSPs(lspsWithout9xAppealStatus: Seq[LSPDetails], penaltiesDetails: GetPenaltyDetails)(startOfMessage: String)(vrn: String): Unit = {
    val numberOfLSPsFilteredOut = {
      if (penaltiesDetails.lateSubmissionPenalty.exists(_.details.nonEmpty)) penaltiesDetails.lateSubmissionPenalty.get.details.size - lspsWithout9xAppealStatus.size
      else 0
    }
    if (lspsWithout9xAppealStatus.nonEmpty) logger.info(s"$startOfMessage Filtered $numberOfLSPsFilteredOut LSP(s) from payload for VRN: $vrn")
    else logger.info(s"$startOfMessage No LSPs to filter from payload for VRN: $vrn")
  }
}
