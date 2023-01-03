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

package controllers.testOnly

import config.featureSwitches.{FeatureSwitch, FeatureSwitching}
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logger.logger

import java.time.LocalDate
import javax.inject.Inject
import scala.util.Try

class FeatureSwitchController @Inject()(cc: ControllerComponents)
                                       (implicit val config: Configuration) extends BackendController(cc) with FeatureSwitching {
  def enableOrDisableFeature(name: String, enable: Boolean): Action[AnyContent] = Action {
    val matchedFeatureSwitch: Option[FeatureSwitch] = FeatureSwitch.listOfAllFeatureSwitches.find(_.name == name)
    matchedFeatureSwitch.fold[Result](NotFound)(
      featureSwitch => {
        if (enable) {
          enableFeatureSwitch(featureSwitch)
        } else {
          disableFeatureSwitch(featureSwitch)
        }
        Ok(s"$featureSwitch set to $enable")
      })
  }

  def setTimeMachineDate(dateToSet: Option[String]): Action[AnyContent] = Action {
    dateToSet.fold({
      super.setTimeMachineDate(None)
      Ok(s"Time machine set to: ${LocalDate.now()}")
    })(
      dateAsString => {
        Try(LocalDate.parse(dateAsString)).fold(
          err => {
            logger.debug(s"[FeatureSwitchController][setDateFeature] - Exception was thrown when setting time machine date: ${err.getMessage}")
            BadRequest("The date provided is in an invalid format")
          },
          date => {
            setTimeMachineDate(Some(date))
            Ok(s"Time machine set to: $date")
          }
        )
      }
    )
  }
}
