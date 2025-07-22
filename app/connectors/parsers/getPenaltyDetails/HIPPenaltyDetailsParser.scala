/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors.parsers.getPenaltyDetails

import models.failure.{BusinessError, HipWrappedError, TechnicalError}
import models.hipPenaltyDetails.PenaltyDetails
import models.hipPenaltyDetails.latePayment.{LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.time.LocalDate

object HIPPenaltyDetailsParser {
  sealed trait HIPPenaltyDetailsFailure

  sealed trait HIPPenaltyDetailsSuccess

  case class HIPPenaltyDetailsSuccessResponse(penaltyDetails: PenaltyDetails) extends HIPPenaltyDetailsSuccess

  case class HIPPenaltyDetailsFailureResponse(status: Int) extends HIPPenaltyDetailsFailure

  case object HIPPenaltyDetailsMalformed extends HIPPenaltyDetailsFailure

  case object HIPPenaltyDetailsNoContent extends HIPPenaltyDetailsFailure

  type HIPPenaltyDetailsResponse = Either[HIPPenaltyDetailsFailure, HIPPenaltyDetailsSuccess]

  implicit object HIPPenaltyDetailsReads extends HttpReads[HIPPenaltyDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): HIPPenaltyDetailsResponse =
      response.status match {
        case OK =>
          handleSuccessResponse(response.json)
        case UNPROCESSABLE_ENTITY =>
          extractErrorResponseBodyFrom422(response.json)
        case status @ (BAD_REQUEST | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | UNSUPPORTED_MEDIA_TYPE | INTERNAL_SERVER_ERROR) =>
          PagerDutyHelper.logStatusCode("HIPPenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          handleErrorResponse(response)
        case status =>
          PagerDutyHelper.logStatusCode("HIPPenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(
            s"[HIPPenaltyDetailsReads][read] Received unexpected response from PenaltyDetails, status code: $status and body: ${response.body}")
          Left(HIPPenaltyDetailsFailureResponse(status))
      }
  }

  private def handleSuccessResponse(json: JsValue): HIPPenaltyDetailsResponse = {
    logger.info(s"[HIPPenaltyDetailsReads][read] Success 200 response returned from API#5329")
    json.validate[PenaltyDetails] match {
      case JsSuccess(penaltyDetails, _) =>
        logger.info(s"[HIPPenaltyDetailsReads][read] PenaltyDetails successfully validated from success response")
        Right(HIPPenaltyDetailsSuccessResponse(addMissingLPP1PrincipalChargeLatestClearing(penaltyDetails)))
      case JsError(errors) =>
        PagerDutyHelper.log("HIPPenaltyDetailsReads", MALFORMED_RESPONSE_FROM_1812_API)
        logger.error(s"[HIPPenaltyDetailsReads][read] Json validation of 200 body failed with errors: $errors")
        Left(HIPPenaltyDetailsMalformed)
    }
  }

  private def extractErrorResponseBodyFrom422(json: JsValue): Left[HIPPenaltyDetailsFailure, Nothing] =
    (json \ "errors").validate[BusinessError] match { // 422 a single error is ever returned regardless of the number of mistakes
      case JsSuccess(error, _) if error.code == "016" && error.text == "Invalid ID Number" =>
        logger.error(s"[HIPPenaltyDetailsReads][read] - Error: ID number did not match any data")
        Left(HIPPenaltyDetailsNoContent)
      case JsSuccess(error, _) =>
        logger.error(s"[HIPPenaltyDetailsReads][read] - 422 Error with code: ${error.code} - ${error.text}")
        Left(HIPPenaltyDetailsFailureResponse(UNPROCESSABLE_ENTITY))
      case _ =>
        PagerDutyHelper.log("HIPPenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1812_API)
        logger.error(s"[HIPPenaltyDetailsReads][read] - Unable to parse 422 error body to expected format. Error: $json")
        Left(HIPPenaltyDetailsFailureResponse(UNPROCESSABLE_ENTITY))
    }

  private def handleErrorResponse(response: HttpResponse): Left[HIPPenaltyDetailsFailure, Nothing] = {
    val status = response.status
    val error  = (response.json \ "response" \ "error").validate[TechnicalError]          // 500 errors
    val errors = (response.json \ "response" \ "failures").validate[Seq[HipWrappedError]] // 400 errors are always in an array
    val errorMsg = (error, errors) match {
      case (JsSuccess(error, _), _)  => s"${error.code} - ${error.message}"
      case (_, JsSuccess(errors, _)) => errors.map(err => s"${err.`type`} - ${err.reason}").mkString(",\n")
      case _                         => response.json.toString()
    }
    logger.error(s"[HIPPenaltyDetailsParser][handleErrorResponse] $status errors: $errorMsg")
    Left(HIPPenaltyDetailsFailureResponse(response.status))
  }

  private def addMissingLPP1PrincipalChargeLatestClearing(penaltyDetails: PenaltyDetails): PenaltyDetails = {
    val newDetails = penaltyDetails.latePaymentPenalty.flatMap(
      _.lppDetails.map(latePaymentPenalties =>
        latePaymentPenalties.map { oldLPPDetails =>
          (oldLPPDetails.penaltyCategory, oldLPPDetails.penaltyStatus, oldLPPDetails.principalChargeLatestClearing.isEmpty) match {
            case (LPPPenaltyCategoryEnum.FirstPenalty, Some(LPPPenaltyStatusEnum.Posted), true) =>
              val filteredList = latePaymentPenalties
                .filter(_.penaltyCategory.equals(LPPPenaltyCategoryEnum.SecondPenalty))
                .filter(_.principalChargeReference.equals(oldLPPDetails.principalChargeReference))
              val optPrincipalChargeClearingDate: Option[LocalDate] = filteredList.headOption.flatMap(_.principalChargeLatestClearing)
              oldLPPDetails.copy(principalChargeLatestClearing = optPrincipalChargeClearingDate)
            case _ =>
              oldLPPDetails
          }
        })
    )
    if (newDetails.nonEmpty) {
      penaltyDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(newDetails, penaltyDetails.latePaymentPenalty.get.manualLPPIndicator)))
    } else {
      penaltyDetails
    }
  }

}
