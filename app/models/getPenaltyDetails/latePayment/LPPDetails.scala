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

package models.getPenaltyDetails.latePayment

import models.getFinancialDetails.MainTransactionEnum
import models.getPenaltyDetails.appealInfo.AppealInformationType
import play.api.libs.json._
import utils.JsonUtils

import java.time.LocalDate

case class LPPDetails(
                       penaltyCategory: LPPPenaltyCategoryEnum.Value,
                       penaltyChargeReference: Option[String],
                       principalChargeReference: String,
                       penaltyChargeCreationDate: Option[LocalDate],
                       penaltyStatus: LPPPenaltyStatusEnum.Value,
                       appealInformation: Option[Seq[AppealInformationType]],
                       principalChargeBillingFrom: LocalDate,
                       principalChargeBillingTo: LocalDate,
                       principalChargeDueDate: LocalDate,
                       communicationsDate: Option[LocalDate],
                       penaltyAmountAccruing: BigDecimal,
                       principalChargeMainTransaction: MainTransactionEnum.Value,
                       penaltyAmountOutstanding: Option[BigDecimal],
                       penaltyAmountPaid: Option[BigDecimal],
                       penaltyAmountPosted: Option[BigDecimal],
                       LPP1LRDays: Option[String],
                       LPP1HRDays: Option[String],
                       LPP2Days: Option[String],
                       LPP1HRCalculationAmount: Option[BigDecimal],
                       LPP1LRCalculationAmount: Option[BigDecimal],
                       LPP2Percentage: Option[BigDecimal],
                       LPP1LRPercentage: Option[BigDecimal],
                       LPP1HRPercentage: Option[BigDecimal],
                       penaltyChargeDueDate: Option[LocalDate],
                       principalChargeLatestClearing: Option[LocalDate],
                       metadata: LPPDetailsMetadata
                     )

