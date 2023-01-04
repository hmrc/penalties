/*
 * Copyright 2023 HM Revenue & Customs
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

import models.failure.{FailureCodeEnum, FailureResponse}
import models.getFinancialDetails.{FinancialDetails, GetFinancialData}
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import scala.util.Try

object GetFinancialDetailsParser {
  sealed trait GetFinancialDetailsFailure

  sealed trait GetFinancialDetailsSuccess

  case class GetFinancialDetailsSuccessResponse(financialDetails: FinancialDetails) extends GetFinancialDetailsSuccess

  case class GetFinancialDetailsFailureResponse(status: Int) extends GetFinancialDetailsFailure

  case object GetFinancialDetailsMalformed extends GetFinancialDetailsFailure

  case object GetFinancialDetailsNoContent extends GetFinancialDetailsFailure

  type GetFinancialDetailsResponse = Either[GetFinancialDetailsFailure, GetFinancialDetailsSuccess]

  implicit object GetFinancialDetailsReads extends HttpReads[GetFinancialDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetFinancialDetailsResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[GetFinancialDetailsReads][read] Json response: ${response.json}")
          response.json.validate[GetFinancialData] match {
            case JsSuccess(getFinancialData, _) =>
              Right(GetFinancialDetailsSuccessResponse(getFinancialData.financialDetails))
            case JsError(errors) =>
              logger.debug(s"[GetFinancialDetailsReads][read] Json validation errors: $errors")
              Left(GetFinancialDetailsMalformed)
          }
        case NOT_FOUND if response.body.nonEmpty => {
          Try(handleNotFoundStatusBody(response.json)).fold(parseError => {
            logger.error(s"[GetFinancialDetailsReads][read] Could not parse 404 body with error ${parseError.getMessage}")
            PagerDutyHelper.log("GetPenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1811_API)
            Left(GetFinancialDetailsFailureResponse(NOT_FOUND))
          }, identity)
        }
        case NO_CONTENT => {
          logger.info("[GetFinancialDetailsReads][read] Received no content from 1811 call")
          Left(GetFinancialDetailsNoContent)
        }
        case status@(BAD_REQUEST | FORBIDDEN | NOT_FOUND | CONFLICT | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) => {
          PagerDutyHelper.logStatusCode("GetFinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(s"[GetFinancialDetailsReads][read] Received $status when trying to call GetFinancialDetails - with body: ${response.body}")
          Left(GetFinancialDetailsFailureResponse(status))
        }
        case _@status =>
          PagerDutyHelper.logStatusCode("GetFinancialDetailsReads", status)(RECEIVED_4XX_FROM_1811_API, RECEIVED_5XX_FROM_1811_API)
          logger.error(s"[GetFinancialDetailsReads][read] Received unexpected response from GetFinancialDetails, status code: $status and body: ${response.body}")
          Left(GetFinancialDetailsFailureResponse(status))
      }
    }
  }

  private def handleNotFoundStatusBody(responseBody: JsValue): Left[GetFinancialDetailsFailure, Nothing] = {
    (responseBody \ "failures").validate[Seq[FailureResponse]].fold(
      errors => {
        logger.debug(s"[GetFinancialDetailsReads][read] - Parsing errors: $errors")
        logger.error(s"[GetFinancialDetailsReads][read] - Could not parse 404 body returned from GetFinancialDetails call")
        Left(GetFinancialDetailsFailureResponse(NOT_FOUND))
      },
      failures => {
        if (failures.exists(_.code.equals(FailureCodeEnum.NoDataFound))) {
          Left(GetFinancialDetailsNoContent)
        } else {
          logger.error(s"[GetFinancialDetailsReads][read] - Received following errors from GetFinancialDetails 404 call: $failures")
          Left(GetFinancialDetailsFailureResponse(NOT_FOUND))
        }
      }
    )
  }
}
