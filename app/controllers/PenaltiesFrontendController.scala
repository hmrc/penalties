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

package controllers

import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsSuccessResponse, _}
import play.api.mvc._
import services.{GetPenaltyDetailsService, PenaltiesFrontendService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{PagerDutyHelper, RegimeHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PenaltiesFrontendController @Inject()(
                                             getPenaltyDetailsService: GetPenaltyDetailsService,
                                             penaltiesFrontendService: PenaltiesFrontendService,
                                             cc: ControllerComponents
                                           )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getPenaltiesData(enrolmentKey: String, arn: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request => {
      val vrn: String = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
      getPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(vrn).flatMap {
        _.fold({
          case GetPenaltyDetailsNoContent => {
            logger.info(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned 404 for VRN: $vrn with NO_DATA_FOUND in response body")
            Future(NoContent)
          }
          case GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND => {
            logger.info(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned 404 for VRN: $vrn")
            Future(NotFound(s"A downstream call returned 404 for VRN: $vrn"))
          }
          case GetPenaltyDetailsFailureResponse(status) => {
            logger.error(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned an unexpected status: $status for VRN: $vrn")
            Future(InternalServerError(s"A downstream call returned an unexpected status: $status"))
          }
          case GetPenaltyDetailsMalformed => {
            PagerDutyHelper.log("getPenaltiesData", MALFORMED_RESPONSE_FROM_1812_API)
            logger.error(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned invalid body - failed to parse penalty details response for VRN: $vrn")
            Future(InternalServerError(s"We were unable to parse penalty data."))
          }
        },
          penaltyDetailsSuccess => {
            logger.info(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned 200 for VRN: $vrn")
            penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(penaltyDetailsSuccess.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, enrolmentKey, arn)
          }
        )
      }
    }
  }
}
