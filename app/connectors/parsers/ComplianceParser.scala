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

import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger

object ComplianceParser {
  sealed trait GetCompliancePayloadFailure
  sealed trait GetCompliancePayloadSuccess {
    val jsValue: JsValue
  }

  case class  GetCompliancePayloadSuccessResponse(jsValue: JsValue) extends GetCompliancePayloadSuccess
  case class  GetCompliancePayloadFailureResponse(status: Int) extends GetCompliancePayloadFailure
  case object GetCompliancePayloadNoContent extends GetCompliancePayloadFailure
  case object GetCompliancePayloadMalformed extends GetCompliancePayloadFailure

  type CompliancePayloadResponse = Either[GetCompliancePayloadFailure, GetCompliancePayloadSuccess]

  implicit object CompliancePayloadReads extends HttpReads[CompliancePayloadResponse] {
    override def read(method: String, url: String, response: HttpResponse): CompliancePayloadResponse = {
      response.status match {
        case OK =>
          response.json.validate[JsValue] match {
            case JsSuccess(compliancePayload, _) =>
              logger.debug(s"[CompliancePayloadReads][read] Json response: ${response.json}")
              Right(GetCompliancePayloadSuccessResponse(compliancePayload))
            case JsError(errors) =>
              logger.debug(s"[CompliancePayloadReads][read] Json validation errors: $errors")
              Left(GetCompliancePayloadMalformed)
          }
        case NO_CONTENT => Left(GetCompliancePayloadNoContent)
        case INTERNAL_SERVER_ERROR =>
          logger.error(s"[CompliancePayloadReads][read] Received ISE when trying to call ETMP - with body: ${response.body}")
          Left(GetCompliancePayloadFailureResponse(INTERNAL_SERVER_ERROR))
        case _@status =>
          logger.error(s"[CompliancePayloadReads][read] Received unexpected response from ETMP, status code: $status and body: ${response.body}")
          Left(GetCompliancePayloadFailureResponse(status))
      }
    }
  }
}
