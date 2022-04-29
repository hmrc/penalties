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

import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsResponse, GetPenaltyDetailsSuccessResponse}
import connectors.v3.getPenaltyDetails.GetPenaltyDetailsConnector
import models.v3.getPenaltyDetails.GetPenaltyDetails
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetPenaltyDetailsService @Inject()(getPenaltyDetailsConnector: GetPenaltyDetailsConnector)
                                        (implicit ec: ExecutionContext){

  def getDataFromPenaltyServiceForVATCVRN(vrn: String)(implicit hc: HeaderCarrier): Future[(Option[GetPenaltyDetails], GetPenaltyDetailsResponse) ] = {
    implicit val startOfLogMsg: String = "[GetPenaltyDetailsService][getDataFromPenaltyServiceForVATCVRN]"
    getPenaltyDetailsConnector.getPenaltyDetails(vrn).map {
      handleConnectorResponse(_)
    }
  }

  private def handleConnectorResponse(connectorResponse: GetPenaltyDetailsResponse)
                                     (implicit startOfLogMsg: String): (Option[GetPenaltyDetails], GetPenaltyDetailsResponse) = {
    connectorResponse match {
      case res@Right(_@GetPenaltyDetailsSuccessResponse(penaltyDetails)) =>
        logger.debug(s"$startOfLogMsg - Got a success response from the connector. Parsed model: $penaltyDetails")
        (Some(penaltyDetails), res)
      case res@Left(GetPenaltyDetailsMalformed) =>
        logger.info(s"$startOfLogMsg - Failed to parse HTTP response into model.")
        (None, res)
      case res@Left(GetPenaltyDetailsFailureResponse(_)) =>
        logger.error(s"$startOfLogMsg - Unknown status returned from connector.")
        (None, res)
    }
  }
}
