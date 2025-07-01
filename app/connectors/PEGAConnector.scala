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

package connectors

import config.AppConfig
import connectors.parsers.AppealsParser.{AppealSubmissionResponse, AppealSubmissionResponseReads, UnexpectedFailure}
import models.appeals.AppealSubmission
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PEGAConnector @Inject()(httpClient: HttpClient,
                              appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def submitAppeal(appealSubmission: AppealSubmission, enrolmentKey: String, isLPP: Boolean, penaltyNumber: String, correlationId: String): Future[AppealSubmissionResponse] = {
    implicit val hc: HeaderCarrier = headersForEIS(correlationId, appConfig.eiOutboundBearerToken, appConfig.eisEnvironment)
    httpClient.POST[AppealSubmission, AppealSubmissionResponse](appConfig.getAppealSubmissionURL(enrolmentKey, isLPP, penaltyNumber), appealSubmission, hc.otherHeaders).recover {
      case e: UpstreamErrorResponse => {
        PagerDutyHelper.logStatusCode("submitAppeal", e.statusCode)(RECEIVED_4XX_FROM_1808_API, RECEIVED_5XX_FROM_1808_API)
        logger.error(s"[PEGAConnector][submitAppeal] -" +
          s" Received ${e.statusCode} status from API 1812 call - returning status to caller")
        Left(UnexpectedFailure(e.statusCode, e.message))
      }
      case e: Exception => {
        PagerDutyHelper.log("submitAppeal", UNKNOWN_EXCEPTION_CALLING_1808_API)
        logger.error(s"[PEGAConnector][submitAppeal] -" +
          s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
        Left(UnexpectedFailure(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information."))
      }
    }
  }

  def headersForEIS(correlationId: String, bearerToken: String, environment: String): HeaderCarrier = {
    val headers = Seq(
      "Environment"      -> environment,
      "CorrelationId" -> correlationId,
      CONTENT_TYPE       -> MimeTypes.JSON,
      ACCEPT             -> MimeTypes.JSON,
      AUTHORIZATION -> s"Bearer $bearerToken"
    )
    HeaderCarrier(otherHeaders = headers)
  }
}
