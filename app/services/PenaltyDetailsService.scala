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

import config.featureSwitches.{CallAPI1812HIP, FeatureSwitching}
import connectors.getPenaltyDetails.{HIPPenaltyDetailsConnector, PenaltyDetailsConnector}
import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser._
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser._
import models.AgnosticEnrolmentKey
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger
import utils.PenaltyDetailsConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class LoggingContext(callingClass: String, function: String, enrolmentKey: String)

class PenaltyDetailsService @Inject() (
                                        getPenaltyDetailsConnector: PenaltyDetailsConnector,
                                        hipPenaltyDetailsConnector: HIPPenaltyDetailsConnector,
                                        filterService: FilterService
)(implicit ec: ExecutionContext, val config: Configuration) extends FeatureSwitching {

  def getPenaltyDetails(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    if (isEnabled(CallAPI1812HIP)) {
      handleHIPResponse(enrolmentKey)
    } else {
      handleRegularResponse(enrolmentKey)
    }
  }

  private def handleHIPResponse(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    val startOfLogMsg = s"[PenaltyDetailsService][getPenaltyDetails][${enrolmentKey.regime.value}]"
    
    hipPenaltyDetailsConnector.getPenaltyDetails(enrolmentKey).map { hipResponse =>
      hipResponse.fold(
        failure => convertHIPFailureToRegular(failure, enrolmentKey, startOfLogMsg),
        success => {
          implicit val loggingContext: LoggingContext = LoggingContext(
            callingClass = "PenaltiesDetailsService",
            function = "handleHIPResponse", 
            enrolmentKey = enrolmentKey.toString
          )
          
          val convertedPenaltyDetails = PenaltyDetailsConverter.convertHIPToGetPenaltyDetails(
            success.asInstanceOf[HIPPenaltyDetailsSuccessResponse].penaltyDetails
          )
          val filteredPenaltyDetails = applyFilters(convertedPenaltyDetails)
          Right(GetPenaltyDetailsSuccessResponse(filteredPenaltyDetails))
        }
      )
    }
  }

  private def handleRegularResponse(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    val startOfLogMsg = s"[PenaltyDetailsService][getPenaltyDetails][${enrolmentKey.regime.value}]"
    
    getPenaltyDetailsConnector.getPenaltyDetails(enrolmentKey).map { connectorResponse =>
      handleConnectorResponse(connectorResponse)(startOfLogMsg, enrolmentKey)
    }
  }

  private def convertHIPFailureToRegular(
    failure: HIPPenaltyDetailsFailure, 
    enrolmentKey: AgnosticEnrolmentKey, 
    startOfLogMsg: String
  ): GetPenaltyDetailsResponse = {
    failure match {
      case HIPPenaltyDetailsNoContent => 
        logger.info(s"$startOfLogMsg - No data was found for GetPenaltyDetails call")
        Left(GetPenaltyDetailsNoContent)
      case HIPPenaltyDetailsMalformed => 
        logger.info(s"$startOfLogMsg - Failed to parse HTTP response into HIP model for $enrolmentKey")
        Left(GetPenaltyDetailsMalformed)
      case HIPPenaltyDetailsFailureResponse(status) => 
        logger.error(s"$startOfLogMsg - Unknown status returned from HIP connector for $enrolmentKey")
        Left(GetPenaltyDetailsFailureResponse(status))
    }
  }

  private def applyFilters(penaltyDetails: models.getPenaltyDetails.GetPenaltyDetails)(implicit loggingContext: LoggingContext): models.getPenaltyDetails.GetPenaltyDetails = {
    val penaltiesWithAppealStatusFiltered = filterService.filterPenaltiesWith9xAppealStatus(penaltyDetails)
    filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesWithAppealStatusFiltered)
  }

  private def handleConnectorResponse(connectorResponse: GetPenaltyDetailsResponse)(implicit
    startOfLogMsg: String,
    enrolmentKeyInfo: AgnosticEnrolmentKey): GetPenaltyDetailsResponse = {
    
    connectorResponse match {
      case Right(GetPenaltyDetailsSuccessResponse(penaltyDetails)) =>
        implicit val loggingContext: LoggingContext = LoggingContext(
          callingClass = "PenaltiesDetailsService",
          function = "handleConnectorResponse",
          enrolmentKey = enrolmentKeyInfo.toString
        )

        logger.info(s"$startOfLogMsg - Got a success response from the connector. Parsed model")
        val filteredPenaltyDetails = applyFilters(penaltyDetails)
        Right(GetPenaltyDetailsSuccessResponse(filteredPenaltyDetails))
        
      case Left(GetPenaltyDetailsNoContent) =>
        logger.info(s"$startOfLogMsg - Got a 404 response and no data was found for GetPenaltyDetails call")
        connectorResponse
        
      case Left(GetPenaltyDetailsMalformed) =>
        logger.info(s"$startOfLogMsg - Failed to parse HTTP response into model for $enrolmentKeyInfo")
        connectorResponse
        
      case Left(GetPenaltyDetailsFailureResponse(_)) =>
        logger.error(s"$startOfLogMsg - Unknown status returned from connector for $enrolmentKeyInfo")
        connectorResponse
    }
  }
}

