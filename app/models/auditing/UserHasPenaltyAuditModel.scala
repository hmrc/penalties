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

package models.auditing

import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.AppealStatusEnum
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyStatusEnum, TimeToPay}
import models.getPenaltyDetails.lateSubmission.{LSPDetails, LSPPenaltyCategoryEnum, LSPPenaltyStatusEnum}
import play.api.libs.json.JsValue
import play.api.mvc.Request
import utils.Logger.logger
import utils.{DateHelper, JsonUtils}

import java.time.LocalDate

case class UserHasPenaltyAuditModel(
                                     penaltyDetails: GetPenaltyDetails,
                                     identifier: String,
                                     identifierType: String,
                                     arn: Option[String],
                                     dateHelper: DateHelper
                                   )(implicit request: Request[_]) extends JsonAuditModel with JsonUtils {

  override val auditType: String = "PenaltyUserHasPenalty"
  override val transactionName: String = "penalties-user-has-penalty"

  private val callingService: String = request.headers.get("User-Agent") match {
    case Some(x) if x.contains("penalties-frontend") => "penalties-frontend"
    case Some(x) if x.contains("business-tax-account") => "BTA"
    case Some(x) if x.contains("vat-agent-client-lookup-frontend") => "VATVC Agent"
    case Some(x) if x.contains("vat-summary-frontend") => "VATVC"
    case Some(service) =>
      logger.warn(s"[UserHasPenaltyAuditModel] - unknown caller has been identified retrieving summary data: $service")
      logger.debug(s"[UserHasPenaltyAuditModel] - request headers: \n ${request.headers.headers}")
      service
    case None =>
      logger.error("[UserHasPenaltyAuditModel] - could not distinguish referer for audit - setting value to blank")
      logger.debug(s"[UserHasPenaltyAuditModel] - request headers: \n ${request.headers.headers}")
      ""
  }

  val userType: String = if (arn.isDefined || callingService == "VATVC Agent") "Agent" else "Trader"

  private val amountOfLspChargesPaid: Int = penaltyDetails.lateSubmissionPenalty.map(_.details.count(point => point.chargeOutstandingAmount.contains(BigDecimal(0))
    && (point.penaltyCategory.contains(LSPPenaltyCategoryEnum.Charge) || point.penaltyCategory.contains(LSPPenaltyCategoryEnum.Threshold))
    && point.penaltyStatus.equals(LSPPenaltyStatusEnum.Active))).getOrElse(0)

  private val lspsUnpaidAndUnappealed: Seq[LSPDetails] = penaltyDetails.lateSubmissionPenalty.map(_.details.filter(point =>
    !point.chargeOutstandingAmount.contains(BigDecimal(0))
      && !point.penaltyStatus.equals(LSPPenaltyStatusEnum.Inactive)
      && (point.penaltyCategory.contains(LSPPenaltyCategoryEnum.Threshold) || point.penaltyCategory.contains(LSPPenaltyCategoryEnum.Charge)))).getOrElse(Seq.empty)

  private val lppsUnpaidAndUnappealed: Option[Seq[LPPDetails]] = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.filter(penalty =>
    !penalty.penaltyAmountOutstanding.contains(BigDecimal(0)) && !penalty.penaltyStatus.equals(LSPPenaltyStatusEnum.Inactive))))

  private val totalTaxDue: BigDecimal = penaltyDetails.totalisations.flatMap(_.penalisedPrincipalTotal).getOrElse(0)

  private val totalFinancialPenaltyDue: BigDecimal = {
    lspsUnpaidAndUnappealed.map(_.chargeOutstandingAmount.getOrElse(BigDecimal(0))).sum +
      lppsUnpaidAndUnappealed.map(_.map(_.penaltyAmountOutstanding.getOrElse(BigDecimal(0))).sum).getOrElse(BigDecimal(0))
  }

  private val totalDue: BigDecimal = totalTaxDue + totalFinancialPenaltyDue

  private val amountOfLSPs: Int = penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0)

  private val amountOfInactiveLSPs: Int = penaltyDetails.lateSubmissionPenalty.map(_.summary.inactivePenaltyPoints).getOrElse(0)

  private val amountOfLspChargesUnpaid: Int = penaltyDetails.lateSubmissionPenalty.map(_.details.count(point =>
    (point.chargeOutstandingAmount.isDefined && !point.chargeOutstandingAmount.contains(BigDecimal(0))) && (
      point.penaltyCategory.contains(LSPPenaltyCategoryEnum.Charge) || point.penaltyCategory.contains(LSPPenaltyCategoryEnum.Threshold)))
  ).getOrElse(0)

  private val financialLSPs: Int = penaltyDetails.lateSubmissionPenalty.map(_.details.count(point =>
    (point.penaltyCategory.contains(LSPPenaltyCategoryEnum.Threshold) || point.penaltyCategory.contains(LSPPenaltyCategoryEnum.Charge))
      && !point.penaltyStatus.equals(LSPPenaltyStatusEnum.Inactive)
  )).getOrElse(0)

  private val amountOfLSPsUnderAppeal: Int = penaltyDetails.lateSubmissionPenalty.map(_.details.count(point =>
    point.appealInformation.exists(_.exists(_.appealStatus.contains(AppealStatusEnum.Under_Appeal))))).getOrElse(0)

  private val numberOfPartiallyPaidLSPs: Int = penaltyDetails.lateSubmissionPenalty.map(_.details.count(point =>
    (point.chargeOutstandingAmount.isDefined && point.chargeAmount.isDefined) &&
      (point.chargeOutstandingAmount.get < point.chargeAmount.get && point.chargeOutstandingAmount.get > 0)
      && !point.appealInformation.exists(_.exists(_.appealStatus.contains(AppealStatusEnum.Upheld)))
  )).getOrElse(0)

  private val lspDetail: JsValue = jsonObjNoNulls(
    "penaltyPointsThreshold" -> penaltyDetails.lateSubmissionPenalty.map(_.summary.regimeThreshold),
    "pointsTotal" -> amountOfLSPs,
    "inactivePoints" -> amountOfInactiveLSPs,
    "financialPenalties" -> financialLSPs,
    "numberOfPaidPenalties" -> amountOfLspChargesPaid,
    "underAppeal" -> amountOfLSPsUnderAppeal,
    "numberOfPartiallyPaidLSPs" -> numberOfPartiallyPaidLSPs,
    "numberOfUnpaidPenalties" -> amountOfLspChargesUnpaid
  )

  private val numberOfPaidLPPs: Int = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.count(penalty =>
     penalty.penaltyAmountPaid.isDefined &&
      penalty.penaltyAmountPosted.equals(penalty.penaltyAmountPaid.get) &&
      !penalty.appealInformation.exists(_.exists(_.appealStatus.contains(AppealStatusEnum.Upheld)))
  ))).getOrElse(0)

  private val numberOfUnpaidLPPs: Int = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.count(penalty =>
    penalty.penaltyAmountOutstanding.isDefined &&
      penalty.penaltyAmountPosted.equals(penalty.penaltyAmountOutstanding.get) &&
      !penalty.appealInformation.exists(_.exists(_.appealStatus.contains(AppealStatusEnum.Upheld)))
  ))).getOrElse(0)

  private val numberOfPartiallyPaidLPPs: Int = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.count(penalty =>
    penalty.penaltyAmountOutstanding.exists(_ > BigDecimal(0)) &&
      penalty.penaltyAmountPaid.exists(_ > BigDecimal(0)) &&
      !penalty.penaltyStatus.equals(LPPPenaltyStatusEnum.Accruing) &&
      !penalty.appealInformation.exists(_.exists(_.appealStatus.contains(AppealStatusEnum.Upheld)))
  ))).getOrElse(0)

  private val totalNumberOfLPPs: Int = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(
    _.count(penalty => !penalty.appealInformation.exists(_.exists(_.appealStatus.contains(AppealStatusEnum.Upheld)))))).getOrElse(0)

  private val amountOfLPPsUnderAppeal: Int = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.count(penalty =>
    penalty.appealInformation.exists(_.exists(_.appealStatus.contains(AppealStatusEnum.Under_Appeal)))))).getOrElse(0)

  def getOptActiveTimeToPay: Option[TimeToPay] = {
    val dateNow = dateHelper.dateNow()
    for {
      lpp <- penaltyDetails.latePaymentPenalty
      lppDetails <- lpp.details
      optSeqTimeToPay <- lppDetails.find(_.metadata.timeToPay.isDefined).map(_.metadata.timeToPay.get)
      optActiveTimeToPay <- optSeqTimeToPay.find(
        penalty => (penalty.TTPEndDate, penalty.TTPStartDate) match {
          case (Some(endDate), Some(startDate)) => DateHelper.isDateAfterOrEqual(dateNow, startDate) && DateHelper.isDateBeforeOrEqual(dateNow, endDate)
          case (None, Some(startDate)) => DateHelper.isDateAfterOrEqual(dateNow, startDate)
          case (_,_) => false
        }
      )
    } yield optActiveTimeToPay
  }

  private val isTTPActive: Boolean = getOptActiveTimeToPay.isDefined

  private val ttpStartDate: Option[LocalDate] = getOptActiveTimeToPay.flatMap(_.TTPStartDate)

  private val ttpEndDate: Option[LocalDate] = getOptActiveTimeToPay.flatMap(_.TTPEndDate)

  private val lppDetail: JsValue = jsonObjNoNulls(
    "numberOfPaidPenalties" -> numberOfPaidLPPs,
    "numberOfUnpaidPenalties" -> numberOfUnpaidLPPs,
    "numberOfPartiallyPaidPenalties" -> numberOfPartiallyPaidLPPs,
    "totalNumberOfPenalties" -> totalNumberOfLPPs,
    "underAppeal" -> amountOfLPPsUnderAppeal
  )

  private val ttpDetail: JsValue = jsonObjNoNulls(
    "isTimeToPayActive" -> isTTPActive,
    "timeToPayStartDate" -> ttpStartDate,
    "timeToPayEndDate" -> ttpEndDate
  )

  private val penaltyInformation: JsValue = jsonObjNoNulls(
    "totalTaxDue" -> totalTaxDue,
    "totalFinancialPenaltyDue" -> totalFinancialPenaltyDue,
    "totalDue" -> totalDue,
    "lateSubmissionPenaltyDetail" -> lspDetail,
    "latePaymentPenaltyDetail" -> lppDetail,
    "timeToPayInformation" -> ttpDetail
  )

  override val detail: JsValue = jsonObjNoNulls(
    "taxIdentifier" -> identifier,
    "identifierType" -> identifierType,
    "agentReferenceNumber" -> arn,
    "userType" -> userType,
    "callingService" -> callingService,
    "penaltyInformation" -> penaltyInformation
  )
}
