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

package models.v2.lateSubmissionPenalty

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class LSPDataSpec extends SpecBase {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      | "summary": {
      |   "activePenaltyPoints": 1,
      |   "inactivePenaltyPoints": 2,
      |   "regimeThreshold": 3,
      |   "POCAchievementDate": "2024-01-01",
      |   "penaltyChargeAmount": 123.45
      | },
      | "details": [{
      |	  "penaltyNumber": "1234ABCD",
      |	  "penaltyOrder": "1",
      |	  "penaltyCategory": "P",
      |	  "penaltyStatus": "ACTIVE",
      |	  "penaltyCreationDate": "2022-01-01",
      |	  "penaltyExpiryDate": "2024-01-01",
      |	  "communicationsDate": "2022-01-01",
      |   "lateSubmissions": [{
      |       "lateSubmissionID": "ID123",
      |       "taxPeriod": "1",
      |       "taxReturnStatus": "2",
      |       "taxPeriodStartDate": "2022-01-01",
      |       "taxPeriodEndDate": "2022-03-31",
      |       "taxPeriodDueDate": "2022-05-07",
      |       "returnReceiptDate": "2022-04-01"
      |   }],
      |   "appealStatus": "1",
      |	  "appealLevel": "1",
      |	  "chargeReference": "foobar",
      |	  "chargeAmount": 123.45,
      |	  "chargeOutstandingAmount": 123.45,
      |	  "chargeDueDate": "2022-01-01"
      | }]
      |}
      |""".stripMargin)

  val model: LSPData = LSPData(
    summary = LSPSummary(
      activePenaltyPoints = 1,
      inactivePenaltyPoints = 2,
      regimeThreshold = 3,
      POCAchievementDate = LocalDate.of(2024, 1, 1),
      penaltyChargeAmount = 123.45
    ),
    details = Seq(LSPDetails(
      penaltyCategory = LSPPenaltyCategoryEnum.Point,
      penaltyNumber = "1234ABCD",
      penaltyOrder = "1",
      penaltyCreationDate = LocalDate.of(2022, 1, 1),
      penaltyExpiryDate = LocalDate.of(2024, 1, 1),
      penaltyStatus = LSPPenaltyStatusEnum.Active,
      appealStatus = Some("1"),
      communicationsDate = LocalDate.of(2022, 1, 1),
      lateSubmissions = Some(Seq(
        LateSubmission(
          lateSubmissionID = "ID123",
          taxPeriod = Some("1"),
          taxReturnStatus = "2",
          taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
          taxPeriodEndDate = Some(LocalDate.of(2022, 3, 31)),
          taxPeriodDueDate = Some(LocalDate.of(2022, 5, 7)),
          returnReceiptDate = Some(LocalDate.of(2022, 4, 1))
        )
      )),
      appealLevel = Some("1"),
      chargeReference = Some("foobar"),
      chargeAmount = Some(123.45),
      chargeOutstandingAmount = Some(123.45),
      chargeDueDate = Some(LocalDate.of(2022, 1, 1))
    ))
  )
  "be readable from JSON" in {
    val result = Json.fromJson(modelAsJson)(LSPData.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result = Json.toJson(model)(LSPData.format)
    result shouldBe modelAsJson
  }
}
