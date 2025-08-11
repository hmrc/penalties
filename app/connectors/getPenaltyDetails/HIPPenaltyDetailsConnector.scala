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

package connectors.getPenaltyDetails

import config.AppConfig
import config.featureSwitches.FeatureSwitching
import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser._
import models.AgnosticEnrolmentKey
import play.api.Configuration
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HIPPenaltyDetailsConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext, val config: Configuration)
    extends FeatureSwitching {

  def getPenaltyDetails(enrolmentKey: AgnosticEnrolmentKey)(implicit hc: HeaderCarrier): Future[HIPPenaltyDetailsResponse] = {
    val url           = appConfig.getHIPPenaltyDetailsUrl(enrolmentKey)
    val hcWithoutAuth = hc.copy(authorization = None)
    val headers       = buildHeadersV1

    logger.info(s"[HIPPenaltiesDetailsConnector][getPenaltyDetails][$enrolmentKey]- Calling GET $url\nHeaders: $headers")

    httpClient
      .get(url"$url")(hcWithoutAuth)
      .setHeader(headers: _*)
      .execute[HIPPenaltyDetailsResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          PagerDutyHelper.logStatusCode("getPenaltyDetails", e.statusCode)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(
            s"[HIPPenaltiesDetailsConnector][getPenaltyDetails] - Received ${e.statusCode} status from API#5329 call " +
              s"- returning status to caller. Error: ${e.getMessage()}")
          Left(HIPPenaltyDetailsFailureResponse(e.statusCode))
        case e: Exception =>
          PagerDutyHelper.log("getPenaltyDetails", UNKNOWN_EXCEPTION_CALLING_1812_API)
          logger.error(
            "[HIPPenaltiesDetailsConnector][getPenaltyDetails] - An unknown exception occurred " +
              s"- returning 500 back to caller - message: ${e.getMessage}")
          Left(HIPPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))
      }
  }

  def getPenaltyDetailsForAPI(enrolmentKey: AgnosticEnrolmentKey, dateLimit: Option[String])(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val url           = appConfig.getHIPPenaltyDetailsUrl(enrolmentKey, dateLimit)
    val hcWithoutAuth = hc.copy(authorization = None)
    val headers       = buildHeadersV1

    val throwingReads: HttpReads[HttpResponse] =
      (_: String, _: String, response: HttpResponse) =>
        if (response.status >= 400) throw UpstreamErrorResponse(response.body, response.status) else response

    logger.info(s"[HIPPenaltiesDetailsConnector][getPenaltyDetails][$enrolmentKey]- Calling GET $url\nHeaders: $headers")

    httpClient
      .get(url"$url")(hcWithoutAuth)
      .setHeader(headers: _*)
      .execute[HttpResponse](throwingReads)
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(
            s"[HIPPenaltiesDetailsConnector][getPenaltyDetailsForAPI] - Received ${e.statusCode} status from API#5329 call " +
              s"- returning status to caller. Error: ${e.getMessage()}")
          HttpResponse(e.statusCode, e.message)
        case e: Exception =>
          PagerDutyHelper.log("getPenaltyDetailsForAPI", UNKNOWN_EXCEPTION_CALLING_1812_API)
          logger.error(
            "[HIPPenaltiesDetailsConnector][getPenaltyDetailsForAPI] - An unknown exception occurred " +
              s"- returning 500 back to caller - message: ${e.getMessage}")
          HttpResponse(
            INTERNAL_SERVER_ERROR,
            "An unknown exception occurred. Contact the Penalties team for more information."
          )
      }
  }

  private val CorrelationIdHeader: String       = "correlationid"
  private val AuthorizationHeader: String       = "Authorization"
  private val xOriginatingSystemHeader: String  = "X-Originating-System"
  private val xReceiptDateHeader: String        = "X-Receipt-Date"
  private val xTransmittingSystemHeader: String = "X-Transmitting-System"
  private val EnvironmentHeader: String         = "Environment"

  private def buildHeadersV1: Seq[(String, String)] =
    Seq(
      appConfig.hipServiceOriginatorIdKeyV1 -> appConfig.hipServiceOriginatorIdV1,
      CorrelationIdHeader                   -> UUID.randomUUID().toString,
      AuthorizationHeader                   -> s"Basic ${appConfig.hipAuthorisationToken}",
      EnvironmentHeader                     -> appConfig.hipEnvironment,
      xOriginatingSystemHeader              -> "MDTP",
      xReceiptDateHeader -> {
        val instant       = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val localDateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC)
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(localDateTime) + "Z"
      },
      xTransmittingSystemHeader -> "HIP"
    )
}
