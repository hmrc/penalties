/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.submitAppeal

import config.AppConfig
import connectors.parsers.submitAppeal.AppealsParser.{AppealSubmissionResponse, UnexpectedFailure}
import connectors.parsers.submitAppeal.HIPAppealParser.HIPAppealSubmissionResponseReads
import models.appeals.{AppealSubmission, AppealSubmissionRequest}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys.{RECEIVED_4XX_FROM_1808_API, RECEIVED_5XX_FROM_1808_API, UNKNOWN_EXCEPTION_CALLING_1808_API}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HIPSubmitAppealConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def submitAppeal(appealSubmission: AppealSubmission, penaltyNumber: String, correlationId: String)(implicit
      headerCarrier: HeaderCarrier): Future[AppealSubmissionResponse] = {

    val hc: HeaderCarrier       = headersForHIP(correlationId)
    val appealSubmissionRequest = AppealSubmissionRequest(appealSubmission, penaltyNumber)

    httpClient
      .POST[AppealSubmissionRequest, AppealSubmissionResponse](
        appConfig.getAppealSubmissionHipUrl,
        appealSubmissionRequest,
        hc.otherHeaders
      )(AppealSubmissionRequest.apiWrites, HIPAppealSubmissionResponseReads, hc, ec)
      .recover {
        case e: UpstreamErrorResponse =>
          PagerDutyHelper.logStatusCode("submitAppeal", e.statusCode)(RECEIVED_4XX_FROM_1808_API, RECEIVED_5XX_FROM_1808_API)
          logger.error(
            s"[HIPConnector][submitAppeal] -" +
              s" Received ${e.statusCode} status from API 1808 call - returning status to caller")
          Left(UnexpectedFailure(e.statusCode, e.message))
        case e: Exception =>
          PagerDutyHelper.log("submitAppeal", UNKNOWN_EXCEPTION_CALLING_1808_API)
          logger.error(
            s"[HIPConnector][submitAppeal] -" +
              s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
          Left(UnexpectedFailure(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information."))
      }
  }

  private val CORRELATION_HEADER: String   = "CorrelationId"
  private val AUTHORIZATION_HEADER: String = "Authorization"

  private def headersForHIP(correlationId: String)(implicit headerCarrier: HeaderCarrier): HeaderCarrier = {
    val headers = Seq(
      CORRELATION_HEADER   -> correlationId,
      AUTHORIZATION_HEADER -> s"Basic ${appConfig.hipAuthorisationToken}"
    )
    headerCarrier.copy(authorization = None, otherHeaders = headers)
  }

}