object LPPDetails extends JsonUtils {
  implicit val format: Format[LPPDetails] = new Format[LPPDetails] {
    override def reads(json: JsValue): JsResult[LPPDetails] = {
      for {
        penaltyCategory <- (json \ "penaltyCategory").validate[LPPPenaltyCategoryEnum.Value]
        penaltyChargeReference <- (json \ "penaltyChargeReference").validateOpt[String]
        principalChargeReference <- (json \ "principalChargeReference").validate[String]
        penaltyChargeCreationDate <- (json \ "penaltyChargeCreationDate").validateOpt[LocalDate]
        penaltyStatus <- (json \ "penaltyStatus").validate[LPPPenaltyStatusEnum.Value]
        appealInformation <- (json \ "appealInformation").validateOpt[Seq[AppealInformationType]]
        principalChargeBillingFrom <- (json \ "principalChargeBillingFrom").validate[LocalDate]
        principalChargeBillingTo <- (json \ "principalChargeBillingTo").validate[LocalDate]
        principalChargeDueDate <- (json \ "principalChargeDueDate").validate[LocalDate]
        communicationsDate <- (json \ "communicationsDate").validateOpt[LocalDate]
        penaltyAmountOutstanding <- (json \ "penaltyAmountOutstanding").validateOpt[BigDecimal]
        penaltyAmountPaid <- (json \ "penaltyAmountPaid").validateOpt[BigDecimal]
        penaltyAmountPosted <- (json \ "penaltyAmountPosted").validateOpt[BigDecimal]
        lpp1LRDays <- (json \ "LPP1LRDays").validateOpt[String]
        lpp1HRDays <- (json \ "LPP1HRDays").validateOpt[String]
        lpp2Days <- (json \ "LPP2Days").validateOpt[String]
        lpp1HRCalculationAmount <- (json \ "LPP1HRCalculationAmount").validateOpt[BigDecimal]
        lpp1LRCalculationAmount <- (json \ "LPP1LRCalculationAmount").validateOpt[BigDecimal]
        lpp2Percentage <- (json \ "LPP2Percentage").validateOpt[BigDecimal]
        lpp1LRPercentage <- (json \ "LPP1LRPercentage").validateOpt[BigDecimal]
        lpp1HRPercentage <- (json \ "LPP1HRPercentage").validateOpt[BigDecimal]
        penaltyChargeDueDate <- (json \ "penaltyChargeDueDate").validateOpt[LocalDate]
        principalChargeLatestClearing <- (json \ "principalChargeLatestClearing").validateOpt[LocalDate]
        penaltyAmountAccruing <- (json \ "penaltyAmountAccruing").validate[BigDecimal]
        principalChargeMainTransaction <- (json \ "principalChargeMainTransaction").validate[MainTransactionEnum.Value]
        metadata <- Json.fromJson(json)(LPPDetailsMetadata.format)
      } yield {
        LPPDetails(
          penaltyCategory,
          penaltyChargeReference,
          principalChargeReference,
          penaltyChargeCreationDate,
          penaltyStatus,
          appealInformation,
          principalChargeBillingFrom,
          principalChargeBillingTo,
          principalChargeDueDate,
          communicationsDate,
          penaltyAmountAccruing,
          principalChargeMainTransaction,
          penaltyAmountOutstanding,
          penaltyAmountPaid,
          penaltyAmountPosted,
          lpp1LRDays,
          lpp1HRDays,
          lpp2Days,
          lpp1HRCalculationAmount,
          lpp1LRCalculationAmount,
          lpp2Percentage,
          lpp1LRPercentage,
          lpp1HRPercentage,
          penaltyChargeDueDate,
          principalChargeLatestClearing,
          metadata
        )
      }
    }

    private def setPenaltyAmountOutstanding(penaltyAmountOutstanding: Option[BigDecimal])
                                           (penaltyAmountPosted: Option[BigDecimal], penaltyAmountPaid: Option[BigDecimal]): Option[BigDecimal] = {
      if(penaltyAmountOutstanding.isEmpty) {
        if(penaltyAmountPaid.isDefined && penaltyAmountPosted.isDefined) {
          if(penaltyAmountPaid.get.equals(penaltyAmountPosted.get)) {
            Some(BigDecimal(0)) //If amount paid is equal to the amount posted then the penalty has been paid
          } else {
            None
          }
        } else {
          None
        }
      } else {
        penaltyAmountOutstanding
      }
    }

    override def writes(o: LPPDetails): JsValue = {
      val penaltyAmountOutstanding = setPenaltyAmountOutstanding(o.penaltyAmountOutstanding)(o.penaltyAmountPosted, o.penaltyAmountPaid)
      jsonObjNoNulls(
        "penaltyCategory" -> o.penaltyCategory,
        "penaltyChargeReference" -> o.penaltyChargeReference,
        "principalChargeReference" -> o.principalChargeReference,
        "penaltyChargeCreationDate" -> o.penaltyChargeCreationDate,
        "penaltyStatus" -> o.penaltyStatus,
        "appealInformation" -> o.appealInformation,
        "principalChargeBillingFrom" -> o.principalChargeBillingFrom,
        "principalChargeBillingTo" -> o.principalChargeBillingTo,
        "principalChargeDueDate" -> o.principalChargeDueDate,
        "communicationsDate" -> o.communicationsDate,
        "penaltyAmountOutstanding" -> penaltyAmountOutstanding,
        "penaltyAmountPaid" -> o.penaltyAmountPaid,
        "LPP1LRDays" -> o.LPP1LRDays,
        "LPP1HRDays" -> o.LPP1HRDays,
        "LPP2Days" -> o.LPP2Days,
        "LPP1HRCalculationAmount" -> o.LPP1HRCalculationAmount,
        "LPP1LRCalculationAmount" -> o.LPP1LRCalculationAmount,
        "LPP2Percentage" -> o.LPP2Percentage,
        "LPP1LRPercentage" -> o.LPP1LRPercentage,
        "LPP1HRPercentage" -> o.LPP1HRPercentage,
        "penaltyChargeDueDate" -> o.penaltyChargeDueDate,
        "principalChargeLatestClearing" -> o.principalChargeLatestClearing,
        "penaltyAmountAccruing" -> o.penaltyAmountAccruing,
        "principalChargeMainTransaction" -> o.principalChargeMainTransaction
      ).deepMerge(Json.toJsObject(o.metadata)(LPPDetailsMetadata.format))
    }
  }
}

case class LPPDetailsMetadata(
                               mainTransaction: Option[MainTransactionEnum.Value] = None,
                               outstandingAmount: Option[BigDecimal] = None,
                               timeToPay: Option[Seq[TimeToPay]] = None
                             )

object LPPDetailsMetadata {
  implicit val format: OFormat[LPPDetailsMetadata] = Json.format[LPPDetailsMetadata]
}
