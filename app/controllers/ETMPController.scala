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

import connectors.parsers.ETMPPayloadParser.GetETMPPayloadNoContent
import javax.inject.Inject
import models.auditing.UserHasPenaltyAuditModel
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.ETMPService
import services.auditing.AuditService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.RegimeHelper

import scala.concurrent.ExecutionContext.Implicits.global

class ETMPController @Inject()(etmpService: ETMPService,
                               auditService: AuditService,
                               cc: ControllerComponents)
  extends BackendController(cc) {

  def getPenaltiesData(enrolmentKey: String, arn: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request => {
      etmpService.getPenaltyDataFromETMPForEnrolment(enrolmentKey).map {
        result => {
          result._1.fold {
            result._2 match {
              case Left(GetETMPPayloadNoContent) => NotFound(s"Could not retrieve ETMP penalty data for $enrolmentKey")
              case Left(_) => InternalServerError("Something went wrong.")
            }
          }(
            etmpData => {
              if(etmpData.pointsTotal > 0) {
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
    }
  }
}
