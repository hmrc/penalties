/*
 * Copyright 2022 HM Revenue & Customs
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

package models.v2.latePaymentPenalty

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class LatePaymentPenaltySpec extends SpecBase {
  val lppWithoutAppealStatus: JsValue = Json.parse(
    """
      |{
      |	"penaltyNumber": "1234ABCD",
      |	"penaltyCategory": "LPP1",
      |	"penaltyStatus": "P",
      |	"penaltyAmountAccruing": "123.45",
      |	"penaltyAmountPosted": "123.45",
      |	"penaltyChargeCreationDate": "2022-01-01",
      |	"penaltyChargeDueDate": "2022-02-01",
      |	"communicationsDate": "2022-01-01",
      |	"penaltyChargeReference": "1234DCBA",
      |  "principalChargeDueDate": "2022-03-01",
      | "principalChargeReference": "CHARGING12345"
      |}
      |
      |""".stripMargin)

  val lppWithAppealStatus: JsValue = Json.parse(
    """
      |{
      |	"penaltyNumber": "1234ABCD",
      |	"penaltyCategory": "LPP1",
      |	"penaltyStatus": "P",
      |	"penaltyAmountAccruing": "123.45",
      |	"penaltyAmountPosted": "123.45",
      |	"penaltyChargeCreationDate": "2022-01-01",
      |	"penaltyChargeDueDate": "2022-02-01",
      |	"communicationsDate": "2022-01-01",
      |	"appealLevel": "1",
      |	"appealStatus": "1",
      |	"penaltyChargeReference": "CHARGE123456",
      | "principalChargeDueDate": "2022-03-01",
      | "principalChargeReference": "CHARGING12345"
      |}
      |""".stripMargin)

  val modelWithAppealStatus: LatePaymentPenalty = LatePaymentPenalty(
    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
    penaltyNumber = "1234ABCD",
    penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
    penaltyChargeDueDate = LocalDate.of(2022, 2, 1),
    penaltyStatus = LPPPenaltyStatusEnum.Posted,
    appealStatus = Some("1"),
    communicationsDate = LocalDate.of(2022, 1, 1),
    principalChargeReference = "CHARGING12345"
  )

  val modelWithoutAppealStatus: LatePaymentPenalty = LatePaymentPenalty(
    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
    penaltyNumber = "1234ABCD",
    penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
    penaltyChargeDueDate = LocalDate.of(2022, 2, 1),
    penaltyStatus = LPPPenaltyStatusEnum.Posted,
    appealStatus = None,
    communicationsDate = LocalDate.of(2022, 1, 1),
    principalChargeReference = "CHARGING12345"
  )

    "be readable from JSON with no appeal status" in {
      val result = Json.fromJson(lppWithoutAppealStatus)(LatePaymentPenalty.format)
      result.isSuccess shouldBe true
      result.get shouldBe modelWithoutAppealStatus
    }

    "be readable from JSON with appeal status" in {
      val result = Json.fromJson(lppWithAppealStatus)(LatePaymentPenalty.format)
      result.isSuccess shouldBe true
      result.get shouldBe modelWithAppealStatus
    }

    "be writable to JSON with no appeal status" in {
      val refinedLPPWithoutAppealStatus: JsValue = Json.parse(
        """
          |{
          |	"penaltyNumber": "1234ABCD",
          |	"penaltyCategory": "LPP1",
          |	"penaltyStatus": "P",
          |	"penaltyChargeCreationDate": "2022-01-01",
          |	"penaltyChargeDueDate": "2022-02-01",
          |	"communicationsDate": "2022-01-01",
          | "principalChargeReference": "CHARGING12345"
          |}
          |""".stripMargin)
      val result = Json.toJson(modelWithoutAppealStatus)(LatePaymentPenalty.format)
      result shouldBe refinedLPPWithoutAppealStatus
    }

    "be writable to JSON with appeal status" in {
      val refinedLPPWithAppealStatus: JsValue = Json.parse(
        """
          |{
          |	"penaltyNumber": "1234ABCD",
          |	"penaltyCategory": "LPP1",
          |	"penaltyStatus": "P",
          |	"penaltyChargeCreationDate": "2022-01-01",
          |	"penaltyChargeDueDate": "2022-02-01",
          |	"communicationsDate": "2022-01-01",
          | "appealStatus": "1",
          | "principalChargeReference": "CHARGING12345"
          |}
          |""".stripMargin)
      val result: JsValue = Json.toJson(modelWithAppealStatus)(LatePaymentPenalty.format)
      result shouldBe refinedLPPWithAppealStatus
    }
  }
