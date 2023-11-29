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

package services

import config.AppConfig
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import models.auditing.UserHasPenaltyAuditModel
import models.getFinancialDetails.MainTransactionEnum.ManualLPP
import models.getFinancialDetails.{DocumentDetails, FinancialDetails}
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.Json
import play.api.mvc.Results.{InternalServerError, NoContent, NotFound, Ok}
import play.api.mvc.{Request, Result}
import services.auditing.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API
import utils.{DateHelper, PagerDutyHelper, RegimeHelper}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PenaltiesFrontendService @Inject()(getFinancialDetailsService: GetFinancialDetailsService,
                                         appConfig: AppConfig,
                                         dateHelper: DateHelper,
                                         auditService: AuditService) {

  def handleAndCombineGetFinancialDetailsData(penaltyDetails: GetPenaltyDetails, enrolmentKey: String, arn: Option[String])
                                             (implicit request: Request[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    val vrn: String = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
    getFinancialDetailsService.getFinancialDetails(vrn, None).flatMap {
      financialDetailsResponseWithClearedItems =>
        financialDetailsResponseWithClearedItems.fold({
          errorResponse => {
            Future(handleErrorResponseFromGetFinancialDetails(errorResponse, vrn)(handleNoContent = {
              logger.info(s"[PenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned 404 for VRN: $vrn with NO_DATA_FOUND in response body")
              if (penaltyDetails.latePaymentPenalty.isEmpty || penaltyDetails.latePaymentPenalty.get.details.isEmpty ||
                penaltyDetails.latePaymentPenalty.get.details.get.isEmpty) {
                returnResponse(penaltyDetails, enrolmentKey, arn)
              } else {
                NoContent
              }
            }))
          }
        },
          financialDetailsSuccessWithClearedItems => { //NOTE: The decision was taken to make 2 calls to retrieve data with and without cleared items
            logger.debug(s"[PenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 clearedItems=true call returned 200 for VRN: $vrn")
            getFinancialDetailsService.getFinancialDetails(vrn, Some(appConfig.queryParametersForGetFinancialDetailsWithoutClearedItems)).map {
              financialDetailsResponseWithoutClearedItems =>
                financialDetailsResponseWithoutClearedItems.fold({
                  handleErrorResponseFromGetFinancialDetails(_, vrn)(handleNoContent = {
                    logger.info(s"[PenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned 404 for VRN: $vrn with NO_DATA_FOUND in response body")
                    val newPenaltyDetails = combineAPIData(penaltyDetails,
                      financialDetailsSuccessWithClearedItems.asInstanceOf[GetFinancialDetailsSuccessResponse].financialDetails,
                      FinancialDetails(None, None))
                    returnResponse(newPenaltyDetails, enrolmentKey, arn)
                  })
                },
                  financialDetailsSuccessWithoutClearedItems => {
                    logger.debug(s"[PenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 clearedItems=false call returned 200 for VRN: $vrn")
                    val newPenaltyDetails = combineAPIData(penaltyDetails,
                      financialDetailsSuccessWithClearedItems.asInstanceOf[GetFinancialDetailsSuccessResponse].financialDetails,
                      financialDetailsSuccessWithoutClearedItems.asInstanceOf[GetFinancialDetailsSuccessResponse].financialDetails)
                    logger.info(s"[PenaltiesFrontendService][handleAndCombineGetFinancialDetailsData] - 1811 call returned 200 for VRN: $vrn")
                    returnResponse(newPenaltyDetails, enrolmentKey, arn)
                  })
            }
          }
        )
    }
  }

  def handleErrorResponseFromGetFinancialDetails(financialDetailsResponseWithClearedItems: GetFinancialDetailsFailure, vrn: String)
                                                (handleNoContent: => Result): Result = {
    financialDetailsResponseWithClearedItems match {
      case GetFinancialDetailsNoContent => handleNoContent
      case GetFinancialDetailsFailureResponse(status) if status == NOT_FOUND => {
        logger.info(s"[PenaltiesFrontendController][handleAndCombineGetFinancialDetailsData] - 1811 call returned 404 for VRN: $vrn")
        NotFound(s"A downstream call returned 404 for VRN: $vrn")
      }
      case GetFinancialDetailsFailureResponse(status) => {
        logger.error(s"[PenaltiesFrontendController][handleAndCombineGetFinancialDetailsData] - 1811 call returned an unexpected status: $status")
        InternalServerError(s"A downstream call returned an unexpected status: $status")
      }
      case GetFinancialDetailsMalformed => {
        PagerDutyHelper.log("getPenaltiesData", MALFORMED_RESPONSE_FROM_1811_API)
        logger.error(s"[PenaltiesFrontendController][handleAndCombineGetFinancialDetailsData] - 1811 call returned invalid body - failed to parse financial details response for VRN: $vrn")
        InternalServerError(s"We were unable to parse penalty data.")
      }
    }
  }

  private def returnResponse(penaltyDetails: GetPenaltyDetails, enrolmentKey: String, arn: Option[String])
                            (implicit request: Request[_], ec: ExecutionContext, hc: HeaderCarrier): Result = {
    val hasLSP = penaltyDetails.lateSubmissionPenalty.map(_.summary.activePenaltyPoints).getOrElse(0) > 0
    val hasLPP = penaltyDetails.latePaymentPenalty.flatMap(_.details.map(_.length)).getOrElse(0) > 0

    if (hasLSP || hasLPP) {
      val auditModel = UserHasPenaltyAuditModel(
        penaltyDetails = penaltyDetails,
        identifier = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey),
        identifierType = RegimeHelper.getIdentifierTypeFromEnrolmentKey(enrolmentKey),
        arn = arn,
        dateHelper = dateHelper)
      auditService.audit(auditModel)
    }
    Ok(Json.toJson(penaltyDetails))
  }

  def combineAPIData(penaltyDetails: GetPenaltyDetails,
                     financialDetailsWithClearedItems: FinancialDetails,
                     financialDetailsWithoutClearedItems: FinancialDetails): GetPenaltyDetails = {
    val allLPPData = combineLPPData(penaltyDetails, financialDetailsWithClearedItems)
    val penaltyDetailsWithCombinedLPPs = penaltyDetails.copy(latePaymentPenalty = Some(LatePaymentPenalty(allLPPData)))
    val totalisationsCombined = combineTotalisations(penaltyDetailsWithCombinedLPPs, financialDetailsWithoutClearedItems)
    totalisationsCombined
  }

  private def combineLPPData(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): Option[Seq[LPPDetails]] = {
    val optManualLPPs: Option[Seq[DocumentDetails]] = financialDetails.documentDetails.map(_.filter(_.lineItemDetails.exists(_.exists(_.mainTransaction.contains(ManualLPP)))))
    val optManualLPPsAs1812Models: Option[Seq[LPPDetails]] = optManualLPPs.map(_.map {
      manualLPPDetails => {
        val principalChargeReference = manualLPPDetails.chargeReferenceNumber.get //Set to penaltyChargeReference because Manual LPP's do not have principal charges and we don't use this in Manual LPP cases
        val penaltyAmountPaid = manualLPPDetails.documentTotalAmount.get - manualLPPDetails.documentOutstandingAmount.getOrElse(manualLPPDetails.documentTotalAmount.get)
        val penaltyChargeCreationDate = manualLPPDetails.issueDate.get
        LPPDetails(
          penaltyCategory = LPPPenaltyCategoryEnum.ManualLPP,
          penaltyChargeReference = None,
          principalChargeReference = principalChargeReference,
          penaltyChargeCreationDate = Some(penaltyChargeCreationDate),
          penaltyStatus = LPPPenaltyStatusEnum.Posted,
          penaltyAmountAccruing = 0,
          penaltyAmountPosted = manualLPPDetails.documentTotalAmount.get,
          penaltyAmountOutstanding = manualLPPDetails.documentOutstandingAmount,
          penaltyAmountPaid = Some(penaltyAmountPaid),
          principalChargeMainTransaction = ManualLPP,
          principalChargeBillingFrom = penaltyChargeCreationDate,
          principalChargeBillingTo = penaltyChargeCreationDate,
          principalChargeDueDate = penaltyChargeCreationDate,
          None, None, None, None, None, None, None, None, None, None, None, None, None, LPPDetailsMetadata(
            mainTransaction = Some(ManualLPP)
          )
        )
      }
    })
    val manualLPPAs1812Models = optManualLPPsAs1812Models.getOrElse(Seq())
    if (penaltyDetails.latePaymentPenalty.isEmpty || penaltyDetails.latePaymentPenalty.exists(_.details.isEmpty)) {
      Some(manualLPPAs1812Models)
    } else {
      val optNotManual = financialDetails.copy(documentDetails = financialDetails.documentDetails.map(_.filter(x => !x.lineItemDetails.exists(_.exists(_.mainTransaction.contains(ManualLPP))))))
      val vatAmounts = optNotManual.documentDetails.map(docs => docs.map(doc => doc.chargeReferenceNumber -> doc.documentOutstandingAmount)).getOrElse(Seq.empty).toMap
      if (vatAmounts.isEmpty) {
        penaltyDetails.latePaymentPenalty.flatMap(
          _.details.map(
            _.map(
              penalty => penalty.copy(metadata = penalty.metadata.copy(mainTransaction = Some(penalty.principalChargeMainTransaction)))
            ) ++ manualLPPAs1812Models
          )
        )
      } else {
      penaltyDetails.latePaymentPenalty.flatMap(
        _.details.map(
          _.map(
            penalty => penalty.copy(metadata = penalty.metadata.copy(
              mainTransaction = Some(penalty.principalChargeMainTransaction)),
              vatOutstandingAmount = if(vatAmounts.contains(Some(penalty.principalChargeReference))) {
                vatAmounts(Some(penalty.principalChargeReference))
              } else None
            )
          ) ++ manualLPPAs1812Models
        )
      )
    }
    }
  }

  private def combineTotalisations(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): GetPenaltyDetails = {
    val totalAmountOfManualLPPs: Option[BigDecimal] = penaltyDetails.latePaymentPenalty.flatMap(
      _.details.map(_.filter(_.principalChargeMainTransaction.equals(ManualLPP)).map(
        _.penaltyAmountOutstanding.getOrElse(BigDecimal(0))).sum)
    ).fold[Option[BigDecimal]](None)(amount => if(amount == BigDecimal(0)) None else Some(amount))
    (financialDetails.totalisation.isDefined, penaltyDetails.totalisations.isDefined) match {
      //If there is totalisations already, add to it
      case (_, true) => {
        val newTotalisations: Option[Totalisations] = penaltyDetails.totalisations.map(
          oldTotalisations => {
            oldTotalisations.copy(
              totalAccountOverdue = financialDetails.totalisation.flatMap(_.regimeTotalisations.flatMap(_.totalAccountOverdue)),
              totalAccountPostedInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountPostedInterest)),
              totalAccountAccruingInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountAccruingInterest)),
              LPPPostedTotal = oldTotalisations.LPPPostedTotal.map(_ + totalAmountOfManualLPPs.getOrElse(BigDecimal(0)))
            )
          }
        )
        penaltyDetails.copy(totalisations = newTotalisations)
      }
      case (true, false) => {
        //If there is no totalisations already, create a new object
        val totalisations: Totalisations = new Totalisations(
          totalAccountOverdue = financialDetails.totalisation.flatMap(_.regimeTotalisations.flatMap(_.totalAccountOverdue)),
          totalAccountPostedInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountPostedInterest)),
          totalAccountAccruingInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountAccruingInterest)),
          LSPTotalValue = None,
          penalisedPrincipalTotal = None,
          LPPPostedTotal = totalAmountOfManualLPPs,
          LPPEstimatedTotal = None
        )
        penaltyDetails.copy(totalisations = Some(totalisations))
      }
      case _ => {
        //No totalisations at all, don't do any processing on totalisation field (except adding LPPPostedTotal for Manual LPPs
        val totalisations: Totalisations = new Totalisations(totalAccountOverdue = None, totalAccountPostedInterest = None,
          totalAccountAccruingInterest = None, LSPTotalValue = None, penalisedPrincipalTotal = None, LPPEstimatedTotal = None,
          LPPPostedTotal = totalAmountOfManualLPPs
        )
        penaltyDetails.copy(totalisations = Some(totalisations))
      }
    }
  }
}
