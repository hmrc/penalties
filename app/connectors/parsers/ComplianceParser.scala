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

import models.compliance.CompliancePayload
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

object ComplianceParser {
  sealed trait GetCompliancePayloadFailure {
    val message: String
  }
  sealed trait GetCompliancePayloadSuccess {
    val model: CompliancePayload
  }

  case class CompliancePayloadSuccessResponse(model: CompliancePayload) extends GetCompliancePayloadSuccess
  case class CompliancePayloadFailureResponse(status: Int) extends GetCompliancePayloadFailure {
    override val message: String = s"Received status code: $status"
  }
  case object CompliancePayloadNoData extends GetCompliancePayloadFailure {
    override val message: String = "Received no data from call"
  }
  case object CompliancePayloadMalformed extends GetCompliancePayloadFailure {
    override val message: String = "Body received was malformed"
  }

  type CompliancePayloadResponse = Either[GetCompliancePayloadFailure, GetCompliancePayloadSuccess]

  implicit object ComplianceCompliancePayloadReads extends HttpReads[CompliancePayloadResponse] {
    override def read(method: String, url: String, response: HttpResponse): CompliancePayloadResponse = {
      response.status match {
        case OK =>
          response.json.validate[Seq[CompliancePayload]](CompliancePayload.seqReads) match {
            case JsSuccess(compliancePayload, _) =>
              logger.debug(s"[ComplianceCompliancePayloadReads][read] Json response: ${response.json}")
              Right(CompliancePayloadSuccessResponse(compliancePayload.head))
            case JsError(errors) =>
              PagerDutyHelper.log("ComplianceCompliancePayloadReads", INVALID_JSON_RECEIVED_FROM_1330_API)
              logger.debug(s"[ComplianceCompliancePayloadReads][read] Json validation errors: $errors")
              Left(CompliancePayloadMalformed)
          }
        case NOT_FOUND => {
          logger.info(s"[ComplianceParser][read] - Received not found response from . No data associated with VRN. Body: ${response.body}")
          Left(CompliancePayloadNoData)
        }
        case BAD_REQUEST => {
          PagerDutyHelper.log("ComplianceCompliancePayloadReads", RECEIVED_4XX_FROM_1330_API)
          logger.error(s"[ComplianceParser][read] - Failed to parse to model with response body: ${response.body} (Status: $BAD_REQUEST)")
          Left(CompliancePayloadFailureResponse(BAD_REQUEST))
        }
        case INTERNAL_SERVER_ERROR =>
          PagerDutyHelper.log("ComplianceCompliancePayloadReads", RECEIVED_5XX_FROM_1330_API)
          logger.error(s"[ComplianceCompliancePayloadReads][read] Received ISE when trying to call 1330 API - with body: ${response.body}")
          Left(CompliancePayloadFailureResponse(INTERNAL_SERVER_ERROR))
        case _@status =>
          PagerDutyHelper.logStatusCode("ComplianceCompliancePayloadReads", status)(RECEIVED_4XX_FROM_1330_API, RECEIVED_5XX_FROM_1330_API)
          logger.error(s"[ComplianceCompliancePayloadReads][read] Received unexpected response from 1330 API, status code: $status and body: ${response.body}")
          Left(CompliancePayloadFailureResponse(status))
      }
    }
  }
}
