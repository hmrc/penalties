
package connectors.parsers

import models.ETMPPayload
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger

object ComplianceParser {
  sealed trait GetCompliancePayloadFailure
  sealed trait GetCompliancePayloadSuccess

  case class  GetCompliancePayloadSuccessResponse(jsValue: JsValue) extends GetCompliancePayloadSuccess
  case class  GetCompliancePayloadFailureResponse(status: Int) extends GetCompliancePayloadFailure
  case object GetCompliancePayloadMalformed extends GetCompliancePayloadFailure

  type CompliancePayloadResponse = Either[GetCompliancePayloadFailure, GetCompliancePayloadSuccess]

  implicit object CompliancePayloadReads extends HttpReads[CompliancePayloadResponse] {
    override def read(method: String, url: String, response: HttpResponse): CompliancePayloadResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[CompliancePayloadReads][read] Json response: ${response.json}")
          Right(GetCompliancePayloadSuccessResponse(response.json))
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
