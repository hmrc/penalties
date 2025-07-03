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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import models.AgnosticEnrolmentKey
import play.api.libs.json.{JsObject, Json}

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

class FinancialDetailsConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private def headersForIF: Seq[(String, String)] = Seq(
    "Authorization" -> s"Bearer ${appConfig.eiOutboundBearerToken}",
    "CorrelationId" -> UUID.randomUUID().toString,
    "Environment"   -> appConfig.eisEnvironment
  )
  private def headersForHIP: Seq[(String, String)] = Seq(
    "Authorization"                       -> s"Basic ${appConfig.hipAuthorisationToken}",
    appConfig.hipServiceOriginatorIdKeyV1 -> appConfig.hipServiceOriginatorIdV1,
    "CorrelationId"                       -> UUID.randomUUID().toString,
    "X-Originating-System"                -> "MDTP",
    "X-Receipt-Date"                      -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
    "X-Transmitting-System"               -> "HIP"
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
      val queryParameters = appConfig.queryParametersForGetFinancialDetails + appConfig.addDateRangeQueryParameters()
      val fullUrlForIF    = url + queryParameters
      httpClient.GET[GetFinancialDetailsResponse](fullUrlForIF, headersForIF).recover(handleErrorResponse)
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
  def getFinancialDetailsForAPI(enrolmentKey: AgnosticEnrolmentKey,
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

    val url = appConfig.getRegimeFinancialDetailsUrl(enrolmentKey.id)

    if (appConfig.isEnabled(CallAPI1811HIP)) {
      val body: JsObject = Json.obj(
        "taxRegime" -> enrolmentKey.regime.value,
        "taxpayerInformation" -> Json.obj(
          "idType"   -> enrolmentKey.idType.value,
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
            "dateTo"   -> dateTo
          ),
          "includeClearedItems"     -> includeClearedItems,
          "includeStatisticalItems" -> includeStatisticalItems,
          "includePaymentOnAccount" -> includePaymentOnAccount
        ),
        "dataEnrichment" -> Json.obj(
          "addRegimeTotalisation"      -> addRegimeTotalisation,
          "addLockInformation"         -> addLockInformation,
          "addPenaltyDetails"          -> addPenaltyDetails,
          "addPostedInterestDetails"   -> addPostedInterestDetails,
          "addAccruingInterestDetails" -> addAccruingInterestDetails
        )
      )
      httpClient.POST[JsObject, HttpResponse](url, body, headersForHIP).recover(handleErrorResponseForAPICall)
    } else {
      val params = Seq(
        "searchType"                 -> searchType,
        "searchItem"                 -> searchItem,
        "dateType"                   -> dateType,
        "dateFrom"                   -> dateFrom,
        "dateTo"                     -> dateTo,
        "includeClearedItems"        -> includeClearedItems,
        "includeStatisticalItems"    -> includeStatisticalItems,
        "includePaymentOnAccount"    -> includePaymentOnAccount,
        "addRegimeTotalisation"      -> addRegimeTotalisation,
        "addLockInformation"         -> addLockInformation,
        "addPenaltyDetails"          -> addPenaltyDetails,
        "addPostedInterestDetails"   -> addPostedInterestDetails,
        "addAccruingInterestDetails" -> addAccruingInterestDetails
      )
      val queryParams: String =
        params.foldLeft("?")((prevString, paramToValue) => prevString + paramToValue._2.fold("")(param => s"${paramToValue._1}=$param&")).dropRight(1)
      val fullUrlForIF = url + queryParams
      httpClient.GET[HttpResponse](fullUrlForIF, headersForIF).recover(handleErrorResponseForAPICall)
    }

  }

  private def handleErrorResponseForAPICall: PartialFunction[Throwable, HttpResponse] = {
    case e: UpstreamErrorResponse =>
      logger.error(
        s"[FinancialDetailsConnector][getFinancialDetailsForAPI] - Received ${e.statusCode} status from API 1811 call - returning status to caller")
      HttpResponse(e.statusCode, e.message)
    case e: Exception =>
      PagerDutyHelper.log("getFinancialDetailsForAPI", UNKNOWN_EXCEPTION_CALLING_1811_API)
      logger.error(
        s"[FinancialDetailsConnector][getFinancialDetailsForAPI] - An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
      HttpResponse(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information.")
  }

}

object FinancialDetailsConnector {

  def buildHipRequestForApiBody(enrolmentKey: AgnosticEnrolmentKey,
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
                                addAccruingInterestDetails: Option[Boolean]): JsObject = {
    val baseBody: JsObject = Json.obj(
      "taxRegime" -> enrolmentKey.regime.value,
      "taxpayerInformation" -> Json.obj(
        "idType"   -> enrolmentKey.idType.value,
        "idNumber" -> enrolmentKey.id.value
      )
    )
    val targetedSearchObj: Option[JsObject] = for {
      sType <- searchType
      sItem <- searchItem
    } yield Json.obj(
      "targetedSearch" -> Json.obj(
        "searchType" -> sType,
        "searchItem" -> sItem
      )
    )
    val selectionCriteriaObj: Option[JsObject] = {
      val dateRangeObj: Option[JsObject] = for {
        dType    <- dateType
        dFrom <- dateFrom
        dTo        <- dateTo
      } yield Json.obj(
        "dateRange" -> Json.obj(
          "dateType"      -> dType,
          "dateFrom"  -> dFrom,
          "dateTo"  -> dTo
        )
      )
      for {
        clearedItems <- includeClearedItems
        statisticalItems <- includeStatisticalItems
        paymentOnAccount <- includePaymentOnAccount
      } yield {
        val innerJson = Json.obj(
          "includeClearedItems" -> clearedItems,
          "includeStatisticalItems" -> statisticalItems,
          "includePaymentOnAccount" -> paymentOnAccount
        ) ++ dateRangeObj.getOrElse(Json.obj())
        Json.obj("selectionCriteria" -> innerJson)
      }
    }
    val dataEnrichmentObj: Option[JsObject] = for {
      regimeTotalisation    <- addRegimeTotalisation
      lockInformation <- addLockInformation
      penaltyDetails        <- addPenaltyDetails
      postedInterestDetails        <- addPostedInterestDetails
      accruingInterestDetails        <- addAccruingInterestDetails
    } yield Json.obj(
      "dataEnrichment" -> Json.obj(
        "addRegimeTotalisation"      -> regimeTotalisation,
        "addLockInformation"  -> lockInformation,
        "addPenaltyDetails"  -> penaltyDetails,
        "addPostedInterestDetails"  -> postedInterestDetails,
        "addAccruingInterestDetails"  -> accruingInterestDetails
      )
    )

    def combine(base: JsObject, extras: Option[JsObject]*): JsObject =
      extras.foldLeft(base)(_ ++ _.getOrElse(Json.obj()))

    combine(baseBody, targetedSearchObj, selectionCriteriaObj, dataEnrichmentObj)
  }

}
