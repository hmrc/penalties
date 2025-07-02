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

import base.{LPPDetailsBase, LogCapturing, SpecBase}
import config.AppConfig
import config.featureSwitches.FeatureSwitching
import connectors.getPenaltyDetails.PenaltyDetailsConnector
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{PenaltyDetailsFailureResponse, PenaltyDetailsMalformed, PenaltyDetailsNoContent, PenaltyDetailsResponse, PenaltyDetailsSuccessResponse}
import models.getFinancialDetails.MainTransactionEnum
import models.penaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.penaltyDetails.breathingSpace.BreathingSpace
import models.penaltyDetails.latePayment._
import models.penaltyDetails.lateSubmission._
import models.penaltyDetails.{PenaltyDetails, Totalisations}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any

import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{IM_A_TEAPOT, await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.Logger.logger

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import java.time.Instant

class PenaltyDetailsServiceSpec extends SpecBase with LogCapturing with LPPDetailsBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockPenaltyDetailsConnector: PenaltyDetailsConnector = mock(classOf[PenaltyDetailsConnector])
  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    Regime("VATC"), 
    IdType("VAT"),
    Id("123456789")
  )
  val instant = Instant.now()

  class Setup(withRealConfig: Boolean = true) {
    implicit val mockConfig: Configuration = mock(classOf[Configuration])
    val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
    val filterService: RegimeFilterService = injector.instanceOf(classOf[RegimeFilterService])

    val featureSwitching: FeatureSwitching = new FeatureSwitching {
      override implicit val config: Configuration = mockConfig
    }

    sys.props -= featureSwitching.TIME_MACHINE_NOW
    sys.props -= featureSwitching.ESTIMATED_LPP1_FILTER_END_DATE

    val mockAppConfig: AppConfig = new AppConfig(mockConfig, mockServicesConfig)
    val service = new PenaltyDetailsService(mockPenaltyDetailsConnector, filterService)

    reset(mockPenaltyDetailsConnector)
    reset(mockConfig)
    reset(mockServicesConfig)
  }

  "getDataFromPenaltyServiceForVATCVRN" should {
    val mockPenaltyDetailsResponseAsModel: PenaltyDetails = PenaltyDetails(
      processingDate = instant,
      totalisations = Some(
        Totalisations(
          lspTotalValue = Some(200),
          penalisedPrincipalTotal = Some(2000),
          lppPostedTotal = Some(165.25),
          lppEstimatedTotal = Some(15.26),
          totalAccountOverdue = None,
          totalAccountPostedInterest = None,
          totalAccountAccruingInterest = None
        )
      ),
      lateSubmissionPenalty = Some(
        LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 10,
            inactivePenaltyPoints = 12,
            regimeThreshold = 10,
            penaltyChargeAmount = 684.25,
            pocAchievementDate = Some(LocalDate.of(2022, 1, 1))
          ),
          details = Seq(
            LSPDetails(
              penaltyNumber = "12345678901234",
              penaltyOrder = Some("01"),
              penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.of(2022, 10, 30),
              penaltyExpiryDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              fapIndicator = Some("X"),
              lateSubmissions = Some(
                Seq(
                  LateSubmission(
                    lateSubmissionID = "001",
                    incomeSource = None,
                    taxPeriod = Some("23AA"),
                    taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                    taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
                    taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
                    returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
                    taxReturnStatus = Some(TaxReturnStatusEnum.Fulfilled)
                  )
                )
              ),
              expiryReason = Some(ExpiryReasonEnum.Adjustment),
              appealInformation = Some(
                Seq(
                  AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value"))
                )
              ),
              chargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              chargeOutstandingAmount = Some(200),
              chargeAmount = Some(200),
              triggeringProcess = None,
              chargeReference = None
            )
          )
        )
      ),
      latePaymentPenalty = Some(LatePaymentPenalty(
        manualLPPIndicator = false,
        lppDetails = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("123456789"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = None,
              penaltyAmountPosted = 0,
              lpp1LRDays = Some("15"),
              lpp1HRDays = Some("31"),
              lpp2Days = Some("31"),
              lpp1HRCalculationAmt = Some(99.99),
              lpp1LRCalculationAmt = Some(99.99),
              lpp2Percentage = Some(BigDecimal(4.00).setScale(2)),
              lpp1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              lpp1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeDocNumber = None,
              principalChargeLatestClearing = None,
              principalChargeSubTr = None,
              penaltyAmountAccruing = BigDecimal(144.21),
              principalChargeMainTr = MainTransactionEnum.VATReturnCharge,
              vatOutstandingAmount = Some(BigDecimal(123.45))
            )
          )
        )
      )),
      breathingSpace = Some(Seq(
        BreathingSpace(bsStartDate = LocalDate.of(2023, 1, 1), bsEndDate = LocalDate.of(2023, 12, 31))
      ))
    )

    s"call the connector and return a $PenaltyDetailsSuccessResponse when the request is successful" in new Setup {
      when(mockPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Right(PenaltyDetailsSuccessResponse(mockPenaltyDetailsResponseAsModel))))

      featureSwitching.setEstimatedLPP1FilterEndDate(Some(LocalDate.of(2022, 10, 28)))

      val result: PenaltyDetailsResponse = await(service.getDataFromPenaltyService(vrn123456789))

      result.isRight shouldBe true
      result.toOption.get shouldBe PenaltyDetailsSuccessResponse(mockPenaltyDetailsResponseAsModel)
    }

    s"return $PenaltyDetailsMalformed when the response body is malformed" in new Setup {
      when(mockPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Left(PenaltyDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: PenaltyDetailsResponse = await(service.getDataFromPenaltyService(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(PenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe PenaltyDetailsMalformed
          logs.map(_.getMessage) should contain ("[PenaltyDetailsService][getDataFromPenaltyService][VATC] - Failed to parse HTTP response into model for VATC~VAT~123456789")
        }
      }
    }

    s"return $PenaltyDetailsNoContent when the response body contains NO_DATA_FOUND" in new Setup {
      when(mockPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Left(PenaltyDetailsNoContent)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: PenaltyDetailsResponse = await(service.getDataFromPenaltyService(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(PenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe PenaltyDetailsNoContent
          logs.map(_.getMessage) should contain ("[PenaltyDetailsService][getDataFromPenaltyService][VATC] - Got a 404 response and no data was found for PenaltyDetails call")
        }
      }
    }

    s"return $PenaltyDetailsFailureResponse when the connector receives an unmatched status code" in new Setup {
      when(mockPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Left(PenaltyDetailsFailureResponse(IM_A_TEAPOT))))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: PenaltyDetailsResponse = await(service.getDataFromPenaltyService(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(PenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe PenaltyDetailsFailureResponse(IM_A_TEAPOT)
          logs.map(_.getMessage) should contain ("[PenaltyDetailsService][getDataFromPenaltyService][VATC] - Unknown status returned from connector for VATC~VAT~123456789")
        }
      }
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(mockPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))

      val result: Exception = intercept[Exception](await(service.getDataFromPenaltyService(vrn123456789)))
      result.getMessage shouldBe "Something has gone wrong."
    }
  }
}
