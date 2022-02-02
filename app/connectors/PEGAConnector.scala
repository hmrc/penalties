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

package connectors

import config.AppConfig
import connectors.parsers.AppealsParser.{AppealSubmissionResponse, AppealSubmissionResponseReads}
import play.api.http.HeaderNames._
import models.appeals.AppealSubmission
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Logger.logger
import play.api.http.MimeTypes


import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PEGAConnector @Inject()(httpClient: HttpClient,
                              appConfig: AppConfig)(implicit ec: ExecutionContext) {
  def submitAppeal(appealSubmission: AppealSubmission, enrolmentKey: String, isLPP: Boolean, penaltyId: String, correlationId: String): Future[AppealSubmissionResponse] = {

    implicit val hc: HeaderCarrier = headersForEIS(correlationId, appConfig.pegaBearerToken, appConfig.pegaEnvironment)

    httpClient.POST[AppealSubmission, AppealSubmissionResponse](appConfig.getAppealSubmissionURL(enrolmentKey, isLPP, penaltyId), appealSubmission, hc.otherHeaders)
  }

  def headersForEIS(correlationId: String, bearerToken: String, environment: String): HeaderCarrier = {
    val headers = Seq(
      "Environment"      -> environment,
      "CorrelationId" -> correlationId,
      CONTENT_TYPE       -> MimeTypes.JSON,
      ACCEPT             -> MimeTypes.JSON,
      AUTHORIZATION -> s"Bearer $bearerToken"
    )

    logger.debug(s"[PEGAConnector][headersForEIS]EIS send headers $headers")

    HeaderCarrier(otherHeaders = headers)
  }
}
