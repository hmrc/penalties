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

import models.failure.{FailureCodeEnum, FailureResponse}
import models.penaltyDetails.latePayment.{LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.time.LocalDate
import scala.util.Try
import models.penaltyDetails.PenaltyDetails
import play.api.libs.json.Reads
import play.api.libs.json.Json

object PenaltyDetailsParser {
  sealed trait PenaltyDetailsFailure

  sealed trait PenaltyDetailsSuccess

  sealed trait PenaltyApiError

  case class PenaltyDetailsSuccessResponse(penaltyDetails: PenaltyDetails) extends PenaltyDetailsSuccess

  case class PenaltyDetailsFailureResponse(
      status: Int,
      errorBody: Option[PenaltyApiError] = None
  ) extends PenaltyDetailsFailure

  case class TechnicalError(code: String, message: String, logID: String)
      extends PenaltyApiError

  object TechnicalError {
    implicit val reads: Reads[TechnicalError] = Json.reads[TechnicalError]
  }
  case class BusinessError(code: String, text: String, processingDate: String)
      extends PenaltyApiError

  object BusinessError {
    implicit val reads: Reads[BusinessError] = Json.reads[BusinessError]
  }

  case object PenaltyDetailsMalformed extends PenaltyDetailsFailure

  case object PenaltyDetailsNoContent extends PenaltyDetailsFailure

  type PenaltyDetailsResponse = Either[PenaltyDetailsFailure, PenaltyDetailsSuccess]

  implicit object PenaltyDetailsReads extends HttpReads[PenaltyDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): PenaltyDetailsResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[PenaltyDetailsReads][read] Json response: ${response.json}")
          response.json.validate[PenaltyDetails] match {
            case JsSuccess(getPenaltyDetails, _) =>
              logger.debug(s"[PenaltyDetailsReads][read] Model: $getPenaltyDetails")
              Right(PenaltyDetailsSuccessResponse(addMissingLPP1PrincipalChargeLatestClearing(getPenaltyDetails)))
            case JsError(errors) =>
              logger.debug(s"[PenaltyDetailsReads][read] Json validation errors: $errors")
              Left(PenaltyDetailsMalformed)
          }
        case NOT_FOUND if response.body.nonEmpty => {
          Try(handleNotFoundStatusBody(response.json)).fold(parseError => {
            logger.error(s"[PenaltyDetailsReads][read] Could not parse 404 body with error ${parseError.getMessage}")
            PagerDutyHelper.log("PenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1812_API)
            Left(PenaltyDetailsFailureResponse(NOT_FOUND))
          }, identity)
        }
          case status @ (BAD_REQUEST | CONFLICT | INTERNAL_SERVER_ERROR |
            SERVICE_UNAVAILABLE | UNPROCESSABLE_ENTITY) => {

          val json = response.json

          val maybeError: Option[PenaltyApiError] =
            (json \ "error")
              .validate[TechnicalError]
              .asOpt
              .orElse(
                (json \ "errors").validate[BusinessError].asOpt
              )

          maybeError.foreach {
            case te: TechnicalError =>
              logger.warn(
                s"[PenaltyDetailsReads] Technical error: ${te.code} / ${te.logID}"
              )
            case be: BusinessError =>
              logger.warn(
                s"[PenaltyDetailsReads] Business error: ${be.code} / ${be.text}"
              )
          }

          Left(PenaltyDetailsFailureResponse(status, maybeError))
        }
        case status@NO_CONTENT => {
          logger.debug(s"[PenaltyDetailsReads][read] Received 204 when calling ETMP")
          Left(PenaltyDetailsFailureResponse(status))
        }
        case _@status =>
          PagerDutyHelper.logStatusCode("PenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(s"[PenaltyDetailsReads][read] Received unexpected response from GetPenaltyDetails, status code: $status and body: ${response.body}")
          Left(PenaltyDetailsFailureResponse(status))
      }
    }
  }

  private def handleNotFoundStatusBody(responseBody: JsValue): PenaltyDetailsResponse = {
    (responseBody \ "failures").validate[Seq[FailureResponse]].fold(
      errors => {
        logger.debug(s"[PenaltyDetailsReads][read] - Parsing errors: $errors")
        logger.error(s"[PenaltyDetailsReads][read] - Could not parse 404 body returned fromPenaltyDetailscall")
        Left(PenaltyDetailsFailureResponse(NOT_FOUND))
      },
      failures => {
        if (failures.exists(_.code.equals(FailureCodeEnum.NoDataFound))) {
          Left(PenaltyDetailsNoContent)
        } else {
          logger.error(s"[PenaltyDetailsReads][read] - Received following errors fromPenaltyDetails404 call: $failures")
          Left(PenaltyDetailsFailureResponse(NOT_FOUND))
        }
      }
    )
  }

  private def addMissingLPP1PrincipalChargeLatestClearing(penaltyDetails: PenaltyDetails): PenaltyDetails = {
    val newDetails = penaltyDetails.latePaymentPenalty.flatMap(
      _.lppDetails.map(latePaymentPenalties => latePaymentPenalties.map(
        oldLPPDetails => {
          (oldLPPDetails.penaltyCategory, oldLPPDetails.penaltyStatus, oldLPPDetails.principalChargeLatestClearing.isEmpty) match {
            case (LPPPenaltyCategoryEnum.FirstPenalty, LPPPenaltyStatusEnum.Posted, true) =>
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
      penaltyDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(newDetails, penaltyDetails.latePaymentPenalty.get.ManualLPPIndicator)))
    } else {
      penaltyDetails
    }
  }
}
