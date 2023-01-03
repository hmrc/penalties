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
import connectors.parsers.ComplianceParser.{CompliancePayloadFailureResponse, CompliancePayloadResponse}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

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
    httpClient.GET[CompliancePayloadResponse](url, headers = desHeaders).recover {
      case e: UpstreamErrorResponse => {
        PagerDutyHelper.logStatusCode("getComplianceData", e.statusCode)(RECEIVED_4XX_FROM_1330_API, RECEIVED_5XX_FROM_1330_API)
        logger.error(s"[ComplianceConnector][] -" +
          s" Received ${e.statusCode} status from API 1330 call - returning status to caller")
        Left(CompliancePayloadFailureResponse(e.statusCode))
      }
      case e: Exception => {
        PagerDutyHelper.log("getComplianceData", UNKNOWN_EXCEPTION_CALLING_1330_API)
        logger.error(s"[ComplianceConnector][getComplianceData] -" +
          s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
        Left(CompliancePayloadFailureResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
