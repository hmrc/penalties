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

package connectors.parsers.getFinancialDetails

import connectors.parsers.SafeHttpReads
import connectors.parsers.getFinancialDetails.FinancialDetailsParser.{FinancialDetailsFailure, FinancialDetailsFailureResponse, FinancialDetailsMalformed, FinancialDetailsNoContent, FinancialDetailsSuccess, FinancialDetailsSuccessResponse}
import models.failure._
import models.getFinancialDetails.{FinancialDetails, FinancialDetailsHIP}
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

object HIPFinancialDetailsParser {

  type HIPFinancialDetailsResponse = Either[HIPFinancialDetailsFailure, HIPFinancialDetailsSuccess]

  private def handleSuccessResponse(json: JsValue): HIPFinancialDetailsResponse = {
    logger.info(s"[HIPFinancialDetailsReads][read] Success 201 response returned from API#5327")
    json.validate[FinancialDetailsHIP] match {
      case JsSuccess(financialDetails, _) =>
        logger.info(s"[HIPFinancialDetailsReads][read] FinancialDetails successfully validated from success response")
        Right(HIPFinancialDetailsSuccessResponse(financialDetails.financialData))
      case JsError(errors) =>
        PagerDutyHelper.log("HIPFinancialDetailsReads", MALFORMED_RESPONSE_FROM_1811_API)
        logger.error(s"[HIPFinancialDetailsReads][read] Json validation of 201 body failed with errors: $errors")
        Left(HIPFinancialDetailsMalformed)
    }
  }

  private def extractErrorResponseBodyFrom422(json: JsValue): Left[HIPFinancialDetailsFailure, Nothing] = {
    def noDataFound(error: BusinessError): Boolean =
      (error.code == "016" && error.text == "Invalid ID Number") || (error.code == "018" && error.text == "No Data Identified")

    (json \ "errors").validate[BusinessError] match { // 422 a single error is ever returned regardless of the number of mistakes
      case JsSuccess(error, _) if noDataFound(error) =>
        logger.error(s"[HIPFinancialDetailsReads][read] - Error: ID number did not match any data")
        Left(HIPFinancialDetailsNoContent)
      case JsSuccess(error, _) =>
        logger.error(s"[HIPFinancialDetailsReads][read] - 422 Error with code: ${error.code} - ${error.text}")
        Left(HIPFinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
      case _ =>
        PagerDutyHelper.log("HIPFinancialDetailsReads", INVALID_JSON_RECEIVED_FROM_1811_API)
        logger.error(s"[HIPFinancialDetailsReads][read] - Unable to parse 422 error body to expected format. Error: $json")
        Left(HIPFinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
    }
  }

  sealed trait HIPFinancialDetailsFailure {
    val toIFFailureResponse: FinancialDetailsFailure
  }

  sealed trait HIPFinancialDetailsSuccess {
    val toIFSuccessResponse: FinancialDetailsSuccess
  }

  case class HIPFinancialDetailsSuccessResponse(financialDetails: FinancialDetails) extends HIPFinancialDetailsSuccess {
    override val toIFSuccessResponse: FinancialDetailsSuccess = FinancialDetailsSuccessResponse(financialDetails)
  }

  case class HIPFinancialDetailsFailureResponse(status: Int) extends HIPFinancialDetailsFailure {
    override val toIFFailureResponse: FinancialDetailsFailureResponse = FinancialDetailsFailureResponse(status)
  }

  implicit object HIPFinancialDetailsReads extends HttpReads[HIPFinancialDetailsResponse] with SafeHttpReads {
    override def read(method: String, url: String, response: HttpResponse): HIPFinancialDetailsResponse = {
      val status = response.status
      val body = Option(response.body).getOrElse("")
      status match {
        case CREATED =>
          parseJson(body) match {
            case Some(json) => handleSuccessResponse(json)
            case None =>
              logger.warn("[HIPFinancialDetailsReads][read] Invalid JSON in success response")
              Left(HIPFinancialDetailsMalformed)
          }
        case UNPROCESSABLE_ENTITY =>
          parseJson(body) match {
            case Some(json) => extractErrorResponseBodyFrom422(json)
            case None =>
              logger.info("[HIPFinancialDetailsReads][read] Non-JSON 422 response received")
              Left(HIPFinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
          }
        case BAD_REQUEST | UNAUTHORIZED | FORBIDDEN | NOT_FOUND |
             UNSUPPORTED_MEDIA_TYPE  =>
          PagerDutyHelper.log("HIPFinancialDetailsReads", RECEIVED_4XX_FROM_1812_API)
          logger.info(s"[HIPFinancialDetailsReads][read] Downstream error status=$status")
          handleErrorResponseSafe(response)
        case INTERNAL_SERVER_ERROR |
             SERVICE_UNAVAILABLE | BAD_GATEWAY | GATEWAY_TIMEOUT =>
          logger.error(s"[HIPFinancialDetailsReads][read] Downstream error status=$status")
          handleErrorResponseSafe(response)
        case _ =>
          PagerDutyHelper.logStatusCode("HIPFinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(
            s"[HIPFinancialDetailsReads][read] Received unexpected response from FinancialDetails, status code: $status and body: $body")
          Left(HIPFinancialDetailsFailureResponse(status))
      }
    }

    private def handleErrorResponseSafe(response: HttpResponse): Left[HIPFinancialDetailsFailure, Nothing] = {
      val body = Option(response.body).getOrElse("")
      parseJson(body) match {
        case Some(json) =>
          val error = (json \ "response" \ "error").validate[TechnicalError]
          val errors = (json \ "response" \ "failures").validate[Seq[HipWrappedError]]
          val message = (error, errors) match {
            case (JsSuccess(e, _), _) =>
              s"${e.code} - ${e.message}"
            case (_, JsSuccess(es, _)) =>
              es.map(e => s"${e.`type`} - ${e.reason}").mkString(", ")
            case _ =>
              json.toString()
          }
          logger.info(s"[HIPFinancialDetailsReads][handleErrorResponseSafe] Parsed downstream error: $message")
          Left(HIPFinancialDetailsFailureResponse(response.status))
        case None =>
          val fallback = classifyNonJson(body)
          logger.info(s"[HIPFinancialDetailsReads][handleErrorResponseSafe] Non-JSON error: $fallback")
          Left(HIPFinancialDetailsFailureResponse(response.status))
      }
    }
  }

  case object HIPFinancialDetailsMalformed extends HIPFinancialDetailsFailure {
    override val toIFFailureResponse: FinancialDetailsFailure = FinancialDetailsMalformed
  }

  case object HIPFinancialDetailsNoContent extends HIPFinancialDetailsFailure {
    override val toIFFailureResponse: FinancialDetailsFailure = FinancialDetailsNoContent
  }

}
