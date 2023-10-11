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
import play.api.libs.json.{JsResult, JsValue, Json}

import java.time.LocalDate

class LateSubmissionSpec extends SpecBase {
  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      | "lateSubmissionID": "001",
      | "taxPeriod":  "23AA",
      | "taxPeriodStartDate": "2022-01-01",
      | "taxPeriodEndDate": "2022-12-31",
      | "taxPeriodDueDate": "2023-02-07",
      | "returnReceiptDate": "2023-02-01",
      | "taxReturnStatus": "Fulfilled"
      |}
      |""".stripMargin)

  val model: LateSubmission = LateSubmission(
    lateSubmissionID = "001",
    taxPeriod = Some("23AA"),
    taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
    taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
    taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
    returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
    taxReturnStatus = Some(TaxReturnStatusEnum.Fulfilled)
  )

  val modelWithEmptyTaxReturnStatus: LateSubmission = LateSubmission(
    lateSubmissionID = "001",
    taxPeriod = Some("23AA"),
    taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
    taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
    taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
    returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
    taxReturnStatus = None
  )

  "be readable from JSON" in {
    val result: JsResult[LateSubmission] = Json.fromJson(jsonRepresentingModel)(LateSubmission.reads)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result: JsValue = Json.toJson(model)(LateSubmission.writes)
    result shouldBe jsonRepresentingModel
  }
}
