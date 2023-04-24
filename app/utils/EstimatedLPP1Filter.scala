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

package utils

import config.AppConfig
import javax.inject.Inject
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import play.api.libs.json.{JsString, JsValue, Json}
import utils.Logger.logger

import scala.concurrent.ExecutionContext
import scala.util.Try

class EstimatedLPP1Filter @Inject()()(implicit ec: ExecutionContext, appConfig: AppConfig) {

  def tryJsonParseOrJsSting(body: String): JsValue = {
    Try(Json.parse(body)).getOrElse(JsString(body))
  }

  def returnFilteredLPPs(penaltiesDetails: GetPenaltyDetails, function: String, method: String, vrn: String): GetPenaltyDetails = {
    if(penaltiesDetails.latePaymentPenalty.nonEmpty) {
      val filtered: Option[Seq[LPPDetails]] = filterEstimatedLPP1(penaltiesDetails)
      if (filtered.nonEmpty) {
        logger.info(s"[EstimatedLPP1Filter][returnFilteredLPPs] Filtering for [$function][$method] -" +
          s" Filtered ${numberOfFilteredLPPs(filtered, penaltiesDetails)} LPP1(s) from payload for VRN: $vrn")
        penaltiesDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(filtered)))
      } else {
        val numberOfFilteredLPPs = penaltiesDetails.latePaymentPenalty.get.details.get.size
        logger.info(s"[EstimatedLPP1Filter][returnFilteredLPPs] Filtering for [$function][$method] - Filtered $numberOfFilteredLPPs LPP1(s) from payload for VRN: $vrn")
        penaltiesDetails.copy(latePaymentPenalty = None)
      }
    } else {
      logger.info(s"[EstimatedLPP1Filter][returnFilteredLPPs] Filtering for [$function][$method] - No LPPs to filter for VRN: $vrn")
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

  private def numberOfFilteredLPPs(filteredLPPs:  Option[Seq[LPPDetails]], penaltiesDetails: GetPenaltyDetails): Int = {
    filteredLPPs.get.size - penaltiesDetails.latePaymentPenalty.get.details.size
  }
}
