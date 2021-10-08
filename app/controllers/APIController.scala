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

import javax.inject.Inject
import models.ETMPPayload
import models.api.APIModel
import models.auditing.UserHasPenaltyAuditModel
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import services.ETMPService
import services.auditing.AuditService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.RegimeHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class APIController @Inject()(etmpService: ETMPService,
                              auditService: AuditService,
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
              returnResponseForAPI(etmpPayload, enrolmentKey)
          }
          )
        }
      }
    }
  }

  private def returnResponseForAPI(etmpPayload: ETMPPayload, enrolmentKey: String)(implicit request: Request[_]): Result = {
    val pointsTotal = etmpPayload.pointsTotal
    val penaltyAmountWithEstimateStatus = etmpService.findEstimatedPenaltiesAmount(etmpPayload)
    val noOfEstimatedPenalties = etmpService.getNumberOfEstimatedPenalties(etmpPayload)
    val crystalizedPenaltyAmount = etmpService.getNumberOfCrystalizedPenalties(etmpPayload)
    val crystalizedPenaltyTotal = etmpService.getCrystalisedPenaltyTotal(etmpPayload)
    val hasAnyPenaltyData = etmpService.checkIfHasAnyPenaltyData(etmpPayload)
    val responseData: APIModel = APIModel(
      noOfPoints = pointsTotal,
      noOfEstimatedPenalties = noOfEstimatedPenalties,
      noOfCrystalisedPenalties = crystalizedPenaltyAmount,
      estimatedPenaltyAmount = penaltyAmountWithEstimateStatus,
      crystalisedPenaltyAmountDue = crystalizedPenaltyTotal,
      hasAnyPenaltyData = hasAnyPenaltyData)
    if(pointsTotal > 0) {
      val auditModel = UserHasPenaltyAuditModel(
        etmpPayload = etmpPayload,
        identifier = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
        identifierType = RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey),
        arn = None) //TODO: need to check this
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(responseData))
  }
}
