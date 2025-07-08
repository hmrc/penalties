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

import connectors.getFinancialDetails.FinancialDetailsConnector
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.AgnosticEnrolmentKey
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsService @Inject() (getFinancialDetailsConnector: FinancialDetailsConnector)(implicit
    ec: ExecutionContext,
    val config: Configuration) {

  def getFinancialDetails(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[GetFinancialDetailsResponse] = {
    val startOfLogMsg: String = s"[FinancialDetailsService][getFinancialDetails][$enrolmentKey]"

    getFinancialDetailsConnector.getFinancialDetails(enrolmentKey).map {
      case res @ Right(_ @GetFinancialDetailsSuccessResponse(financialDetails)) =>
        logger.debug(s"$startOfLogMsg - Success response returned from connector. Parsed model: $financialDetails")
        res
      case res @ Right(_ @GetFinancialDetailsHipSuccessResponse(financialData)) =>
        logger.debug(s"$startOfLogMsg - Success response returned from connector. Parsed model: $financialData")
        res
      case res @ Left(GetFinancialDetailsNoContent) =>
        logger.debug(s"$startOfLogMsg - 404 response returned as no data was found for GetFinancialDetails call")
        res
      case res @ Left(GetFinancialDetailsMalformed) =>
        logger.error(s"$startOfLogMsg - Failed to parse HTTP response into model")
        res
      case res @ Left(GetFinancialDetailsFailureResponse(_)) =>
        logger.error(s"$startOfLogMsg - Unknown status returned from connector")
        res
    }
  }
}
