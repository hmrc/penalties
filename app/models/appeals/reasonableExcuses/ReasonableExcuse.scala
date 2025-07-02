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

package models.appeals.reasonableExcuses

import config.AppConfig
import play.api.libs.json.{JsValue, Json}

sealed trait ReasonableExcuse {
  val descriptionMessageKey: String

  def isEnabled(appConfig: AppConfig): Boolean = {
    appConfig.isReasonableExcuseEnabled(toString)
  }
}

class WithName(string: String) {
  override val toString: String = string
}

object ReasonableExcuse {
  case object Bereavement extends WithName("bereavement") with ReasonableExcuse {
    override val descriptionMessageKey: String = "reasonableExcuses.bereavementReason"
  }

  case object Crime extends WithName("crime") with ReasonableExcuse {
    override val descriptionMessageKey: String = "reasonableExcuses.crimeReason"
  }

  case object FireOrFlood extends WithName("fireOrFlood") with ReasonableExcuse {
    override val descriptionMessageKey: String = "reasonableExcuses.fireOrFloodReason"
  }

  case object Health extends WithName("health") with ReasonableExcuse {
    override val descriptionMessageKey: String = "reasonableExcuses.healthReason"
  }

  case object LossOfStaff extends WithName("lossOfStaff") with ReasonableExcuse {
    override val descriptionMessageKey: String = "reasonableExcuses.lossOfStaffReason"
  }

  case object TechnicalIssues extends WithName("technicalIssues") with ReasonableExcuse {
    override val descriptionMessageKey: String = "reasonableExcuses.technicalIssuesReason"
  }

  case object Other extends WithName("other") with ReasonableExcuse {
    override val descriptionMessageKey: String = "reasonableExcuses.otherReason"
  }

  val allReasonableExcuses: Seq[ReasonableExcuse] = {
    Seq(
      Bereavement,
      Crime,
      FireOrFlood,
      Health,
      LossOfStaff,
      TechnicalIssues,
      Other
    )
  }

  def allExcusesToJson(appConfig: AppConfig): JsValue = {
    val filteredActiveReasonableExcuses: Seq[ReasonableExcuse] = allReasonableExcuses.filter(_.isEnabled(appConfig))
    Json.obj(
      "excuses" -> filteredActiveReasonableExcuses.map {
        excuse => {
          Json.obj(
            "type" -> excuse.toString,
            "descriptionKey" -> excuse.descriptionMessageKey
          )
        }
      }
    )
  }
}
