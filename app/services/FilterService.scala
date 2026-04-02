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
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.AppealInformationType
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import models.getPenaltyDetails.lateSubmission.LSPDetails
import models.hipPenaltyDetails.appealInfo.AppealStatusEnum
import play.api.libs.json.{JsString, JsValue, Json}
import services.FilterService.{filterOutLPPsWith9xAppealStatus, filterOutLSPsWith9xAppealStatus, loggerPrefix}
import utils.Logger.logger
import utils.PenaltyDetailsConverter.putSeqInsideOption

import javax.inject.Inject
import scala.util.Try

class FilterService @Inject() ()(implicit appConfig: AppConfig) {

  def filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails: GetPenaltyDetails)(implicit
      loggingContext: LoggingContext): GetPenaltyDetails = {
    val logPrefix = loggerPrefix("filterEstimatedLPP1DuringPeriodOfFamiliarisation")
    if (penaltiesDetails.latePaymentPenalty.nonEmpty) {
      val filteredLPPs: Seq[LPPDetails] = findEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails)
      if (filteredLPPs.nonEmpty) {
        logger.info(s"$logPrefix Filtered ${filteredLPPs.size} LPP1(s) from payload for ${loggingContext.enrolmentKey}")
        penaltiesDetails.copy(latePaymentPenalty =
          Some(LatePaymentPenalty(putSeqInsideOption[LPPDetails](filteredLPPs), penaltiesDetails.latePaymentPenalty.flatMap(_.ManualLPPIndicator))))
      } else {
        logger.info(s"$logPrefix Filtered all LPP1s from payload for ${loggingContext.enrolmentKey}")
        penaltiesDetails.copy(latePaymentPenalty = None)
      }
    } else {
      logger.info(s"$logPrefix No LPPs to filter for ${loggingContext.enrolmentKey}")
      penaltiesDetails
    }
  }

  private def findEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails: GetPenaltyDetails): Seq[LPPDetails] =
    penaltiesDetails.latePaymentPenalty
      .flatMap(
        _.details.map(latePaymentPenalties =>
          latePaymentPenalties.filterNot { lpp =>
            lpp.penaltyCategory.equals(LPPPenaltyCategoryEnum.FirstPenalty) &&
            lpp.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing) &&
            appConfig.withinLPP1FilterWindow(lpp.principalChargeDueDate)
          })
      )
      .getOrElse(Seq.empty[LPPDetails])

  def filterPenaltiesWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails)(implicit loggingContext: LoggingContext): GetPenaltyDetails = {
    val filteredLspDetails: Seq[LSPDetails] = filterOutLSPsWith9xAppealStatus(penaltiesDetails)
    val filteredLppDetails: Seq[LPPDetails] = filterOutLPPsWith9xAppealStatus(penaltiesDetails)

    penaltiesDetails.copy(
      lateSubmissionPenalty = penaltiesDetails.lateSubmissionPenalty.map { lsp =>
        lsp.copy(details = filteredLspDetails)
      },
      latePaymentPenalty = penaltiesDetails.latePaymentPenalty.map { lpp =>
        lpp.copy(details = Some(filteredLppDetails))
      }
    )
  }

}

object FilterService {

  private[FilterService] def loggerPrefix(methodName: String)(implicit loggingContext: LoggingContext) =
    s"[RegimeFilterService][$methodName] Filtering for [${loggingContext.callingClass}][${loggingContext.function}] -"

  private[FilterService] def filterOutLSPsWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails)(implicit
      loggingContext: LoggingContext): Seq[LSPDetails] = {
    val lspDetails: Seq[LSPDetails] = penaltiesDetails.lateSubmissionPenalty.map(_.details).getOrElse(Seq.empty[LSPDetails])
    val lspDetailsPostFilter        = lspDetails.filterNot(details => doesAppealInformationHave9xAppealStatus(details.appealInformation))

    val amountFiltered = lspDetails.size - lspDetailsPostFilter.size
    logger.info(s"${loggerPrefix("filterOutLSPsWith9xAppealStatus")} Filtered $amountFiltered LSPs from payload for ${loggingContext.enrolmentKey}")
    lspDetailsPostFilter
  }

  private[FilterService] def filterOutLPPsWith9xAppealStatus(penaltiesDetails: GetPenaltyDetails)(implicit
      loggingContext: LoggingContext): Seq[LPPDetails] = {
    val lppDetails: Seq[LPPDetails] = penaltiesDetails.latePaymentPenalty.flatMap(_.details).getOrElse(Seq.empty[LPPDetails])
    val lppDetailsPostFilter        = lppDetails.filterNot(details => doesAppealInformationHave9xAppealStatus(details.appealInformation))

    val amountFiltered = lppDetails.size - lppDetailsPostFilter.size
    logger.info(s"${loggerPrefix("filterOutLPPsWith9xAppealStatus")} Filtered $amountFiltered LPPs from payload for ${loggingContext.enrolmentKey}")
    lppDetailsPostFilter
  }

  def doesAppealInformationHave9xAppealStatus(appealInformation: Option[Seq[AppealInformationType]]): Boolean =
    // 91-94 appeal statuses are not displayed or accepted by the frontend and so are filtered out
    appealInformation.getOrElse(Seq.empty).exists(_.appealStatus.exists(status => AppealStatusEnum.ignoredStatuses.contains(status)))

  def tryJsonParseOrJsString(body: String): JsValue =
    Try(Json.parse(body)).getOrElse(JsString(body))

}
