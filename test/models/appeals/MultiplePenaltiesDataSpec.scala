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
import play.api.libs.json.Json

import java.time.LocalDate

class MultiplePenaltiesDataSpec extends AnyWordSpec with Matchers {
  "be writable to JSON" in {
    val model = MultiplePenaltiesData(
      firstPenaltyChargeReference = "123456789",
      firstPenaltyAmount = 101.01,
      secondPenaltyChargeReference = "123456790",
      secondPenaltyAmount = 101.02,
      firstPenaltyCommunicationDate = LocalDate.of(2022, 8, 8),
      secondPenaltyCommunicationDate = LocalDate.of(2022, 8, 9),
    )
    val expectedResult = Json.parse(
      """
        |{
        | "firstPenaltyChargeReference": "123456789",
        | "firstPenaltyAmount": 101.01,
        | "secondPenaltyChargeReference": "123456790",
        | "secondPenaltyAmount": 101.02,
        | "firstPenaltyCommunicationDate": "2022-08-08",
        | "secondPenaltyCommunicationDate": "2022-08-09"
        |}
        |""".stripMargin)
    val result = Json.toJson(model)
    result shouldBe expectedResult
  }
}
