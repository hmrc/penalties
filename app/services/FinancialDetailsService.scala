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

import config.featureSwitches.{CallAPI1811HIP, FeatureSwitching}
import connectors.getFinancialDetails.{FinancialDetailsConnector, FinancialDetailsHipConnector}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import connectors.parsers.getFinancialDetails.HIPFinancialDetailsParser.HIPFinancialDetailsResponse
import models.AgnosticEnrolmentKey
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsService @Inject() (
    getFinancialDetailsConnector: FinancialDetailsConnector,
    financialDetailsHipConnector: FinancialDetailsHipConnector)(implicit ec: ExecutionContext, val config: Configuration)
    extends FeatureSwitching {

  def getFinancialDetails(enrolmentKey: AgnosticEnrolmentKey, optionalParameters: Option[String])(implicit
      hc: HeaderCarrier): Future[FinancialDetailsResponse] = {

    def callHipConnector: Future[FinancialDetailsResponse] = {
      val includeClearedItems: Boolean = optionalParameters.isEmpty
      financialDetailsHipConnector
        .getFinancialDetails(enrolmentKey, includeClearedItems)
        .map(mapHipToIfResponse)
        .map(logResponse(_, enrolmentKey))
    }

    def callIfConnector: Future[FinancialDetailsResponse] =
      getFinancialDetailsConnector.getFinancialDetails(enrolmentKey, optionalParameters).map(logResponse(_, enrolmentKey))

    if (isEnabled(CallAPI1811HIP)) callHipConnector else callIfConnector
  }

  private def mapHipToIfResponse(response: HIPFinancialDetailsResponse): FinancialDetailsResponse =
    response.left.map(_.toIFFailureResponse).map(_.toIFSuccessResponse)

  private def logResponse(connectorResponse: FinancialDetailsResponse, enrolmentKey: AgnosticEnrolmentKey): FinancialDetailsResponse = {
    val startOfLogMsg: String = s"[FinancialDetailsService][getFinancialDetails][$enrolmentKey]"
    connectorResponse match {
      case _ @Right(_ @FinancialDetailsSuccessResponse(_)) =>
        logger.info(s"$startOfLogMsg - Success response with data returned from connector")
      case _ @Left(FinancialDetailsNoContent) =>
        logger.info(s"$startOfLogMsg - No data found for ID")
      case _ @Left(FinancialDetailsMalformed) =>
        logger.error(s"$startOfLogMsg - Failed to parse HTTP response into model for ID")
      case _ @Left(FinancialDetailsFailureResponse(_)) =>
        logger.error(s"$startOfLogMsg - Unknown status returned from connector for ID")
    }
    connectorResponse
  }

  def getFinancialDetailsForAPI(enrolmentKey: AgnosticEnrolmentKey,
                                searchType: Option[String],
                                searchItem: Option[String],
                                dateType: Option[String],
                                dateFrom: Option[String],
                                dateTo: Option[String],
                                includeClearedItems: Option[Boolean],
                                includeStatisticalItems: Option[Boolean],
                                includePaymentOnAccount: Option[Boolean],
                                addRegimeTotalisation: Option[Boolean],
                                addLockInformation: Option[Boolean],
                                addPenaltyDetails: Option[Boolean],
                                addPostedInterestDetails: Option[Boolean],
                                addAccruingInterestDetails: Option[Boolean])(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    def callHipConnector: Future[HttpResponse] =
      financialDetailsHipConnector.getFinancialDetailsForAPI(
        enrolmentKey,
        searchType,
        searchItem,
        dateType,
        dateFrom,
        dateTo,
        includeClearedItems,
        includeStatisticalItems,
        includePaymentOnAccount,
        addRegimeTotalisation,
        addLockInformation,
        addPenaltyDetails,
        addPostedInterestDetails,
        addAccruingInterestDetails
      )

    def callIfConnector: Future[HttpResponse] =
      getFinancialDetailsConnector.getFinancialDetailsForAPI(
        enrolmentKey,
        searchType,
        searchItem,
        dateType,
        dateFrom,
        dateTo,
        includeClearedItems,
        includeStatisticalItems,
        includePaymentOnAccount,
        addRegimeTotalisation,
        addLockInformation,
        addPenaltyDetails,
        addPostedInterestDetails,
        addAccruingInterestDetails
      )

    if (isEnabled(CallAPI1811HIP)) callHipConnector else callIfConnector
  }
}
