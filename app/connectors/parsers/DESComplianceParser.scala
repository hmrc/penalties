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

import models.compliance.CompliancePayloadObligationAPI
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger

object DESComplianceParser {
  sealed trait GetCompliancePayloadFailure
  sealed trait GetCompliancePayloadSuccess {
    val model: CompliancePayloadObligationAPI
  }

  case class  DESCompliancePayloadSuccessResponse(model: CompliancePayloadObligationAPI) extends GetCompliancePayloadSuccess
  case class  DESCompliancePayloadFailureResponse(status: Int) extends GetCompliancePayloadFailure
  case object DESCompliancePayloadNoData extends GetCompliancePayloadFailure
  case object DESCompliancePayloadMalformed extends GetCompliancePayloadFailure

  type DESCompliancePayloadResponse = Either[GetCompliancePayloadFailure, GetCompliancePayloadSuccess]

  implicit object DESComplianceCompliancePayloadReads extends HttpReads[DESCompliancePayloadResponse] {
    override def read(method: String, url: String, response: HttpResponse): DESCompliancePayloadResponse = {
      response.status match {
        case OK =>
          response.json.validate[Seq[CompliancePayloadObligationAPI]](CompliancePayloadObligationAPI.seqReads) match {
            case JsSuccess(compliancePayload, _) =>
              logger.debug(s"[DESComplianceCompliancePayloadReads][read] Json response: ${response.json}")
              Right(DESCompliancePayloadSuccessResponse(compliancePayload.head))
            case JsError(errors) =>
              logger.debug(s"[DESComplianceCompliancePayloadReads][read] Json validation errors: $errors")
              Left(DESCompliancePayloadMalformed)
          }
        case NOT_FOUND => {
          logger.info(s"[DESComplianceParser][read] - Received not found response from DES. No data associated with VRN. Body: ${response.body}")
          Left(DESCompliancePayloadNoData)
        }
        case BAD_REQUEST => {
          logger.error(s"[DESComplianceParser][read] - Failed to parse to model with response body: ${response.body} (Status: $BAD_REQUEST)")
          Left(DESCompliancePayloadFailureResponse(BAD_REQUEST))
        }
        case INTERNAL_SERVER_ERROR =>
          logger.error(s"[DESComplianceCompliancePayloadReads][read] Received ISE when trying to call ETMP - with body: ${response.body}")
          Left(DESCompliancePayloadFailureResponse(INTERNAL_SERVER_ERROR))
        case _@status =>
          logger.error(s"[DESComplianceCompliancePayloadReads][read] Received unexpected response from ETMP, status code: $status and body: ${response.body}")
          Left(DESCompliancePayloadFailureResponse(status))
      }
    }
  }
}
