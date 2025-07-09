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

package connectors.parsers.getFinancialDetails

import models.failure._
import models.getFinancialDetails.{FinancialDetails, FinancialDetailsHIP, GetFinancialData}
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import scala.util.Try

object FinancialDetailsParser {
  sealed trait FinancialDetailsFailure

  sealed trait FinancialDetailsSuccess {
    val financialDetails: FinancialDetails
  }

  case class FinancialDetailsSuccessResponse(financialDetails: FinancialDetails) extends FinancialDetailsSuccess
  case class FinancialDetailsHipSuccessResponse(financialData: FinancialDetailsHIP) extends FinancialDetailsSuccess {
    val financialDetails: FinancialDetails = financialData.financialData
  }

  case class FinancialDetailsFailureResponse(status: Int) extends FinancialDetailsFailure

  case object FinancialDetailsMalformed extends FinancialDetailsFailure

  case object FinancialDetailsNoContent extends FinancialDetailsFailure

  type FinancialDetailsResponse = Either[FinancialDetailsFailure, FinancialDetailsSuccess]

  implicit object FinancialDetailsReads extends HttpReads[FinancialDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): FinancialDetailsResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[FinancialDetailsParser][FinancialDetailsReads][read] Json response: ${response.json}")
          val attemptHipValidation: JsResult[FinancialDetailsHIP] = response.json.validate[FinancialDetailsHIP]
          val attemptIfValidation: JsResult[GetFinancialData]     = response.json.validate[GetFinancialData]

          (attemptHipValidation, attemptIfValidation) match {
            case (JsSuccess(financialDetailsHIP, _), _) =>
              Right(FinancialDetailsHipSuccessResponse(financialDetailsHIP))
            case (_, JsSuccess(financialDetailsIF, _)) =>
              Right(FinancialDetailsSuccessResponse(financialDetailsIF.financialDetails))
            case (JsError(errorsHIP), JsError(errorsIF)) =>
              logger.debug(
                "[FinancialDetailsParser][FinancialDetailsReads][read] Unable to validate Json for HIP nor IF schemas.\n" +
                  s"HIP validation errors: $errorsHIP\n IF validation errors: $errorsIF")
              Left(FinancialDetailsMalformed)
          }
        case NOT_FOUND if response.body.nonEmpty => {
          Try(handleNotFoundStatusBody(response.json)).fold(parseError => {
            logger.error(s"[FinancialDetailsParser][FinancialDetailsReads][read] Could not parse 404 body with error ${parseError.getMessage}")
            PagerDutyHelper.log(" GetPenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1811_API)
            Left(FinancialDetailsFailureResponse(NOT_FOUND))
          }, identity)
        }
        case NO_CONTENT => {
          logger.info("[FinancialDetailsParser][FinancialDetailsReads][read] Received no content from 1811 call")
          Left(FinancialDetailsNoContent)
        }
        case status@(BAD_REQUEST | FORBIDDEN | NOT_FOUND | CONFLICT | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) => {
          PagerDutyHelper.logStatusCode("FinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(s"[FinancialDetailsParser][FinancialDetailsReads][read] Received $status when trying to call FinancialDetails - with body: ${response.body}")
          if (status == BAD_REQUEST || status == INTERNAL_SERVER_ERROR) handleHipWrappedErrorResponse(response) else handleErrorResponse(response)
        }
        case _@status =>
          PagerDutyHelper.logStatusCode("FinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(s"[FinancialDetailsParser][FinancialDetailsReads][read] Received unexpected response from FinancialDetails, status code: $status and body: ${response.body}")
          Left(FinancialDetailsFailureResponse(status))
      }
    }
  }
//  implicit object FinancialDetailsReads extends HttpReads[FinancialDetailsResponse] {
//    // noinspection ScalaStyle
//    override def read(method: String, url: String, response: HttpResponse): FinancialDetailsResponse =
//      response.status match {
//        case OK =>
//          logger.debug(s"[FinancialDetailsParser][FinancialDetailsReads][read] Json response: ${response.json}")
//          val attemptHipValidation: JsResult[FinancialDetailsHIP] = response.json.validate[FinancialDetailsHIP]
//          val attemptIfValidation: JsResult[GetFinancialData]     = response.json.validate[GetFinancialData]
//
//          (attemptHipValidation, attemptIfValidation) match {
//            case (JsSuccess(financialDetailsHIP, _), _) =>
//              Right(FinancialDetailsHipSuccessResponse(financialDetailsHIP))
//            case (_, JsSuccess(financialDetailsIF, _)) =>
//              Right(FinancialDetailsSuccessResponse(financialDetailsIF.financialDetails))
//            case (JsError(errorsHIP), JsError(errorsIF)) =>
//              logger.debug(
//                "[FinancialDetailsParser][FinancialDetailsReads][read] Unable to validate Json for HIP nor IF schemas.\n" +
//                  s"HIP validation errors: $errorsHIP\n IF validation errors: $errorsIF")
//              Left(FinancialDetailsMalformed)
//          }
//        case NOT_FOUND if response.body.nonEmpty =>
//          Try(handleNotFoundStatusBody(response.json)).fold(
//            parseError => {
//              logger.error(s"[FinancialDetailsParser][FinancialDetailsReads][read] Could not parse 404 body with error ${parseError.getMessage}")
//              PagerDutyHelper.log(" GetPenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1811_API)
//              Left(FinancialDetailsFailureResponse(NOT_FOUND))
//            },
//            identity
//          )
//        case NO_CONTENT =>
//          logger.info("[FinancialDetailsParser][FinancialDetailsReads][read] Received no content from 1811 call")
//          Left(FinancialDetailsNoContent)
//        case status @ (BAD_REQUEST | FORBIDDEN | NOT_FOUND | CONFLICT | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) =>
//          PagerDutyHelper.logStatusCode("FinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
//          logger.error(
//            s"[FinancialDetailsParser][FinancialDetailsReads][read] Received $status when trying to call FinancialDetails - with body: ${response.body}")
//          if (status == BAD_REQUEST || status == INTERNAL_SERVER_ERROR) handleHipWrappedErrorResponse(response) else handleErrorResponse(response)
//        case _ @status =>
//          PagerDutyHelper.logStatusCode("FinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
//          logger.error(
//            s"[FinancialDetailsParser][FinancialDetailsReads][read] Received unexpected response from FinancialDetails, status code: $status and body: ${response.body}")
//          Left(FinancialDetailsFailureResponse(status))
//      }
//  }

