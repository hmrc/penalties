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

package models.v3.getFinancialDetails

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class DocumentDetailsSpec extends SpecBase {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      | "taxYear": "2022",
      | "documentId": "DOC1234",
      | "documentDate": "2022-01-01",
      | "documentText": "1234",
      | "documentDueDate": "2022-01-01",
      | "documentDescription": "1234",
      | "formBundleNumber": "1234",
      | "totalAmount": 123.45,
      | "documentOutstandingAmount": 123.45,
      | "lastClearingDate": "2022-01-01",
      | "lastClearingReason": "1234",
      | "lastClearedAmount": 123.45,
      | "statisticalFlag": true,
      | "informationCode": "1",
      | "paymentLot": "1",
      | "paymentLotItem": "1",
      | "accruingInterestAmount": 123.45,
      | "interestRate": 123.45,
      | "interestFromDate": "2022-01-01",
      | "interestEndDate": "2022-01-01",
      | "latePaymentInterestID": "1234",
      | "latePaymentInterestAmount": 123.45,
      | "lpiWithDunningBlock": 123.45,
      | "interestOutstandingAmount": 123.45,
      | "accruingPenaltyLPP1": "1234"
      |}
      |""".stripMargin)

  val model: DocumentDetails = DocumentDetails(
    documentId = "DOC1234",
    accruingInterestAmount = Some(123.45),
    interestOutstandingAmount = Some(123.45),
    metadata = DocumentDetailsMetadata(
      taxYear = "2022",
      documentDate = LocalDate.of(2022, 1, 1),
      documentText = "1234",
      documentDueDate = LocalDate.of(2022, 1, 1),
      documentDescription = Some("1234"),
      formBundleNumber = Some("1234"),
      totalAmount = 123.45,
      documentOutstandingAmount = 123.45,
      lastClearingDate = Some(LocalDate.of(2022, 1, 1)),
      lastClearingReason = Some("1234"),
      lastClearedAmount = Some(123.45),
      statisticalFlag = true,
      informationCode = Some("1"),
      paymentLot = Some("1"),
      paymentLotItem = Some("1"),
      interestRate = Some(123.45),
      interestFromDate = Some(LocalDate.of(2022, 1, 1)),
      interestEndDate = Some(LocalDate.of(2022, 1, 1)),
      latePaymentInterestID = Some("1234"),
      latePaymentInterestAmount = Some(123.45),
      lpiWithDunningBlock = Some(123.45),
      accruingPenaltyLPP1 = Some("1234")
    )
  )

  "be readable from JSON" in {
    val result = Json.fromJson(modelAsJson)(DocumentDetails.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result = Json.toJson(model)(DocumentDetails.format)
    result shouldBe modelAsJson
  }
}
