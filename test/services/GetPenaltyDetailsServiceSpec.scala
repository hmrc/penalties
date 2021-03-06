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

package services

import base.SpecBase
import connectors.getPenaltyDetails.GetPenaltyDetailsConnector
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsNoContent, GetPenaltyDetailsResponse, GetPenaltyDetailsSuccessResponse}
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPDetailsMetadata, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import models.getPenaltyDetails.lateSubmission.{LSPDetails, LSPPenaltyCategoryEnum, LSPPenaltyStatusEnum, LSPSummary, LateSubmission, LateSubmissionPenalty, TaxReturnStatusEnum}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.test.Helpers.{IM_A_TEAPOT, await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class GetPenaltyDetailsServiceSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockGetPenaltyDetailsConnector: GetPenaltyDetailsConnector = mock(classOf[GetPenaltyDetailsConnector])
  class Setup {
    val service = new GetPenaltyDetailsService(mockGetPenaltyDetailsConnector)
    reset(mockGetPenaltyDetailsConnector)
  }

  "getPenaltyDataFromETMPForEnrolment" should {
    val mockGetPenaltyDetailsResponseAsModel: GetPenaltyDetails = GetPenaltyDetails(
      totalisations = Some(
        Totalisations(
          LSPTotalValue = Some(200),
          penalisedPrincipalTotal = Some(2000),
          LPPPostedTotal = Some(165.25),
          LPPEstimatedTotal = Some(15.26),
          LPIPostedTotal = Some(1968.2),
          LPIEstimatedTotal = Some(7)
        )
      ),
      lateSubmissionPenalty = Some(
        LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 10,
            inactivePenaltyPoints = 12,
            regimeThreshold = 10,
            penaltyChargeAmount = 684.25
          ),
          details = Seq(
            LSPDetails(
              penaltyNumber = "12345678901234",
              penaltyOrder = "01",
              penaltyCategory = LSPPenaltyCategoryEnum.Point,
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.of(2022, 10, 30),
              penaltyExpiryDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              FAPIndicator = Some("X"),
              lateSubmissions = Some(
                Seq(
                  LateSubmission(
                    taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                    taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
                    taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
                    returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
                    taxReturnStatus = TaxReturnStatusEnum.Fulfilled
                  )
                )
              ),
              expiryReason = Some("FAP"),
              appealInformation = Some(
                Seq(
                    AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC))
                )
              ),
              chargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              chargeOutstandingAmount = Some(200),
              chargeAmount = Some(200)
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
              penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata()
            )
          )
        )
      ))
    )

    s"call the connector and return a $GetPenaltyDetailsSuccessResponse when the request is successful" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsResponseAsModel))))

      val result: GetPenaltyDetailsResponse = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isRight shouldBe true
      result.right.get shouldBe GetPenaltyDetailsSuccessResponse(mockGetPenaltyDetailsResponseAsModel)
    }

    s"return $GetPenaltyDetailsMalformed when the response body is malformed" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))

      val result: GetPenaltyDetailsResponse = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetPenaltyDetailsMalformed
    }

    s"return $GetPenaltyDetailsNoContent when the response body contains NO_DATA_FOUND" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsNoContent)))

      val result: GetPenaltyDetailsResponse = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetPenaltyDetailsNoContent
    }

    s"return $GetPenaltyDetailsFailureResponse when the connector receives an unmatched status code" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT))))

      val result: GetPenaltyDetailsResponse = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))

      val result: Exception = intercept[Exception](await(service.getDataFromPenaltyServiceForVATCVRN("123456789")))
      result.getMessage shouldBe "Something has gone wrong."
    }
  }
}
