/*
 * Copyright 2021 HM Revenue & Customs
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

import models.ETMPPayload
import models.appeals.AppealStatusEnum
import models.payment.LatePaymentPenalty
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import play.api.libs.json.JsValue
import play.api.mvc.Request
import utils.JsonUtils
import utils.Logger.logger

case class UserHasPenaltyAuditModel(
                                     etmpPayload: ETMPPayload,
                                     identifier: String,
                                     identifierType: String,
                                     arn: Option[String]
                                   )(implicit request: Request[_]) extends JsonAuditModel with JsonUtils {

  override val auditType: String = "PenaltyUserHasPenalty"
  override val transactionName: String = "penalties-user-has-penalty"

  private val callingService: String = request.headers.get("User-Agent") match {
    case Some(x) if x.contains("penalties-frontend") => "penalties-frontend"
    case Some(x) if x.contains("business-account") => "BTA"
    case Some(x) if x.contains("vat-through-software") => "VATVC"
    case Some(_) | None =>
      logger.error("[UserHasPenaltyAuditModel] - could not distinguish referer for audit")
      logger.debug(s"[UserHasPenaltyAuditModel] - request headers: \n ${request.headers.headers}")
      ""
  }

  private val lspsUnpaidAndUnappealed: Seq[PenaltyPoint] = etmpPayload.penaltyPoints.filter(point =>
    !point.status.equals(PointStatusEnum.Paid) && !point.status.equals(PointStatusEnum.Removed) && point.`type`.equals(PenaltyTypeEnum.Financial))

  private val lppsUnpaidAndUnappealed: Option[Seq[LatePaymentPenalty]] = etmpPayload.latePaymentPenalties.map(_.filter(point =>
    !point.status.equals(PointStatusEnum.Paid) && !point.status.equals(PointStatusEnum.Removed)))

  private val totalTaxDue: BigDecimal = etmpPayload.vatOverview.map(_.map(_.amount).sum).getOrElse(BigDecimal(0))

  private val totalInterestDue: BigDecimal = {
    etmpPayload.vatOverview.map(charge =>
      charge.map(_.crystalizedInterest.getOrElse(BigDecimal(0))).sum + charge.map(_.estimatedInterest.getOrElse(BigDecimal(0))).sum).getOrElse(BigDecimal(0)) +
      lspsUnpaidAndUnappealed.map(
      point => point.financial.flatMap(_.estimatedInterest).getOrElse(BigDecimal(0)) +
        point.financial.flatMap(_.crystalizedInterest).getOrElse(BigDecimal(0))).sum +
      lppsUnpaidAndUnappealed.map(
      _.map(lpp => lpp.financial.crystalizedInterest.getOrElse(BigDecimal(0)) +
        lpp.financial.estimatedInterest.getOrElse(BigDecimal(0))).sum).getOrElse(BigDecimal(0))
  }

  private val totalFinancialPenaltyDue: BigDecimal = lspsUnpaidAndUnappealed.map(_.financial.map(_.amountDue).getOrElse(BigDecimal(0))).sum +
    lppsUnpaidAndUnappealed.map(_.map(_.financial.amountDue).sum).getOrElse(BigDecimal(0))

  private val totalDue: BigDecimal = totalTaxDue + totalInterestDue + totalFinancialPenaltyDue

  private val amountOfLSPs: Int = etmpPayload.penaltyPoints.count(point => !point.status.equals(PointStatusEnum.Removed))

  private val financialLSPs: Int = etmpPayload.penaltyPoints.count(point =>
    point.`type` == PenaltyTypeEnum.Financial && !point.status.equals(PointStatusEnum.Removed))

  private val amountOfLSPsUnderAppeal: Int = etmpPayload.penaltyPoints.count(point =>
    point.appealStatus.contains(AppealStatusEnum.Under_Review) || point.appealStatus.contains(AppealStatusEnum.Under_Tribunal_Review))

  private val lspDetail: JsValue = jsonObjNoNulls(
    "penaltyPointsThreshold" -> etmpPayload.penaltyPointsThreshold,
    "pointsTotal" -> amountOfLSPs,
    "financialPenalties" -> financialLSPs,
    "underAppeal" -> amountOfLSPsUnderAppeal
  )

  private val numberOfPaidLPPs: Int = etmpPayload.latePaymentPenalties.map(_.count(_.status == PointStatusEnum.Paid)).getOrElse(0)

  private val numberOfUnpaidLPPs: Int = etmpPayload.latePaymentPenalties.map(_.count(point =>
    !point.status.equals(PointStatusEnum.Paid) && !point.status.equals(PointStatusEnum.Removed))).getOrElse(0)

  private val totalNumberOfLPPs: Int = etmpPayload.latePaymentPenalties.map(_.count(point => !point.status.equals(PointStatusEnum.Removed))).getOrElse(0)

  private val amountOfLPPsUnderAppeal: Int = etmpPayload.latePaymentPenalties.map(_.count(point =>
    point.appealStatus.contains(AppealStatusEnum.Under_Review) || point.appealStatus.contains(AppealStatusEnum.Under_Tribunal_Review))).getOrElse(0)

  private val lppDetail: JsValue = jsonObjNoNulls(
    "numberOfPaidPenalties" -> numberOfPaidLPPs,
    "numberOfUnpaidPenalties" -> numberOfUnpaidLPPs,
    "totalNumberOfPenalties" -> totalNumberOfLPPs,
    "underAppeal" -> amountOfLPPsUnderAppeal
  )

  private val penaltyInformation: JsValue = jsonObjNoNulls(
    "totalTaxDue" -> totalTaxDue,
    "totalInterestDue" -> totalInterestDue,
    "totalFinancialPenaltyDue" -> totalFinancialPenaltyDue,
    "totalDue" -> totalDue,
    "lSPDetail" -> lspDetail,
    "lPPDetail" -> lppDetail
  )

  override val detail: JsValue = jsonObjNoNulls(
    "taxIdentifier" -> identifier,
    "identifierType" -> identifierType,
    "agentReferenceNumber" -> arn,
    "callingService" -> callingService,
    "penaltyInformation" -> penaltyInformation
  )
}
