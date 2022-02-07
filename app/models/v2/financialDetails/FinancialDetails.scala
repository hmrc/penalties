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

package models.v2.financialDetails

import play.api.libs.json.{Format, JsResult, JsValue, Json, OFormat}
import utils.JsonUtils

import java.time.LocalDate

case class FinancialDetails(
                           documentId: String,
                           taxPeriodFrom: Option[LocalDate],
                           taxPeriodTo: Option[LocalDate],
                           items: Seq[FinancialItem],
                           originalAmount: Option[BigDecimal],
                           outstandingAmount: Option[BigDecimal],
                           metadata: FinancialDetailsMetadata
                           )

object FinancialDetails extends JsonUtils {
  implicit val format: Format[FinancialDetails] = new Format[FinancialDetails] {
    override def reads(json: JsValue): JsResult[FinancialDetails] = {
      for {
        documentId <- (json \ "documentId").validate[String]
        taxPeriodFrom <- (json \ "taxPeriodFrom").validateOpt[LocalDate]
        taxPeriodTo <- (json \ "taxPeriodTo").validateOpt[LocalDate]
        items <- (json \ "items").validate[Seq[FinancialItem]]
        originalAmount <- (json \ "originalAmount").validateOpt[BigDecimal]
        outstandingAmount <- (json \ "outstandingAmount").validateOpt[BigDecimal]
        metadata <- Json.fromJson(json)(FinancialDetailsMetadata.format)
      } yield {
        FinancialDetails(documentId, taxPeriodFrom, taxPeriodTo, items, originalAmount, outstandingAmount, metadata)
      }
    }

    override def writes(o: FinancialDetails): JsValue = {
      jsonObjNoNulls(
        "documentId" -> o.documentId,
        "taxPeriodFrom" -> o.taxPeriodFrom,
        "taxPeriodTo" -> o.taxPeriodTo,
        "items" -> o.items,
        "originalAmount" -> o.originalAmount,
        "outstandingAmount" -> o.outstandingAmount
      ).deepMerge(Json.toJsObject(o.metadata)(FinancialDetailsMetadata.format))
    }
  }
}

case class FinancialDetailsMetadata(
                                     taxYear: String,
                                     chargeType: Option[String],
                                     mainType: Option[String],
                                     periodKey: Option[String],
                                     periodKeyDescription: Option[String],
                                     businessPartner: Option[String],
                                     contractAccountCategory: Option[String],
                                     contractAccount: Option[String],
                                     contractObjectType: Option[String],
                                     contractObject: Option[String],
                                     sapDocumentNumber: Option[String],
                                     sapDocumentNumberItem: Option[String],
                                     chargeReference: Option[String],
                                     mainTransaction: Option[String],
                                     subTransaction: Option[String],
                                     clearedAmount: Option[BigDecimal],
                                     accruedInterest: Option[BigDecimal]
                                   )

object FinancialDetailsMetadata {
  implicit val format: OFormat[FinancialDetailsMetadata] = Json.format[FinancialDetailsMetadata]
}
