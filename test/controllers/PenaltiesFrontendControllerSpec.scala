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

package controllers

import base.{LPPDetailsBase, LSPDetailsBase, LogCapturing, SpecBase}
import config.featureSwitches.FeatureSwitching
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{
  GetPenaltyDetailsFailureResponse,
  GetPenaltyDetailsMalformed,
  GetPenaltyDetailsNoContent,
  GetPenaltyDetailsSuccessResponse
}
import controllers.auth.AuthAction
import models.getPenaltyDetails.latePayment.PrincipalChargeMainTr.VATReturnCharge
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.PenaltiesFrontendService._
import services.auditing.AuditService
import services.{PenaltiesFrontendService, PenaltyDetailsService}
import utils.PagerDutyHelper.PagerDutyKeys
import utils.{AuthActionMock, DateHelper, Logger}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PenaltiesFrontendControllerSpec extends SpecBase with LogCapturing with LPPDetailsBase with LSPDetailsBase with FeatureSwitching {
  val mockPenaltyDetailsService: PenaltyDetailsService       = mock(classOf[PenaltyDetailsService])
  val mockPenaltiesFrontendService: PenaltiesFrontendService = mock(classOf[PenaltiesFrontendService])
  val mockAuditService: AuditService                         = mock(classOf[AuditService])
  val mockAuthAction: AuthAction                             = injector.instanceOf(classOf[AuthActionMock])
  val dateHelper: DateHelper                                 = injector.instanceOf[DateHelper]

  implicit val config: play.api.Configuration = appConfig.config

  val regime = Regime("VATC")
  val idType = IdType("VRN")
  val id     = Id("123456789")

  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    regime,
    idType,
    id
  )

  class Setup(isFSEnabled: Boolean = true) {
    reset(mockPenaltyDetailsService)
    reset(mockPenaltiesFrontendService)
    reset(mockAuditService)

    implicit val config: play.api.Configuration = appConfig.config
    val controller: PenaltiesFrontendController = new PenaltiesFrontendController(
      mockPenaltyDetailsService,
      mockPenaltiesFrontendService,
      mockAuditService,
      dateHelper,
      stubControllerComponents(),
      mockAuthAction
    )(global, config)
  }

  val penaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = Some(
      Totalisations(
        LSPTotalValue = Some(200),
        penalisedPrincipalTotal = Some(2000),
        LPPPostedTotal = Some(165.25),
        LPPEstimatedTotal = Some(15.26),
        totalAccountOverdue = None,
        totalAccountPostedInterest = None,
        totalAccountAccruingInterest = None
      )
    ),
    lateSubmissionPenalty = Some(
      models.getPenaltyDetails.lateSubmission.LateSubmissionPenalty(
        summary = models.getPenaltyDetails.lateSubmission.LSPSummary(
          activePenaltyPoints = 2,
          inactivePenaltyPoints = 0,
          regimeThreshold = 4,
          penaltyChargeAmount = 200,
          PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
        ),
        details = Seq()
      )),
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              principalChargeReference = "12345675",
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              penaltyAmountAccruing = BigDecimal(0),
              penaltyAmountPosted = 144.21,
              penaltyAmountPaid = Some(0.21),
              penaltyAmountOutstanding = Some(144),
              LPP1LRCalculationAmount = None,
              LPP1LRDays = None,
              LPP1LRPercentage = None,
              LPP1HRCalculationAmount = None,
              LPP1HRDays = None,
              LPP1HRPercentage = None,
              LPP2Days = None,
              LPP2Percentage = None,
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
              communicationsDate = Some(LocalDate.of(2022, 1, 1)),
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              appealInformation = None,
              principalChargeMainTransaction = VATReturnCharge,
              principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
              principalChargeBillingTo = LocalDate.of(2022, 1, 1),
              principalChargeDueDate = LocalDate.of(2022, 1, 1),
              principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1)),
              vatOutstandingAmount = None,
              metadata = LPPDetailsMetadata(
                principalChargeDocNumber = Some("DOC1"),
                principalChargeSubTransaction = Some("SUB1")
              ),
              supplement = false
            )
          )
        ),
        ManualLPPIndicator = Some(true)
      )),
    breathingSpace = None
  )
  val penaltyDetails2: GetPenaltyDetails = penaltyDetails.copy(totalisations = None)

  private def verifyCallMadeToGetPenaltyDetails() =
    verify(mockPenaltyDetailsService, times(1)).getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
  private def verifyCallMadeToPenaltiesFrontendService() = verify(mockPenaltiesFrontendService, times(1)).handleAndCombineGetFinancialDetailsData(
    ArgumentMatchers.any(),
    ArgumentMatchers.any(),
    ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
  private def verifyNoCallMadeToPenaltiesFrontendService() = verify(mockPenaltiesFrontendService, never()).handleAndCombineGetFinancialDetailsData(
    ArgumentMatchers.any(),
    ArgumentMatchers.any(),
    ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

  "getPenaltyDetails" should {

    "attempt to combine data from #5329 get-penalty-details and #5327 get-financial-data calls, and" should {
      "return the combined data from PenaltiesFrontendService as a Json in a 200" when {
        "the PenaltyDetailsService returns data and the PenaltiesFrontendService returns 'CombinedPenaltyDetails' with the combined data" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
          when(
            mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
              ArgumentMatchers.any(),
              ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(CombinedPenaltyDetails(penaltyDetails2))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.OK
          contentAsJson(result) shouldBe Json.toJson(penaltyDetails2)

          verifyCallMadeToGetPenaltyDetails()
          verifyCallMadeToPenaltiesFrontendService()
        }

        "the PenaltyDetailsService returns data and the PenaltiesFrontendService returns 'PenaltyDetailsFromSecondNoContent' with the combined data" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
          when(
            mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
              ArgumentMatchers.any(),
              ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(PenaltyDetailsFromSecondNoContent(penaltyDetails2))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.OK
          contentAsJson(result) shouldBe Json.toJson(penaltyDetails2)

          verifyCallMadeToGetPenaltyDetails()
          verifyCallMadeToPenaltiesFrontendService()
        }
      }

      "return the data returned from PenaltiesFrontendService (only penalties, not financial), as a Json in a 200" when {
        "the PenaltyDetailsService returns data and the PenaltiesFrontendService returns 'PenaltyDetailsFromFirstNoContent' with the penalty data" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
          when(
            mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
              ArgumentMatchers.any(),
              ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(PenaltyDetailsFromFirstNoContent(penaltyDetails2))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.OK
          contentAsJson(result) shouldBe Json.toJson(penaltyDetails2)

          verifyCallMadeToGetPenaltyDetails()
          verifyCallMadeToPenaltiesFrontendService()
        }
      }

      "return a 204 with no data" when {
        "the PenaltyDetailsService returns 'GetPenaltyDetailsNoContent' in a Left, and so the PenaltiesFrontendService is not called" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Left(GetPenaltyDetailsNoContent)))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.NO_CONTENT

          verifyCallMadeToGetPenaltyDetails()
          verifyNoCallMadeToPenaltiesFrontendService()
        }

        "the PenaltyDetailsService returns data in a Right, and the PenaltiesFrontendService returns 'NoPenaltyDetailsFromFirstNoContent' in a Right" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
          when(
            mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
              ArgumentMatchers.any(),
              ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NoPenaltyDetailsFromFirstNoContent)))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.NO_CONTENT

          verifyCallMadeToGetPenaltyDetails()
          verifyCallMadeToPenaltiesFrontendService()
        }

        "the PenaltyDetailsService returns data in a Right, and the PenaltiesFrontendService returns 'FinancialDetailsNoDataFound' in a Left" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
          when(
            mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
              ArgumentMatchers.any(),
              ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Left(FinancialDetailsNoDataFound(vrn123456789, clearedItemsCallSucceeded = false))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.NO_CONTENT

          verifyCallMadeToGetPenaltyDetails()
          verifyCallMadeToPenaltiesFrontendService()
        }
      }

      "return a 404" when {
        "the PenaltyDetailsService returns a NOT_FOUND in a 'GetPenaltyDetailsFailureResponse' in a Left, and so the PenaltiesFrontendService is not called" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(NOT_FOUND))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.NOT_FOUND

          verifyCallMadeToGetPenaltyDetails()
          verifyNoCallMadeToPenaltiesFrontendService()
        }

        "the PenaltyDetailsService returns data in a Right, and the PenaltiesFrontendService returns 'FinancialDetailsNotFound' in a Left" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
          when(
            mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
              ArgumentMatchers.any(),
              ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Left(FinancialDetailsNotFound(vrn123456789, clearedItemsCallSucceeded = false))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.NOT_FOUND

          verifyCallMadeToGetPenaltyDetails()
          verifyCallMadeToPenaltiesFrontendService()
        }
      }

      "return a 500" when {
        "the PenaltyDetailsService returns a 'GetPenaltyDetailsFailureResponse' in a Left with a non-400 status, and so the PenaltiesFrontendService is not called" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(BAD_REQUEST))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR

          verifyCallMadeToGetPenaltyDetails()
          verifyNoCallMadeToPenaltiesFrontendService()
        }

        "the PenaltyDetailsService returns data in a Right, and the PenaltiesFrontendService returns 'FinancialDetailsUnexpectedStatus' in a Left" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
          when(
            mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
              ArgumentMatchers.any(),
              ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Left(FinancialDetailsUnexpectedStatus(PAYMENT_REQUIRED, clearedItemsCallSucceeded = false))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR

          verifyCallMadeToGetPenaltyDetails()
          verifyCallMadeToPenaltiesFrontendService()
        }

        "the PenaltyDetailsService returns data in a Right, and the PenaltiesFrontendService returns 'MissingManualLPPField' in a Left" in new Setup {
          when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
          when(
            mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
              ArgumentMatchers.any(),
              ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Left(MissingManualLPPField(fieldName = "thisIsTheMissingField"))))

          private val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR

          verifyCallMadeToGetPenaltyDetails()
          verifyCallMadeToPenaltiesFrontendService()
        }
      }

      "return a 500 and raise a PagerDuty" when {
        "the PenaltyDetailsService returns a 'GetPenaltyDetailsMalformed' in a Left, and so the PenaltiesFrontendService is not called" should {
          "return a 500 and raise a 'MALFORMED_RESPONSE_FROM_1812_API' PagerDuty" in new Setup {
            when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))

            withCaptureOfLoggingFrom(Logger.logger) { logs =>
              val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1812_API.toString)) shouldBe true

              verifyCallMadeToGetPenaltyDetails()
              verifyNoCallMadeToPenaltiesFrontendService()
            }
          }
        }

        "the PenaltyDetailsService returns data in a Right, and the PenaltiesFrontendService returns 'FinancialDetailsMalformedResponse' in a Left" should {
          "return a 500 and raise a 'MALFORMED_RESPONSE_FROM_1811_API' PagerDuty" in new Setup {
            when(mockPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(penaltyDetails))))
            when(
              mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(Left(FinancialDetailsMalformedResponse(vrn123456789, clearedItemsCallSucceeded = false))))

            withCaptureOfLoggingFrom(Logger.logger) { logs =>
              val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true

              verifyCallMadeToGetPenaltyDetails()
              verifyCallMadeToPenaltiesFrontendService()
            }
          }
        }
      }
    }
  }

}
