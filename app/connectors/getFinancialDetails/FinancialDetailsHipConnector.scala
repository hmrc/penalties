/*
 * Copyright 2025 HM Revenue & Customs
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
import connectors.parsers.getFinancialDetails.HIPFinancialDetailsParser.{HIPFinancialDetailsFailureResponse, HIPFinancialDetailsResponse}
import models.AgnosticEnrolmentKey
import models.getFinancialDetails.FinancialDetailsRequestModel
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsHipConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private def headers: Seq[(String, String)] = Seq(
    "Authorization"                       -> s"Basic ${appConfig.hipAuthorisationToken}",
    appConfig.hipServiceOriginatorIdKeyV1 -> appConfig.hipServiceOriginatorIdV1,
    "CorrelationId"                       -> UUID.randomUUID().toString,
    "X-Originating-System"                -> "MDTP",
    "X-Receipt-Date"                      -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
    "X-Transmitting-System"               -> "HIP"
  )

  def getFinancialDetails(enrolmentKey: AgnosticEnrolmentKey, includeClearedItems: Boolean)(implicit
      hc: HeaderCarrier): Future[HIPFinancialDetailsResponse] = {
    // Without clearedItems when from RegimePenaltiesFrontendService.handleAndCombineGetFinancialDetailsData
    val url           = appConfig.getFinancialDetailsHipUrl
    val body          = appConfig.baseFinancialDetailsRequestModel.copy(includeClearedItems = Some(includeClearedItems)).toJsonRequest(enrolmentKey)
    val hcWithoutAuth = hc.copy(authorization = None)

    httpClient
      .POST[JsObject, HIPFinancialDetailsResponse](url, body, headers)(implicitly, implicitly, hcWithoutAuth, implicitly)
      .recover(handleErrorResponse)
  }

  private def handleErrorResponse: PartialFunction[Throwable, HIPFinancialDetailsResponse] = {
    case e: UpstreamErrorResponse => // TODO do these make the parser errors redundant, plus confusion from 404 -> 422?
      PagerDutyHelper.logStatusCode("getFinancialDetails", e.statusCode)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
      logger.error(
        s"[FinancialDetailsConnector][getFinancialDetails] Received ${e.statusCode} from API 1811 call - returning status to caller. Error: ${e.getMessage()}")
      Left(HIPFinancialDetailsFailureResponse(e.statusCode))
    case e: Exception =>
      PagerDutyHelper.log("getFinancialDetails", UNKNOWN_EXCEPTION_CALLING_1811_API)
      logger.error(s"[FinancialDetailsConnector][getFinancialDetails] An unknown exception occurred - returning 500 back to caller: ${e.getMessage}")
      Left(HIPFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))
  }

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
    val url = appConfig.getFinancialDetailsHipUrl
    val body = FinancialDetailsRequestModel(
      searchType,
      searchItem,
      dateType,
      dateFrom,
      dateTo,
      includeClearedItems,
      includeStatisticalItems,
      includePaymentOnAccount,
      addRegimeTotalisation,
      addLockInformation,
      addPenaltyDetails,
      addPostedInterestDetails,
      addAccruingInterestDetails
    ).toJsonRequest(enrolmentKey)
    val hcWithoutAuth = hc.copy(authorization = None)

    httpClient
      .POST[JsObject, HttpResponse](url, body, headers)(implicitly, implicitly, hcWithoutAuth, implicitly)
      .recover(handleErrorResponseForAPICall)
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
