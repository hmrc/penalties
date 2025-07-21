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

import connectors.parsers.getFinancialDetails.FinancialDetailsParser.{FinancialDetailsFailure, FinancialDetailsFailureResponse, FinancialDetailsMalformed, FinancialDetailsNoContent, FinancialDetailsSuccess, FinancialDetailsSuccessResponse}
import models.failure._
import models.getFinancialDetails.{FinancialDetails, FinancialDetailsHIP, GetFinancialData}
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import scala.util.Try

object HIPFinancialDetailsParser {
  sealed trait HIPFinancialDetailsFailure {
    val toIFResponse: FinancialDetailsFailure
  }

  sealed trait HIPFinancialDetailsSuccess {
    val toIFResponse: FinancialDetailsSuccess
  }

  case class HIPFinancialDetailsSuccessResponse(financialDetails: FinancialDetails) extends HIPFinancialDetailsSuccess {
    override val toIFResponse: FinancialDetailsSuccess = FinancialDetailsSuccessResponse(financialDetails)
  }

  case class HIPFinancialDetailsFailureResponse(status: Int) extends HIPFinancialDetailsFailure {
    override val toIFResponse: FinancialDetailsFailureResponse = FinancialDetailsFailureResponse(status)
  }

  case object HIPFinancialDetailsMalformed extends HIPFinancialDetailsFailure {
    override val toIFResponse: FinancialDetailsFailure = FinancialDetailsMalformed
  }

  case object HIPFinancialDetailsNoContent extends HIPFinancialDetailsFailure {
    override val toIFResponse: FinancialDetailsFailure = FinancialDetailsNoContent
  }

  type HIPFinancialDetailsResponse = Either[HIPFinancialDetailsFailure, HIPFinancialDetailsSuccess]

  implicit object HIPFinancialDetailsReads extends HttpReads[HIPFinancialDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): HIPFinancialDetailsResponse =
      response.status match {
        case CREATED =>
          logger.info(s"[HIPFinancialDetailsReads][read] Success HIPFinancialDetailsSuccessResponse returned from connector.")
          response.json.validate[FinancialDetailsHIP] match {
            case JsSuccess(financialDetails, _) =>
              Right(HIPFinancialDetailsSuccessResponse(financialDetails.financialData))
            case JsError(errors) =>
              PagerDutyHelper.log("HIPFinancialDetailsReads", MALFORMED_RESPONSE_FROM_1811_API)
              logger.error(s"[HIPFinancialDetailsReads][read] Json validation errors: $errors")
              Left(HIPFinancialDetailsMalformed)
          }
        case UNPROCESSABLE_ENTITY =>
          extractErrorResponseBodyFrom422(response.json)
        case status @ (BAD_REQUEST | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | UNSUPPORTED_MEDIA_TYPE | INTERNAL_SERVER_ERROR) =>
          PagerDutyHelper.logStatusCode("HIPFinancialDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(
            s"[HIPFinancialDetailsReads][read] Received $status when trying to call FinancialDetails - with body: ${response.body}")
          handleErrorResponse(response)
        case status =>
          PagerDutyHelper.logStatusCode("HIPFinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(
            s"[HIPFinancialDetailsReads][read] Received unexpected response from FinancialDetails, status code: $status and body: ${response.body}")
          Left(HIPFinancialDetailsFailureResponse(status))
      }
  }

  private def extractErrorResponseBodyFrom422(json: JsValue): Left[HIPFinancialDetailsFailure, Nothing] = {
    val errorOpt             = (json \ "errors").validate[BusinessError].asOpt // TODO can these be multiple or always singular?
    errorOpt match {
      case Some(error) if error.code == "018" && error.text == "No Data Identified" =>
        logger.error(s"[HIPFinancialDetailsReads][read] - Error: ID number did not match any data")
        Left(HIPFinancialDetailsNoContent)
      case Some(error) =>
        logger.error(s"[HIPFinancialDetailsReads][read] - 422 Error with code: ${error.code} - ${error.text}")
        Left(HIPFinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
      case _ =>
        PagerDutyHelper.log("HIPFinancialDetailsReads", INVALID_JSON_RECEIVED_FROM_1812_API)
        logger.error(s"[HIPFinancialDetailsReads][read] - Unable to parse error to expected format. Error: $json")
        Left(HIPFinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
    }
  }

  private def handleErrorResponse(response: HttpResponse): Left[HIPFinancialDetailsFailure, Nothing] = {
    val status = response.status
    val json                  = Try(response.json).getOrElse(play.api.libs.json.Json.obj())
//    val errorOpt              = (json \ "error").validate[TechnicalError].asOpt
//    val errorsOpt             = (json \ "errors").validate[Seq[BusinessError]].asOpt // TODO are these only for 422?
    val error1   = (json \ "response").validate[TechnicalError].asOpt
    val error2   = (json \ "response").validate[Seq[HipWrappedError]].asOpt
    val error3   = (json \ "response" \ "failures").validate[Seq[HipWrappedError]].asOpt // 400

    val errorMsg = (error1, error2.orElse(error3)) match {
      case (Some(error), _) =>
        s"Technical error returned: ${error.code} - ${error.message}"
      case (_, Some(errors)) =>
        val errorMessages = errors.map(err => s"${err.`type`} - ${err.reason}").mkString(",\n")
        s"HIP wrapped errors returned: $errorMessages"
      case (None, None) => json.toString()
    }
    logger.warn(s"[HIPFinancialDetailsParser][handleErrorResponse] $status error: $errorMsg")
    Left(HIPFinancialDetailsFailureResponse(response.status))
  }

//  private def handleErrorResponse(response: HttpResponse): Left[HIPFinancialDetailsFailure, Nothing] = {
//    val json               = Try(response.json).getOrElse(Json.obj())
//    val errorOpt           = (json \ "error").validate[TechnicalError].asOpt
//    val errorsOpt          = (json \ "errors").validate[Seq[BusinessError]].asOpt
//    val hipWrappedErrorOpt = (json \ "response").validate[HipWrappedError].asOpt
//
//    (errorOpt, errorsOpt, hipWrappedErrorOpt) match {
//      case (Some(error), _, _) =>
//        logger.warn(s"[HIPFinancialDetailsParser][handleErrorResponse] Technical error returned: ${error.code} - ${error.message}")
//        Left(HIPFinancialDetailsFailureResponse(response.status))
//      case (_, Some(errors), _) =>
//        logger.warn(s"[HIPFinancialDetailsParser][handleErrorResponse] Business errors returned: ${errors.mkString("\n")}")
//        Left(HIPFinancialDetailsFailureResponse(response.status))
//      case (_, _, Some(hipWrappedError)) =>
//        logger.warn(
//          s"[HIPFinancialDetailsParser][handleErrorResponse] HIP wrapped error returned: ${hipWrappedError.`type`} - ${hipWrappedError.reason}")
//        Left(HIPFinancialDetailsFailureResponse(response.status))
//      case (None, None, None) =>
//        logger.error("[HIPFinancialDetailsParser][handleErrorResponse] No recognizable error structure found")
//        Left(HIPFinancialDetailsFailureResponse(response.status))
//    }
//  }
}
