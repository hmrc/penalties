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

import config.featureSwitches.FeatureSwitching
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import connectors.getFinancialDetails.GetFinancialDetailsConnector
import play.api.Configuration
import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger

import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsService @Inject()(getFinancialDetailsConnector: GetFinancialDetailsConnector)
                                          (implicit ec: ExecutionContext, val config: Configuration) extends FeatureSwitching {

  def getDataFromFinancialServiceForVATVCN(vrn: String)(implicit hc: HeaderCarrier): Future[GetFinancialDetailsResponse] = {
    val dateFrom = getTimeMachineDate.minusYears(2)
    val dateTo = getTimeMachineDate
    implicit val startOfLogMsg: String = "[GetFinancialDetailsService][getDataFromFinancialServiceForVATVCN]"
    getFinancialDetailsConnector.getFinancialDetails(vrn, dateFrom, dateTo).map {
      handleConnectorResponse(_)
    }
  }

  private def handleConnectorResponse(connectorResponse: GetFinancialDetailsResponse)
                                     (implicit startOfLogMsg: String): GetFinancialDetailsResponse = {
    connectorResponse match {
      case res@Right(_@GetFinancialDetailsSuccessResponse(financialDetails)) =>
        logger.debug(s"$startOfLogMsg - Got a success response from the connector. Parsed model: $financialDetails")
        res
      case res@Left(GetFinancialDetailsNoContent) =>
        logger.debug(s"$startOfLogMsg - Got a 404 response and no data was found for GetFinancialDetails call")
        res
      case res@Left(GetFinancialDetailsMalformed) =>
        logger.info(s"$startOfLogMsg - Failed to parse HTTP response into model.")
        res
      case res@Left(GetFinancialDetailsFailureResponse(_)) =>
        logger.error(s"$startOfLogMsg - Unknown status returned from connector.")
        res
    }
  }
}
