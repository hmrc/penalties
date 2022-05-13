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

package models.v3.getPenaltyDetails.latePayment

import base.SpecBase
import models.v3.getPenaltyDetails.AppealInformation
import play.api.libs.json.{JsResult, JsValue, Json}

import java.time.LocalDate

class LatePaymentPenaltySpec extends SpecBase {
  val receivedJson: JsValue = Json.parse(
    """
      |{
      | "details": [{
      |   "penaltyChargeReference": "12345678901235",
      |   "penaltyCategory": "LPP1",
      |   "penaltyStatus": "A",
      |   "penaltyAmountAccruing": 99.99,
      |   "penaltyAmountPosted": 1001.45,
      |   "penaltyAmountPaid": 1001.45,
      |   "penaltyAmountOutstanding": 99.99,
      |   "LPP1LRCalculationAmount": 99.99,
      |   "LPP1LRDays": "15",
      |   "LPP1LRPercentage": 2.00,
      |   "LPP1HRCalculationAmount": 99.99,
      |   "LPP1HRDays": "31",
      |   "LPP1HRPercentage": 2.00,
      |   "LPP2Days": "31",
      |   "LPP2Percentage": 4.00,
      |   "penaltyChargeCreationDate": "2022-10-30",
      |   "communicationsDate": "2022-10-30",
      |   "penaltyChargeDueDate": "2022-10-30",
      |   "principalChargeReference": "1234567890",
      |   "appealInformation":
      |   [{
      |     "appealStatus": "99",
      |     "appealLevel": "01"
      |   }],
      |   "principalChargeDocNumber": "123456789012",
      |   "principalChargeMainTransaction": "4700",
      |   "principalChargeSubTransaction": "1174",
      |   "principalChargeBillingFrom": "2022-10-30",
      |   "principalChargeBillingTo": "2022-10-30",
      |   "principalChargeDueDate": "2022-10-30"
      | },
      | {
      |   "penaltyChargeReference": "12345678901234",
      |   "penaltyCategory": "LPP1",
      |   "penaltyStatus": "A",
      |   "penaltyAmountAccruing": 99.99,
      |   "penaltyAmountPosted": 1001.45,
      |   "penaltyAmountPaid": 1001.45,
      |   "penaltyAmountOutstanding": 99.99,
      |   "LPP1LRCalculationAmount": 99.99,
      |   "LPP1LRDays": "15",
      |   "LPP1LRPercentage": 2.00,
      |   "LPP1HRCalculationAmount": 99.99,
      |   "LPP1HRDays": "31",
      |   "LPP1HRPercentage": 2.00,
      |   "LPP2Days": "31",
      |   "LPP2Percentage": 4.00,
      |   "penaltyChargeCreationDate": "2022-10-30",
      |   "communicationsDate": "2022-10-30",
      |   "penaltyChargeDueDate": "2022-10-30",
      |   "principalChargeReference": "1234567890",
      |   "appealInformation":
      |   [{
      |     "appealStatus": "99",
      |     "appealLevel": "01"
      |   }],
      |   "principalChargeDocNumber": "123456789012",
      |   "principalChargeMainTransaction": "4700",
      |   "principalChargeSubTransaction": "1174",
      |   "principalChargeBillingFrom": "2022-10-30",
      |   "principalChargeBillingTo": "2022-10-30",
      |   "principalChargeDueDate": "2022-10-30"
      | }]
      |}
      |""".stripMargin
  )

  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      | "details": [{
      |   "penaltyChargeReference": "12345678901235",
      |   "penaltyCategory": "LPP1",
      |   "penaltyStatus": "A",
      |   "penaltyAmountPaid": 1001.45,
      |   "penaltyAmountOutstanding": 99.99,
      |   "LPP1LRCalculationAmount": 99.99,
      |   "LPP1LRDays": "15",
      |   "LPP1LRPercentage": 2.00,
      |   "LPP1HRCalculationAmount": 99.99,
      |   "LPP1HRDays": "31",
      |   "LPP1HRPercentage": 2.00,
      |   "LPP2Days": "31",
      |   "LPP2Percentage": 4.00,
      |   "penaltyChargeCreationDate": "2022-10-30",
      |   "communicationsDate": "2022-10-30",
      |   "penaltyChargeDueDate": "2022-10-30",
      |   "principalChargeReference": "1234567890",
      |   "appealInformation":
      |   [{
      |     "appealStatus": "99",
      |     "appealLevel": "01"
      |   }],
      |   "principalChargeBillingFrom": "2022-10-30",
      |   "principalChargeBillingTo": "2022-10-30",
      |   "principalChargeDueDate": "2022-10-30"
      | },
      | {
      |   "penaltyChargeReference": "12345678901234",
      |   "penaltyCategory": "LPP1",
      |   "penaltyStatus": "A",
      |   "penaltyAmountPaid": 1001.45,
      |   "penaltyAmountOutstanding": 99.99,
      |   "LPP1LRCalculationAmount": 99.99,
      |   "LPP1LRDays": "15",
      |   "LPP1LRPercentage": 2.00,
      |   "LPP1HRCalculationAmount": 99.99,
      |   "LPP1HRDays": "31",
      |   "LPP1HRPercentage": 2.00,
      |   "LPP2Days": "31",
      |   "LPP2Percentage": 4.00,
      |   "penaltyChargeCreationDate": "2022-10-30",
      |   "communicationsDate": "2022-10-30",
      |   "penaltyChargeDueDate": "2022-10-30",
      |   "principalChargeReference": "1234567890",
      |   "appealInformation":
      |   [{
      |     "appealStatus": "99",
      |     "appealLevel": "01"
      |   }],
      |   "principalChargeBillingFrom": "2022-10-30",
      |   "principalChargeBillingTo": "2022-10-30",
      |   "principalChargeDueDate": "2022-10-30"
      | }]
      |}
      |""".stripMargin
  )

  val model: LatePaymentPenalty = LatePaymentPenalty(
    details = Some(
      Seq(
        LPPDetails(
          penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
          principalChargeReference = "1234567890",
          penaltyChargeReference = Some("12345678901235"),
          penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
          penaltyStatus = LPPPenaltyStatusEnum.Accruing,
          appealInformation = Some(Seq(AppealInformation(appealStatus = Some("99"), appealLevel = Some("01")))),
          principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
          principalChargeBillingTo = LocalDate.of(2022, 10, 30),
          principalChargeDueDate = LocalDate.of(2022, 10, 30),
          communicationsDate = LocalDate.of(2022, 10, 30),
          penaltyAmountOutstanding = Some(99.99),
          penaltyAmountPaid = Some(1001.45),
          LPP1LRDays = Some("15"),
          LPP1HRDays = Some("31"),
          LPP2Days = Some("31"),
          LPP1HRCalculationAmount = Some(99.99),
          LPP1LRCalculationAmount = Some(99.99),
          LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
          LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
          LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
          penaltyChargeDueDate = LocalDate.of(2022, 10, 30)
        ),
        LPPDetails(
          penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
          principalChargeReference = "1234567890",
          penaltyChargeReference = Some("12345678901234"),
          penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
          penaltyStatus = LPPPenaltyStatusEnum.Accruing,
          appealInformation = Some(Seq(AppealInformation(appealStatus = Some("99"), appealLevel = Some("01")))),
          principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
          principalChargeBillingTo = LocalDate.of(2022, 10, 30),
          principalChargeDueDate = LocalDate.of(2022, 10, 30),
          communicationsDate = LocalDate.of(2022, 10, 30),
          penaltyAmountOutstanding = Some(99.99),
          penaltyAmountPaid = Some(1001.45),
          LPP1LRDays = Some("15"),
          LPP1HRDays = Some("31"),
          LPP2Days = Some("31"),
          LPP1HRCalculationAmount = Some(99.99),
          LPP1LRCalculationAmount = Some(99.99),
          LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
          LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
          LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
          penaltyChargeDueDate = LocalDate.of(2022, 10, 30)
        )
      )
    )
  )

  "be readable from JSON" in {
    val result: JsResult[LatePaymentPenalty] = Json.fromJson(receivedJson)(LatePaymentPenalty.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result: JsValue = Json.toJson(model)(LatePaymentPenalty.format)
    result shouldBe jsonRepresentingModel
  }
}
