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

package connectors

import config.AppConfig
import config.featureSwitches.CallAPI1808HIP
import connectors.parsers.AppealsParser.{AppealSubmissionResponse, AppealSubmissionResponseReads, UnexpectedFailure}
import models.appeals.AppealSubmission
import play.api.http.HeaderNames._
import play.api.http.{MimeTypes, Writeable}
import play.api.http.Writeable.writeableOf_JsValue
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{Json, Writes}
import play.api.libs.ws.BodyWritable
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, StringContextOps, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegimePEGAConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def submitAppeal(appealSubmission: AppealSubmission, penaltyNumber: String, correlationId: String): Future[AppealSubmissionResponse] = {
    implicit val hc: HeaderCarrier = headersForEIS(correlationId, appConfig.eiOutboundBearerToken, appConfig.eisEnvironment)
    //    implicit val writes = if (appConfig.isEnabled(CallAPI1808HIP)) AppealSubmission.apiWritesHIP else AppealSubmission.apiWrites
    implicit val writesAppealSubmission: BodyWritable[AppealSubmission] = {
            val writes = if (appConfig.isEnabled(CallAPI1808HIP)) AppealSubmission.apiWritesHIP else AppealSubmission.apiWrites
      BodyWritable(a => Writeable.writeableOf_JsValue.transform(Json.toJson(a)), "application/json")
          }
      val submitAppealUrl = appConfig.getRegimeAgnosticAppealSubmissionUrl(penaltyNumber)

      httpClient
        .post(url"$submitAppealUrl")
        .withBody(appealSubmission)
        .setHeader(hc.otherHeaders: _*)
        .execute[AppealSubmissionResponse]
        //    httpClient
        //      .POST[AppealSubmission, AppealSubmissionResponse](submitAppealUrl, appealSubmission, hc.otherHeaders)
        .recover {
          case e: UpstreamErrorResponse =>
            PagerDutyHelper.logStatusCode("submitAppeal", e.statusCode)(RECEIVED_4XX_FROM_1808_API, RECEIVED_5XX_FROM_1808_API)
            logger.error(
              s"[RegimePEGAConnector][submitAppeal] -" +
                s" Received ${e.statusCode} status from API 1808 call - returning status to caller")
            Left(UnexpectedFailure(e.statusCode, e.message))
          case e: Exception =>
            PagerDutyHelper.log("submitAppeal", UNKNOWN_EXCEPTION_CALLING_1808_API)
            logger.error(
              s"[RegimePEGAConnector][submitAppeal] -" +
                s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
            Left(UnexpectedFailure(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information."))
        }
    }

  def headersForEIS(correlationId: String, bearerToken: String, environment: String): HeaderCarrier = {
    val headers = Seq(
      "Environment"   -> environment,
      "CorrelationId" -> correlationId,
      CONTENT_TYPE    -> MimeTypes.JSON,
      ACCEPT          -> MimeTypes.JSON,
      AUTHORIZATION   -> s"Bearer $bearerToken"
    )
    HeaderCarrier(otherHeaders = headers)
  }
}
