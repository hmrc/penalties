/*
 * Copyright 2026 HM Revenue & Customs
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

package models.getPenaltyDetails.latePayment

import models.hipPenaltyDetails.{latePayment => hip}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class TimeToPaySpec extends AnyWordSpec with Matchers {

  private val aDate: LocalDate = LocalDate.of(2024, 1, 1)

  private val fullModel: hip.TimeToPay = hip.TimeToPay(
    ttpStartDate = Some(aDate),
    ttpEndDate = Some(aDate),
    ttpProposalDate = Some(aDate),
    ttpAgreementDate = Some(aDate)
  )
  private val fullJson: JsValue = Json.parse(
    """
      |{
      |  "ttpStartDate": "2024-01-01",
      |  "ttpEndDate": "2024-01-01",
      |  "ttpProposalDate": "2024-01-01",
      |  "ttpAgreementDate": "2024-01-01"
      |}
          """.stripMargin
  )

  private val emptyModel: hip.TimeToPay = hip.TimeToPay(None, None, None, None)
  private val emptyJson: JsValue        = Json.parse("{}")

  "TimeToPay" should {
    "be able to deserialize from JSON and serialise back to JSON" when {
      "all fields are present" in {
        val model = fullJson.as[hip.TimeToPay]

        model shouldBe fullModel
        Json.toJson(model) shouldBe fullJson
      }

      "all fields are None" in {
        val model = emptyJson.as[hip.TimeToPay]

        model shouldBe emptyModel
        Json.toJson(model) shouldBe emptyJson
      }
    }
  }
}
