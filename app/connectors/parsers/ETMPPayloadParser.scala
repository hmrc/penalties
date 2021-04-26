/*
 * Copyright 2021 HM Revenue & Customs
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

import models.ETMPPayload
import play.api.Logger.logger
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}

object ETMPPayloadParser {

  sealed trait GetETMPPayloadFailure
  sealed trait GetETMPPayloadSuccess

  case class  GetETMPPayloadSuccessResponse(etmpPayload: ETMPPayload) extends GetETMPPayloadSuccess
  case class  GetETMPPayloadFailureResponse(status: Int) extends GetETMPPayloadFailure
  case object GetETMPPayloadNoContent extends GetETMPPayloadFailure
  case object GetETMPPayloadMalformed extends GetETMPPayloadFailure

  type ETMPPayloadResponse = Either[GetETMPPayloadFailure, GetETMPPayloadSuccess]

  implicit object ETMPPayloadReads extends HttpReads[ETMPPayloadResponse] {
    override def read(method: String, url: String, response: HttpResponse): ETMPPayloadResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[ETMPPayloadReads][read] Json response: ${response.json}")
          response.json.validate[ETMPPayload] match {
            case JsSuccess(etmpPayload, _) =>
              Right(GetETMPPayloadSuccessResponse(etmpPayload))
            case JsError(errors) =>
              logger.debug(s"[ETMPPayloadReads][read] Json validation errors: $errors")
              Left(GetETMPPayloadMalformed)
          }
        case NO_CONTENT => Left(GetETMPPayloadNoContent)
        case INTERNAL_SERVER_ERROR => {
          logger.error(s"[ETMPPayloadReads][read] Received ISE when trying to call ETMP - with body: ${response.body}")
          Left(GetETMPPayloadFailureResponse(INTERNAL_SERVER_ERROR))
        }
        case _@status =>
          logger.error(s"[ETMPPayloadReads][read] Received unexpected response from ETMP, status code: $status and body: ${response.body}")
          Left(GetETMPPayloadFailureResponse(status))
      }
    }
  }
}
