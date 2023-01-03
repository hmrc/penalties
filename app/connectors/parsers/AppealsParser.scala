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

package connectors.parsers

import models.appeals.AppealResponseModel
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.JsSuccess
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

object AppealsParser {
  type AppealSubmissionResponse = Either[ErrorResponse, AppealResponseModel]

  implicit object AppealSubmissionResponseReads extends HttpReads[AppealSubmissionResponse] {

    def read(method: String, url: String, response: HttpResponse): AppealSubmissionResponse = {
      response.status match {
        case OK =>
          response.json.validate[AppealResponseModel](AppealResponseModel.format) match {
            case JsSuccess(model, _) =>
              Right(model)
            case _ =>
              PagerDutyHelper.log("AppealSubmissionResponseReads", INVALID_JSON_RECEIVED_FROM_1808_API)
              Left(InvalidJson)
          }
        case BAD_REQUEST =>
          PagerDutyHelper.log("AppealSubmissionResponseReads", RECEIVED_4XX_FROM_1808_API)
          logger.debug(s"[AppealSubmissionResponseReads][read]: Bad request returned with reason: ${response.body}")
          Left(BadRequest)
        case status =>
          PagerDutyHelper.logStatusCode("AppealSubmissionResponseReads", status)(RECEIVED_4XX_FROM_1808_API, RECEIVED_5XX_FROM_1808_API)
          logger.warn(s"[AppealSubmissionResponseReads][read]: Unexpected response, status $status returned")
          Left(UnexpectedFailure(status, s"Unexpected response, status $status returned"))
      }
    }
  }

  sealed trait ErrorResponse {
    val status: Int
    val body: String
  }

  case object InvalidJson extends ErrorResponse {
    override val status: Int = BAD_REQUEST
    override val body: String = "Invalid JSON received"
  }

  case object BadRequest extends ErrorResponse {
    override val status: Int = BAD_REQUEST
    override val body: String = "Incorrect Json body sent"
  }

  case class UnexpectedFailure(
                                override val status: Int,
                                override val body: String
                              ) extends ErrorResponse
}

