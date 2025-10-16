/*
 * Copyright 2025 HM Revenue & Customs
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

package models.hipPenaltyDetails.latePayment

import models.hipPenaltyDetails.appealInfo.AppealInformationType
import play.api.libs.json._
import utils.JsonUtils

import java.time.LocalDate

case class LPPDetails(
  principalChargeReference: String,
  penaltyCategory: LPPPenaltyCategoryEnum.Value,
  penaltyStatus: Option[LPPPenaltyStatusEnum.Value],
  penaltyAmountAccruing: BigDecimal,
  penaltyAmountPosted: BigDecimal,
  penaltyAmountPaid: Option[BigDecimal],
  penaltyAmountOutstanding: Option[BigDecimal],
  lpp1LRCalculationAmt: Option[BigDecimal],
  lpp1LRDays: Option[String],
  lpp1LRPercentage: Option[BigDecimal],
  lpp1HRCalculationAmt: Option[BigDecimal],
  lpp1HRDays: Option[String],
  lpp1HRPercentage: Option[BigDecimal],
  lpp2Days: Option[String],
  lpp2Percentage: Option[BigDecimal],
  penaltyChargeCreationDate: Option[LocalDate],
  communicationsDate: Option[LocalDate],
  penaltyChargeReference: Option[String],
  penaltyChargeDueDate: Option[LocalDate],
  appealInformation: Option[Seq[AppealInformationType]],
  principalChargeDocNumber: Option[String],
  principalChargeMainTr: String,
  principalChargeSubTr: Option[String],
  principalChargeBillingFrom: LocalDate,
  principalChargeBillingTo: LocalDate,
  principalChargeDueDate: LocalDate,
  principalChargeLatestClearing: Option[LocalDate],
  timeToPay: Option[Seq[TimeToPay]] = None,
  supplement: Option[Boolean] = None // DL-17577: Make this field mandatory once API change is live in Dec '25
)

object LPPDetails extends JsonUtils {
  implicit val format: Format[LPPDetails] = new Format[LPPDetails] {
    override def reads(json: JsValue): JsResult[LPPDetails] = {
      for {
        principalChargeReference <- (json \ "principalChargeReference").validate[String]
        penaltyCategory <- (json \ "penaltyCategory").validate[LPPPenaltyCategoryEnum.Value]
        penaltyStatus <- (json \ "penaltyStatus").validateOpt[LPPPenaltyStatusEnum.Value]
        penaltyAmountAccruing <- (json \ "penaltyAmountAccruing").validate[BigDecimal]
        penaltyAmountPosted <- (json \ "penaltyAmountPosted").validate[BigDecimal]
        penaltyAmountPaid <- (json \ "penaltyAmountPaid").validateOpt[BigDecimal]
        penaltyAmountOutstanding <- (json \ "penaltyAmountOutstanding").validateOpt[BigDecimal]
        lpp1LRCalculationAmt <- (json \ "lpp1LRCalculationAmt").validateOpt[BigDecimal]
        lpp1LRDays <- (json \ "lpp1LRDays").validateOpt[String]
        lpp1LRPercentage <- (json \ "lpp1LRPercentage").validateOpt[BigDecimal]
        lpp1HRCalculationAmt <- (json \ "lpp1HRCalculationAmt").validateOpt[BigDecimal]
        lpp1HRDays <- (json \ "lpp1HRDays").validateOpt[String]
        lpp1HRPercentage <- (json \ "lpp1HRPercentage").validateOpt[BigDecimal]
        lpp2Days <- (json \ "lpp2Days").validateOpt[String]
        lpp2Percentage <- (json \ "lpp2Percentage").validateOpt[BigDecimal]
        penaltyChargeCreationDate <- (json \ "penaltyChargeCreationDate").validateOpt[LocalDate]
        communicationsDate <- (json \ "communicationsDate").validateOpt[LocalDate]
        penaltyChargeReference <- (json \ "penaltyChargeReference").validateOpt[String]
        penaltyChargeDueDate <- (json \ "penaltyChargeDueDate").validateOpt[LocalDate]
        appealInformation <- (json \ "appealInformation").validateOpt[Seq[AppealInformationType]]
        principalChargeDocNumber <- (json \ "principalChargeDocNumber").validateOpt[String]
        principalChargeMainTr <- (json \ "principalChargeMainTr").validate[String]
        principalChargeSubTr <- (json \ "principalChargeSubTr").validateOpt[String]
        principalChargeBillingFrom <- (json \ "principalChargeBillingFrom").validate[LocalDate]
        principalChargeBillingTo <- (json \ "principalChargeBillingTo").validate[LocalDate]
        principalChargeDueDate <- (json \ "principalChargeDueDate").validate[LocalDate]
        principalChargeLatestClearing <- (json \ "principalChargeLatestClearing").validateOpt[LocalDate]
        timeToPay <- (json \ "timeToPay").validateOpt[Seq[TimeToPay]]
        supplement <- (json \ "supplement").validateOpt[Boolean]
      } yield {
        LPPDetails(
          principalChargeReference = principalChargeReference,
          penaltyCategory = penaltyCategory,
          penaltyStatus = penaltyStatus,
          penaltyAmountAccruing = penaltyAmountAccruing,
          penaltyAmountPosted = penaltyAmountPosted,
          penaltyAmountPaid = penaltyAmountPaid,
          penaltyAmountOutstanding = penaltyAmountOutstanding,
          lpp1LRCalculationAmt = lpp1LRCalculationAmt,
          lpp1LRDays = lpp1LRDays,
          lpp1LRPercentage = lpp1LRPercentage,
          lpp1HRCalculationAmt = lpp1HRCalculationAmt,
          lpp1HRDays = lpp1HRDays,
          lpp1HRPercentage = lpp1HRPercentage,
          lpp2Days = lpp2Days,
          lpp2Percentage = lpp2Percentage,
          penaltyChargeCreationDate = penaltyChargeCreationDate,
          communicationsDate = communicationsDate,
          penaltyChargeReference = penaltyChargeReference,
          penaltyChargeDueDate = penaltyChargeDueDate,
          appealInformation = appealInformation,
          principalChargeDocNumber = principalChargeDocNumber,
          principalChargeMainTr = principalChargeMainTr,
          principalChargeSubTr = principalChargeSubTr,
          principalChargeBillingFrom = principalChargeBillingFrom,
          principalChargeBillingTo = principalChargeBillingTo,
          principalChargeDueDate = principalChargeDueDate,
          principalChargeLatestClearing = principalChargeLatestClearing,
          timeToPay = timeToPay,
          supplement = supplement
        )
      }
    }

    override def writes(o: LPPDetails): JsValue = {
      jsonObjNoNulls(
        "principalChargeReference" -> o.principalChargeReference,
        "penaltyCategory" -> o.penaltyCategory,
        "penaltyStatus" -> o.penaltyStatus,
        "penaltyAmountAccruing" -> o.penaltyAmountAccruing,
        "penaltyAmountPosted" -> o.penaltyAmountPosted,
        "penaltyAmountPaid" -> o.penaltyAmountPaid,
        "penaltyAmountOutstanding" -> o.penaltyAmountOutstanding,
        "lpp1LRCalculationAmt" -> o.lpp1LRCalculationAmt,
        "lpp1LRDays" -> o.lpp1LRDays,
        "lpp1LRPercentage" -> o.lpp1LRPercentage,
        "lpp1HRCalculationAmt" -> o.lpp1HRCalculationAmt,
        "lpp1HRDays" -> o.lpp1HRDays,
        "lpp1HRPercentage" -> o.lpp1HRPercentage,
        "lpp2Days" -> o.lpp2Days,
        "lpp2Percentage" -> o.lpp2Percentage,
        "penaltyChargeCreationDate" -> o.penaltyChargeCreationDate,
        "communicationsDate" -> o.communicationsDate,
        "penaltyChargeReference" -> o.penaltyChargeReference,
        "penaltyChargeDueDate" -> o.penaltyChargeDueDate,
        "appealInformation" -> o.appealInformation,
        "principalChargeDocNumber" -> o.principalChargeDocNumber,
        "principalChargeMainTr" -> o.principalChargeMainTr,
        "principalChargeSubTr" -> o.principalChargeSubTr,
        "principalChargeBillingFrom" -> o.principalChargeBillingFrom,
        "principalChargeBillingTo" -> o.principalChargeBillingTo,
        "principalChargeDueDate" -> o.principalChargeDueDate,
        "principalChargeLatestClearing" -> o.principalChargeLatestClearing,
        "timeToPay" -> o.timeToPay,
        "supplement" -> o.supplement
      )
    }
  }
}