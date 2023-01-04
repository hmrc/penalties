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

import base.SpecBase
import models.appeals.reasonableExcuses.ReasonableExcuse._
import play.api.libs.json.{JsValue, Json}

class ReasonableExcuseSpec extends SpecBase {

  "allReasonableExcuses" should {
    "return all reasonable excuses" in {
      val expectedResult = Seq(
        Bereavement,
        Crime,
        FireOrFlood,
        Health,
        LossOfStaff,
        TechnicalIssues,
        Other
      )
      val result = ReasonableExcuse.allReasonableExcuses
      result shouldBe expectedResult
    }
  }

  "allExcusesToJson" should {
    "return the JSON equivalent of all active excuses" in {
      val jsonExpectedToReturn: JsValue = Json.parse(
        """
          |{
          |  "excuses": [
          |    {
          |      "type": "bereavement",
          |      "descriptionKey": "reasonableExcuses.bereavementReason"
          |    },
          |    {
          |      "type": "crime",
          |      "descriptionKey": "reasonableExcuses.crimeReason"
          |    },
          |    {
          |      "type": "fireOrFlood",
          |      "descriptionKey": "reasonableExcuses.fireOrFloodReason"
          |    },
          |    {
          |      "type": "health",
          |      "descriptionKey": "reasonableExcuses.healthReason"
          |    },
          |    {
          |      "type": "lossOfStaff",
          |      "descriptionKey": "reasonableExcuses.lossOfStaffReason"
          |    },
          |    {
          |      "type": "technicalIssues",
          |      "descriptionKey": "reasonableExcuses.technicalIssuesReason"
          |    },
          |    {
          |      "type": "other",
          |      "descriptionKey": "reasonableExcuses.otherReason"
          |    }
          |  ]
          |}
          |""".stripMargin)
        val result = ReasonableExcuse.allExcusesToJson(appConfig)
        result shouldBe jsonExpectedToReturn
    }

  }
}
