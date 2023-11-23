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

package models.getPenaltyDetails.latePayment

import base.SpecBase
import models.getFinancialDetails.MainTransactionEnum
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
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
      |   "penaltyAmountPosted": 0.00,
      |   "penaltyAmountAccruing": 144.21,
      |   "LPP1LRCalculationAmount": 144.21,
      |   "LPP1LRDays": "15",
      |   "LPP1LRPercentage": 2.00,
      |   "LPP1HRCalculationAmount": 144.21,
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
      |     "appealLevel": "01",
      |     "appealDescription": "Some value"
      |   }],
      |   "principalChargeDocNumber": "123456789012",
      |   "principalChargeMainTransaction": "4700",
      |   "principalChargeSubTransaction": "1174",
      |   "principalChargeBillingFrom": "2022-10-30",
      |   "principalChargeBillingTo": "2022-10-30",
      |   "principalChargeDueDate": "2022-10-30",
      |   "principalChargeDocNumber": "DOC1",
      |   "principalChargeSubTransaction": "SUB1"
      | },
      | {
      |   "penaltyChargeReference": "12345678901234",
      |   "penaltyCategory": "LPP1",
      |   "penaltyStatus": "A",
      |   "penaltyAmountPosted": 0.00,
      |   "penaltyAmountAccruing": 144.21,
      |   "LPP1LRCalculationAmount": 144.21,
      |   "LPP1LRDays": "15",
      |   "LPP1LRPercentage": 2.00,
      |   "LPP1HRCalculationAmount": 144.21,
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
      |     "appealLevel": "01",
      |     "appealDescription": "Some value"
      |   }],
      |   "principalChargeDocNumber": "123456789012",
      |   "principalChargeMainTransaction": "4700",
      |   "principalChargeSubTransaction": "1174",
      |   "principalChargeBillingFrom": "2022-10-30",
      |   "principalChargeBillingTo": "2022-10-30",
      |   "principalChargeDueDate": "2022-10-30",
      |   "principalChargeDocNumber": "DOC1",
      |   "principalChargeSubTransaction": "SUB1"
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
      |   "penaltyAmountPosted": 0.00,
      |   "penaltyAmountAccruing": 144.21,
      |   "LPP1LRCalculationAmount": 144.21,
      |   "LPP1LRDays": "15",
      |   "LPP1LRPercentage": 2.00,
      |   "LPP1HRCalculationAmount": 144.21,
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
      |     "appealLevel": "01",
      |     "appealDescription": "Some value"
      |   }],
      |   "principalChargeBillingFrom": "2022-10-30",
      |   "principalChargeBillingTo": "2022-10-30",
      |   "principalChargeDueDate": "2022-10-30",
      |   "principalChargeMainTransaction": "4700",
      |   "principalChargeDocNumber": "DOC1",
      |   "principalChargeSubTransaction": "SUB1"
      | },
      | {
      |   "penaltyChargeReference": "12345678901234",
      |   "penaltyCategory": "LPP1",
      |   "penaltyStatus": "A",
      |   "penaltyAmountPosted": 0.00,
      |   "penaltyAmountAccruing": 144.21,
      |   "LPP1LRCalculationAmount": 144.21,
      |   "LPP1LRDays": "15",
      |   "LPP1LRPercentage": 2.00,
      |   "LPP1HRCalculationAmount": 144.21,
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
      |     "appealLevel": "01",
      |     "appealDescription": "Some value"
      |   }],
      |   "principalChargeBillingFrom": "2022-10-30",
      |   "principalChargeBillingTo": "2022-10-30",
      |   "principalChargeDueDate": "2022-10-30",
      |   "principalChargeDocNumber": "DOC1",
      |   "principalChargeSubTransaction": "SUB1",
      |   "principalChargeMainTransaction": "4700"
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
          penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
          penaltyStatus = LPPPenaltyStatusEnum.Accruing,
          appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
          principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
          principalChargeBillingTo = LocalDate.of(2022, 10, 30),
          principalChargeDueDate = LocalDate.of(2022, 10, 30),
          communicationsDate = Some(LocalDate.of(2022, 10, 30)),
          penaltyAmountOutstanding = None,
          penaltyAmountPaid = None,
          penaltyAmountPosted = 0,
          LPP1LRDays = Some("15"),
          LPP1HRDays = Some("31"),
          LPP2Days = Some("31"),
          LPP1HRCalculationAmount = Some(144.21),
          LPP1LRCalculationAmount = Some(144.21),
          LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
          LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
          LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
          penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
          principalChargeLatestClearing = None,
          metadata = LPPDetailsMetadata(
            principalChargeDocNumber = Some("DOC1"),
            principalChargeSubTransaction = Some("SUB1")
          ),
          penaltyAmountAccruing = BigDecimal(144.21),
          principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
          vatOutstandingAmount = None
        ),
        LPPDetails(
          penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
          principalChargeReference = "1234567890",
          penaltyChargeReference = Some("12345678901234"),
          penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
          penaltyStatus = LPPPenaltyStatusEnum.Accruing,
          appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
          principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
          principalChargeBillingTo = LocalDate.of(2022, 10, 30),
          principalChargeDueDate = LocalDate.of(2022, 10, 30),
          communicationsDate = Some(LocalDate.of(2022, 10, 30)),
          penaltyAmountOutstanding = None,
          penaltyAmountPaid = None,
          penaltyAmountPosted = 0,
          LPP1LRDays = Some("15"),
          LPP1HRDays = Some("31"),
          LPP2Days = Some("31"),
          LPP1HRCalculationAmount = Some(144.21),
          LPP1LRCalculationAmount = Some(144.21),
          LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
          LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
          LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
          penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
          principalChargeLatestClearing = None,
          metadata = LPPDetailsMetadata(
            principalChargeDocNumber = Some("DOC1"),
            principalChargeSubTransaction = Some("SUB1")
          ),
          penaltyAmountAccruing = BigDecimal(144.21),
          principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
          vatOutstandingAmount = None
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
