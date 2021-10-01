/*
 * Copyright 2021 HM Revenue & Customs
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

import models.ETMPPayload
import models.api.APIModel
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.ETMPService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.RegimeHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class APIController @Inject()(etmpService: ETMPService,
                              cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  private val vrnRegex: Regex = "^[0-9]{1,9}$".r

  def getSummaryDataForVRN(vrn: String): Action[AnyContent] = Action.async {
    implicit request => {
      if (!vrn.matches(vrnRegex.regex)) {
        Future(BadRequest(s"VRN: $vrn was not in a valid format."))
      } else {
        val enrolmentKey = RegimeHelper.constructMTDVATEnrolmentKey(vrn)
        etmpService.getPenaltyDataFromETMPForEnrolment(enrolmentKey).map {
          _._1.fold(
            NotFound(s"Unable to find data for VRN: $vrn")
          )(
            etmpPayload => {
              returnResponseForAPI(etmpPayload)
            }
          )
        }
      }
    }
  }
  private def returnResponseForAPI(etmpPayload: ETMPPayload):Result = {
    val pointsTotal = etmpPayload.pointsTotal
    val penaltyAmountWithEstimateStatus = etmpService.findEstimatedPenaltiesAmount(etmpPayload)
    val noOfEstimatedPenalties = etmpService.getNumberOfEstimatedPenalties(etmpPayload)
    val responseData: APIModel = APIModel(
      noOfPoints = pointsTotal,
      noOfEstimatedPenalties = noOfEstimatedPenalties,
      noOfCrystalisedPenalties = 0,
      estimatedPenaltyAmount = penaltyAmountWithEstimateStatus,
      crystalisedPenaltyAmountDue = BigDecimal(0),
      hasAnyPenaltyData = false)
    Ok(Json.toJson(responseData))
  }
}
