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
import models.getFinancialDetails.{FinancialDetails, MainTransactionEnum}
import models.getPenaltyDetails.appealInfo.AppealStatusEnum
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
    val totalisationsCombined = combineTotalisations(penaltyDetails, financialDetailsWithoutClearedItems)
    val allLPPData = combineLPPData(penaltyDetails, financialDetailsWithClearedItems)
    if (allLPPData.isDefined) {
      totalisationsCombined.copy(latePaymentPenalty = Some(LatePaymentPenalty(allLPPData)))
    } else {
      totalisationsCombined
    }
  }

  private def combineLPPData(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): Option[Seq[LPPDetails]] = {
    penaltyDetails.latePaymentPenalty.flatMap(
      _.details.map(_.map(
        oldLPPDetails => {
          (oldLPPDetails.penaltyStatus, oldLPPDetails.appealInformation) match {
            case (_, Some(appealInformation)) if appealInformation.exists(_.appealStatus.exists(isAppealStatusUpheldOrChargeReversed)) => {
              oldLPPDetails.copy(
                metadata = LPPDetailsMetadata(
                  mainTransaction = Some(oldLPPDetails.principalChargeMainTransaction),
                  timeToPay = oldLPPDetails.metadata.timeToPay,
                  principalChargeDocNumber = oldLPPDetails.metadata.principalChargeDocNumber,
                  principalChargeSubTransaction = oldLPPDetails.metadata.principalChargeSubTransaction
                )
              )
            }
            case (LPPPenaltyStatusEnum.Accruing, _) => {
              oldLPPDetails.copy(
                metadata = LPPDetailsMetadata(
                  mainTransaction = Some(oldLPPDetails.principalChargeMainTransaction),
                  timeToPay = oldLPPDetails.metadata.timeToPay,
                  principalChargeDocNumber = oldLPPDetails.metadata.principalChargeDocNumber,
                  principalChargeSubTransaction = oldLPPDetails.metadata.principalChargeSubTransaction
                )
              )
            }
            case _ => {
              val isLPP2 = oldLPPDetails.penaltyCategory.equals(LPPPenaltyCategoryEnum.SecondPenalty)
              val firstAndMaybeSecondPenalty = financialDetails.documentDetails.get.filter(_.chargeReferenceNumber.exists(_.equals(oldLPPDetails.penaltyChargeReference.get)))
              val penaltyToCopy = firstAndMaybeSecondPenalty.find(lpp => {
                if (isLPP2) MainTransactionEnum.secondCharges.contains(lpp.lineItemDetails.get.head.mainTransaction.get)
                else MainTransactionEnum.firstCharges.contains(lpp.lineItemDetails.get.head.mainTransaction.get)
              }
              )
              oldLPPDetails.copy(
                metadata = LPPDetailsMetadata(
                  mainTransaction = penaltyToCopy.get.lineItemDetails.get.head.mainTransaction,
                  outstandingAmount = penaltyToCopy.get.documentOutstandingAmount,
                  timeToPay = oldLPPDetails.metadata.timeToPay,
                  principalChargeDocNumber = oldLPPDetails.metadata.principalChargeDocNumber,
                  principalChargeSubTransaction = oldLPPDetails.metadata.principalChargeSubTransaction
                )
              )
            }
          }
        }
      ))
    )
  }

  private def isAppealStatusUpheldOrChargeReversed(appealStatus: AppealStatusEnum.Value): Boolean = {
    appealStatus.toString == AppealStatusEnum.Upheld.toString ||
      appealStatus == AppealStatusEnum.AppealUpheldChargeAlreadyReversed ||
      appealStatus == AppealStatusEnum.AppealRejectedChargeAlreadyReversed
  }

  private def combineTotalisations(penaltyDetails: GetPenaltyDetails, financialDetails: FinancialDetails): GetPenaltyDetails = {
    (financialDetails.totalisation.isDefined, penaltyDetails.totalisations.isDefined) match {
      //If there is totalisations already, add to it
      case (_, true) => {
        val newTotalisations: Option[Totalisations] = penaltyDetails.totalisations.map(
          oldTotalisations => {
            oldTotalisations.copy(
              totalAccountOverdue = financialDetails.totalisation.flatMap(_.regimeTotalisations.flatMap(_.totalAccountOverdue)),
              totalAccountPostedInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountPostedInterest)),
              totalAccountAccruingInterest = financialDetails.totalisation.flatMap(_.interestTotalisations.flatMap(_.totalAccountAccruingInterest))
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
          LPPPostedTotal = None,
          LPPEstimatedTotal = None
        )
        penaltyDetails.copy(totalisations = Some(totalisations))
      }
      case _ => {
        //No totalisations at all, don't do any processing on totalisation field
        penaltyDetails
      }
    }
  }
}
