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

package connectors.parsers.getPenaltyDetails

import models.failure.{FailureCodeEnum, FailureResponse}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment.{LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Logger.logger
import utils.PagerDutyHelper
import utils.PagerDutyHelper.PagerDutyKeys._

import java.time.LocalDate
import scala.util.Try

object GetPenaltyDetailsParser {
  sealed trait GetPenaltyDetailsFailure

  sealed trait GetPenaltyDetailsSuccess

  case class GetPenaltyDetailsSuccessResponse(penaltyDetails: GetPenaltyDetails) extends GetPenaltyDetailsSuccess

  case class GetPenaltyDetailsFailureResponse(status: Int) extends GetPenaltyDetailsFailure

  case object GetPenaltyDetailsMalformed extends GetPenaltyDetailsFailure

  case object GetPenaltyDetailsNoContent extends GetPenaltyDetailsFailure

  type GetPenaltyDetailsResponse = Either[GetPenaltyDetailsFailure, GetPenaltyDetailsSuccess]

  implicit object GetPenaltyDetailsReads extends HttpReads[GetPenaltyDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetPenaltyDetailsResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[GetPenaltyDetailsReads][read] Json response: ${response.json}")
          response.json.validate[GetPenaltyDetails] match {
            case JsSuccess(getPenaltyDetails, _) =>
              logger.debug(s"[GetPenaltyDetailsReads][read] Model: $getPenaltyDetails")
              Right(GetPenaltyDetailsSuccessResponse(addMissingLPP1PrincipalChargeLatestClearing(getPenaltyDetails)))
            case JsError(errors) =>
              logger.debug(s"[GetPenaltyDetailsReads][read] Json validation errors: $errors")
              Left(GetPenaltyDetailsMalformed)
          }
        case NOT_FOUND if response.body.nonEmpty => {
          Try(handleNotFoundStatusBody(response.json)).fold(parseError => {
            logger.error(s"[GetPenaltyDetailsReads][read] Could not parse 404 body with error ${parseError.getMessage}")
            PagerDutyHelper.log("GetPenaltyDetailsReads", INVALID_JSON_RECEIVED_FROM_1812_API)
            Left(GetPenaltyDetailsFailureResponse(NOT_FOUND))
          }, identity)
        }
        case status@(NOT_FOUND | BAD_REQUEST | CONFLICT | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) => {
          PagerDutyHelper.logStatusCode("GetPenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(s"[GetPenaltyDetailsReads][read] Received $status when trying to call GetPenaltyDetails - with body: ${response.body}")
          Left(GetPenaltyDetailsFailureResponse(status))
        }
        case status@NO_CONTENT => {
          logger.debug(s"[GetPenaltyDetailsReads][read] Received 204 when calling ETMP")
          Left(GetPenaltyDetailsFailureResponse(status))
        }
        case status@UNPROCESSABLE_ENTITY => {
          PagerDutyHelper.log("GetPenaltyDetailsReads", RECEIVED_4XX_FROM_1812_API)
          logger.error(s"[GetPenaltyDetailsReads][read] Received 422 when trying to call GetPenaltyDetails - with body: ${response.body}")
          Left(GetPenaltyDetailsFailureResponse(status))
        }
        case _@status =>
          PagerDutyHelper.logStatusCode("GetPenaltyDetailsReads", status)(RECEIVED_4XX_FROM_1812_API, RECEIVED_5XX_FROM_1812_API)
          logger.error(s"[GetPenaltyDetailsReads][read] Received unexpected response from GetPenaltyDetails, status code: $status and body: ${response.body}")
          Left(GetPenaltyDetailsFailureResponse(status))
      }
    }
  }

  private def handleNotFoundStatusBody(responseBody: JsValue): GetPenaltyDetailsResponse = {
    (responseBody \ "failures").validate[Seq[FailureResponse]].fold(
      errors => {
        logger.debug(s"[GetPenaltyDetailsReads][read] - Parsing errors: $errors")
        logger.error(s"[GetPenaltyDetailsReads][read] - Could not parse 404 body returned from GetPenaltyDetails call")
        Left(GetPenaltyDetailsFailureResponse(NOT_FOUND))
      },
      failures => {
        if (failures.exists(_.code.equals(FailureCodeEnum.NoDataFound))) {
          Left(GetPenaltyDetailsNoContent)
        } else {
          logger.error(s"[GetPenaltyDetailsReads][read] - Received following errors from GetPenaltyDetails 404 call: $failures")
          Left(GetPenaltyDetailsFailureResponse(NOT_FOUND))
        }
      }
    )
  }

  private def addMissingLPP1PrincipalChargeLatestClearing(penaltyDetails: GetPenaltyDetails): GetPenaltyDetails = {
    val newDetails = penaltyDetails.latePaymentPenalty.flatMap(
      _.details.map(latePaymentPenalties => latePaymentPenalties.map(
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
