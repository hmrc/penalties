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


import models.failure.{BusinessError, FailureCodeEnum, FailureResponse, TechnicalError}
import models.getFinancialDetails.{FinancialDetails, FinancialDetailsHIP}
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import scala.util.Try

object FinancialDetailsParser {
  sealed trait GetFinancialDetailsFailure

  sealed trait GetFinancialDetailsSuccess {
    val financialDetails: FinancialDetails
  }

  case class GetFinancialDetailsSuccessResponse(financialDetails: FinancialDetails) extends GetFinancialDetailsSuccess
  case class GetFinancialDetailsHipSuccessResponse(financialData: FinancialDetailsHIP) extends GetFinancialDetailsSuccess {
    val financialDetails: FinancialDetails = financialData.financialData
  }

  case class GetFinancialDetailsFailureResponse(status: Int) extends GetFinancialDetailsFailure

  case object GetFinancialDetailsMalformed extends GetFinancialDetailsFailure

  case object GetFinancialDetailsNoContent extends GetFinancialDetailsFailure

  type GetFinancialDetailsResponse = Either[GetFinancialDetailsFailure, GetFinancialDetailsSuccess]

  implicit object GetFinancialDetailsReads extends HttpReads[GetFinancialDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetFinancialDetailsResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[FinancialDetailsParser][GetFinancialDetailsReads][read] Json response: ${response.json}")
          val attemptHipValidation: JsResult[FinancialDetailsHIP] = response.json.validate[FinancialDetailsHIP]
          val attemptIfValidation: JsResult[FinancialDetails] = response.json.validate[FinancialDetails]

          (attemptHipValidation, attemptIfValidation) match {
            case (JsSuccess(financialDetailsHIP, _), _) =>
              Right(GetFinancialDetailsHipSuccessResponse(financialDetailsHIP))
            case (_, JsSuccess(financialDetailsIF, _)) =>
              Right(GetFinancialDetailsSuccessResponse(financialDetailsIF))
            case (JsError(errorsHIP), JsError(errorsIF)) =>
              logger.debug("[FinancialDetailsParser][GetFinancialDetailsReads][read] Unable to validate Json for HIP nor IF schemas.\n" +
                s"HIP validation errors: $errorsHIP\n IF validation errors: $errorsIF")
              Left(GetFinancialDetailsMalformed)
          }
        case NOT_FOUND if response.body.nonEmpty => {
          Try(handleNotFoundStatusBody(response.json)).fold(parseError => {
            logger.error(s"[FinancialDetailsParser][GetFinancialDetailsReads][read] Could not parse 404 body with error ${parseError.getMessage}")
            PagerDutyHelper.log(" GetPenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1811_API)
            Left(GetFinancialDetailsFailureResponse(NOT_FOUND))
          }, identity)
        }
        case NO_CONTENT => {
          logger.info("[FinancialDetailsParser][GetFinancialDetailsReads][read] Received no content from 1811 call")
          Left(GetFinancialDetailsNoContent)
        }
        case status@(BAD_REQUEST | FORBIDDEN | NOT_FOUND | CONFLICT | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) => {
          PagerDutyHelper.logStatusCode("GetFinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(s"[FinancialDetailsParser][GetFinancialDetailsReads][read] Received $status when trying to call GetFinancialDetails - with body: ${response.body}")
          handleErrorResponse(response)
        }
        case _@status =>
          PagerDutyHelper.logStatusCode("GetFinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(s"[FinancialDetailsParser][GetFinancialDetailsReads][read] Received unexpected response from GetFinancialDetails, status code: $status and body: ${response.body}")
          Left(GetFinancialDetailsFailureResponse(status))
      }
    }
  }

  private def handleNotFoundStatusBody(responseBody: JsValue): Left[GetFinancialDetailsFailure, Nothing] = {
    (responseBody \ "failures").validate[Seq[FailureResponse]].fold(
      errors => {
        logger.debug(s"[FinancialDetailsParser][GetFinancialDetailsReads][read] - Parsing errors: $errors")
        logger.error(s"[FinancialDetailsParser][GetFinancialDetailsReads][read] - Could not parse 404 body returned from GetFinancialDetails call")
        Left(GetFinancialDetailsFailureResponse(NOT_FOUND))
      },
      failures => {
        if (failures.exists(_.code.equals(FailureCodeEnum.NoDataFound))) {
          Left(GetFinancialDetailsNoContent)
        } else {
          logger.error(s"[FinancialDetailsParser][GetFinancialDetailsReads][read] - Received following errors from GetFinancialDetails 404 call: $failures")
          Left(GetFinancialDetailsFailureResponse(NOT_FOUND))
        }
      }
    )
  }

  private def handleErrorResponse(response: HttpResponse): Left[GetFinancialDetailsFailure, Nothing] = {
    val json = Try(response.json).getOrElse(Json.obj())

    (json \ "error").validate[TechnicalError].asOpt match {
      case Some(singleError) =>
        logger.warn(s"[FinancialDetailsParser][handleErrorResponse] HIP Technical error returned: ${singleError.code}")
        Left(GetFinancialDetailsFailureResponse(response.status))

      case None =>
        (json \ "errors").validate[Seq[BusinessError]].asOpt match {
          case Some(multipleErrors) =>
            logger.warn(s"[FinancialDetailsParser][handleErrorResponse] HIP Business errors returned. First error: $multipleErrors")
            Left(GetFinancialDetailsFailureResponse(response.status))

          case _ =>
            logger.error("[FinancialDetailsParser][handleErrorResponse] No recognizable error structure found")
            Left(GetFinancialDetailsFailureResponse(response.status))
        }
    }
  }
}
