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

import play.api.libs.json.{Format, JsResult, JsValue, Json, OFormat}
import utils.JsonUtils

import java.time.LocalDate


case class DocumentDetails(
                            documentId: String,
                            accruingInterestAmount: Option[BigDecimal],
                            interestOutstandingAmount: Option[BigDecimal],
                            metadata: DocumentDetailsMetadata
                          )

object DocumentDetails extends JsonUtils {
  implicit val format: Format[DocumentDetails] = new Format[DocumentDetails] {
    override def reads(json: JsValue): JsResult[DocumentDetails] = {
      for {
        documentId <- (json \ "documentId").validate[String]
        accruingInterestAmount <- (json \ "accruingInterestAmount").validateOpt[BigDecimal]
        interestOutstandingAmount <- (json \ "interestOutstandingAmount").validateOpt[BigDecimal]
        metadata <- Json.fromJson(json)(DocumentDetailsMetadata.format)
      } yield {
        DocumentDetails(documentId, accruingInterestAmount, interestOutstandingAmount, metadata)
      }
    }

    override def writes(o: DocumentDetails): JsValue = {
      jsonObjNoNulls(
        "documentId" -> o.documentId,
        "accruingInterestAmount" -> o.accruingInterestAmount,
        "interestOutstandingAmount" -> o.interestOutstandingAmount
      ).deepMerge(Json.toJsObject(o.metadata)(DocumentDetailsMetadata.format))
    }
  }
}

case class DocumentDetailsMetadata(
                                    taxYear: String,
                                    documentDate: LocalDate,
                                    documentText: String,
                                    documentDueDate: LocalDate,
                                    documentDescription: Option[String],
                                    formBundleNumber: Option[String],
                                    totalAmount: BigDecimal,
                                    documentOutstandingAmount: BigDecimal,
                                    lastClearingDate: Option[LocalDate],
                                    lastClearingReason: Option[String],
                                    lastClearedAmount: Option[BigDecimal],
                                    statisticalFlag: Boolean,
                                    informationCode: Option[String],
                                    paymentLot: Option[String],
                                    paymentLotItem: Option[String],
                                    interestRate: Option[BigDecimal],
                                    interestFromDate: Option[LocalDate],
                                    interestEndDate: Option[LocalDate],
                                    latePaymentInterestID: Option[String],
                                    latePaymentInterestAmount: Option[BigDecimal],
                                    lpiWithDunningBlock: Option[BigDecimal],
                                    accruingPenaltyLPP1: Option[String]
                                    //NOTE: commented out due to 22-limit on case class parameters with JSON serialiser
//                                    lpp1Amount: Option[BigDecimal],
//                                    lpp1ID: Option[String],
//                                    accruingPenaltyLPP2: Option[String],
//                                    lpp2Amount: Option[BigDecimal],
//                                    lpp2ID: Option[String]
                                  )

object DocumentDetailsMetadata {
  implicit val format: OFormat[DocumentDetailsMetadata] = Json.format[DocumentDetailsMetadata]
}

