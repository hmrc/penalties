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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.ComplianceService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ComplianceController @Inject()(complianceService: ComplianceService,
                                     cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getComplianceData(vrn: String, fromDate: String, toDate: String): Action[AnyContent] = Action.async {
    implicit request => {
      complianceService.getComplianceData(vrn, fromDate, toDate).map {
        _.fold(
          error => Status(error),
          model => {
            logger.info(s"[ComplianceController][getComplianceData] - 1330 call returned 200 for VRN: $vrn")
            Ok(Json.toJson(model))
          }
        )
      }
    }
  }
}
