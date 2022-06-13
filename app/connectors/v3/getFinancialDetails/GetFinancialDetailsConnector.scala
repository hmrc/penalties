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

package connectors.v3.getFinancialDetails

import java.time.LocalDate
import java.util.UUID.randomUUID

import config.AppConfig
import connectors.parsers.v3.getFinancialDetails.GetFinancialDetailsParser.GetFinancialDetailsResponse
import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsConnector @Inject()(httpClient: HttpClient,
                                             appConfig: AppConfig)
                                            (implicit ec: ExecutionContext) {

  def getFinancialDetails(vrn: String, dateFrom: LocalDate, dateTo: LocalDate)(implicit hc: HeaderCarrier): Future[GetFinancialDetailsResponse] = {
    val headers = Seq("Authorization" -> s"Bearer ${appConfig.eiOutboundBearerToken}",
      "CorrelationId" -> randomUUID().toString, "Enviroment" -> appConfig.eisEnvironment)

    httpClient.GET[GetFinancialDetailsResponse](url =
      appConfig.getFinancialDetailsUrl + vrn + appConfig.queryParametersForGetFinancialDetail(dateFrom, dateTo), headers = headers)
  }
}
