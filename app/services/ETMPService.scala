/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.ETMPConnector
import connectors.parsers.ETMPPayloadParser.{GetETMPPayloadFailureResponse, GetETMPPayloadMalformed, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import models.ETMPPayload
import play.api.Logger.logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ETMPService @Inject()(etmpConnector: ETMPConnector)
                           (implicit ec: ExecutionContext) {

  def getPenaltyDataFromETMPForEnrolment(enrolmentKey: String)(implicit hc: HeaderCarrier): Future[Option[ETMPPayload]] = {
    val startOfLogMsg: String = "[ETMPService][getPenaltyDataFromETMPForEnrolment]"
    etmpConnector.getPenaltiesDataForEnrolmentKey(enrolmentKey).map {
        case Right(_@GetETMPPayloadSuccessResponse(payload)) => {
          logger.debug(s"$startOfLogMsg - Got a success response from the connector. Parsed model: $payload")
          Some(payload)
        }
        case Left(GetETMPPayloadNoContent) => {
          logger.info(s"$startOfLogMsg - No content returned from ETMP.")
          None
        }
        case Left(GetETMPPayloadMalformed) => {
          logger.info(s"$startOfLogMsg - Failed to parse HTTP response into model.")
          None
        }
        case Left(GetETMPPayloadFailureResponse(_)) => {
          logger.info(s"$startOfLogMsg - Unknown status returned from connector.")
          None
        }
    }
  }
}
