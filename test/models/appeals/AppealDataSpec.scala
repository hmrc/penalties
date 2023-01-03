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

package models.appeals

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsResult, JsValue, Json}

import java.time.LocalDate

class AppealDataSpec extends AnyWordSpec with Matchers {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      | "type": "LATE_SUBMISSION",
      | "startDate": "2021-01-01",
      | "endDate": "2021-02-01",
      | "dueDate": "2021-03-07",
      | "dateCommunicationSent": "2021-03-08"
      |}
      |""".stripMargin)

  val model: AppealData = AppealData(
    `type` = AppealTypeEnum.Late_Submission,
    startDate = LocalDate.of(2021, 1, 1),
    endDate = LocalDate.of(2021, 2, 1),
    dueDate = LocalDate.of(2021, 3, 7),
    dateCommunicationSent = LocalDate.of(2021, 3, 8)
  )

  "AppealData" should {
    "be writable to JSON" in {
      val resultAsJson: JsValue = Json.toJson(model)
      resultAsJson shouldBe modelAsJson
    }

    "be readable from JSON" in {
      val resultAsModel: JsResult[AppealData] = Json.fromJson(modelAsJson)(AppealData.format)
      resultAsModel.isSuccess shouldBe true
      resultAsModel.get shouldBe model
    }
  }
}
