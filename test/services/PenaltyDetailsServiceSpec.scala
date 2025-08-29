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
import config.featureSwitches.{CallAPI1812HIP, FeatureSwitching}
import connectors.getPenaltyDetails.{HIPPenaltyDetailsConnector, PenaltyDetailsConnector}
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{
  GetPenaltyDetailsFailureResponse,
  GetPenaltyDetailsMalformed,
  GetPenaltyDetailsNoContent,
  GetPenaltyDetailsResponse,
  GetPenaltyDetailsSuccessResponse
}
import models.getFinancialDetails.MainTransactionEnum
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.breathingSpace.BreathingSpace
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.lateSubmission._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
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

class PenaltyDetailsServiceSpec extends SpecBase with LogCapturing with LPPDetailsBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockGetPenaltyDetailsConnector: PenaltyDetailsConnector = mock(classOf[PenaltyDetailsConnector])
  val mockHIPPenaltyDetailsConnector: HIPPenaltyDetailsConnector = mock(classOf[HIPPenaltyDetailsConnector])
  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    Regime("VATC"), 
    IdType("VAT"),
    Id("123456789")
  )

  class Setup extends FeatureSwitching {
    val mockConfig: Configuration = mock(classOf[Configuration])
    val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
    val filterService: FilterService = injector.instanceOf(classOf[FilterService])
    override implicit val config: Configuration = mockConfig

    sys.props -= TIME_MACHINE_NOW
    sys.props -= ESTIMATED_LPP1_FILTER_END_DATE

    val mockAppConfig: AppConfig = new AppConfig(mockConfig, mockServicesConfig)
    val service = new PenaltyDetailsService(mockGetPenaltyDetailsConnector, mockHIPPenaltyDetailsConnector, filterService)

    reset(mockGetPenaltyDetailsConnector)
    reset(mockHIPPenaltyDetailsConnector)
    reset(mockConfig)
    reset(mockServicesConfig)
  }

  "getPenaltyDetails (unified method)" should {
    val mockGetPenaltyDetailsResponseAsModel: GetPenaltyDetails = GetPenaltyDetails(
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
        LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 10,
            inactivePenaltyPoints = 12,
            regimeThreshold = 10,
            penaltyChargeAmount = 684.25,
            PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
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
              FAPIndicator = Some("X"),
              lateSubmissions = Some(
                Seq(
                  LateSubmission(
                    lateSubmissionID = "001",
                    incomeSource = Some("IT"),
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
        details = Some(
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
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(144.21),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
              vatOutstandingAmount = Some(BigDecimal(123.45))
            )
          )
        ),
        ManualLPPIndicator = None
      )),
      breathingSpace = Some(Seq(
        BreathingSpace(BSStartDate = LocalDate.of(2023, 1, 1), BSEndDate = LocalDate.of(2023, 12, 31))
      ))
    )

    "call the regular connector when CallAPI1812HIP feature switch is disabled" in new Setup {
      disableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsResponseAsModel))))

      setEstimatedLPP1FilterEndDate(Some(LocalDate.of(2022, 10, 28)))

      val result: GetPenaltyDetailsResponse = await(service.getPenaltyDetails(vrn123456789))

      result.isRight shouldBe true
      result.toOption.get shouldBe GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsResponseAsModel)
    }

    "call the HIP connector when CallAPI1812HIP feature switch is enabled" in new Setup {
      enableFeatureSwitch(CallAPI1812HIP)
      
      import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser._

      import java.time.Instant

      val mockHIPPenaltyDetailsResponseAsModel: models.hipPenaltyDetails.PenaltyDetails = models.hipPenaltyDetails.PenaltyDetails(
        processingDate = Instant.now(),
        totalisations = Some(
          models.hipPenaltyDetails.Totalisations(
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
          models.hipPenaltyDetails.lateSubmission.LateSubmissionPenalty(
            summary = models.hipPenaltyDetails.lateSubmission.LSPSummary(
              activePenaltyPoints = 10,
              inactivePenaltyPoints = 12,
              regimeThreshold = 10,
              penaltyChargeAmount = 684.25,
              pocAchievementDate = Some(LocalDate.of(2022, 1, 1))
            ),
            details = Seq(
              models.hipPenaltyDetails.lateSubmission.LSPDetails(
                penaltyNumber = "12345678901234",
                penaltyOrder = Some("01"),
                penaltyCategory = Some(models.hipPenaltyDetails.lateSubmission.LSPPenaltyCategoryEnum.Point),
                penaltyStatus = models.hipPenaltyDetails.lateSubmission.LSPPenaltyStatusEnum.Active,
                penaltyCreationDate = LocalDate.of(2022, 10, 30),
                penaltyExpiryDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                fapIndicator = Some("X"),
                lateSubmissions = Some(
                  Seq(
                    models.hipPenaltyDetails.lateSubmission.LateSubmission(
                      lateSubmissionID = "001",
                      incomeSource = Some("IT"),
                      taxPeriod = Some("23AA"),
                      taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                      taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
                      taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
                      returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
                      taxReturnStatus = Some(models.hipPenaltyDetails.lateSubmission.TaxReturnStatusEnum.Fulfilled)
                    )
                  )
                ),
                expiryReason = Some(models.hipPenaltyDetails.lateSubmission.ExpiryReasonEnum.Adjustment),
                appealInformation = Some(
                  Seq(
                    models.hipPenaltyDetails.appealInfo.AppealInformationType(appealStatus = Some(models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable), appealLevel = Some(models.hipPenaltyDetails.appealInfo.AppealLevelEnum.HMRC), appealDescription = Some("Some value"))
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
        latePaymentPenalty = Some(models.hipPenaltyDetails.latePayment.LatePaymentPenalty(
          lppDetails = Some(
            Seq(
              models.hipPenaltyDetails.latePayment.LPPDetails(
                principalChargeReference = "1234567890",
                penaltyCategory = models.hipPenaltyDetails.latePayment.LPPPenaltyCategoryEnum.FirstPenalty,
                penaltyStatus = Some(models.hipPenaltyDetails.latePayment.LPPPenaltyStatusEnum.Accruing),
                penaltyAmountAccruing = BigDecimal(144.21),
                penaltyAmountPosted = 0,
                penaltyAmountPaid = None,
                penaltyAmountOutstanding = None,
                lpp1LRCalculationAmt = Some(99.99),
                lpp1LRDays = Some("15"),
                lpp1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                lpp1HRCalculationAmt = Some(99.99),
                lpp1HRDays = Some("31"),
                lpp1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                lpp2Days = Some("31"),
                lpp2Percentage = Some(BigDecimal(4.00).setScale(2)),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyChargeReference = Some("123456789"),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                appealInformation = Some(Seq(models.hipPenaltyDetails.appealInfo.AppealInformationType(appealStatus = Some(models.hipPenaltyDetails.appealInfo.AppealStatusEnum.Unappealable), appealLevel = Some(models.hipPenaltyDetails.appealInfo.AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeDocNumber = Some("DOC1"),
                principalChargeMainTr = "4700",
                principalChargeSubTr = Some("SUB1"),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                principalChargeLatestClearing = None,
                timeToPay = None
              )
            )
          ),
          manualLPPIndicator = true
        )),
        breathingSpace = Some(Seq(
          models.hipPenaltyDetails.breathingSpace.BreathingSpace(bsStartDate = LocalDate.of(2023, 1, 1), bsEndDate = LocalDate.of(2023, 12, 31))
        ))
      )

      when(mockHIPPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Right(HIPPenaltyDetailsSuccessResponse(mockHIPPenaltyDetailsResponseAsModel))))

      val result: GetPenaltyDetailsResponse = await(service.getPenaltyDetails(vrn123456789))

      result.isRight shouldBe true
      result.toOption.get.isInstanceOf[GetPenaltyDetailsSuccessResponse] shouldBe true
      
      disableFeatureSwitch(CallAPI1812HIP)
    }

    s"return $GetPenaltyDetailsMalformed when the regular connector response body is malformed" in new Setup {
      disableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))
      
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(service.getPenaltyDetails(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetPenaltyDetailsMalformed
          logs.map(_.getMessage) should contain ("[PenaltyDetailsService][getPenaltyDetails][VATC] - Failed to parse HTTP response into model for VATC~VAT~123456789")
        }
      }
    }

    s"return $GetPenaltyDetailsNoContent when the regular connector response contains NO_DATA_FOUND" in new Setup {
      disableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsNoContent)))
      
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(service.getPenaltyDetails(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetPenaltyDetailsNoContent
          logs.map(_.getMessage) should contain ("[PenaltyDetailsService][getPenaltyDetails][VATC] - Got a 404 response and no data was found for GetPenaltyDetails call")
        }
      }
    }

    s"return $GetPenaltyDetailsFailureResponse when the regular connector receives an unmatched status code" in new Setup {
      disableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT))))
      
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(service.getPenaltyDetails(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)
          logs.map(_.getMessage) should contain ("[PenaltyDetailsService][getPenaltyDetails][VATC] - Unknown status returned from connector for VATC~VAT~123456789")
        }
      }
    }

    "throw an exception when the regular connector fails with an exception" in new Setup {
      disableFeatureSwitch(CallAPI1812HIP)
      
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))

      val result: Exception = intercept[Exception](await(service.getPenaltyDetails(vrn123456789)))
      result.getMessage shouldBe "Something has gone wrong."
    }

    "handle HIP connector failures and convert to regular response format" in new Setup {
      enableFeatureSwitch(CallAPI1812HIP)
      
      import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser._
      
      when(mockHIPPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.successful(Left(HIPPenaltyDetailsMalformed)))
      
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(service.getPenaltyDetails(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetPenaltyDetailsMalformed
          logs.map(_.getMessage) should contain ("[PenaltyDetailsService][getPenaltyDetails][VATC] - Failed to parse HTTP response into HIP model for VATC~VAT~123456789")
        }
      }
      
      disableFeatureSwitch(CallAPI1812HIP)
    }

    "throw an exception when the HIP connector fails with an exception" in new Setup {
      enableFeatureSwitch(CallAPI1812HIP)
      
      when(mockHIPPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(vrn123456789))(any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong with HIP.")))

      val result: Exception = intercept[Exception](await(service.getPenaltyDetails(vrn123456789)))
      result.getMessage shouldBe "Something has gone wrong with HIP."
      
      disableFeatureSwitch(CallAPI1812HIP)
    }
  }
}
