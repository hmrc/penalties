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
import config.featureSwitches.{CallAPI1812HIP, FeatureSwitching}
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{
  GetPenaltyDetailsFailureResponse,
  GetPenaltyDetailsMalformed,
  GetPenaltyDetailsSuccessResponse
}
import controllers.auth.AuthAction
import models.getPenaltyDetails.latePayment.PrincipalChargeMainTr.{VATReturnCharge, VATReturnFirstLPP, VATReturnSecondLPP}
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.test.Helpers._
import services.{PenaltiesFrontendService, PenaltyDetailsService}
import utils.AuthActionMock
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PenaltiesFrontendControllerSpec extends SpecBase with LogCapturing with LPPDetailsBase with LSPDetailsBase with FeatureSwitching {
  val mockGetPenaltyDetailsService: PenaltyDetailsService = mock(classOf[PenaltyDetailsService])
  val mockPenaltiesFrontendService: PenaltiesFrontendService = mock(classOf[PenaltiesFrontendService])
  val mockAuthAction: AuthAction = injector.instanceOf(classOf[AuthActionMock])
  
  implicit val config: play.api.Configuration = appConfig.config

  val regime = Regime("VATC") 
  val idType = IdType("VRN")
  val id = Id("123456789")

  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    regime,
    idType,
    id
  )

  class Setup(isFSEnabled: Boolean = true) {
    reset(mockGetPenaltyDetailsService)
    reset(mockPenaltiesFrontendService)
    
    disableFeatureSwitch(CallAPI1812HIP)
    
    implicit val config: play.api.Configuration = appConfig.config
    val controller: PenaltiesFrontendController = new PenaltiesFrontendController(
      mockGetPenaltyDetailsService,
      mockPenaltiesFrontendService,
      stubControllerComponents(),
      mockAuthAction
    )(global, config)
  }

  "use unified API data and combine with API 1811 data" must {
    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the penalty details call fails" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))
      val result = controller.getPenaltiesData(regime, idType, id, Some(""))(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the penalty details call response body is malformed" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(controller.getPenaltiesData(regime, idType, id, Some(""))(fakeRequest))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1812_API.toString)) shouldBe true
        }
      }
    }

    s"return the service result for the financial details combination" in new Setup(isFSEnabled = true) {
      val getPenaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(
          LatePaymentPenalty(
            Some(
              Seq(
                lpp2,
                lpp1PrincipalChargeDueToday.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted),
                lpp2.copy(principalChargeReference = "123456790"),
                lpp1PrincipalChargeDueToday.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted)
              )
            ),
            ManualLPPIndicator = None
          )
        ),
        breathingSpace = None
      )
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))))
      when(mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(InternalServerError("")))
      val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)
      
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return OK based on the result of the service call" in new Setup(isFSEnabled = true) {
      val getPenaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(
          LatePaymentPenalty(
            Some(
              Seq(
                lpp2,
                lpp1PrincipalChargeDueToday.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted),
                lpp2.copy(principalChargeReference = "123456790"),
                lpp1PrincipalChargeDueToday.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted)
              )
            ),
            ManualLPPIndicator = None
          )
        ),
        breathingSpace = None
      )

      val penaltyDetailsAfterCombining = getPenaltyDetails.copy(
        totalisations = Some(
          Totalisations(
            LSPTotalValue = None,
            penalisedPrincipalTotal = None,
            LPPPostedTotal = None,
            LPPEstimatedTotal = None,
            totalAccountOverdue = Some(1000),
            totalAccountPostedInterest = Some(123.45),
            totalAccountAccruingInterest = Some(23.45)
          )
        ),
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(
          LatePaymentPenalty(
            Some(
              Seq(
                lpp2.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted,
                  penaltyChargeReference = Some("123456790"),
                  metadata = LPPDetailsMetadata(
                    mainTransaction = Some(VATReturnSecondLPP),
                    timeToPay = Some(Seq(TimeToPay(
                      TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                      TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                    )))
                  )
                ),
                lpp1PrincipalChargeDueToday.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted,
                  penaltyChargeReference = Some("123456789"),
                  metadata = LPPDetailsMetadata(
                    mainTransaction = Some(VATReturnFirstLPP),
                    timeToPay = Some(Seq(TimeToPay(
                      TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                      TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                    )))
                  ))
              )
            ),
            ManualLPPIndicator = None
          )
        )
      )
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))))
      when(mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Ok(Json.toJson(penaltyDetailsAfterCombining))))
      val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(penaltyDetailsAfterCombining)
    }
  }

  "unified API with feature switching" must {
    val mockConvertedPenaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
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
      lateSubmissionPenalty = Some(models.getPenaltyDetails.lateSubmission.LateSubmissionPenalty(
        summary = models.getPenaltyDetails.lateSubmission.LSPSummary(
          activePenaltyPoints = 2,
          inactivePenaltyPoints = 0,
          regimeThreshold = 4,
          penaltyChargeAmount = 200,
          PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
        ),
        details = Seq()
      )),
      latePaymentPenalty = Some(LatePaymentPenalty(
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
              )
            )
          )
        ),
        ManualLPPIndicator = Some(true)
      )),
      breathingSpace = None
    )

    s"use unified service and handle HIP backend when CallAPI1812HIP feature switch is enabled" in new Setup {
      enableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(mockConvertedPenaltyDetails))))
      when(mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Ok(Json.toJson(mockConvertedPenaltyDetails))))

      val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)
      status(result) shouldBe Status.OK
      
      verify(mockGetPenaltyDetailsService, times(1)).getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      verify(mockPenaltiesFrontendService, times(1)).handleAndCombineGetFinancialDetailsData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      
      disableFeatureSwitch(CallAPI1812HIP)
    }

    s"use unified service with regular backend when CallAPI1812HIP feature switch is disabled" in new Setup {
      disableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(mockConvertedPenaltyDetails))))
      when(mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Ok(Json.toJson(mockConvertedPenaltyDetails))))

      val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)
      status(result) shouldBe Status.OK
      
      verify(mockGetPenaltyDetailsService, times(1)).getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      verify(mockPenaltiesFrontendService, times(1)).handleAndCombineGetFinancialDetailsData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when unified service call fails" in new Setup {
      enableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))

      val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      
      disableFeatureSwitch(CallAPI1812HIP)
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when unified service returns 404" in new Setup {
      enableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.NOT_FOUND))))

      val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
      contentAsString(result) shouldBe s"A downstream call returned 404 for $vrn123456789"
      
      disableFeatureSwitch(CallAPI1812HIP)
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when unified service returns malformed data" in new Setup {
      enableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsService.getPenaltyDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))

      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1812_API.toString)) shouldBe true
        }
      }
      
      disableFeatureSwitch(CallAPI1812HIP)
    }
  }
}
