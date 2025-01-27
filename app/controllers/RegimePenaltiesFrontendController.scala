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

package controllers

import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{GetPenaltyDetailsSuccessResponse, _}
import controllers.auth.AuthAction
import models.EnrolmentKey
import play.api.mvc._
import services.{PenaltyDetailsService, RegimePenaltiesFrontendService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegimePenaltiesFrontendController @Inject()(
                                             getPenaltyDetailsService: PenaltyDetailsService,
                                             penaltiesFrontendService: RegimePenaltiesFrontendService,
                                             cc: ControllerComponents,
                                             authAction: AuthAction
                                           )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getPenaltiesData(enrolmentKey: EnrolmentKey, arn: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request => {
      getPenaltyDetailsService.getDataFromPenaltyService(enrolmentKey).flatMap {
        _.fold({
          case GetPenaltyDetailsNoContent => {
            logger.info(s"[RegimePenaltiesFrontendController][getPenaltiesData] - 1812 call returned 404 for ${enrolmentKey.info} with NO_DATA_FOUND in response body")
            Future(NoContent)
          }
          case GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND => {
            logger.info(s"[RegimePenaltiesFrontendController][getPenaltiesData] - 1812 call returned 404 for ${enrolmentKey.info}")
            Future(NotFound(s"A downstream call returned 404 for ${enrolmentKey.info}"))
          }
          case GetPenaltyDetailsFailureResponse(status) => {
            logger.error(s"[RegimePenaltiesFrontendController][getPenaltiesData] - 1812 call returned an unexpected status: $status for ${enrolmentKey.info}")
            Future(InternalServerError(s"A downstream call returned an unexpected status: $status"))
          }
          case GetPenaltyDetailsMalformed => {
            PagerDutyHelper.log("getPenaltiesData", MALFORMED_RESPONSE_FROM_1812_API)
            logger.error(s"[RegimePenaltiesFrontendController][getPenaltiesData] - 1812 call returned invalid body - failed to parse penalty details response for ${enrolmentKey.info}")
            Future(InternalServerError(s"We were unable to parse penalty data."))
          }
        },
          penaltyDetailsSuccess => {
            logger.info(s"[RegimePenaltiesFrontendController][getPenaltiesData] - 1812 call returned 200 for ${enrolmentKey.info}")
            penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(penaltyDetailsSuccess.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, enrolmentKey, arn)
          }
        )
      }
    }
  }
}
