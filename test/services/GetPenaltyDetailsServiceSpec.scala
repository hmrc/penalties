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

import base.{LPPDetailsBase, LogCapturing, SpecBase}
import config.AppConfig
import connectors.getPenaltyDetails.GetPenaltyDetailsConnector
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsNoContent, GetPenaltyDetailsResponse, GetPenaltyDetailsSuccessResponse}
import models.getFinancialDetails.MainTransactionEnum
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.breathingSpace.BreathingSpace
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.lateSubmission._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration
import play.api.test.Helpers.{IM_A_TEAPOT, await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.Logger.logger

import java.time.LocalDate
import config.featureSwitches.FeatureSwitching
import play.api.http.Status.INTERNAL_SERVER_ERROR

import scala.concurrent.{ExecutionContext, Future}

class GetPenaltyDetailsServiceSpec extends SpecBase with LogCapturing with LPPDetailsBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockGetPenaltyDetailsConnector: GetPenaltyDetailsConnector = mock(classOf[GetPenaltyDetailsConnector])
  class Setup(withRealConfig: Boolean = true) {
    implicit val mockConfig: Configuration = mock(classOf[Configuration])
    val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
    val filterService: FilterService = injector.instanceOf(classOf[FilterService])

    val featureSwitching: FeatureSwitching = new FeatureSwitching {
      override implicit val config: Configuration = mockConfig
    }

    sys.props -= featureSwitching.TIME_MACHINE_NOW
    sys.props -= featureSwitching.ESTIMATED_LPP1_FILTER_END_DATE

    val mockAppConfig: AppConfig = new AppConfig(mockConfig, mockServicesConfig)
    val service = new GetPenaltyDetailsService(mockGetPenaltyDetailsConnector, filterService)

    reset(mockGetPenaltyDetailsConnector)
    reset(mockConfig)
    reset(mockServicesConfig)
  }

  "getDataFromPenaltyServiceForVATCVRN" should {
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
        )
      )),
      breathingSpace = Some(Seq(
        BreathingSpace(BSStartDate = LocalDate.of(2023, 1, 1), BSEndDate = LocalDate.of(2023, 12, 31))
      ))
    )

    s"call the connector and return a $GetPenaltyDetailsSuccessResponse when the request is successful" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsResponseAsModel))))

      featureSwitching.setEstimatedLPP1FilterEndDate(Some(LocalDate.of(2022, 10, 28)))

      val result: GetPenaltyDetailsResponse = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsResponseAsModel)
    }

    s"return $GetPenaltyDetailsMalformed when the response body is malformed" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
          result.isLeft shouldBe true
          result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetPenaltyDetailsMalformed
          logs.exists(_.getMessage.contains("[GetPenaltyDetailsService][getDataFromPenaltyServiceForVATCVRN] - Failed to parse HTTP response into model for VRN: 123456789")) shouldBe true
        }
      }
    }

    s"return $GetPenaltyDetailsNoContent when the response body contains NO_DATA_FOUND" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsNoContent)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
          result.isLeft shouldBe true
          result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetPenaltyDetailsNoContent
          logs.exists(_.getMessage.contains("[GetPenaltyDetailsService][getDataFromPenaltyServiceForVATCVRN] - Got a 404 response and no data was found for GetPenaltyDetails call")) shouldBe true
        }
      }
    }

    s"return $GetPenaltyDetailsFailureResponse when the connector receives an unmatched status code" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT))))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetPenaltyDetailsResponse = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
          result.isLeft shouldBe true
          result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)
          logs.exists(_.getMessage.contains("[GetPenaltyDetailsService][getDataFromPenaltyServiceForVATCVRN] - Unknown status returned from connector for VRN: 123456789")) shouldBe true
        }
      }
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))

      val result: Exception = intercept[Exception](await(service.getDataFromPenaltyServiceForVATCVRN("123456789")))
      result.getMessage shouldBe "Something has gone wrong."
    }
  }
}
