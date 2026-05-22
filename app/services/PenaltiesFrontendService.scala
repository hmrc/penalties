/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import config.AppConfig
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.AgnosticEnrolmentKey
import models.getFinancialDetails.{DocumentDetails, FinancialDetails}
import models.getPenaltyDetails.latePayment.PrincipalChargeMainTr.ManualLPP
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier
import services.PenaltiesFrontendService._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PenaltiesFrontendService @Inject() (getFinancialDetailsService: FinancialDetailsService,
                                          appConfig: AppConfig) {

  def handleAndCombineGetFinancialDetailsData(penaltyDetails: GetPenaltyDetails, enrolmentKey: AgnosticEnrolmentKey, _arn: Option[String])(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier): Future[Either[PenaltiesFrontendError, FinancialDetailsCombinationResult]] = {
    getFinancialDetailsService.getFinancialDetails(enrolmentKey, None).flatMap { financialDetailsResponseWithClearedItems =>
      financialDetailsResponseWithClearedItems match {
        case Left(FinancialDetailsNoContent) =>
          val result =
            if (hasNoLatePaymentPenalties(penaltyDetails)) PenaltyDetailsFromFirstNoContent(penaltyDetails)
            else NoPenaltyDetailsFromFirstNoContent
          Future.successful(Right(result))

        case Left(errorResponse) =>
          Future.successful(Left(toPenaltiesFrontendError(errorResponse, enrolmentKey, clearedItemsCallSucceeded = false)))

        case Right(FinancialDetailsSuccessResponse(financialDetailsWithClearedItems)) =>
          getFinancialDetailsService.getFinancialDetails(enrolmentKey, Some(appConfig.queryParametersForGetFinancialDetailsWithoutClearedItems)).map {
            financialDetailsResponseWithoutClearedItems =>
              financialDetailsResponseWithoutClearedItems match {
                case Left(FinancialDetailsNoContent) =>
                  combineAPIData(penaltyDetails, financialDetailsWithClearedItems, FinancialDetails(None, None))
                    .map(PenaltyDetailsFromSecondNoContent)
                case Left(errorResponse) =>
                  Left(toPenaltiesFrontendError(errorResponse, enrolmentKey, clearedItemsCallSucceeded = true))
                case Right(FinancialDetailsSuccessResponse(financialDetailsWithoutClearedItems)) =>
                  combineAPIData(penaltyDetails, financialDetailsWithClearedItems, financialDetailsWithoutClearedItems)
                    .map(CombinedPenaltyDetails)
              }
          }
      }
    }
  }

  private def toPenaltiesFrontendError(financialDetailsFailure: FinancialDetailsFailure,
                                       enrolmentKey: AgnosticEnrolmentKey,
                                       clearedItemsCallSucceeded: Boolean): PenaltiesFrontendError =
    financialDetailsFailure match {
      case FinancialDetailsFailureResponse(status) if status == NOT_FOUND =>
        FinancialDetailsNotFound(enrolmentKey, clearedItemsCallSucceeded)
      case FinancialDetailsFailureResponse(status) =>
        FinancialDetailsUnexpectedStatus(status, clearedItemsCallSucceeded)
      case FinancialDetailsMalformed =>
        FinancialDetailsMalformedResponse(enrolmentKey, clearedItemsCallSucceeded)
      case FinancialDetailsNoContent =>
        FinancialDetailsNoDataFound(enrolmentKey, clearedItemsCallSucceeded)
    }

  def combineAPIData(penaltyDetails: GetPenaltyDetails,
                     financialDetailsWithClearedItems: FinancialDetails,
                     financialDetailsWithoutClearedItems: FinancialDetails): Either[PenaltiesFrontendError, GetPenaltyDetails] =
    combineLPPData(penaltyDetails, financialDetailsWithClearedItems).map { allLPPData =>
      val penaltyDetailsWithCombinedLPPs = penaltyDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(allLPPData)))
      combineTotalisations(penaltyDetailsWithCombinedLPPs, financialDetailsWithoutClearedItems)
    }

  private def combineLPPData(penaltyDetails: GetPenaltyDetails,
                             financialDetails: FinancialDetails): Either[PenaltiesFrontendError, Option[Seq[LPPDetails]]] =
    manualLPPsAs1812Models(financialDetails).map { manualLPPs =>
      if (hasNoLatePaymentPenalties(penaltyDetails)) {
        Some(manualLPPs)
      } else {
        Some(enrichLatePaymentPenalties(penaltyDetails, financialDetails) ++ manualLPPs)
      }
    }

  private def manualLPPsAs1812Models(financialDetails: FinancialDetails): Either[PenaltiesFrontendError, Seq[LPPDetails]] =
    sequence(manualLPPDocuments(financialDetails).map(manualLPPAs1812Model))

  private def manualLPPDocuments(financialDetails: FinancialDetails): Seq[DocumentDetails] =
    financialDetails.documentDetails.getOrElse(Seq.empty).filter(isManualLPPDocument)

  private def nonManualLPPDocuments(financialDetails: FinancialDetails): Seq[DocumentDetails] =
    financialDetails.documentDetails.getOrElse(Seq.empty).filterNot(isManualLPPDocument)

  private def isManualLPPDocument(documentDetails: DocumentDetails): Boolean =
    documentDetails.lineItemDetails.exists(_.exists(_.mainTransaction.contains(ManualLPP)))

  private def manualLPPAs1812Model(manualLPPDetails: DocumentDetails): Either[PenaltiesFrontendError, LPPDetails] =
    for {
      principalChargeReference <- manualLPPDetails.chargeReferenceNumber.toRight(MissingManualLPPField("chargeReferenceNumber"))
      documentTotalAmount     <- manualLPPDetails.documentTotalAmount.toRight(MissingManualLPPField("documentTotalAmount"))
      penaltyChargeCreationDate <- manualLPPDetails.issueDate.toRight(MissingManualLPPField("issueDate"))
      penaltyAmountPaid = documentTotalAmount - manualLPPDetails.documentOutstandingAmount.getOrElse(documentTotalAmount)
    } yield LPPDetails(
      penaltyCategory = LPPPenaltyCategoryEnum.ManualLPPenalty,
      penaltyChargeReference = None,
      principalChargeReference = principalChargeReference,
      penaltyChargeCreationDate = Some(penaltyChargeCreationDate),
      penaltyStatus = LPPPenaltyStatusEnum.Posted,
      penaltyAmountAccruing = 0,
      penaltyAmountPosted = documentTotalAmount,
      penaltyAmountOutstanding = manualLPPDetails.documentOutstandingAmount,
      penaltyAmountPaid = Some(penaltyAmountPaid),
      principalChargeMainTransaction = ManualLPP,
      principalChargeBillingFrom = penaltyChargeCreationDate,
      principalChargeBillingTo = penaltyChargeCreationDate,
      principalChargeDueDate = penaltyChargeCreationDate,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      LPPDetailsMetadata(
        mainTransaction = Some(ManualLPP)
      ),
      supplement = false // Manual LPPs can only ever be 'supplement = false'
    )

  private def enrichLatePaymentPenalties(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): Seq[LPPDetails] = {
    val vatOutstandingAmounts = nonManualLPPDocuments(financialDetails)
      .map(doc => doc.chargeReferenceNumber -> doc.documentOutstandingAmount)
      .toMap

    latePaymentPenaltyDetails(penaltyDetails).map { penalty =>
      val penaltyWithMainTransaction = penalty.copy(metadata = penalty.metadata.copy(mainTransaction = Some(penalty.principalChargeMainTransaction)))

      if (vatOutstandingAmounts.isEmpty) {
        penaltyWithMainTransaction
      } else {
        penaltyWithMainTransaction.copy(vatOutstandingAmount = vatOutstandingAmounts.get(Some(penalty.principalChargeReference)).flatten)
      }
    }
  }

  private def combineTotalisations(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): GetPenaltyDetails = {
    val totalAmountOfManualLPPs: Option[BigDecimal] = penaltyDetails.latePaymentPenalty
      .flatMap(
        _.details.map(_.filter(_.principalChargeMainTransaction == ManualLPP).map(_.penaltyAmountOutstanding.getOrElse(BigDecimal(0))).sum)
      )
      .fold[Option[BigDecimal]](None)(amount => if (amount == BigDecimal(0)) None else Some(amount))
    (financialDetails.totalisation.isDefined, penaltyDetails.totalisations.isDefined) match {
      // If there is totalisations already, add to it
      case (_, true) =>
        val newTotalisations: Option[Totalisations] = penaltyDetails.totalisations.map { oldTotalisations =>
          oldTotalisations.copy(
            totalAccountOverdue = financialDetails.totalisation.flatMap(_.regimeTotalisation.flatMap(_.totalAccountOverdue)),
            totalAccountPostedInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountPostedInterest)),
            totalAccountAccruingInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountAccruingInterest)),
            LPPPostedTotal = oldTotalisations.LPPPostedTotal.map(_ + totalAmountOfManualLPPs.getOrElse(BigDecimal(0)))
          )
        }
        penaltyDetails.copy(totalisations = newTotalisations)
      case (true, false) =>
        // If there is no totalisations already, create a new object
        val totalisations: Totalisations = new Totalisations(
          totalAccountOverdue = financialDetails.totalisation.flatMap(_.regimeTotalisation.flatMap(_.totalAccountOverdue)),
          totalAccountPostedInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountPostedInterest)),
          totalAccountAccruingInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountAccruingInterest)),
          LSPTotalValue = None,
          penalisedPrincipalTotal = None,
          LPPPostedTotal = totalAmountOfManualLPPs,
          LPPEstimatedTotal = None
        )
        penaltyDetails.copy(totalisations = Some(totalisations))
      case _ =>
        // No totalisations at all, don't do any processing on totalisation field (except adding LPPPostedTotal for Manual LPPs
        val totalisations: Totalisations = new Totalisations(
          totalAccountOverdue = None,
          totalAccountPostedInterest = None,
          totalAccountAccruingInterest = None,
          LSPTotalValue = None,
          penalisedPrincipalTotal = None,
          LPPEstimatedTotal = None,
          LPPPostedTotal = totalAmountOfManualLPPs
        )
        penaltyDetails.copy(totalisations = Some(totalisations))
    }
  }

  private def hasNoLatePaymentPenalties(penaltyDetails: GetPenaltyDetails): Boolean =
    penaltyDetails.latePaymentPenalty.flatMap(_.details).forall(_.isEmpty)

  private def latePaymentPenaltyDetails(penaltyDetails: GetPenaltyDetails): Seq[LPPDetails] =
    penaltyDetails.latePaymentPenalty.flatMap(_.details).getOrElse(Seq.empty)

  private def sequence[E, A](xs: Seq[Either[E, A]]): Either[E, Seq[A]] =
    xs.foldRight(Right(Seq.empty): Either[E, Seq[A]]) { (next, accumulated) =>
      for {
        value  <- next
        values <- accumulated
      } yield value +: values
    }
}

