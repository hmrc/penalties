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

package controllers

import base.{LogCapturing, SpecBase}
import config.AppConfig
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser.{GetFinancialDetailsFailureResponse, GetFinancialDetailsMalformed, GetFinancialDetailsNoContent, GetFinancialDetailsSuccessResponse}
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsNoContent, GetPenaltyDetailsSuccessResponse}
import models.getFinancialDetails
import models.getFinancialDetails._
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.lateSubmission.{LSPSummary, LateSubmissionPenalty}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.auditing.AuditService
import services.{AppealService, GetFinancialDetailsService, GetPenaltyDetailsService, PenaltiesFrontendService}
import utils.DateHelper
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PenaltiesFrontendControllerSpec extends SpecBase with LogCapturing {
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val mockAppealService: AppealService = mock(classOf[AppealService])
  val mockAuditService: AuditService = mock(classOf[AuditService])
  val dateHelper: DateHelper = injector.instanceOf[DateHelper]
  val mockGetPenaltyDetailsService: GetPenaltyDetailsService = mock(classOf[GetPenaltyDetailsService])
  val mockPenaltiesFrontendService: PenaltiesFrontendService = mock(classOf[PenaltiesFrontendService])
  val mockGetFinancialDetailsService: GetFinancialDetailsService = mock(classOf[GetFinancialDetailsService])

  class Setup(isFSEnabled: Boolean = true) {
    reset(mockAppConfig)
    reset(mockAppealService)
    reset(mockAuditService)
    reset(mockGetPenaltyDetailsService)
    reset(mockGetFinancialDetailsService)
    reset(mockPenaltiesFrontendService)
    val controller: PenaltiesFrontendController = new PenaltiesFrontendController(
      mockAuditService,
      mockGetPenaltyDetailsService,
      mockGetFinancialDetailsService,
      mockPenaltiesFrontendService,
      dateHelper,
      stubControllerComponents()
    )
  }

  val getPenaltyDetailsFullAPIResponse: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = Some(
      LateSubmissionPenalty(
        summary = LSPSummary(
          activePenaltyPoints = 2,
          inactivePenaltyPoints = 0,
          regimeThreshold = 4,
          penaltyChargeAmount = 200,
          PoCAchievementDate = LocalDate.of(2022, 1, 1)
        ),
        details = Seq()
      )
    ),
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
              principalChargeReference = "12345678",
              penaltyChargeReference = Some("1234567892"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = None,
              penaltyAmountPosted = None,
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(144.21),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
              principalChargeReference = "12345677",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = None,
              penaltyAmountPosted = None,
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(144.21),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "12345676",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(144),
              penaltyAmountPaid = Some(0.21),
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "12345675",
              penaltyChargeReference = Some("1234567889"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyAmountOutstanding = Some(144),
              penaltyAmountPaid = Some(0.21),
              penaltyAmountPosted = Some(144.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )
    ),
    breathingSpace = None
  )

  "use API 1812 data and combine with API 1811 data" must {
    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the 1812 call fails" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the 1812 call response body is malformed" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(controller.getPenaltiesData("123456789", Some(""))(fakeRequest))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1812_API.toString)) shouldBe true
        }
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the 1812 call returns no data" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.NOT_FOUND))))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return NO_CONTENT (${Status.NO_CONTENT}) when the 1812 call returns no data (DATA_NOT_FOUND response)" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsNoContent)))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.NO_CONTENT
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the 1811 call fails" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))
      val result = controller.getPenaltiesData("123456789", Some("123456789"))(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the 1811 call response body is malformed" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(controller.getPenaltiesData("123456789", Some(""))(fakeRequest))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true
        }
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the 1811 call returns no data (if penalty data contains LPPs - edge case)" in new Setup(isFSEnabled = true) {
      val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = None,
        breathingSpace = None
      )
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.NOT_FOUND))))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
    s"return NO_CONTENT (${Status.NO_CONTENT}) when the 1811 call returns no data (DATA_NOT_FOUND response)" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.NO_CONTENT
    }

    s"return OK (${Status.OK}) when the 1811 call returns no data (if penalty data contains no LPPs)" in new Setup(isFSEnabled = true) {
      val penaltyDetails: GetPenaltyDetails = getPenaltyDetailsFullAPIResponse.copy(latePaymentPenalty = None)
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(penaltyDetails)
    }

    "combine the 1812 and 1811 data and return a new GetPenaltyDetails model" in new Setup(isFSEnabled = true) {
      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(getFinancialDetails.DocumentDetails(
          chargeReferenceNumber = None,
          documentOutstandingAmount = Some(0.00),
          lineItemDetails = Some(Seq(getFinancialDetails.LineItemDetails(None))))
        )),
        totalisation = Some(FinancialDetailsTotalisation(
          regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
        ))
      )

      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(financialDetails))))
      when(mockPenaltiesFrontendService.combineAPIData(Matchers.any(), Matchers.any()))
        .thenReturn(getPenaltyDetailsFullAPIResponse)
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
