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

import connectors.parsers.ETMPPayloadParser.{ETMPPayloadResponse,
  GetETMPPayloadFailureResponse, GetETMPPayloadMalformed, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import connectors.{AppealsConnector, ETMPConnector}
import models.ETMPPayload
import models.appeals.AppealSubmission
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Logger.logger

import java.time.LocalDate
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

  def isMultiplePenaltiesInSamePeriod(penaltyId: String, enrolmentKey: String, isLPP: Boolean)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val startOfLogMsg: String = "[ETMPService][isMultiplePenaltiesInSamePeriod]"
    etmpConnector.getPenaltiesDataForEnrolmentKey(enrolmentKey).map {
      handleConnectorResponse(_)._1.fold({
        false
      })(
        penaltyData => {
          val optPenaltyPeriod = getPeriodForPenalty(penaltyData, isLPP, penaltyId)
          if(optPenaltyPeriod.isEmpty) {
            logger.error(s"$startOfLogMsg - Could not find period for penalty - returning false")
            false
          } else {
            val penaltyPeriod = optPenaltyPeriod.get
            logger.debug(s"$startOfLogMsg - Found period for penalty - start : ${penaltyPeriod._1} to end : ${penaltyPeriod._2}")
            val isOtherLSPInSamePeriod = penaltyData.penaltyPoints.exists(
              penalty => penalty.period.exists(
                period => period.startDate.toLocalDate == penaltyPeriod._1 && period.endDate.toLocalDate == penaltyPeriod._2 && penalty.id != penaltyId)
            )
            val isOtherLPPInSamePeriod = penaltyData.latePaymentPenalties.exists(
              _.exists(
                lpp => lpp.period.startDate.toLocalDate == penaltyPeriod._1 && lpp.period.endDate.toLocalDate == penaltyPeriod._2 && lpp.id != penaltyId
              )
            )
            logger.debug(s"$startOfLogMsg - is other LSP in same period: $isOtherLSPInSamePeriod & is other LPP in same period: $isOtherLPPInSamePeriod")
            isOtherLSPInSamePeriod || isOtherLPPInSamePeriod
          }
        }
      )
    }
  }

  private def getPeriodForPenalty(payload: ETMPPayload, isLPP: Boolean, penaltyId: String): Option[(LocalDate, LocalDate)] = {
    if (isLPP) {
      val optPenalty = payload.latePaymentPenalties.flatMap(_.find(_.id == penaltyId))
      optPenalty.map(penalty => (penalty.period.startDate.toLocalDate, penalty.period.endDate.toLocalDate))
    } else {
      val optPenalty = payload.penaltyPoints.find(_.id == penaltyId)
      optPenalty.flatMap(_.period.map(period => (period.startDate.toLocalDate, period.endDate.toLocalDate)))
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

  def submitAppeal(appealSubmission: AppealSubmission,
                   enrolmentKey: String, isLPP: Boolean, penaltyId: String)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {appealsConnector.submitAppeal(appealSubmission, enrolmentKey, isLPP, penaltyId)
  }
}
