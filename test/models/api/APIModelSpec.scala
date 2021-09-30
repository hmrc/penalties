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

package models.api

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsResult, JsValue, Json}

class APIModelSpec extends AnyWordSpec with Matchers {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      | "noOfPoints": 4,
      | "noOfEstimatedPenalties":4
      |}
      |""".stripMargin)

  val model: APIModel = APIModel(
    noOfPoints = 4,
    noOfEstimatedPenalties = 4
  )
  "APIModel" should {
    "be writable to JSON" in {
      val resultAsJson: JsValue = Json.toJson(model)
      resultAsJson shouldBe modelAsJson
    }

    "be readable from JSON" in {
      val resultAsModel: JsResult[APIModel] = Json.fromJson(modelAsJson)(APIModel.format)
      resultAsModel.isSuccess shouldBe true
      resultAsModel.get shouldBe model
    }
  }
}
