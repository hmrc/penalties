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

package models.getPenaltyDetails.lateSubmission

import base.SpecBase
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import play.api.libs.json.{JsResult, JsValue, Json}

import java.time.LocalDate

class LateSubmissionPenaltySpec extends SpecBase {
  val jsonReceived: JsValue = Json.parse(
    """
      |{
      | "summary": {
      |   "activePenaltyPoints": 10,
      |   "inactivePenaltyPoints": 12,
      |   "PoCAchievementDate": "2022-01-01",
      |   "regimeThreshold": 10,
      |   "penaltyChargeAmount": 684.25
      | },
      | "details": [
      |   {
      |     "penaltyNumber": "12345678901234",
      |     "penaltyOrder": "01",
      |     "penaltyCategory": "P",
      |     "penaltyStatus": "ACTIVE",
      |     "FAPIndicator": "X",
      |     "penaltyCreationDate": "2022-10-30",
      |     "penaltyExpiryDate": "2022-10-30",
      |     "triggeringProcess": "XYZ",
      |     "expiryReason": "FAP",
      |     "chargeReference": "CHARGE123",
      |     "communicationsDate": "2022-10-30",
      |     "lateSubmissions": [
      |       {
      |         "lateSubmissionID": "001",
      |         "taxPeriod":  "23AA",
      |         "taxPeriodStartDate": "2022-01-01",
      |         "taxPeriodEndDate": "2022-12-31",
      |         "taxPeriodDueDate": "2023-02-07",
      |         "returnReceiptDate": "2023-02-01",
      |         "taxReturnStatus": "Fulfilled"
      |       }
      |     ],
      |     "appealInformation": [
      |       {
      |         "appealStatus": "99",
      |         "appealLevel": "01",
      |         "appealDescription": "Some value"
      |       }
      |     ],
      |     "chargeDueDate": "2022-10-30",
      |     "chargeOutstandingAmount": 200,
      |     "chargeAmount": 200,
      |     "triggeringProcess": "P123",
      |     "chargeReference": "CHARGEREF1"
      |   }
      | ]
      |}
      |""".stripMargin)

  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      | "summary": {
      |   "activePenaltyPoints": 10,
      |   "inactivePenaltyPoints": 12,
      |   "regimeThreshold": 10,
      |   "PoCAchievementDate": "2022-01-01",
      |   "penaltyChargeAmount": 684.25
      | },
      | "details": [
      |   {
      |     "penaltyNumber": "12345678901234",
      |     "penaltyOrder": "01",
      |     "penaltyCategory": "P",
      |     "penaltyStatus": "ACTIVE",
      |     "FAPIndicator": "X",
      |     "penaltyCreationDate": "2022-10-30",
      |     "penaltyExpiryDate": "2022-10-30",
      |     "expiryReason": "FAP",
      |     "communicationsDate": "2022-10-30",
      |     "lateSubmissions": [
      |       {
      |         "lateSubmissionID": "001",
      |         "taxPeriod": "23AA",
      |         "taxPeriodStartDate": "2022-01-01",
      |         "taxPeriodEndDate": "2022-12-31",
      |         "taxPeriodDueDate": "2023-02-07",
      |         "returnReceiptDate": "2023-02-01",
      |         "taxReturnStatus": "Fulfilled"
      |       }
      |     ],
      |     "appealInformation": [
      |       {
      |         "appealStatus": "99",
      |         "appealLevel": "01",
      |         "appealDescription": "Some value"
      |       }
      |     ],
      |     "chargeDueDate": "2022-10-30",
      |     "chargeOutstandingAmount": 200,
      |     "chargeAmount": 200,
      |     "triggeringProcess": "P123",
      |     "chargeReference": "CHARGEREF1"
      |   }
      | ]
      |}
      |""".stripMargin)

  val model: LateSubmissionPenalty = LateSubmissionPenalty(
    summary = LSPSummary(
      activePenaltyPoints = 10,
      inactivePenaltyPoints = 12,
      regimeThreshold = 10,
      penaltyChargeAmount = 684.25,
      PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
    ),
    details = Seq(
      LSPDetails(
        penaltyNumber = "12345678901234",
        penaltyOrder = Some("01"),
        penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
        penaltyStatus = LSPPenaltyStatusEnum.Active,
        penaltyCreationDate = LocalDate.of(2022, 10, 30),
        penaltyExpiryDate = LocalDate.of(2022, 10, 30),
        communicationsDate = Some(LocalDate.of(2022, 10, 30)),
        FAPIndicator = Some("X"),
        lateSubmissions = Some(
          Seq(
            LateSubmission(
              lateSubmissionID = "001",
              taxPeriod = Some("23AA"),
              taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
              taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
              taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
              returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
              taxReturnStatus = Some(TaxReturnStatusEnum.Fulfilled)
            )
          )
        ),
        expiryReason = Some(ExpiryReasonEnum.Adjustment),
        appealInformation = Some(
          Seq(
            AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value"))
          )
        ),
        chargeDueDate = Some(LocalDate.of(2022, 10, 30)),
        chargeOutstandingAmount = Some(200),
        chargeAmount = Some(200),
        triggeringProcess = Some("P123"),
        chargeReference = Some("CHARGEREF1")
      )
    )
  )

  "be readable from JSON" in {
    val result: JsResult[LateSubmissionPenalty] = Json.fromJson(jsonReceived)(LateSubmissionPenalty.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result: JsValue = Json.toJson(model)(LateSubmissionPenalty.format)
    result shouldBe jsonRepresentingModel
  }
}
