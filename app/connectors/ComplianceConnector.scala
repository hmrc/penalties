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
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ComplianceConnector @Inject()(httpClient: HttpClient,
                                   appConfig: AppConfig)
                                   (implicit ec: ExecutionContext){
  def getComplianceData(identifier: String, fromDate: String, toDate: String)(implicit hc: HeaderCarrier): Future[CompliancePayloadResponse] = {
    val environmentHeader: String = appConfig.eisEnvironment
    val bearerToken: String = appConfig.eiOutboundBearerToken
    val headers: Seq[(String, String)] = Seq(
      "Environment" -> environmentHeader
    )
    val hcForDes: HeaderCarrier = hc.copy(authorization = Some(Authorization(bearerToken))).withExtraHeaders(headers: _*)
    httpClient.GET[CompliancePayloadResponse](appConfig.getComplianceData(identifier, fromDate, toDate))(implicitly, hcForDes, implicitly)
  }
}
