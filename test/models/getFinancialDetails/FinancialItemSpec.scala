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

package models.getFinancialDetails

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class FinancialItemSpec extends SpecBase {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      |  "subItem": "001",
      |  "dueDate": "2018-08-13",
      |  "amount": 10000,
      |  "clearingDate": "2018-08-13",
      |  "clearingReason": "01",
      |  "outgoingPaymentMethod": "outgoing payment",
      |  "paymentLock": "paymentLock",
      |  "clearingLock": "clearingLock",
      |  "interestLock": "interestLock",
      |  "dunningLock": "dunningLock",
      |  "returnFlag": true,
      |  "paymentReference": "Ab12453535",
      |  "paymentAmount": 10000,
      |  "paymentMethod": "Payment",
      |  "paymentLot": "081203010024",
      |  "paymentLotItem": "000001",
      |  "clearingSAPDocument": "3350000253",
      |  "codingInitiationDate": "2021-01-11",
      |  "statisticalDocument": "S",
      |  "returnReason": "ABCA",
      |  "DDCollectionInProgress": true,
      |  "promisetoPay": "Y"
      |}
      |""".stripMargin)

  val model: FinancialItem = FinancialItem(
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

  "be writable to JSON" in {
    val result = Json.toJson(model)(FinancialItem.format)
    result shouldBe modelAsJson
  }

  "be readable from JSON" in {
    val result = Json.fromJson(modelAsJson)(FinancialItem.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }
}
