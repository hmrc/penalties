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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HIPPenaltyDetailsConnector @Inject() (
    httpClient: HttpClient,
    appConfig: AppConfig
)(implicit ec: ExecutionContext, val config: Configuration)
    extends FeatureSwitching {

implicit val throwingReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
  override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
    if (response.status >= 400) throw UpstreamErrorResponse(response.body, response.status)
    else response
  }
}

  def getPenaltyDetails(
      enrolmentKey: AgnosticEnrolmentKey
  )(implicit hc: HeaderCarrier): Future[HIPPenaltyDetailsResponse] = {
    val url = appConfig.getHIPPenaltyDetailsUrl(enrolmentKey)
    val headerCarrier = hc.copy(authorization = None)
    val headers = buildHeadersV1


    logger.info(s"[getPenaltyDetails] Resolved URL: ${Option(url).getOrElse("null")}")
    logger.debug(s"[getPenaltyDetails] Headers: $headers")
    logger.debug(
      s"[HIPPenaltiesDetailsConnector][getPenaltyDetails][appConfig.getHIPPenaltyDetailsUrl($enrolmentKey)]- Calling GET $url \nHeaders: $headers"
    )

    httpClient
      .GET[HIPPenaltyDetailsResponse](
        url,
        headers = headers
      ).map { response =>
         logger.info(s"[getPenaltyDetails] Successful call to 1812 API")
         response
      }
      .recover {
        case e: UpstreamErrorResponse => {
          PagerDutyHelper.logStatusCode("getPenaltyDetails", e.statusCode)(
            RECEIVED_4XX_FROM_1812_API,
            RECEIVED_5XX_FROM_1812_API
          )
          logger.error(
            s"[HIPPenaltiesDetailsConnector][getPenaltyDetails] -" +
              s" Received ${e.statusCode} status from API 1812 call - returning status to caller"
          )
          Left(HIPPenaltyDetailsFailureResponse(e.statusCode))
        }
        case e: Exception => {
          PagerDutyHelper.log(
            "getPenaltyDetails",
            UNKNOWN_EXCEPTION_CALLING_1812_API
          )
          logger.error(
            s"[HIPPenaltiesDetailsConnector][getPenaltyDetails] -" +
              s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}"
          )
          Left(HIPPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))
        }
      }
  }

  def getPenaltyDetailsForAPI(
      enrolmentKey: AgnosticEnrolmentKey,
      dateLimit: Option[String]
  )(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val headerCarrier = hc.copy(authorization = None)

    val url =
      appConfig.getHIPPenaltyDetailsUrl(enrolmentKey, dateLimit)

 logger.info(s"[getPenaltyDetailsForAPI] Resolved URL: ${Option(url).getOrElse("null")}")
  logger.debug(s"[getPenaltyDetailsForAPI] Headers: ${buildHeadersV1}")

    httpClient.GET[HttpResponse](
      url, headers = buildHeadersV1
      )(throwingReads, implicitly, implicitly).recover {
      case e: UpstreamErrorResponse => {
        logger.error(
          s"[HIPPenaltiesDetailsConnector][getPenaltyDetailsForAPI] -" +
            s" Received ${e.statusCode} status from API 1812 call - returning status to caller"
        )
        HttpResponse(e.statusCode, e.message)
      }
      case e: Exception => {
        PagerDutyHelper
          .log("getPenaltyDetailsForAPI", UNKNOWN_EXCEPTION_CALLING_1812_API)
        logger.error(
          s"[HIPPenaltiesDetailsConnector][getPenaltyDetailsForAPI] -" +
            s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}"
        )
        HttpResponse(
          INTERNAL_SERVER_ERROR,
          "An unknown exception occurred. Contact the Penalties team for more information."
        )
      }
    }
  }

  private val requestIdPattern =
    """.*([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}).*""".r

  private val CorrelationIdHeader: String = "correlationid"
  private val AuthorizationHeader: String = "Authorization"
  private val xOriginatingSystemHeader: String = "X-Originating-System"
  private val xReceiptDateHeader: String = "X-Receipt-Date"
  private val xTransmittingSystemHeader: String = "X-Transmitting-System"
  private val EnvironmentHeader: String = "Environment"

  private def buildHeadersV1(implicit
      hc: HeaderCarrier
  ): Seq[(String, String)] =
    Seq(
      appConfig.hipServiceOriginatorIdKeyV1 -> appConfig.hipServiceOriginatorIdV1,
      CorrelationIdHeader -> UUID.randomUUID().toString,
      AuthorizationHeader -> s"Basic ${appConfig.hipAuthorisationToken}",
      EnvironmentHeader -> appConfig.hipEnvironment,
      xOriginatingSystemHeader -> "MDTP",
      xReceiptDateHeader -> {
        val instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val localDateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC)
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(localDateTime) + "Z"
      },
      xTransmittingSystemHeader -> "HIP"
    )
}