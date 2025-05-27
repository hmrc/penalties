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
import connectors.parsers.getFinancialDetails.FinancialDetailsParser.{GetFinancialDetailsFailureResponse, GetFinancialDetailsResponse}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import models.AgnosticEnrolmentKey
import play.api.libs.json.{JsObject, Json}

import java.util.UUID

class FinancialDetailsConnector @Inject()(httpClient: HttpClient,
                                          appConfig: AppConfig)
                                         (implicit ec: ExecutionContext) {

  private def headers: Seq[(String, String)] = Seq(
    "Authorization" -> s"Basic ${appConfig.hipAuthorisationToken}",
    appConfig.hipEnvironmentHeader,
    appConfig.hipServiceOriginatorIdKeyV1 -> appConfig.hipServiceOriginatorIdV1,
    "CorrelationId" -> UUID.randomUUID().toString,
    "X-Originating-System" -> "MDTP",
    "X-Receipt-Date" -> java.time.Instant.now.toString,
    "X-Transmitting-System" -> "HIP"
  )

  def getFinancialDetails(enrolmentKey: AgnosticEnrolmentKey
                         )(implicit hc: HeaderCarrier): Future[GetFinancialDetailsResponse] = {

    val url = appConfig.getRegimeFinancialDetailsUrl(enrolmentKey)
    val body = Json.obj(
      "regime" -> enrolmentKey.regime.value,
      "idType" -> enrolmentKey.idType.value,
      "idValue" -> enrolmentKey.id.value
    )

    httpClient.POST[JsObject, GetFinancialDetailsResponse](url, body, headers = headers).recover {
      case e: UpstreamErrorResponse =>
        PagerDutyHelper.logStatusCode("getFinancialDetails", e.statusCode)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
        logger.error(s"[FinancialDetailsConnector][getFinancialDetails] - Received ${e.statusCode} from API 1811 call - returning status to caller")
        Left(GetFinancialDetailsFailureResponse(e.statusCode))
      case e: Exception =>
        PagerDutyHelper.log("getFinancialDetails", UNKNOWN_EXCEPTION_CALLING_1811_API)
        logger.error(s"[FinancialDetailsConnector][getFinancialDetails] - An unknown exception occurred - returning 500 back to caller: ${e.getMessage}")
        Left(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))
    }
  }

  def getFinancialDetailsForAPI(
                                 enrolmentKey: AgnosticEnrolmentKey,
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
                                 addAccruingInterestDetails: Option[Boolean]
                               )(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val body: JsObject = Json.obj(
      "taxRegime" -> enrolmentKey.regime.value,
      "taxpayerInformation" -> Json.obj(
        "idType" -> enrolmentKey.idType.value,
        "idNumber" -> enrolmentKey.id.value
      ),
      "targetedSearch" -> Json.obj(
        "searchType" -> searchType,
        "searchItem" -> searchItem
      ),
      "selectionCriteria" -> Json.obj(
        "dateRange" -> Json.obj(
          "dateType" -> dateType,
          "dateFrom" -> dateFrom,
          "dateTo" -> dateTo
        ),
        "includeClearedItems" -> includeClearedItems,
        "includeStatisticalItems" -> includeStatisticalItems,
        "includePaymentOnAccount" -> includePaymentOnAccount
      ),
      "dataEnrichment" -> Json.obj(
        "addRegimeTotalisation" -> addRegimeTotalisation,
        "addLockInformation" -> addLockInformation,
        "addPenaltyDetails" -> addPenaltyDetails,
        "addPostedInterestDetails" -> addPostedInterestDetails,
        "addAccruingInterestDetails" -> addAccruingInterestDetails
      )
    )

    val url = appConfig.getRegimeFinancialDetailsUrl(enrolmentKey)

    httpClient.POST[JsObject, HttpResponse](url, body, headers).recover {
      case e: UpstreamErrorResponse =>
        logger.error(s"[FinancialDetailsConnector][getFinancialDetailsForAPI] - Received ${e.statusCode} status from API 1811 call - returning status to caller")
        HttpResponse(e.statusCode, e.message)
      case e: Exception =>
        PagerDutyHelper.log("getFinancialDetailsForAPI", UNKNOWN_EXCEPTION_CALLING_1811_API)
        logger.error(s"[FinancialDetailsConnector][getFinancialDetailsForAPI] - An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
        HttpResponse(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information.")
    }
  }
}