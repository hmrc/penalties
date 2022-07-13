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
import connectors.parsers.ComplianceParser.CompliancePayloadResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ComplianceConnector @Inject()(httpClient: HttpClient,
                                   appConfig: AppConfig)
                                   (implicit ec: ExecutionContext){
  def getComplianceData(identifier: String, fromDate: String, toDate: String)(implicit hc: HeaderCarrier): Future[CompliancePayloadResponse] = {
    val environmentHeader: String = appConfig.eisEnvironment
    val desHeaders: Seq[(String, String)] = Seq(
      "Environment" -> environmentHeader,
      "Authorization" -> s"Bearer ${appConfig.desBearerToken}"
    )
    val url: String = appConfig.getComplianceData(identifier, fromDate, toDate)
    logger.debug(s"[ComplianceConnector][getComplianceData] - Calling GET $url with headers: $desHeaders")
    httpClient.GET[CompliancePayloadResponse](url, headers = desHeaders)
  }
}
