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

package connectors.getFinancialDetails

import config.AppConfig
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser.{GetFinancialDetailsFailureResponse, GetFinancialDetailsResponse}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsConnector @Inject()(httpClient: HttpClient,
                                             appConfig: AppConfig)
                                            (implicit ec: ExecutionContext) {

  private def headers: Seq[(String, String)] = Seq(
    "Authorization" -> s"Bearer ${appConfig.eiOutboundBearerToken}",
    "CorrelationId" -> randomUUID().toString,
    "Environment" -> appConfig.eisEnvironment
  )


  def getFinancialDetails(vrn: String, overridingParameters: Option[String])(implicit hc: HeaderCarrier): Future[GetFinancialDetailsResponse] = {
    val parameters = overridingParameters.fold(appConfig.queryParametersForGetFinancialDetails + appConfig.addDateRangeQueryParameters())(_ + appConfig.addDateRangeQueryParameters())
    httpClient.GET[GetFinancialDetailsResponse](url =
      appConfig.getFinancialDetailsUrl(vrn) + parameters, headers = headers).recover {
      case e: UpstreamErrorResponse => {
        PagerDutyHelper.logStatusCode("getFinancialDetails", e.statusCode)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
        logger.error(s"[GetFinancialDetailsConnector][getFinancialDetails] - Received ${e.statusCode} status from API 1811 call - returning status to caller")
        Left(GetFinancialDetailsFailureResponse(e.statusCode))
      }
      case e: Exception => {
        PagerDutyHelper.log("getFinancialDetails", UNKNOWN_EXCEPTION_CALLING_1811_API)
        logger.error(s"[GetFinancialDetailsConnector][getFinancialDetails] - An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
        Left(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  def getFinancialDetailsForAPI(vrn: String,
                                searchType: Option[String],
                                searchItem: Option[String],
                                dateType: Option[String],
                                dateFrom: Option[String],
                                dateTo: Option[String],
                                includeClearedItems: Option[Boolean],
                                includeStatisticalItems: Option[Boolean],
                                includePaymentOnAccount: Option[Boolean],
                                addRegimeTotalisation: Option[Boolean],
                                addLockInformation: Option[Boolean],
                                addPenaltyDetails: Option[Boolean],
                                addPostedInterestDetails: Option[Boolean],
                                addAccruingInterestDetails: Option[Boolean])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val params = Seq("searchType" -> searchType, "searchItem" -> searchItem, "dateType" -> dateType, "dateFrom" -> dateFrom, "dateTo" -> dateTo, "includeClearedItems" -> includeClearedItems, "includeStatisticalItems" -> includeStatisticalItems, "includePaymentOnAccount" -> includePaymentOnAccount, "addRegimeTotalisation" -> addRegimeTotalisation,
      "addLockInformation" -> addLockInformation, "addPenaltyDetails" -> addPenaltyDetails, "addPostedInterestDetails" -> addPostedInterestDetails, "addAccruingInterestDetails" -> addAccruingInterestDetails)
    val queryParams: String = params.foldLeft("?")((prevString, paramToValue) => prevString + paramToValue._2.fold("")(param => s"${paramToValue._1}=$param&")).dropRight(1)
    httpClient.GET[HttpResponse](appConfig.getFinancialDetailsUrl(vrn) + queryParams, headers = headers).recover {
      case e: UpstreamErrorResponse => {
        logger.error(s"[GetFinancialDetailsConnector][getFinancialDetailsForAPI] - Received ${e.statusCode} status from API 1811 call - returning status to caller")
        HttpResponse(e.statusCode, e.message)
      }
      case e: Exception => {
        PagerDutyHelper.log("getFinancialDetailsForAPI", UNKNOWN_EXCEPTION_CALLING_1811_API)
        logger.error(s"[GetFinancialDetailsConnector][getFinancialDetailsForAPI] - An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
        HttpResponse(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information.")
      }
    }
  }
}
