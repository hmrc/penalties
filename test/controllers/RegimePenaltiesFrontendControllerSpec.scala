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
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsSuccessResponse}
import controllers.auth.AuthAction
import models.getFinancialDetails.MainTransactionEnum.{VATReturnFirstLPP, VATReturnSecondLPP}
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.test.Helpers._
import services.{PenaltyDetailsService, RegimePenaltiesFrontendService}
import utils.AuthActionMock
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}

class RegimePenaltiesFrontendControllerSpec extends SpecBase with LogCapturing with LPPDetailsBase with LSPDetailsBase {
  val mockGetPenaltyDetailsService: PenaltyDetailsService = mock(classOf[PenaltyDetailsService])
  val mockPenaltiesFrontendService: RegimePenaltiesFrontendService = mock(classOf[RegimePenaltiesFrontendService])
  val mockAuthAction: AuthAction = injector.instanceOf(classOf[AuthActionMock])

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
    val controller: RegimePenaltiesFrontendController = new RegimePenaltiesFrontendController(
      mockGetPenaltyDetailsService,
      mockPenaltiesFrontendService,
      stubControllerComponents(),
      mockAuthAction
    )
  }

  "use API 1812 data and combine with API 1811 data" must {
    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the 1812 call fails" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyService(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))
      val result = controller.getPenaltiesData(regime, idType, id, Some(""))(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the 1812 call response body is malformed" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyService(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(controller.getPenaltiesData(regime, idType, id, Some(""))(fakeRequest))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1812_API.toString)) shouldBe true
        }
      }
    }

    s"return the service result for the API 1811 call" in new Setup(isFSEnabled = true) {
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
            )
          )
        ),
        breathingSpace = None
      )
      when(mockGetPenaltyDetailsService.getDataFromPenaltyService(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
            )
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
            )
          )
        )
      )
      when(mockGetPenaltyDetailsService.getDataFromPenaltyService(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))))
      when(mockPenaltiesFrontendService.handleAndCombineGetFinancialDetailsData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Ok(Json.toJson(penaltyDetailsAfterCombining))))
      val result = controller.getPenaltiesData(regime, idType, id, Some("123456789"))(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(penaltyDetailsAfterCombining)
    }
  }
}
