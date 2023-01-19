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

package models.communication

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CommunicationSpec extends AnyWordSpec with Matchers {
  val communicationModelAsJson: JsValue = Json.parse(
    """
      |{
      | "type": "letter",
      | "dateSent": "2021-04-23T18:25:43.511",
      | "documentId": "123456789"
      |}
      |""".stripMargin)

  val communicationModel: Communication = Communication(
    `type` = CommunicationTypeEnum.letter,
    dateSent = LocalDateTime.of(
      2021, 4, 23, 18, 25, 43).plus(511, ChronoUnit.MILLIS),
    documentId = "123456789"
  )
  "Communication" should {
    "be writeable to JSON" in {
      val result = Json.toJson(communicationModel)
      result shouldBe communicationModelAsJson
    }

    "be readable from JSON" in {
      val result = Json.fromJson(communicationModelAsJson)(Communication.format)
      result.isSuccess shouldBe true
      result.get shouldBe communicationModel
    }
  }
}
