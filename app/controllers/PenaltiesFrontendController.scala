/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.parsers.ETMPPayloadParser.GetETMPPayloadNoContent
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser.GetPenaltyDetailsSuccessResponse
import featureSwitches.{FeatureSwitching, UseAPI1812Model}

import javax.inject.Inject
import models.auditing.UserHasPenaltyAuditModel
import models.auditing.v2.{UserHasPenaltyAuditModel => AuditModelV2}
import models.v3.getPenaltyDetails.GetPenaltyDetails
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import services.{ETMPService, GetPenaltyDetailsService}
import services.auditing.AuditService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger
import utils.RegimeHelper

import scala.concurrent.ExecutionContext.Implicits.global

class PenaltiesFrontendController @Inject()(etmpService: ETMPService,
                               auditService: AuditService,
                               getPenaltyDetailsService: GetPenaltyDetailsService,
                               cc: ControllerComponents)
  extends BackendController(cc) with FeatureSwitching {

  def getPenaltiesData(enrolmentKey: String, arn: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request => {
      if(!isEnabled(UseAPI1812Model)) {
        etmpService.getPenaltyDataFromETMPForEnrolment(enrolmentKey).map {
          result => {
            result._1.fold {
              result._2 match {
                case Left(GetETMPPayloadNoContent) => NotFound(s"Could not retrieve ETMP penalty data for $enrolmentKey")
                case _ => InternalServerError("Something went wrong.")
              }
            }(
              etmpData => {
                if (etmpData.pointsTotal > 0) {
                  val auditModel = UserHasPenaltyAuditModel(etmpData, RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
                    RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey),
                    arn)
                  auditService.audit(auditModel)
                }
                Ok(Json.toJson(etmpData))
              }
            )
          }
        }
      } else {
        getPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(enrolmentKey).map {
          _.fold({
            case GetPenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) if status == NOT_FOUND => {
              logger.info(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned 404 for VRN: $enrolmentKey")
              NotFound(s"A downstream call returned 404 for VRN: $enrolmentKey")
            }
            case GetPenaltyDetailsParser.GetPenaltyDetailsFailureResponse(status) => {
              logger.info(s"[PenaltiesFrontendController][getPenaltiesData] - 1812 call returned an unexpected status: $status")
              InternalServerError(s"A downstream call returned an unexpected status: $status")
            }
            case GetPenaltyDetailsParser.GetPenaltyDetailsMalformed => {
              logger.error(s"[PenaltiesFrontendController][getPenaltiesData] - Failed to parse penalty details response")
              InternalServerError(s"We were unable to parse penalty data.")
            }
          },
            success => {
              returnResponse(success.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails, enrolmentKey, arn)
            }
          )
        }
      }
    }
  }

  private def returnResponse(penaltyDetails: GetPenaltyDetails, enrolmentKey: String, arn: Option[String] = None) (implicit request: Request[_]): Result ={
    if (penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0) > 0) {
      val auditModel = AuditModelV2(penaltyDetails, RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
        RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey),
        arn)
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(penaltyDetails))
  }
}
