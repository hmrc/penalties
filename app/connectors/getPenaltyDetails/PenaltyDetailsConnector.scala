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
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsResponse}
import models.EnrolmentKey
import models.EnrolmentKey._
import models.TaxRegime.{ITSA, VAT}
import play.api.Configuration
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{DateHelper, PagerDutyHelper}

import java.time.LocalDateTime
import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PenaltyDetailsConnector @Inject()(httpClient: HttpClient,
                                        appConfig: AppConfig)
                                       (implicit ec: ExecutionContext, val config: Configuration) extends FeatureSwitching {

  private def headers: Seq[(String, String)] = {
    val timeMachineDateAsDateTime: LocalDateTime = getTimeMachineDateTime
    Seq(
      "Authorization" -> s"Bearer ${appConfig.eiOutboundBearerToken}",
      "CorrelationId" -> randomUUID().toString,
      "Environment" -> appConfig.eisEnvironment,
      "ReceiptDate" -> timeMachineDateAsDateTime.format(DateHelper.dateTimeFormatter)
    ).filter(_._1.nonEmpty)
  }

  def getPenaltyDetails(enrolmentKey: EnrolmentKey)(implicit hc: HeaderCarrier): Future[GetPenaltyDetailsResponse] = {
    val url: String = appConfig.getRegimePenaltyDetailsUrl(enrolmentKey);
    logger.debug(s"[PenaltyDetailsConnector][getPenaltyDetails][appConfig.getRegimePenaltyDetailsUrl(enrolmentKey)]- Calling GET $url \nHeaders: $headers")
    httpClient.GET[GetPenaltyDetailsResponse](url, Seq.empty[(String, String)], headers).recover {
      case e: UpstreamErrorResponse => {
        PagerDutyHelper.logStatusCode("getPenaltyDetails", e.statusCode)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
        logger.error(s"[PenaltyDetailsConnector][getPenaltyDetails] -" +
          s" Received ${e.statusCode} status from API 1812 call - returning status to caller")
        Left(GetPenaltyDetailsFailureResponse(e.statusCode))
      }
      case e: Exception => {
        PagerDutyHelper.log("getPenaltyDetails", UNKNOWN_EXCEPTION_CALLING_1812_API)
        logger.error(s"[PenaltyDetailsConnector][getPenaltyDetails] -" +
          s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
        Left(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  private def getPenaltyDetailsUrl(enrolmentKey: EnrolmentKey) = enrolmentKey match {
    case EnrolmentKey(VAT, VRN, vrn) => appConfig.getVatPenaltyDetailsUrl + vrn
    case EnrolmentKey(ITSA, NINO, nino) => appConfig.getItsaPenaltyDetailsUrl + nino
    //case EnrolmentKey(CT, UTR, utr) => appConfig.getCtPenaltyDetailsUrl + utr
    case _ => throw new Exception(s"No getPenaltyDetails URL available for $enrolmentKey")
  }

  def getPenaltyDetailsForAPI(enrolmentKey: EnrolmentKey, dateLimit: Option[String])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val queryParam: String = s"${dateLimit.fold("")(dateLimit => s"?dateLimit=$dateLimit")}"
    httpClient.GET[HttpResponse](getPenaltyDetailsUrl(enrolmentKey) + queryParam, headers = headers).recover {
      case e: UpstreamErrorResponse => {
        logger.error(s"[GetPenaltyDetailsConnector][getPenaltyDetailsForAPI] -" +
          s" Received ${e.statusCode} status from API 1812 call - returning status to caller")
        HttpResponse(e.statusCode, e.message)
      }
      case e: Exception => {
        PagerDutyHelper.log("getPenaltyDetailsForAPI", UNKNOWN_EXCEPTION_CALLING_1812_API)
        logger.error(s"[GetPenaltyDetailsConnector][getPenaltyDetailsForAPI] -" +
          s" An unknown exception occurred - returning 500 back to caller - message: ${e.getMessage}")
        HttpResponse(INTERNAL_SERVER_ERROR, "An unknown exception occurred. Contact the Penalties team for more information.")
      }
    }
  }
}