object PenaltiesFrontendService {

  sealed trait PenaltiesFrontendError {
    def message: String
    def clearedItemsCallSucceeded: Boolean
  }

  final case class FinancialDetailsNotFound(enrolmentKey: AgnosticEnrolmentKey, clearedItemsCallSucceeded: Boolean)
      extends PenaltiesFrontendError {
    override val message: String =
      s"[RegimePenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned 404 for $enrolmentKey"
  }

  final case class FinancialDetailsUnexpectedStatus(status: Int, clearedItemsCallSucceeded: Boolean) extends PenaltiesFrontendError {
    override val message: String =
      s"[RegimePenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned an unexpected status: $status"
  }

  final case class FinancialDetailsMalformedResponse(enrolmentKey: AgnosticEnrolmentKey, clearedItemsCallSucceeded: Boolean)
      extends PenaltiesFrontendError {
    override val message: String =
      s"[RegimePenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned invalid body - failed to parse financial details response for $enrolmentKey"
  }

  final case class FinancialDetailsNoDataFound(enrolmentKey: AgnosticEnrolmentKey, clearedItemsCallSucceeded: Boolean) extends PenaltiesFrontendError {
    override val message: String =
      s"[RegimePenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned 404 for $enrolmentKey with NO_DATA_FOUND in response body"
  }

  final case class MissingManualLPPField(fieldName: String) extends PenaltiesFrontendError {
    override val clearedItemsCallSucceeded: Boolean = true
    override val message: String =
      s"[RegimePenaltiesFrontendService][combineAPIData] - Manual LPP document is missing $fieldName"
  }

  sealed trait FinancialDetailsCombinationResult
  final case class PenaltyDetailsFromFirstNoContent(penaltyDetails: GetPenaltyDetails) extends FinancialDetailsCombinationResult
  case object NoPenaltyDetailsFromFirstNoContent extends FinancialDetailsCombinationResult
  final case class PenaltyDetailsFromSecondNoContent(penaltyDetails: GetPenaltyDetails) extends FinancialDetailsCombinationResult
  final case class CombinedPenaltyDetails(penaltyDetails: GetPenaltyDetails) extends FinancialDetailsCombinationResult
}
