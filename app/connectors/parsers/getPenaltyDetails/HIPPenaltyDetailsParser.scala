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
    override def read(method: String, url: String, response: HttpResponse): HIPPenaltyDetailsResponse = {
      response.status match {
        case OK =>
          response.json.validate[PenaltyDetails] match {
            case JsSuccess(getPenaltyDetails, _) =>
              logger.info(s"[HIPPenaltyDetailsReads][read] Success HIPPenaltyDetailsSuccessResponse returned from connector.")
              Right(HIPPenaltyDetailsSuccessResponse(addMissingLPP1PrincipalChargeLatestClearing(getPenaltyDetails)))
            case JsError(errors) =>
              logger.error(s"[HIPPenaltyDetailsReads][read] Json validation errors: $errors")
              Left(HIPPenaltyDetailsMalformed)
          }
        case NOT_FOUND if response.body.nonEmpty =>
          Try(handleNotFoundStatusBody(response.json)).fold(
            parseError => {
              logger.error(s"[HIPPenaltyDetailsReads][read] Could not parse 404 body with error ${parseError.getMessage}")
              PagerDutyHelper.log("HIPPenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1812_API)
              Left(HIPPenaltyDetailsFailureResponse(NOT_FOUND))
            },
            identity
          )
        case NO_CONTENT =>
          logger.info("[HIPPenaltyDetailsReads][read] Received no content from 1812 call")
          Left(HIPPenaltyDetailsNoContent)
        case status @ (BAD_REQUEST | FORBIDDEN | NOT_FOUND | CONFLICT | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) =>
          PagerDutyHelper.logStatusCode("HIPPenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(
            s"[HIPPenaltyDetailsReads][read] Received $status when trying to call PenaltyDetails - with body: ${response.body}")
          handleErrorResponse(response)
        case _ @status =>
          PagerDutyHelper.logStatusCode("HIPPenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(
            s"[HIPPenaltyDetailsReads][read] Received unexpected response from PenaltyDetails, status code: $status and body: ${response.body}")
          Left(HIPPenaltyDetailsFailureResponse(status))
      }
    }
  }

  private def handleNotFoundStatusBody(responseBody: JsValue): Either[HIPPenaltyDetailsFailure, Nothing] = {
    val validateFailuresIF = (responseBody \ "failures").validate[Seq[FailureResponse]].asOpt
    val validateErrorsHIP  = (responseBody \ "errors").validate[BusinessError].asOpt
    (validateFailuresIF, validateErrorsHIP) match {
      case (Some(failures), _) if failures.exists(_.code.equals(FailureCodeEnum.NoDataFound)) =>
        Left(HIPPenaltyDetailsNoContent)
      case (_, Some(errors)) if errors.code == "016" && errors.text == "Invalid ID Number" =>
        Left(HIPPenaltyDetailsNoContent)
      case _ =>
        logger.error(s"[HIPPenaltyDetailsReads][read] - Unable to parse 404 body returned from PenaltyDetails call")
        logger.error(s"[HIPPenaltyDetailsReads][read] - Error response body: $responseBody")
        Left(HIPPenaltyDetailsFailureResponse(NOT_FOUND))
    }
  }

  private def handleErrorResponse(response: HttpResponse): Left[HIPPenaltyDetailsFailure, Nothing] = {
    val json                  = Try(response.json).getOrElse(play.api.libs.json.Json.obj())
    val errorOpt              = (json \ "error").validate[TechnicalError].asOpt
    val errorsOpt             = (json \ "errors").validate[Seq[BusinessError]].asOpt
    val hipWrappedErrorsOpt   = (json \ "response").validate[Seq[HipWrappedError]].asOpt

    (errorOpt, errorsOpt, hipWrappedErrorsOpt) match {
      case (Some(error), _, _) =>
        logger.warn(s"[HIPPenaltyDetailsParser][handleErrorResponse] Technical error returned: ${error.code} - ${error.message}")
        Left(HIPPenaltyDetailsFailureResponse(response.status))
      case (_, Some(errors), _) =>
        logger.warn(s"[HIPPenaltyDetailsParser][handleErrorResponse] Business errors returned: ${errors.mkString("\n")}")
        Left(HIPPenaltyDetailsFailureResponse(response.status))
      case (_, _, Some(hipWrappedErrors)) =>
        val errorMessages = hipWrappedErrors.map(err => s"${err.`type`} - ${err.reason}").mkString(", ")
        logger.warn(s"[HIPPenaltyDetailsParser][handleErrorResponse] HIP wrapped errors returned: $errorMessages")
        Left(HIPPenaltyDetailsFailureResponse(response.status))
      case (None, None, None) =>
        logger.error("[HIPPenaltyDetailsParser][handleErrorResponse] No recognizable error structure found")
        Left(HIPPenaltyDetailsFailureResponse(response.status))
    }
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