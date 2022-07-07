/*
 * Copyright 2022 HM Revenue & Customs
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

import base.SpecBase
import config.AppConfig
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser.{GetFinancialDetailsFailureResponse, GetFinancialDetailsMalformed, GetFinancialDetailsNoContent, GetFinancialDetailsSuccessResponse}
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsNoContent, GetPenaltyDetailsSuccessResponse}
import models.ETMPPayload
import models.getFinancialDetails._
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.lateSubmission.{LSPSummary, LateSubmissionPenalty}
import models.mainTransaction.MainTransactionEnum
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.auditing.AuditService
import services.{ETMPService, GetFinancialDetailsService, GetPenaltyDetailsService, PenaltiesFrontendService}

import java.time.LocalDate
import scala.concurrent.Future

class PenaltiesFrontendControllerSpec extends SpecBase {
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val mockETMPService: ETMPService = mock(classOf[ETMPService])
  val mockAuditService: AuditService = mock(classOf[AuditService])
  val mockGetPenaltyDetailsService: GetPenaltyDetailsService = mock(classOf[GetPenaltyDetailsService])
  val mockPenaltiesFrontendService: PenaltiesFrontendService = mock(classOf[PenaltiesFrontendService])
  val mockGetFinancialDetailsService: GetFinancialDetailsService = mock(classOf[GetFinancialDetailsService])
  val etmpPayloadWithNoPenalties: ETMPPayload = ETMPPayload(
    pointsTotal = 0, lateSubmissions = 0, adjustmentPointsTotal = 0, fixedPenaltyAmount = 0, penaltyAmountsTotal = 0, otherPenalties = None, vatOverview = None, penaltyPointsThreshold = 4, penaltyPoints = Seq.empty, latePaymentPenalties = None
  )

  class Setup(isFSEnabled: Boolean = true) {
    reset(mockAppConfig, mockETMPService, mockAuditService, mockGetPenaltyDetailsService, mockGetFinancialDetailsService, mockPenaltiesFrontendService)
    val controller: PenaltiesFrontendController = new PenaltiesFrontendController(
      mockETMPService,
      mockAuditService,
      mockGetPenaltyDetailsService,
      mockGetFinancialDetailsService,
      mockPenaltiesFrontendService,
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
          penaltyChargeAmount = 200
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
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(100),
              penaltyAmountPaid = Some(44.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata()
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
              principalChargeReference = "12345677",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(23.45),
              penaltyAmountPaid = Some(100),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata()
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "12345676",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(144),
              penaltyAmountPaid = Some(0.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "12345675",
              penaltyChargeReference = Some("1234567889"),
              penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = None,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              communicationsDate = LocalDate.of(2022, 1, 1),
              penaltyAmountOutstanding = Some(144),
              penaltyAmountPaid = Some(0.21),
              LPP1LRDays = None,
              LPP1HRDays = None,
              LPP2Days = None,
              LPP1HRCalculationAmount = None,
              LPP1LRCalculationAmount = None,
              LPP2Percentage = None,
              LPP1LRPercentage = None,
              LPP1HRPercentage = None,
              penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              metadata = LPPDetailsMetadata()
            )
          )
        )
      )
    )
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
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
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
      when(mockGetFinancialDetailsService.getDataFromFinancialServiceForVATVCN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))
      val result = controller.getPenaltiesData("123456789", Some("123456789"))(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the 1811 call response body is malformed" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
      when(mockGetFinancialDetailsService.getDataFromFinancialServiceForVATVCN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsMalformed)))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the 1811 call returns no data (if penalty data contains LPPs - edge case)" in new Setup(isFSEnabled = true) {
      val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = None
      )
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
      when(mockGetFinancialDetailsService.getDataFromFinancialServiceForVATVCN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.NOT_FOUND))))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
    s"return NO_CONTENT (${Status.NO_CONTENT}) when the 1811 call returns no data (DATA_NOT_FOUND response)" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
      when(mockGetFinancialDetailsService.getDataFromFinancialServiceForVATVCN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.NO_CONTENT
    }

    s"return OK (${Status.OK}) when the 1811 call returns no data (if penalty data contains no LPPs)" in new Setup(isFSEnabled = true) {
      val penaltyDetails: GetPenaltyDetails = getPenaltyDetailsFullAPIResponse.copy(latePaymentPenalty = None)
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
      when(mockGetFinancialDetailsService.getDataFromFinancialServiceForVATVCN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(penaltyDetails)
    }

    "combine the 1812 and 1811 data and return a new GetPenaltyDetails model" in new Setup(isFSEnabled = true) {
      val financialDetails = GetFinancialDetails(
        documentDetails = Seq.empty,
        financialDetails = Seq(
          FinancialDetails(
            documentId = "DOC1234",
            taxPeriodFrom = Some(LocalDate.of(2022, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2022, 3, 31)),
            items = Seq(
              FinancialItem(
                dueDate = Some(LocalDate.of(2018, 8, 13)),
                clearingDate = Some(LocalDate.of(2018, 8, 13)),
                metadata = FinancialItemMetadata(
                  subItem = Some("001"),
                  amount = Some(10000),
                  clearingReason = Some("01"),
                  outgoingPaymentMethod = Some("outgoing payment"),
                  paymentLock = Some("paymentLock"),
                  clearingLock = Some("clearingLock"),
                  interestLock = Some("interestLock"),
                  dunningLock = Some("dunningLock"),
                  returnFlag = Some(true),
                  paymentReference = Some("Ab12453535"),
                  paymentAmount = Some(10000),
                  paymentMethod = Some("Payment"),
                  paymentLot = Some("081203010024"),
                  paymentLotItem = Some("000001"),
                  clearingSAPDocument = Some("3350000253"),
                  codingInitiationDate = Some(LocalDate.of(2021, 1, 11)),
                  statisticalDocument = Some("S"),
                  DDCollectionInProgress = Some(true),
                  returnReason = Some("ABCA"),
                  promisetoPay = Some("Y")
                )
              )
            ),
            originalAmount = Some(123.45),
            outstandingAmount = Some(123.45),
            mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
            chargeReference = Some("1"),
            metadata = FinancialDetailsMetadata(
              taxYear = "2022",
              chargeType = Some("1234"),
              mainType = Some("1234"),
              periodKey = Some("123"),
              periodKeyDescription = Some("foobar"),
              businessPartner = Some("123"),
              contractAccountCategory = Some("1"),
              contractAccount = Some("1"),
              contractObjectType = Some("1"),
              contractObject = Some("1"),
              sapDocumentNumber = Some("1"),
              sapDocumentNumberItem = Some("1"),
              subTransaction = Some("1"),
              clearedAmount = Some(123.45),
              accruedInterest = Some(123.45)
            )
          )
        )
      )
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
      when(mockGetFinancialDetailsService.getDataFromFinancialServiceForVATVCN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(financialDetails))))
      when(mockPenaltiesFrontendService.combineAPIData(Matchers.any(), Matchers.any()))
        .thenReturn(getPenaltyDetailsFullAPIResponse)
      val result = controller.getPenaltiesData("123456789", Some(""))(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
