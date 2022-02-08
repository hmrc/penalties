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

class LSPDetailsSpec extends SpecBase {

  val lspDetailsWithoutAppealStatus: JsValue = Json.parse(
    """
      |{
      |	"penaltyNumber": "1234ABCD",
      |	"penaltyOrder": "1",
      |	"penaltyCategory": "P",
      |	"penaltyStatus": "ACTIVE",
      |	"penaltyCreationDate": "2022-01-01",
      |	"penaltyExpiryDate": "2024-01-01",
      |	"communicationsDate": "2022-01-01",
      |	"chargeReference": "foobar",
      |	"chargeAmount": 123.45,
      |	"chargeOutstandingAmount": 123.45,
      | "chargeDueDate": "2022-01-01",
      | "lateSubmissions": [{
      |    "lateSubmissionID": "ID123",
      |    "taxPeriod": "1",
      |    "taxReturnStatus": "2",
      |    "taxPeriodStartDate": "2022-01-01",
      |    "taxPeriodEndDate": "2022-03-31",
      |    "taxPeriodDueDate": "2022-05-07",
      |    "returnReceiptDate": "2022-04-01"
      | }]
      |}
      |""".stripMargin)


  val lspDetailsWithAppealStatus: JsValue = Json.parse(
    """
      |{
      |	"penaltyNumber": "1234ABCD",
      |	"penaltyOrder": "1",
      |	"penaltyCategory": "P",
      |	"penaltyStatus": "ACTIVE",
      |	"penaltyCreationDate": "2022-01-01",
      |	"penaltyExpiryDate": "2024-01-01",
      |	"communicationsDate": "2022-01-01",
      | "appealStatus": "1",
      |	"appealLevel": "1",
      |	"chargeReference": "foobar",
      |	"chargeAmount": 123.45,
      |	"chargeOutstandingAmount": 123.45,
      |	"chargeDueDate": "2022-01-01",
      |  "lateSubmissions": [{
      |    "lateSubmissionID": "ID123",
      |    "taxPeriod": "1",
      |    "taxReturnStatus": "2",
      |    "taxPeriodStartDate": "2022-01-01",
      |    "taxPeriodEndDate": "2022-03-31",
      |    "taxPeriodDueDate": "2022-05-07",
      |    "returnReceiptDate": "2022-04-01"
      |  }]
      |}
      |""".stripMargin)

  val modelWithAppealStatus: LSPDetails = LSPDetails(
    penaltyCategory = LSPPenaltyCategoryEnum.Point,
    penaltyNumber = "1234ABCD",
    penaltyOrder = "1",
    penaltyCreationDate = LocalDate.of(2022, 1, 1),
    penaltyExpiryDate = LocalDate.of(2024, 1, 1),
    penaltyStatus = LSPPenaltyStatusEnum.Active,
    appealStatus = Some("1"),
    communicationsDate = LocalDate.of(2022, 1, 1),
    appealLevel = Some("1"),
    chargeReference = Some("foobar"),
    chargeAmount = Some(123.45),
    chargeOutstandingAmount = Some(123.45),
    chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
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
    ))
  )

  val modelWithoutAppealStatus: LSPDetails = LSPDetails(
    penaltyCategory = LSPPenaltyCategoryEnum.Point,
    penaltyNumber = "1234ABCD",
    penaltyOrder = "1",
    penaltyCreationDate = LocalDate.of(2022, 1, 1),
    penaltyExpiryDate = LocalDate.of(2024, 1, 1),
    penaltyStatus = LSPPenaltyStatusEnum.Active,
    appealStatus = None,
    communicationsDate = LocalDate.of(2022, 1, 1),
    appealLevel = None,
    chargeReference = Some("foobar"),
    chargeAmount = Some(123.45),
    chargeOutstandingAmount = Some(123.45),
    chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
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
    ))
  )

  "be readable from JSON with no appeal status" in {
    val result = Json.fromJson(lspDetailsWithoutAppealStatus)(LSPDetails.format)
    result.isSuccess shouldBe true
    result.get shouldBe modelWithoutAppealStatus
  }

  "be readable from JSON with appeal status" in {
    val result = Json.fromJson(lspDetailsWithAppealStatus)(LSPDetails.format)
    result.isSuccess shouldBe true
    result.get shouldBe modelWithAppealStatus
  }

  "be writable to JSON with no appeal status" in {
    val result = Json.toJson(modelWithoutAppealStatus)(LSPDetails.format)
    result shouldBe lspDetailsWithoutAppealStatus
  }

  "be writable to JSON with appeal status" in {
    val result = Json.toJson(modelWithAppealStatus)(LSPDetails.format)
    result shouldBe lspDetailsWithAppealStatus
  }
}
