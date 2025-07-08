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

package connectors.getFinancialDetails

import config.AppConfig
import config.featureSwitches.CallAPI1811HIP
import connectors.parsers.getFinancialDetails.FinancialDetailsParser.{GetFinancialDetailsFailureResponse, GetFinancialDetailsResponse}
import models.AgnosticEnrolmentKey
import models.getFinancialDetails.FinancialDetailsRequestModel
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private def headersForHIP: Seq[(String, String)] = Seq(
    "Authorization"                       -> s"Basic ${appConfig.hipAuthorisationToken}",
    appConfig.hipServiceOriginatorIdKeyV1 -> appConfig.hipServiceOriginatorIdV1,
    "CorrelationId"                       -> UUID.randomUUID().toString,
    "X-Originating-System"                -> "MDTP",
    "X-Receipt-Date"                      -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
    "X-Transmitting-System"               -> "HIP"
  )
  private def headersForIF: Seq[(String, String)] = Seq(
    "Authorization" -> s"Bearer ${appConfig.eiOutboundBearerToken}",
    "CorrelationId" -> UUID.randomUUID().toString,
    "Environment"   -> appConfig.eisEnvironment
  )

  def getFinancialDetails(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[GetFinancialDetailsResponse] = {

    val url = appConfig.getRegimeFinancialDetailsUrl(enrolmentKey.id)

    if (appConfig.isEnabled(CallAPI1811HIP)) {
      val bodyForHIP = Json.obj(
        "taxRegime" -> enrolmentKey.regime.value,
        "taxpayerInformation" -> Json.obj(
          "idType"   -> enrolmentKey.idType.value,
          "idNumber" -> enrolmentKey.id.value
        )
      )
      httpClient.POST[JsObject, GetFinancialDetailsResponse](url, bodyForHIP, headersForHIP).recover(handleErrorResponse)
    } else {
      val queryParameters: Seq[(String, String)] = appConfig.queryParametersForGetFinancialDetailsMap ++ appConfig.addDateRangeQueryParametersMap()
      httpClient.GET[GetFinancialDetailsResponse](url, queryParameters, headersForIF).recover(handleErrorResponse)
    }

  }

  private def handleErrorResponse: PartialFunction[Throwable, GetFinancialDetailsResponse] = {
    case e: UpstreamErrorResponse =>
      PagerDutyHelper.logStatusCode("getFinancialDetails", e.statusCode)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
      logger.error(
        s"[FinancialDetailsConnector][getFinancialDetails] Received ${e.statusCode} from API 1811 call - returning status to caller. Error: ${e.getMessage()}")
      Left(GetFinancialDetailsFailureResponse(e.statusCode))
    case e: Exception =>
      PagerDutyHelper.log("getFinancialDetails", UNKNOWN_EXCEPTION_CALLING_1811_API)
      logger.error(s"[FinancialDetailsConnector][getFinancialDetails] An unknown exception occurred - returning 500 back to caller: ${e.getMessage}")
      Left(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))
  }

  // This is a call from vat-api, not from a frontend
  def getFinancialDetailsForAPI(enrolmentKey: AgnosticEnrolmentKey, requestBody: FinancialDetailsRequestModel)(implicit
      hc: HeaderCarrier): Future[HttpResponse] = {

    val url = appConfig.getRegimeFinancialDetailsUrl(enrolmentKey.id)

    if (appConfig.isEnabled(CallAPI1811HIP)) {
      val bodyForHIP = requestBody.toJsonRequest(enrolmentKey)
      httpClient.POST[JsObject, HttpResponse](url, bodyForHIP, headersForHIP).recover(handleErrorResponseForAPICall)
    } else {
      val queryParams = requestBody.toJsonRequestQueryParamsMap
      httpClient.GET[HttpResponse](url, queryParams, headersForIF).recover(handleErrorResponseForAPICall)
    }
  }

  private def handleErrorResponseForAPICall: PartialFunction[Throwable, HttpResponse] = {
    case e: UpstreamErrorResponse =>
      logger.error(
        s"[FinancialDetailsConnector][getFinancialDetailsForAPI] - Received ${e.statusCode} status from API 1811 call - returning status to caller")
      HttpResponse(e.statusCode, e.message)
    case e: Exception =>
      PagerDutyHelper.log("getFinancialDetailsForAPI", UNKNOWN_EXCEPTION_CALLING_1811_API)
      logger.error("------------- e"  + e)
      logger.error(
        s"[FinancialDetailsConnector][getFinancialDetailsForAPI] - An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
      HttpResponse(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information.")
  }

}
