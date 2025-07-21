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

import models.failure.{BusinessError, FailureCodeEnum, FailureResponse, HipWrappedError, TechnicalError}
import models.hipPenaltyDetails.latePayment.{LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.time.LocalDate
import scala.util.Try
import models.hipPenaltyDetails.PenaltyDetails

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
          response.json.validate[PenaltyDetails] match {
            case JsSuccess(getPenaltyDetails, _) =>
              logger.info(s"[HIPPenaltyDetailsReads][read] Success HIPPenaltyDetailsSuccessResponse returned from connector.")
              Right(HIPPenaltyDetailsSuccessResponse(addMissingLPP1PrincipalChargeLatestClearing(getPenaltyDetails)))
            case JsError(errors) =>
              PagerDutyHelper.log("HIPPenaltyDetailsReads", MALFORMED_RESPONSE_FROM_1812_API)
              logger.error(s"[HIPPenaltyDetailsReads][read] Json validation errors: $errors")
              Left(HIPPenaltyDetailsMalformed)
          }
        case UNPROCESSABLE_ENTITY =>
          extractErrorResponseBodyFrom422(response.json)
        case status @ (BAD_REQUEST | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | UNSUPPORTED_MEDIA_TYPE | INTERNAL_SERVER_ERROR) =>
          PagerDutyHelper.logStatusCode("HIPPenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(
            s"[HIPPenaltyDetailsReads][read] Received $status when trying to call PenaltyDetails - with body: ${response.body}")
          handleErrorResponse(response)
        case status =>
          PagerDutyHelper.logStatusCode("HIPPenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(
            s"[HIPPenaltyDetailsReads][read] Received unexpected response from PenaltyDetails, status code: $status and body: ${response.body}")
          Left(HIPPenaltyDetailsFailureResponse(status))
      }
  }

  private def extractErrorResponseBodyFrom422(json: JsValue): Left[HIPPenaltyDetailsFailure, Nothing] = {
    val errorOpt             = (json \ "errors").validate[BusinessError].asOpt // TODO can these be multiple or always singular?
    errorOpt match {
      case Some(error) if error.code == "016" && error.text == "Invalid ID Number" => // TODO This is invalid response,
        // not the NOTFOUND response.
        // is 018 for financial details.
        // Is that still correct for penalties though?
        logger.error(s"[HIPPenaltyDetailsReads][read] - Error: ID number did not match any data")
        Left(HIPPenaltyDetailsNoContent)
      case Some(error) =>
        logger.error(s"[HIPPenaltyDetailsReads][read] - 422 Error with code: ${error.code} - ${error.text}")
        Left(HIPPenaltyDetailsFailureResponse(UNPROCESSABLE_ENTITY))
      case _ =>
        PagerDutyHelper.log("HIPPenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1812_API)
        logger.error(s"[HIPPenaltyDetailsReads][read] - Unable to parse error to expected format. Error: $json")
        Left(HIPPenaltyDetailsFailureResponse(UNPROCESSABLE_ENTITY))
    }
  }

  private def handleErrorResponse(response: HttpResponse): Left[HIPPenaltyDetailsFailure, Nothing] = {
    val status = response.status
    val json                  = Try(response.json).getOrElse(play.api.libs.json.Json.obj())
    val errorOpt              = (json \ "error").validate[TechnicalError].asOpt
    val errorsOpt             = (json \ "errors").validate[Seq[BusinessError]].asOpt
    val hipWrappedErrors1Opt   = (json \ "response").validate[TechnicalError].asOpt
    val hipWrappedErrors1Opt   = (json \ "response").validate[Seq[HipWrappedError]].asOpt
    val hipWrappedErrors2Opt   = (json \ "response" \ "failures").validate[Seq[HipWrappedError]].asOpt // 400

    val errorMsg = (errorOpt, errorsOpt, hipWrappedErrorsOpt) match {
      case (Some(error), _, _) =>
        s"Technical error returned: ${error.code} - ${error.message}"
      case (_, Some(errors), _) =>
        s"Business errors returned: ${errors.mkString("\n")}"
      case (_, _, Some(hipWrappedErrors)) =>
        val errorMessages = hipWrappedErrors.map(err => s"${err.`type`} - ${err.reason}").mkString(", ")
        s"HIP wrapped errors returned: $errorMessages"
      case (None, None, None) => json.toString()
    }
    logger.warn(s"[HIPPenaltyDetailsParser][handleErrorResponse] $status error: $errorMsg")
    Left(HIPPenaltyDetailsFailureResponse(response.status))
  }

  private def addMissingLPP1PrincipalChargeLatestClearing(penaltyDetails: PenaltyDetails): PenaltyDetails = {
    val newDetails = penaltyDetails.latePaymentPenalty.flatMap(
      _.lppDetails.map(latePaymentPenalties => latePaymentPenalties.map(
        oldLPPDetails => {
          (oldLPPDetails.penaltyCategory, oldLPPDetails.penaltyStatus, oldLPPDetails.principalChargeLatestClearing.isEmpty) match {
            case (LPPPenaltyCategoryEnum.FirstPenalty, Some(LPPPenaltyStatusEnum.Posted), true) =>
              val filteredList = latePaymentPenalties.filter(_.penaltyCategory.equals(LPPPenaltyCategoryEnum.SecondPenalty))
                .filter(_.principalChargeReference.equals(oldLPPDetails.principalChargeReference))
              val optPrincipalChargeClearingDate: Option[LocalDate] = filteredList.headOption.flatMap(_.principalChargeLatestClearing)
              oldLPPDetails.copy(principalChargeLatestClearing = optPrincipalChargeClearingDate)
            case _ =>
              oldLPPDetails
          }
        }
      )
      )
    )
    if (newDetails.nonEmpty) {
      penaltyDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(newDetails, penaltyDetails.latePaymentPenalty.get.manualLPPIndicator)))
    } else {
      penaltyDetails
    }
  }
}