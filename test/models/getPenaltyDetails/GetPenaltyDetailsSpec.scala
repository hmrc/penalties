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

package models.getPenaltyDetails

import base.SpecBase
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.lateSubmission._
import play.api.libs.json.{JsResult, JsValue, Json}

import java.time.LocalDate

class GetPenaltyDetailsSpec extends SpecBase {
  val receivedJson: JsValue = Json.parse(
    """
      |{
      | "totalisations": {
      |   "LSPTotalValue": 200,
      |   "penalisedPrincipalTotal": 2000,
      |   "LPPPostedTotal": 165.25,
      |   "LPPEstimatedTotal": 15.26
      | },
      | "lateSubmissionPenalty": {
      |   "summary": {
      |     "activePenaltyPoints": 10,
      |     "inactivePenaltyPoints": 12,
      |     "PoCAchievementDate": "2022-01-01",
      |     "regimeThreshold": 10,
      |     "penaltyChargeAmount": 684.25
      |   },
      |   "details": [
      |     {
      |       "penaltyNumber": "12345678901234",
      |       "penaltyOrder": "01",
      |       "penaltyCategory": "P",
      |       "penaltyStatus": "ACTIVE",
      |       "FAPIndicator": "X",
      |       "penaltyCreationDate": "2022-10-30",
      |       "penaltyExpiryDate": "2022-10-30",
      |       "triggeringProcess": "XYZ",
      |       "expiryReason": "FAP",
      |       "chargeReference": "CHARGE123",
      |       "communicationsDate": "2022-10-30",
      |       "lateSubmissions": [
      |         {
      |           "taxPeriodStartDate": "2022-01-01",
      |           "taxPeriodEndDate": "2022-12-31",
      |           "taxPeriodDueDate": "2023-02-07",
      |           "returnReceiptDate": "2023-02-01",
      |           "taxReturnStatus": "Fulfilled"
      |         }
      |       ],
      |       "appealInformation": [
      |         {
      |           "appealStatus": "99",
      |           "appealLevel": "01"
      |         }
      |       ],
      |       "chargeDueDate": "2022-10-30",
      |       "chargeOutstandingAmount": 200,
      |       "chargeAmount": 200
      |     }
      |   ]
      | },
      | "latePaymentPenalty": {
      |     "details": [{
      |       "penaltyChargeReference": "12345678901234",
      |       "penaltyChargeReference": "1234567890",
      |       "penaltyCategory": "LPP1",
      |       "penaltyStatus": "A",
      |       "penaltyAmountAccruing": 99.99,
      |       "penaltyAmountPosted": 1001.45,
      |       "penaltyAmountPaid": 1001.45,
      |       "penaltyAmountOutstanding": 99.99,
      |       "LPP1LRCalculationAmount": 99.99,
      |       "LPP1LRDays": "15",
      |       "LPP1LRPercentage": 2.00,
      |       "LPP1HRCalculationAmount": 99.99,
      |       "LPP1HRDays": "31",
      |       "LPP1HRPercentage": 2.00,
      |       "LPP2Days": "31",
      |       "LPP2Percentage": 4.00,
      |       "penaltyChargeCreationDate": "2022-10-30",
      |       "communicationsDate": "2022-10-30",
      |       "penaltyChargeDueDate": "2022-10-30",
      |       "principalChargeReference": "1234567890",
      |       "appealInformation":
      |       [{
      |         "appealStatus": "99",
      |         "appealLevel": "01"
      |       }],
      |       "principalChargeDocNumber": "123456789012",
      |       "principalChargeMainTransaction": "4700",
      |       "principalChargeSubTransaction": "1174",
      |       "principalChargeBillingFrom": "2022-10-30",
      |       "principalChargeBillingTo": "2022-10-30",
      |       "principalChargeDueDate": "2022-10-30"
      |   }]
      | }
      |}
      |""".stripMargin)

  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      | "totalisations": {
      |   "LSPTotalValue": 200,
      |   "penalisedPrincipalTotal": 2000,
      |   "LPPPostedTotal": 165.25,
      |   "LPPEstimatedTotal": 15.26
      | },
      | "lateSubmissionPenalty": {
      |   "summary": {
      |     "activePenaltyPoints": 10,
      |     "inactivePenaltyPoints": 12,
      |     "PoCAchievementDate": "2022-01-01",
      |     "regimeThreshold": 10,
      |     "penaltyChargeAmount": 684.25
      |   },
      |   "details": [
      |     {
      |       "penaltyNumber": "12345678901234",
      |       "penaltyOrder": "01",
      |       "penaltyCategory": "P",
      |       "penaltyStatus": "ACTIVE",
      |       "FAPIndicator": "X",
      |       "penaltyCreationDate": "2022-10-30",
      |       "penaltyExpiryDate": "2022-10-30",
      |       "expiryReason": "FAP",
      |       "communicationsDate": "2022-10-30",
      |       "lateSubmissions": [
      |         {
      |           "taxPeriodStartDate": "2022-01-01",
      |           "taxPeriodEndDate": "2022-12-31",
      |           "taxPeriodDueDate": "2023-02-07",
      |           "returnReceiptDate": "2023-02-01",
      |           "taxReturnStatus": "Fulfilled"
      |         }
      |       ],
      |       "appealInformation": [
      |         {
      |           "appealStatus": "99",
      |           "appealLevel": "01"
      |         }
      |       ],
      |       "chargeDueDate": "2022-10-30",
      |       "chargeOutstandingAmount": 200,
      |       "chargeAmount": 200
      |   }]
      | },
      | "latePaymentPenalty": {
      |     "details": [{
      |       "penaltyChargeReference": "1234567890",
      |       "penaltyCategory": "LPP1",
      |       "penaltyStatus": "A",
      |       "penaltyAmountPaid": 1001.45,
      |       "penaltyAmountOutstanding": 99.99,
      |       "LPP1LRCalculationAmount": 99.99,
      |       "LPP1LRDays": "15",
      |       "LPP1LRPercentage": 2.00,
      |       "LPP1HRCalculationAmount": 99.99,
      |       "LPP1HRDays": "31",
      |       "LPP1HRPercentage": 2.00,
      |       "LPP2Days": "31",
      |       "LPP2Percentage": 4.00,
      |       "penaltyChargeCreationDate": "2022-10-30",
      |       "communicationsDate": "2022-10-30",
      |       "penaltyChargeDueDate": "2022-10-30",
      |       "principalChargeReference": "1234567890",
      |       "appealInformation":
      |       [{
      |         "appealStatus": "99",
      |         "appealLevel": "01"
      |       }],
      |       "principalChargeBillingFrom": "2022-10-30",
      |       "principalChargeBillingTo": "2022-10-30",
      |       "principalChargeDueDate": "2022-10-30"
      |   }]
      | }
      |}
      |""".stripMargin)

  val model: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = Some(
      Totalisations(
        LSPTotalValue = Some(200),
        penalisedPrincipalTotal = Some(2000),
        LPPPostedTotal = Some(165.25),
        LPPEstimatedTotal = Some(15.26),
        totalAccountOverdue = None,
        totalAccountPostedInterest = None,
        totalAccountAccruingInterest = None
      )
    ),
    lateSubmissionPenalty = Some(
      LateSubmissionPenalty(
        summary = LSPSummary(
          activePenaltyPoints = 10,
          inactivePenaltyPoints = 12,
          regimeThreshold = 10,
          penaltyChargeAmount = 684.25,
          PoCAchievementDate = LocalDate.of(2022, 1, 1)
        ),
        details = Seq(
          LSPDetails(
            penaltyNumber = "12345678901234",
            penaltyOrder = "01",
            penaltyCategory = LSPPenaltyCategoryEnum.Point,
            penaltyStatus = LSPPenaltyStatusEnum.Active,
            penaltyCreationDate = LocalDate.of(2022, 10, 30),
            penaltyExpiryDate = LocalDate.of(2022, 10, 30),
            communicationsDate = Some(LocalDate.of(2022, 10, 30)),
            FAPIndicator = Some("X"),
            lateSubmissions = Some(
              Seq(
                LateSubmission(
                  taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                  taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
                  taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
                  returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
                  taxReturnStatus = TaxReturnStatusEnum.Fulfilled
                )
              )
            ),
            expiryReason = Some(ExpiryReasonEnum.Adjustment),
            appealInformation = Some(
              Seq(
                AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC))
              )
            ),
            chargeDueDate = Some(LocalDate.of(2022, 10, 30)),
            chargeOutstandingAmount = Some(200),
            chargeAmount = Some(200)
          )
        )
      )
    ),
    latePaymentPenalty = Some(LatePaymentPenalty(
      details = Some(
        Seq(
          LPPDetails(
            penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
            principalChargeReference = "1234567890",
            penaltyChargeReference = Some("1234567890"),
            penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
            penaltyStatus = LPPPenaltyStatusEnum.Accruing,
            appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
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
            penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
            principalChargeLatestClearing = None,
            metadata = LPPDetailsMetadata()
          )
        )
      )
    ))
  )

  "be readable from JSON" in {
    val result: JsResult[GetPenaltyDetails] = Json.fromJson(receivedJson)(GetPenaltyDetails.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result: JsValue = Json.toJson(model)(GetPenaltyDetails.format)
    result shouldBe jsonRepresentingModel
  }
}
