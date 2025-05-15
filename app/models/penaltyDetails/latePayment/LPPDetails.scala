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

package models.penaltyDetails.latePayment

import models.getFinancialDetails.MainTransactionEnum
import models.penaltyDetails.appealInfo.AppealInformationType
import play.api.libs.json._
import utils.JsonUtils

import java.time.LocalDate

case class LPPDetails(
  penaltyCategory: LPPPenaltyCategoryEnum.Value,
  penaltyChargeReference: Option[String],
  principalChargeReference: String,
  penaltyChargeCreationDate: Option[LocalDate],
  penaltyStatus: LPPPenaltyStatusEnum.Value,
  penaltyAmountAccruing: BigDecimal,
  penaltyAmountPosted: BigDecimal,
  penaltyAmountOutstanding: Option[BigDecimal],
  penaltyAmountPaid: Option[BigDecimal],
  principalChargeMainTransaction: MainTransactionEnum.Value,
  principalChargeBillingFrom: LocalDate,
  principalChargeBillingTo: LocalDate,
  principalChargeDueDate: LocalDate,
  timeToPay: Option[Seq[TimeToPay]] = None,
  principalChargeDocNumber: Option[String],
  principalChargeSubTransaction: Option[String],
  LPP1LRDays: Option[String],
  LPP1HRDays: Option[String],
  LPP2Days: Option[String],
  LPP1HRCalculationAmount: Option[BigDecimal],
  LPP1LRCalculationAmount: Option[BigDecimal],
  LPP2Percentage: Option[BigDecimal],
  LPP1LRPercentage: Option[BigDecimal],
  LPP1HRPercentage: Option[BigDecimal],
  communicationsDate: Option[LocalDate],
  penaltyChargeDueDate: Option[LocalDate],
  appealInformation: Option[Seq[AppealInformationType]],
  principalChargeLatestClearing: Option[LocalDate],
  vatOutstandingAmount: Option[BigDecimal]
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
        penaltyAmountPosted <- (json \ "penaltyAmountPosted").validate[BigDecimal]
        lpp1LRDays <- (json \ "lpp1LRDays").validateOpt[String]
        lpp1HRDays <- (json \ "lpp1HRDays").validateOpt[String]
        lpp2Days <- (json \ "lpp2Days").validateOpt[String]
        lpp1HRCalculationAmount <- (json \ "lpp1HRCalculationAmount").validateOpt[BigDecimal]
        lpp1LRCalculationAmount <- (json \ "lpp1LRCalculationAmount").validateOpt[BigDecimal]
        lpp2Percentage <- (json \ "lpp2Percentage").validateOpt[BigDecimal]
        lpp1LRPercentage <- (json \ "lpp1LRPercentage").validateOpt[BigDecimal]
        lpp1HRPercentage <- (json \ "lpp1HRPercentage").validateOpt[BigDecimal]
        penaltyChargeDueDate <- (json \ "penaltyChargeDueDate").validateOpt[LocalDate]
        principalChargeLatestClearing <- (json \ "principalChargeLatestClearing").validateOpt[LocalDate]
        penaltyAmountAccruing <- (json \ "penaltyAmountAccruing").validate[BigDecimal]
        principalChargeMainTransaction <- (json \ "principalChargeMainTransaction").validate[MainTransactionEnum.Value]
        timeToPay <- (json \ "timeToPay").validateOpt[Seq[TimeToPay]]
        principalChargeDocNumber <- (json \ "principalChargeDocNumber").validateOpt[String]
        principalChargeSubTransaction <- (json \ "principalChargeSubTransaction").validateOpt[String]
        vatOutstandingAmount <- (json \ "vatOutstandingAmount").validateOpt[BigDecimal]
      } yield {
        LPPDetails(
          penaltyCategory = penaltyCategory,
          penaltyChargeReference = penaltyChargeReference,
          principalChargeReference = principalChargeReference,
          penaltyChargeCreationDate = penaltyChargeCreationDate,
          penaltyStatus = penaltyStatus,
          penaltyAmountAccruing = penaltyAmountAccruing,
          penaltyAmountPosted = penaltyAmountPosted,
          penaltyAmountOutstanding = penaltyAmountOutstanding,
          penaltyAmountPaid = penaltyAmountPaid,
          principalChargeMainTransaction = principalChargeMainTransaction,
          LPP1LRDays = lpp1LRDays,
          LPP1HRDays = lpp1HRDays,
          LPP2Days = lpp2Days,
          LPP1HRCalculationAmount = lpp1HRCalculationAmount,
          LPP1LRCalculationAmount = lpp1LRCalculationAmount,
          LPP2Percentage = lpp2Percentage,
          LPP1LRPercentage = lpp1LRPercentage,
          LPP1HRPercentage = lpp1HRPercentage,
          communicationsDate = communicationsDate,
          penaltyChargeDueDate = penaltyChargeDueDate,
          timeToPay = timeToPay,
          principalChargeBillingFrom = principalChargeBillingFrom,
          principalChargeBillingTo = principalChargeBillingTo,
          principalChargeDueDate = principalChargeDueDate,
          appealInformation = appealInformation,
          principalChargeLatestClearing = principalChargeLatestClearing,
          vatOutstandingAmount = vatOutstandingAmount,
          principalChargeDocNumber = principalChargeDocNumber,
          principalChargeSubTransaction = principalChargeSubTransaction
        )
      }
    }

    override def writes(o: LPPDetails): JsValue = {
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
        "penaltyAmountOutstanding" -> o.penaltyAmountOutstanding,
        "penaltyAmountPosted" -> o.penaltyAmountPosted,
        "penaltyAmountPaid" -> o.penaltyAmountPaid,
        "lpp1LrDays" -> o.LPP1LRDays,
        "lpp1HrDays" -> o.LPP1HRDays,
        "lpp2Days" -> o.LPP2Days,
        "lpp1HrCalculationAmount" -> o.LPP1HRCalculationAmount,
        "lpp1LrCalculationAmount" -> o.LPP1LRCalculationAmount,
        "timeToPay" -> o.timeToPay,
        "lpp2Percentage" -> o.LPP2Percentage,
        "lpp1LrPercentage" -> o.LPP1LRPercentage,
        "lpp1HrPercentage" -> o.LPP1HRPercentage,
        "penaltyChargeDueDate" -> o.penaltyChargeDueDate,
        "principalChargeLatestClearing" -> o.principalChargeLatestClearing,
        "penaltyAmountAccruing" -> o.penaltyAmountAccruing,
        "principalChargeMainTransaction" -> o.principalChargeMainTransaction,
        "vatOutstandingAmount" -> o.vatOutstandingAmount,
        "principalChargeDocNumber" -> o.principalChargeDocNumber,
        "principalChargeSubTransaction" -> o.principalChargeSubTransaction
      )
    }
  }
}