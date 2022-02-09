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

import models.v2.GetPenaltyDetails
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger

object GetPenaltyDetailsParser {
  sealed trait GetPenaltyDetailsFailure
  sealed trait GetPenaltyDetailsSuccess

  case class  GetPenaltyDetailsSuccessResponse(penaltyDetails: GetPenaltyDetails) extends GetPenaltyDetailsSuccess
  case class  GetPenaltyDetailsFailureResponse(status: Int) extends GetPenaltyDetailsFailure
  case object GetPenaltyDetailsMalformed extends GetPenaltyDetailsFailure

  type GetPenaltyDetailsResponse = Either[GetPenaltyDetailsFailure, GetPenaltyDetailsSuccess]

  implicit object GetPenaltyDetailsReads extends HttpReads[GetPenaltyDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetPenaltyDetailsResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[GetPenaltyDetailsReads][read] Json response: ${response.json}")
          response.json.validate[GetPenaltyDetails] match {
            case JsSuccess(getPenaltyDetails, _) =>
              Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))
            case JsError(errors) =>
              logger.debug(s"[GetPenaltyDetailsReads][read] Json validation errors: $errors")
              Left(GetPenaltyDetailsMalformed)
          }
        case status@(NOT_FOUND | BAD_REQUEST | CONFLICT | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) => {
          logger.error(s"[GetPenaltyDetailsReads][read] Received $status when trying to call GetPenaltyDetails - with body: ${response.body}")
          Left(GetPenaltyDetailsFailureResponse(status))
        }
        case _@status =>
          logger.error(s"[GetPenaltyDetailsReads][read] Received unexpected response from GetPenaltyDetails, status code: $status and body: ${response.body}")
          Left(GetPenaltyDetailsFailureResponse(status))
      }
    }
  }
}