  private def handleNotFoundStatusBody(responseBody: JsValue): Left[FinancialDetailsFailure, Nothing] =
    (responseBody \ "failures")
      .validate[Seq[FailureResponse]]
      .fold(
        errors => {
          logger.debug(s"[FinancialDetailsParser][FinancialDetailsReads][read] - Parsing errors: $errors")
          logger.error(s"[FinancialDetailsParser][FinancialDetailsReads][read] - Could not parse 404 body returned from FinancialDetails call")
          Left(FinancialDetailsFailureResponse(NOT_FOUND))
        },
        failures =>
          if (failures.exists(_.code.equals(FailureCodeEnum.NoDataFound))) {
            Left(FinancialDetailsNoContent)
          } else {
            logger.error(
              s"[FinancialDetailsParser][FinancialDetailsReads][read] - Received following errors from FinancialDetails 404 call: $failures")
            Left(FinancialDetailsFailureResponse(NOT_FOUND))
          }
      )

  private def handleErrorResponse(response: HttpResponse): Left[FinancialDetailsFailure, Nothing] = {
    val json      = Try(response.json).getOrElse(Json.obj())
    val errorOpt  = (json \ "error").validate[TechnicalError].asOpt
    val errorsOpt = (json \ "errors").validate[Seq[BusinessError]].asOpt

    (errorOpt, errorsOpt) match {
      case (Some(error), _) =>
        logger.warn(s"[FinancialDetailsParser][handleErrorResponse] HIP Technical error returned: ${error.code}")
        Left(FinancialDetailsFailureResponse(response.status))
      case (_, Some(errors)) =>
        logger.warn(s"[FinancialDetailsParser][handleErrorResponse] HIP Business errors returned: ${errors.mkString("\n")}")
        Left(FinancialDetailsFailureResponse(response.status))
      case (None, None) =>
        logger.error("[FinancialDetailsParser][handleErrorResponse] No recognizable error structure found")
        Left(FinancialDetailsFailureResponse(response.status))
    }
  }

  private def handleHipWrappedErrorResponse(response: HttpResponse): Left[FinancialDetailsFailure, Nothing] = {
    val json = Try(response.json).getOrElse(Json.obj())

    (json \ "response").validate[HipWrappedError].asOpt match {
      case Some(wrappedError) =>
        logger.warn(
          s"[FinancialDetailsParser][handleHipWrappedErrorResponse] ${response.status} HIP error returned - " +
            s"Type: ${wrappedError.`type`}, Reason: ${wrappedError.reason}")
        Left(FinancialDetailsFailureResponse(response.status))

      case None =>
        logger.error(
          s"[FinancialDetailsParser][handleHipWrappedErrorResponse] ${response.status} HIP error returned with unknown structure - Error: $json")
        Left(FinancialDetailsFailureResponse(response.status))
    }
  }
}
