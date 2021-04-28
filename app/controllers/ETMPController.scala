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

import config.AppConfig
import connectors.parsers.ETMPPayloadParser.GetETMPPayloadNoContent
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.ETMPService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

class ETMPController @Inject()(appConfig: AppConfig,
                               etmpService: ETMPService,
                               cc: ControllerComponents)
  extends BackendController(cc) {

  def getPenaltiesData(enrolmentKey: String): Action[AnyContent] = Action.async {
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
              Ok(Json.toJson(etmpData))
            }
          )
        }
      }
    }
  }
}
