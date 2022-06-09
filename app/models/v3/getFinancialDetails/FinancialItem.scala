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

import play.api.libs.json.{Format, JsResult, JsValue, Json, OFormat}
import utils.JsonUtils

import java.time.LocalDate

case class FinancialItem(
                          dueDate: Option[LocalDate],
                          clearingDate: Option[LocalDate],
                          metadata: FinancialItemMetadata
                        )

object FinancialItem extends JsonUtils {
  implicit val format: Format[FinancialItem] = new Format[FinancialItem] {
    override def reads(json: JsValue): JsResult[FinancialItem] = {
      for {
        taxPeriodFrom <- (json \ "clearingDate").validateOpt[LocalDate]
        taxPeriodTo <- (json \ "dueDate").validateOpt[LocalDate]
        metadata <- Json.fromJson(json)(FinancialItemMetadata.format)
      } yield {
        FinancialItem(taxPeriodFrom, taxPeriodTo, metadata)
      }
    }

    override def writes(o: FinancialItem): JsValue = {
      jsonObjNoNulls(
        "dueDate" -> o.dueDate,
        "clearingDate" -> o.clearingDate
      ).deepMerge(Json.toJsObject(o.metadata)(FinancialItemMetadata.format))
    }
  }
}

case class FinancialItemMetadata(
                                  subItem: Option[String],
                                  amount: Option[BigDecimal],
                                  clearingReason: Option[String],
                                  outgoingPaymentMethod: Option[String],
                                  paymentLock: Option[String],
                                  clearingLock: Option[String],
                                  interestLock: Option[String],
                                  dunningLock: Option[String],
                                  returnFlag: Option[Boolean],
                                  paymentReference: Option[String],
                                  paymentAmount: Option[BigDecimal],
                                  paymentMethod: Option[String],
                                  paymentLot: Option[String],
                                  paymentLotItem: Option[String],
                                  clearingSAPDocument: Option[String],
                                  codingInitiationDate: Option[LocalDate],
                                  statisticalDocument: Option[String],
                                  DDCollectionInProgress: Option[Boolean],
                                  returnReason: Option[String],
                                  promisetoPay: Option[String]
                                )

object FinancialItemMetadata {
  implicit val format: OFormat[FinancialItemMetadata] = Json.format[FinancialItemMetadata]
}
