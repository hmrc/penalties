/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.parsers.v2

import models.v2.financialDetails.GetFinancialDetails
import play.api.http.Status.{BAD_REQUEST, CONFLICT, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SERVICE_UNAVAILABLE, UNPROCESSABLE_ENTITY}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger

object GetFinancialDetailsParser {
  sealed trait GetFinancialDetailsFailure

  sealed trait GetFinancialDetailsSuccess

  case class GetFinancialDetailsSuccessResponse(FinancialDetails: GetFinancialDetails) extends GetFinancialDetailsSuccess

  case class GetFinancialDetailsFailureResponse(status: Int) extends GetFinancialDetailsFailure

  case object GetFinancialDetailsMalformed extends GetFinancialDetailsFailure

  type GetFinancialDetailsResponse = Either[GetFinancialDetailsFailure, GetFinancialDetailsSuccess]

  implicit object GetFinancialDetailsReads extends HttpReads[GetFinancialDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetFinancialDetailsResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[GetFinancialDetailsReads][read] Json response: ${response.json}")
          response.json.validate[GetFinancialDetails] match {
            case JsSuccess(getFinancialDetails, _) =>
              Right(GetFinancialDetailsSuccessResponse(getFinancialDetails))
            case JsError(errors) =>
              logger.debug(s"[GetFinancialDetailsReads][read] Json validation errors: $errors")
              Left(GetFinancialDetailsMalformed)
          }
        case status@(BAD_REQUEST | FORBIDDEN | NOT_FOUND | CONFLICT | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) => {
          logger.error(s"[GetFinancialDetailsReads][read] Received $status when trying to call GetFinancialDetails - with body: ${response.body}")
          Left(GetFinancialDetailsFailureResponse(status))
        }
        case _@status =>
          logger.error(s"[GetFinancialDetailsReads][read] Received unexpected response from GetFinancialDetails," +
            s" status code: $status and body: ${response.body}")
          Left(GetFinancialDetailsFailureResponse(status))
      }
    }
  }
}

