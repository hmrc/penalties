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

package connectors.v3.getPenaltyDetails

import config.AppConfig
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsReads, GetPenaltyDetailsResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Logger.logger

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetPenaltyDetailsConnector @Inject()(httpClient: HttpClient,
                                           appConfig: AppConfig)
                                          (implicit ec: ExecutionContext) {

  def getPenaltyDetails(vrn: String)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    val eisHeaders = Seq("Authorization" -> s"Bearer ${appConfig.eiOutboundBearerToken}",
      "CorrelationId" -> randomUUID().toString, "Environment" -> appConfig.eisEnvironment)

    val url = appConfig.getPenaltyDetailsUrl + vrn

    logger.debug(s"[GetPenaltyDetailsConnector][getPenaltyDetails] - Calling GET $url \nHeaders: $eisHeaders")
    httpClient.GET[GetPenaltyDetailsResponse](url, Seq.empty[(String, String)] ,eisHeaders)
  }
}
