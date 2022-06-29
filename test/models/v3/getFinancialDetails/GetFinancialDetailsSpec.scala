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
import models.v3.MainTransactionEnum
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class GetFinancialDetailsSpec extends SpecBase {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      | "documentDetails": [
      | {
      |   "taxYear": "2022",
      |   "documentId": "DOC1234",
      |   "documentDate": "2022-01-01",
      |   "documentText": "1234",
      |   "documentDueDate": "2022-01-01",
      |   "documentDescription": "1234",
      |   "formBundleNumber": "1234",
      |   "totalAmount": 123.45,
      |   "documentOutstandingAmount": 123.45,
      |   "lastClearingDate": "2022-01-01",
      |   "lastClearingReason": "1234",
      |   "lastClearedAmount": 123.45,
      |   "statisticalFlag": true,
      |   "informationCode": "1",
      |   "paymentLot": "1",
      |   "paymentLotItem": "1",
      |   "accruingInterestAmount": 123.45,
      |   "interestRate": 123.45,
      |   "interestFromDate": "2022-01-01",
      |   "interestEndDate": "2022-01-01",
      |   "latePaymentInterestID": "1234",
      |   "latePaymentInterestAmount": 123.45,
      |   "lpiWithDunningBlock": 123.45,
      |   "interestOutstandingAmount": 123.45,
      |   "accruingPenaltyLPP1": "1234"
      | }
      | ],
      | "financialDetails": [
      | {
      |   "taxYear": "2022",
      |   "documentId": "DOC1234",
      |   "chargeType": "1234",
      |   "mainType": "1234",
      |   "periodKey": "123",
      |   "periodKeyDescription": "foobar",
      |   "taxPeriodFrom": "2022-01-01",
      |   "taxPeriodTo": "2022-03-31",
      |   "businessPartner": "123",
      |   "contractAccountCategory": "1",
      |   "contractAccount": "1",
      |   "contractObjectType": "1",
      |   "contractObject": "1",
      |   "sapDocumentNumber": "1",
      |   "sapDocumentNumberItem": "1",
      |   "chargeReference": "1",
      |   "mainTransaction": "4703",
      |   "subTransaction": "1",
      |   "originalAmount": 123.45,
      |   "outstandingAmount": 123.45,
      |   "clearedAmount": 123.45,
      |   "accruedInterest": 123.45,
      |   "items": [{
      |     "subItem": "001",
      |     "dueDate": "2018-08-13",
      |     "amount": 10000,
      |     "clearingDate": "2018-08-13",
      |     "clearingReason": "01",
      |     "outgoingPaymentMethod": "outgoing payment",
      |     "paymentLock": "paymentLock",
      |     "clearingLock": "clearingLock",
      |     "interestLock": "interestLock",
      |     "dunningLock": "dunningLock",
      |     "returnFlag": true,
      |     "paymentReference": "Ab12453535",
      |     "paymentAmount": 10000,
      |     "paymentMethod": "Payment",
      |     "paymentLot": "081203010024",
      |     "paymentLotItem": "000001",
      |     "clearingSAPDocument": "3350000253",
      |     "codingInitiationDate": "2021-01-11",
      |     "statisticalDocument": "S",
      |     "returnReason": "ABCA",
      |     "DDCollectionInProgress": true,
      |     "promisetoPay": "Y"
      |   }]
      | }
      | ]
      |}
      |""".stripMargin)

  val model: GetFinancialDetails = GetFinancialDetails(
    documentDetails = Seq(
      DocumentDetails(
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
    ),
    financialDetails = Seq(
      FinancialDetails(
        documentId = "DOC1234",
        taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
        taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
        items = Seq(
          FinancialItem(
            dueDate = Some(LocalDate.of(2018, 8, 13)),
            clearingDate = Some(LocalDate.of(2018, 8, 13)),
            metadata = FinancialItemMetadata(
              subItem = Some("001"),
              amount = Some(10000),
              clearingReason = Some("01"),
              outgoingPaymentMethod = Some("outgoing payment"),
              paymentLock = Some("paymentLock"),
              clearingLock = Some("clearingLock"),
              interestLock = Some("interestLock"),
              dunningLock = Some("dunningLock"),
              returnFlag = Some(true),
              paymentReference = Some("Ab12453535"),
              paymentAmount = Some(10000),
              paymentMethod = Some("Payment"),
              paymentLot = Some("081203010024"),
              paymentLotItem = Some("000001"),
              clearingSAPDocument = Some("3350000253"),
              codingInitiationDate = Some(LocalDate.of(2021, 1, 11)),
              statisticalDocument = Some("S"),
              DDCollectionInProgress = Some(true),
              returnReason = Some("ABCA"),
              promisetoPay = Some("Y")
            )
          )
        ),
        originalAmount = Some(123.45),
        outstandingAmount = Some(123.45),
        mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
        chargeReference = Some("1"),
        metadata = FinancialDetailsMetadata(
          taxYear = "2022",
          chargeType = Some("1234"),
          mainType = Some("1234"),
          periodKey = Some("123"),
          periodKeyDescription = Some("foobar"),
          businessPartner = Some("123"),
          contractAccountCategory = Some("1"),
          contractAccount = Some("1"),
          contractObjectType = Some("1"),
          contractObject = Some("1"),
          sapDocumentNumber = Some("1"),
          sapDocumentNumberItem = Some("1"),
          subTransaction = Some("1"),
          clearedAmount = Some(123.45),
          accruedInterest = Some(123.45)
        )
      )
    )
  )

  "be readable from JSON" in {
    val result = Json.fromJson(modelAsJson)(GetFinancialDetails.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result = Json.toJson(model)(GetFinancialDetails.format)
    result shouldBe modelAsJson
  }
}
