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

package config.featureSwitches

import play.api.Configuration
import utils.Logger.logger

import java.time.LocalDateTime

trait FeatureSwitching {
  implicit val config: Configuration
  val TIME_MACHINE_NOW = "TIME_MACHINE_NOW"
  val FEATURE_SWITCH_ON = "true"
  val FEATURE_SWITCH_OFF = "false"

  def isEnabled(featureSwitch: FeatureSwitch): Boolean =
    sys.props.get(featureSwitch.name).map(_.toBoolean).getOrElse(config.get[Boolean](featureSwitch.name))

  def enableFeatureSwitch(featureSwitch: FeatureSwitch): Unit =
    sys.props += featureSwitch.name -> FEATURE_SWITCH_ON

  def setTimeMachineDate(dateTimeToSet: Option[LocalDateTime]): Unit = {
    logger.debug(s"[FeatureSwitching][setTimeMachineDate] - setting time machine date to: $dateTimeToSet")
    dateTimeToSet.fold(sys.props -= TIME_MACHINE_NOW)(sys.props += TIME_MACHINE_NOW -> _.toString)
  }

  def getTimeMachineDateTime: LocalDateTime = {
    sys.props.get(TIME_MACHINE_NOW).fold({
      val optDateAsString = config.getOptional[String]("feature.switch.time-machine-now")
      val dateAsString = optDateAsString.getOrElse("")
      if(dateAsString.isEmpty) {
        LocalDateTime.now()
      } else {
        LocalDateTime.parse(dateAsString)
      }
    })(LocalDateTime.parse(_))
  }

  def disableFeatureSwitch(featureSwitch: FeatureSwitch): Unit =
    sys.props += featureSwitch.name -> FEATURE_SWITCH_OFF
}


