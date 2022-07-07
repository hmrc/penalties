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

package connectors.getPenaltyDetails

import config.AppConfig
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.GetPenaltyDetailsResponse
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetPenaltyDetailsConnector @Inject()(httpClient: HttpClient,
                                           appConfig: AppConfig)
                                          (implicit ec: ExecutionContext) {

  private val headers = Seq("Authorization" -> s"Bearer ${appConfig.eiOutboundBearerToken}",
    "CorrelationId" -> randomUUID().toString, "Environment" -> appConfig.eisEnvironment)

  def getPenaltyDetails(vrn: String)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    val url = appConfig.getPenaltyDetailsUrl + vrn
    logger.debug(s"[GetPenaltyDetailsConnector][getPenaltyDetails] - Calling GET $url \nHeaders: $headers")
    httpClient.GET[GetPenaltyDetailsResponse](url, Seq.empty[(String, String)], headers)
  }

  def getPenaltyDetailsForAPI(vrn: String, dateLimit: Option[String])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val queryParam: String = s"${dateLimit.fold("")(dateLimit => s"?dateLimit=$dateLimit")}"
    httpClient.GET[HttpResponse](appConfig.getPenaltyDetailsUrl + vrn + queryParam, headers = headers).recover {
      case e: UpstreamErrorResponse => {
        logger.error(s"[GetPenaltyDetailsConnector][getPenaltyDetailsForAPI] -" +
          s" Received ${e.statusCode} status from API 1812 call - returning status to caller")
        HttpResponse(e.statusCode, e.message)
      }
      case e: Exception => {
        logger.error(s"[GetPenaltyDetailsConnector][getPenaltyDetailsForAPI] -" +
          s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
        HttpResponse(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information.")
      }
    }
  }
}
