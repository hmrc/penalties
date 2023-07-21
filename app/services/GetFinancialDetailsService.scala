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

import connectors.getFinancialDetails.GetFinancialDetailsConnector
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsService @Inject()(getFinancialDetailsConnector: GetFinancialDetailsConnector)
                                          (implicit ec: ExecutionContext, val config: Configuration) {

  def getFinancialDetails(vrn: String, optionalParameters: Option[String])(implicit hc: HeaderCarrier): Future[GetFinancialDetailsResponse] = {
    val startOfLogMsg: String = "[GetFinancialDetailsService][getDataFromFinancialServiceForVATCVRN]"
    getFinancialDetailsConnector.getFinancialDetails(vrn, optionalParameters).map {
      handleConnectorResponse(_)(startOfLogMsg, vrn)
    }
  }

  private def handleConnectorResponse(connectorResponse: GetFinancialDetailsResponse)
                                     (implicit startOfLogMsg: String, vrn: String): GetFinancialDetailsResponse = {
    connectorResponse match {
      case res@Right(_@GetFinancialDetailsSuccessResponse(financialDetails)) =>
        logger.debug(s"$startOfLogMsg - Got a success response from the connector. Parsed model: $financialDetails")
        res
      case res@Left(GetFinancialDetailsNoContent) =>
        logger.debug(s"$startOfLogMsg - Got a 404 response and no data was found for GetFinancialDetails call")
        res
      case res@Left(GetFinancialDetailsMalformed) =>
        logger.info(s"$startOfLogMsg - Failed to parse HTTP response into model for VRN: $vrn")
        res
      case res@Left(GetFinancialDetailsFailureResponse(_)) =>
        logger.error(s"$startOfLogMsg - Unknown status returned from connector for VRN: $vrn")
        res
    }
  }
}
