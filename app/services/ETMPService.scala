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

import connectors.{AppealsConnector, ETMPConnector}
import connectors.parsers.ETMPPayloadParser.{ETMPPayloadResponse, GetETMPPayloadFailureResponse, GetETMPPayloadMalformed, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import models.ETMPPayload
import models.appeals.AppealSubmission
import utils.Logger.logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ETMPService @Inject()(etmpConnector: ETMPConnector,
                            appealsConnector: AppealsConnector)
                           (implicit ec: ExecutionContext) {

  def getPenaltyDataFromETMPForEnrolment(enrolmentKey: String)(implicit hc: HeaderCarrier): Future[(Option[ETMPPayload], ETMPPayloadResponse) ] = {
    implicit val startOfLogMsg: String = "[ETMPService][getPenaltyDataFromETMPForEnrolment]"
    etmpConnector.getPenaltiesDataForEnrolmentKey(enrolmentKey).map {
      handleConnectorResponse(_)
    }
  }

  private def handleConnectorResponse(connectorResponse: ETMPPayloadResponse)(implicit startOfLogMsg: String): (Option[ETMPPayload], ETMPPayloadResponse) = {
    connectorResponse match {
      case res@Right(_@GetETMPPayloadSuccessResponse(payload)) =>
        logger.debug(s"$startOfLogMsg - Got a success response from the connector. Parsed model: $payload")
        (Some(payload), res)
      case res@Left(GetETMPPayloadNoContent) =>
        logger.info(s"$startOfLogMsg - No content returned from ETMP.")
        (None, res)
      case res@Left(GetETMPPayloadMalformed) =>
        logger.info(s"$startOfLogMsg - Failed to parse HTTP response into model.")
        (None, res)
      case res@Left(GetETMPPayloadFailureResponse(_)) =>
        logger.error(s"$startOfLogMsg - Unknown status returned from connector.")
        (None, res)
    }
  }

  def submitAppeal(appealSubmission: AppealSubmission)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
    appealsConnector.submitAppeal(appealSubmission)
  }
}
