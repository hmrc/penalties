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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger
import uk.gov.hmrc.http.HttpReads.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsConnector @Inject()(httpClient: HttpClient,
                                             appConfig: AppConfig)
                                            (implicit ec: ExecutionContext) {

  private val headers = Seq("Authorization" -> s"Bearer ${appConfig.eiOutboundBearerToken}",
    "CorrelationId" -> randomUUID().toString, "Environment" -> appConfig.eisEnvironment)

  def getFinancialDetails(vrn: String, dateFrom: LocalDate, dateTo: LocalDate)(implicit hc: HeaderCarrier): Future[GetFinancialDetailsResponse] = {
    httpClient.GET[GetFinancialDetailsResponse](url =
      appConfig.getFinancialDetailsUrlv3(vrn) + appConfig.queryParametersForGetFinancialDetail(dateFrom, dateTo), headers = headers)
  }

  def getFinancialDetailsForAPI(vrn: String,
                                docNumber: Option[String],
                                dateFrom: Option[String],
                                dateTo: Option[String],
                                onlyOpenItems: Boolean,
                                includeStatistical: Boolean,
                                includeLocks: Boolean,
                                calculateAccruedInterest: Boolean,
                                removePOA: Boolean,
                                customerPaymentInformation: Boolean)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val queryParams: String = {
      s"?${docNumber.fold("")(docNumber => s"docNumber=$docNumber&")}${dateFrom.fold("")(dateFrom => s"dateFrom=$dateFrom&")}" +
        s"${dateTo.fold("")(dateTo => s"dateTo=$dateTo&")}onlyOpenItems=$onlyOpenItems&includeStatistical=$includeStatistical" +
        s"&includeLocks=$includeLocks&calculateAccruedInterest=$calculateAccruedInterest&removePOA=$removePOA" +
        s"&customerPaymentInformation=$customerPaymentInformation"
    }
    httpClient.GET[HttpResponse](appConfig.getFinancialDetailsUrlv3(vrn) + queryParams, headers = headers).recover {
      case e: UpstreamErrorResponse => {
        logger.error(s"[GetFinancialDetailsConnector][getFinancialDetailsForAPI] - Received ${e.statusCode}" +
          s" status from API 1811 call - returning status to caller")
        HttpResponse(e.statusCode, e.message)
      }
      case e: Exception => {
        logger.error(s"[GetFinancialDetailsConnector][getFinancialDetailsForAPI] - An unknown exception occurred -" +
          s" returning 500 back to caller - message: ${e.getMessage}")
        HttpResponse(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information.")
      }
    }
  }
}
