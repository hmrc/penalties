/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.parsers.submitAppeal

import connectors.parsers.submitAppeal.AppealsParser.{AppealSubmissionResponse, BadRequest, DuplicateAppeal, InvalidJson, UnexpectedFailure}
import models.appeals.AppealResponseModel
import play.api.http.Status.{CONFLICT, CREATED}
import play.api.libs.json._
import uk.gov.hmrc.http.HttpErrorFunctions.{is4xx, is5xx}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys.{INVALID_JSON_RECEIVED_FROM_1808_API, RECEIVED_4XX_FROM_1808_API, RECEIVED_5XX_FROM_1808_API}

object HIPAppealParser {

  implicit object HIPAppealSubmissionResponseReads extends HttpReads[AppealSubmissionResponse] {

    def read(method: String, url: String, response: HttpResponse): AppealSubmissionResponse =
      response.status match {
        case CREATED =>
          response.json.validate[AppealResponseModel](AppealResponseModel.format) match {
            case JsSuccess(model, _) =>
              Right(model)
            case _ =>
              PagerDutyHelper.log("HIPAppealSubmissionResponseReads", INVALID_JSON_RECEIVED_FROM_1808_API)
              Left(InvalidJson)
          }

        case CONFLICT =>
          logger.error(s"[HIPAppealSubmissionResponseReads][read]: Conflict status has been returned with body: ${response.body}")
          Left(DuplicateAppeal)

        case status if is4xx(status) =>
          PagerDutyHelper.log("HIPAppealSubmissionResponseReads", RECEIVED_4XX_FROM_1808_API)
          val (status, message) = extractResponse(response)
          logger.error(s"[HIPAppealSubmissionResponseReads][read]: Unexpected response, status $status returned with reason: $message")
          Left(BadRequest)

        case status if is5xx(status) =>
          PagerDutyHelper.log("HIPAppealSubmissionResponseReads", RECEIVED_5XX_FROM_1808_API)
          val (status, message) = extractResponse(response)
          Left(UnexpectedFailure(status, s"Unexpected response, status $status returned on submission to HIP with reason:$message"))

        case status =>
          PagerDutyHelper.logStatusCode("HIPAppealSubmissionResponseReads", status)(RECEIVED_4XX_FROM_1808_API, RECEIVED_5XX_FROM_1808_API)
          logger.error(s"[HIPAppealSubmissionResponseReads][read]: Unexpected response, status $status returned with body: ${response.body}")
          Left(UnexpectedFailure(status, s"Unexpected response, status $status returned on submission to HIP"))
      }

    private def extractResponse(response: HttpResponse): (Int, String) =
      response.json.validate[HIPErrorResponse] match {
        case JsSuccess(model, _) =>
          model.failures.headOption match {
            case Some(failure) if failure.originatedFrom == ETMP => (failure.dependentSystemHTTPCode, failure.reason)
            case _                                               => (response.status, response.body)
          }
        case _ => (response.status, response.body)
      }
  }

  sealed trait HIPFailureOrigin
  case object PEGA extends HIPFailureOrigin
  case object ETMP extends HIPFailureOrigin

  object HIPFailureOrigin {
    implicit val format: Format[HIPFailureOrigin] = new Format[HIPFailureOrigin] {
      override def reads(json: JsValue): JsResult[HIPFailureOrigin] = json.validate[String] match {
        case JsSuccess(value, _) =>
          value match {
            case "pegacms" => JsSuccess(PEGA)
            case "etmp"    => JsSuccess(ETMP)
          }
        case e: JsError => e
      }

      override def writes(o: HIPFailureOrigin): JsValue = o match {
        case PEGA => JsString("pegacms")
        case ETMP => JsString("etmp")
      }
    }
  }

  case class HIPFailureResponse(dependentSystemHTTPCode: Int, originatedFrom: HIPFailureOrigin, code: String, reason: String)
  object HIPFailureResponse {
    implicit val format: Format[HIPFailureResponse] = Json.format[HIPFailureResponse]
  }

  case class HIPErrorResponse(failures: Seq[HIPFailureResponse])
  object HIPErrorResponse {
    implicit val format: Format[HIPErrorResponse] = Json.format[HIPErrorResponse]
  }
}
