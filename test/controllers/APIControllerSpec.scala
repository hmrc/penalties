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

import java.time.LocalDate

import base.{LogCapturing, SpecBase}
import config.featureSwitches.FeatureSwitching
import connectors.getFinancialDetails.GetFinancialDetailsConnector
import connectors.getPenaltyDetails.GetPenaltyDetailsConnector
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser.{GetFinancialDetailsFailureResponse, GetFinancialDetailsMalformed, GetFinancialDetailsNoContent, GetFinancialDetailsSuccessResponse}
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser._
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, LineItemDetails, MainTransactionEnum}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.lateSubmission.{LSPSummary, LateSubmissionPenalty}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import services.auditing.AuditService
import services._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.DateHelper
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIControllerSpec extends SpecBase with FeatureSwitching with LogCapturing {
  val mockAppealsService: AppealService = mock(classOf[AppealService])
  val mockAuditService: AuditService = mock(classOf[AuditService])
  val dateHelper: DateHelper = injector.instanceOf(classOf[DateHelper])
  val mockAPIService: APIService = mock(classOf[APIService])
  val mockGetPenaltyDetailsService: GetPenaltyDetailsService = mock(classOf[GetPenaltyDetailsService])
  val mockGetFinancialDetailsService: GetFinancialDetailsService = mock(classOf[GetFinancialDetailsService])
  val mockGetFinancialDetailsConnector: GetFinancialDetailsConnector = mock(classOf[GetFinancialDetailsConnector])
  val mockGetPenaltyDetailsConnector: GetPenaltyDetailsConnector = mock(classOf[GetPenaltyDetailsConnector])
  val controllerComponents: ControllerComponents = injector.instanceOf(classOf[ControllerComponents])
  implicit val config: Configuration = appConfig.config
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val filterService: FilterService = injector.instanceOf(classOf[FilterService])

  class Setup(isFSEnabled: Boolean = false) {
    reset(mockAppealsService)
    reset(mockAuditService)
    reset(mockAPIService)
    reset(mockGetPenaltyDetailsConnector)
    reset(mockGetFinancialDetailsConnector)
    val controller = new APIController(
      mockAuditService,
      mockAPIService,
      mockGetPenaltyDetailsService,
      mockGetFinancialDetailsService,
      mockGetFinancialDetailsConnector,
      mockGetPenaltyDetailsConnector,
      dateHelper,
      controllerComponents,
      filterService
    )
  }

  "getSummaryDataForVRN" should {
    val getPenaltyDetailsNoEstimatedLPPs: GetPenaltyDetails = GetPenaltyDetails(
      totalisations = None,
      lateSubmissionPenalty = Some(
        LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 4,
            inactivePenaltyPoints = 0,
            regimeThreshold = 5,
            penaltyChargeAmount = 200,
            PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
          ),
          details = Seq() //omitted
        )
      ),
      latePaymentPenalty = None,
      breathingSpace = None
    )

    val getPenaltyDetailsEmptyBody: GetPenaltyDetails = GetPenaltyDetails(
      totalisations = None,
      lateSubmissionPenalty = None,
      latePaymentPenalty = None,
      breathingSpace = None
    )

    val getPenaltyDetailsFullAPIResponse: GetPenaltyDetails = GetPenaltyDetails(
      totalisations = None,
      lateSubmissionPenalty = Some(
        LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 2,
            inactivePenaltyPoints = 0,
            regimeThreshold = 4,
            penaltyChargeAmount = 200,
            PoCAchievementDate = Some(LocalDate.of(2022, 1, 1))
          ),
          details = Seq() //omitted
        )
      ),
      latePaymentPenalty = Some(
        LatePaymentPenalty(
          Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "12345678",
                penaltyChargeReference = Some("1234567893"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = None,
                principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                principalChargeDueDate = LocalDate.of(2022, 1, 1),
                communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 0,
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
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(100.00),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "12345677",
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
                penaltyAmountPosted = 0,
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
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(100.00),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "12345676",
                penaltyChargeReference = Some("1234567891"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = None,
                principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                principalChargeBillingTo = LocalDate.of(2022, 1, 1),
                principalChargeDueDate = LocalDate.of(2022, 1, 1),
                communicationsDate = Some(LocalDate.of(2022, 1, 1)),
                penaltyAmountOutstanding = Some(144),
                penaltyAmountPaid = Some(0.21),
                penaltyAmountPosted = 144.21,
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
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(0),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "12345675",
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
                penaltyAmountPosted = 144.21,
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
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(0),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              )
            )
          )
        )
      ),
      breathingSpace = None
    )

    val latePaymentPenaltyDetails = getPenaltyDetailsFullAPIResponse.latePaymentPenalty.get.details

    val penaltyDetailsWithManualLPP = getPenaltyDetailsFullAPIResponse.latePaymentPenalty.get.copy(details = latePaymentPenaltyDetails, ManualLPPIndicator = Some(true))

    val getPenaltyDetailsWithManualLPP = getPenaltyDetailsFullAPIResponse.copy(latePaymentPenalty = Some(penaltyDetailsWithManualLPP))

    val financialDetailsWithManualLPP = FinancialDetails(
      None,
      documentDetails = Some(Seq(DocumentDetails(
        chargeReferenceNumber = Some("xyz1234"),
        documentOutstandingAmount = Some(100.00),
        documentTotalAmount = Some(200.00),
        lineItemDetails = Some(Seq(LineItemDetails(
          mainTransaction = Some(MainTransactionEnum.ManualLPP)
        ))),
        issueDate = Some(LocalDate.of(2023,1,1))
      )))
    )

    val financialDetailsWithoutManualLPP = FinancialDetails(
      None,
      documentDetails = Some(Seq(DocumentDetails(
        chargeReferenceNumber = Some("xyz1234"),
        documentOutstandingAmount = Some(100.00),
        documentTotalAmount = Some(200.00),
        lineItemDetails = Some(Seq(LineItemDetails(
          mainTransaction = None
        ))),
        issueDate = Some(LocalDate.of(2023,1,1))
      )))
    )

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call fails" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns not found" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.NOT_FOUND))))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return NO_CONTENT (${Status.NO_CONTENT}) when the call returns invalid ID" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.UNPROCESSABLE_ENTITY))))
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(false)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(0)
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(0))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(0)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(0))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.NO_CONTENT
      verify(mockAuditService, times(0)).audit(any())(any(), any(), any())
    }

    s"return NO_CONTENT (${Status.NO_CONTENT}) when the call returns an empty body" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsEmptyBody))))
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(false)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(0)
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(0))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(0)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(0))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(controller.getSummaryDataForVRN("123456789")(fakeRequest))
          result.header.status shouldBe Status.NO_CONTENT
          logs.exists(_.getMessage == "[APIController][returnResponseForAPI] - User had no penalty data, returning 204 to caller") shouldBe true
        }
      }

      verify(mockAuditService, times(0)).audit(any())(any(), any(), any())
    }

    s"return NO_CONTENT (${Status.NO_CONTENT}) when the VRN is found but has no data" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsNoContent)))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.NO_CONTENT
    }

    s"return BAD_REQUEST (${Status.BAD_REQUEST}) when the user supplies an invalid VRN" in new Setup(isFSEnabled = true) {
      val result = controller.getSummaryDataForVRN("1234567891234567890")(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) shouldBe "VRN: 1234567891234567890 was not in a valid format."
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call returns malformed data" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1812_API.toString)) shouldBe true
        }
      }
    }

    s"return OK (${Status.OK}) when the call returns some data and can be parsed to the correct response" in new Setup(isFSEnabled = true) {
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(2)
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(123.45))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(2)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(288))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          |{
          |  "noOfPoints": 2,
          |  "noOfEstimatedPenalties": 2,
          |  "noOfCrystalisedPenalties": 2,
          |  "estimatedPenaltyAmount": 123.45,
          |  "crystalisedPenaltyAmountDue": 288,
          |  "hasAnyPenaltyData": true
          |}
          |""".stripMargin
      )
      verify(mockAuditService, times(1)).audit(any())(any(), any(), any())
    }

    s"return OK (${Status.OK}) when there are no estimated LPPs in penalty details" in new Setup(isFSEnabled = true) {
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(0)
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsNoEstimatedLPPs))))
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(0))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(0)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(0))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          |{
          |  "noOfPoints": 4,
          |  "noOfEstimatedPenalties": 0,
          |  "noOfCrystalisedPenalties": 0,
          |  "estimatedPenaltyAmount": 0,
          |  "crystalisedPenaltyAmountDue": 0,
          |  "hasAnyPenaltyData": true
          |}
          |""".stripMargin
      )
    }

    s"return OK (${Status.OK}) when ManualLPPIndicator is true and there is a Manual LPP in the 1811 details" in new Setup(isFSEnabled = true) {
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(2)
      when(mockGetFinancialDetailsService.getFinancialDetails(any(), any())(any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(financialDetailsWithManualLPP))))
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsWithManualLPP))))
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(123.45))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(3)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(388))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          |{
          |  "noOfPoints": 2,
          |  "noOfEstimatedPenalties": 2,
          |  "noOfCrystalisedPenalties": 3,
          |  "estimatedPenaltyAmount": 123.45,
          |  "crystalisedPenaltyAmountDue": 388,
          |  "hasAnyPenaltyData": true
          |}
          |""".stripMargin
      )
    }

    s"return OK (${Status.OK}) when ManualLPPIndicator is true and but there is no Manual LPP in the 1811 details" in new Setup(isFSEnabled = true) {
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(2)
      when(mockGetFinancialDetailsService.getFinancialDetails(any(), any())(any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(financialDetailsWithoutManualLPP))))
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsWithManualLPP))))
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(123.45))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(2)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(288))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          |{
          |  "noOfPoints": 2,
          |  "noOfEstimatedPenalties": 2,
          |  "noOfCrystalisedPenalties": 2,
          |  "estimatedPenaltyAmount": 123.45,
          |  "crystalisedPenaltyAmountDue": 288,
          |  "hasAnyPenaltyData": true
          |}
          |""".stripMargin
      )
    }

    s"return OK (${Status.OK}) when ManualLPPIndicator is true but the 1811 returns a failure response" in new Setup(isFSEnabled = true) {
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(2)
      when(mockGetFinancialDetailsService.getFinancialDetails(any(), any())(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.UNPROCESSABLE_ENTITY))))
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsWithManualLPP))))
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(123.45))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(2)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(288))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          |{
          |  "noOfPoints": 2,
          |  "noOfEstimatedPenalties": 2,
          |  "noOfCrystalisedPenalties": 2,
          |  "estimatedPenaltyAmount": 123.45,
          |  "crystalisedPenaltyAmountDue": 288,
          |  "hasAnyPenaltyData": true
          |}
          |""".stripMargin
      )
    }

    s"return OK (${Status.OK}) when ManualLPPIndicator is true but the 1811 returns No Content" in new Setup(isFSEnabled = true) {
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(2)
      when(mockGetFinancialDetailsService.getFinancialDetails(any(), any())(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsWithManualLPP))))
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(123.45))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(2)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(288))
      val result: Future[Result] = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          |{
          |  "noOfPoints": 2,
          |  "noOfEstimatedPenalties": 2,
          |  "noOfCrystalisedPenalties": 2,
          |  "estimatedPenaltyAmount": 123.45,
          |  "crystalisedPenaltyAmountDue": 288,
          |  "hasAnyPenaltyData": true
          |}
          |""".stripMargin
      )
    }

    s"return OK (${Status.OK}) when ManualLPPIndicator is true but the 1811 returns a Malformed Response, which is logged out" in new Setup(isFSEnabled = true) {
      when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
      when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(2)
      when(mockGetFinancialDetailsService.getFinancialDetails(any(), any())(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsMalformed)))
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsWithManualLPP))))
      when(mockAPIService.findEstimatedPenaltiesAmount(any()))
        .thenReturn(BigDecimal(123.45))
      when(mockAPIService.getNumberOfCrystallisedPenalties(any(), any())).thenReturn(2)
      when(mockAPIService.getCrystallisedPenaltyTotal(any(), any())).thenReturn(BigDecimal(288))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          |{
          |  "noOfPoints": 2,
          |  "noOfEstimatedPenalties": 2,
          |  "noOfCrystalisedPenalties": 2,
          |  "estimatedPenaltyAmount": 123.45,
          |  "crystalisedPenaltyAmountDue": 288,
          |  "hasAnyPenaltyData": true
          |}
          |""".stripMargin
      )
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
          status(result) shouldBe Status.OK
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true
        }
      }
    }
  }

  "getFinancialDetails" should {
    s"return OK (${Status.OK}) when a JSON payload is received from EIS (auditing the response)" in new Setup(isFSEnabled = true) {
      val sampleAPI1811Response = Json.parse(
        """
          {
          |  "totalisation": {
          |    "regimeTotalisation": {
          |      "totalAccountOverdue": "1000.0,",
          |      "totalAccountNotYetDue": "250.0,",
          |      "totalAccountCredit": "40.0,",
          |      "totalAccountBalance": 1210
          |    },
          |    "targetedSearch_SelectionCriteriaTotalisation": {
          |      "totalOverdue": "100.0,",
          |      "totalNotYetDue": "0.0,",
          |      "totalBalance": "100.0,",
          |      "totalCredit": "10.0,",
          |      "totalCleared": 50
          |    },
          |    "additionalReceivableTotalisations": {
          |      "totalAccountPostedInterest": "-99999999999.99,",
          |      "totalAccountAccruingInterest": -99999999999.99
          |    }
          |  },
          |  "documentDetails": [
          |    {
          |      "documentNumber": "187346702498,",
          |      "documentType": "TRM New Charge,",
          |      "chargeReferenceNumber": "XP001286394838,",
          |      "businessPartnerNumber": "100893731,",
          |      "contractAccountNumber": "900726630,",
          |      "contractAccountCategory": "VAT,",
          |      "contractObjectNumber": "104920928302302,",
          |      "contractObjectType": "ZVAT,",
          |      "postingDate": "2022-01-01,",
          |      "issueDate": "2022-01-01,",
          |      "documentTotalAmount": "100.0,",
          |      "documentClearedAmount": "100.0,",
          |      "documentOutstandingAmount": "0.0,",
          |      "documentLockDetails": {
          |        "lockType": "Payment,",
          |        "lockStartDate": "2022-01-01,",
          |        "lockEndDate": "2022-01-01"
          |      },
          |      "documentInterestTotals": {
          |        "interestPostedAmount": "13.12,",
          |        "interestPostedChargeRef": "XB001286323438,",
          |        "interestAccruingAmount": 12.1
          |      },
          |      "documentPenaltyTotals": [
          |        {
          |          "penaltyType": "LPP1,",
          |          "penaltyStatus": "POSTED,",
          |          "penaltyAmount": "10.01,",
          |          "postedChargeReference": "XR00123933492"
          |        }
          |      ],
          |      "lineItemDetails": [
          |        {
          |          "itemNumber": "0001,",
          |          "subItemNumber": "003,",
          |          "mainTransaction": "4576,",
          |          "subTransaction": "1000,",
          |          "chargeDescription": "VAT Return,",
          |          "periodFromDate": "2022-01-01,",
          |          "periodToDate": "2022-01-31,",
          |          "periodKey": "22A1,",
          |          "netDueDate": "2022-02-08,",
          |          "formBundleNumber": "125435934761,",
          |          "statisticalKey": "1,",
          |          "amount": "3420.0,",
          |          "clearingDate": "2022-02-09,",
          |          "clearingReason": "Payment at External Payment Collector Reported,",
          |          "clearingDocument": "719283701921,",
          |          "outgoingPaymentMethod": "B,",
          |          "ddCollectionInProgress": "true,",
          |          "lineItemLockDetails": [
          |            {
          |              "lockType": "Payment,",
          |              "lockStartDate": "2022-01-01,",
          |              "lockEndDate": "2022-01-01"
          |            }
          |          ],
          |          "lineItemInterestDetails": {
          |            "interestKey": "String,",
          |            "currentInterestRate": "-999.999999,",
          |            "interestStartDate": "1920-02-29,",
          |            "interestPostedAmount": "-99999999999.99,",
          |            "interestAccruingAmount": -99999999999.99
          |          }
          |        }
          |      ]
          |    }
          |  ]
          |}""".stripMargin)


      when(mockGetFinancialDetailsConnector.getFinancialDetailsForAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, sampleAPI1811Response.toString)))
      val result = controller.getFinancialDetails(vrn = "123456789",
        searchType = Some("CHGREF"),
        searchItem = Some("XC00178236592"),
        dateType = Some("BILLING"),
        dateFrom = Some("2020-10-03"),
        dateTo = Some("2021-07-12"),
        includeClearedItems = Some(false),
        includeStatisticalItems = Some(true),
        includePaymentOnAccount = Some(true),
        addRegimeTotalisation = Some(false),
        addLockInformation = Some(true),
        addPenaltyDetails = Some(true),
        addPostedInterestDetails = Some(true),
        addAccruingInterestDetails = Some(true))(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe sampleAPI1811Response
      verify(mockAuditService, times(1)).audit(any())(any(), any(), any())
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data (auditing the response)" in new Setup(true) {
      when(mockGetFinancialDetailsConnector.getFinancialDetailsForAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, "NOT_FOUND")))

      val result = controller.getFinancialDetails(vrn = "123456789",
        searchType = Some("CHGREF"),
        searchItem = Some("XC00178236592"),
        dateType = Some("BILLING"),
        dateFrom = Some("2020-10-03"),
        dateTo = Some("2021-07-12"),
        includeClearedItems = Some(false),
        includeStatisticalItems = Some(true),
        includePaymentOnAccount = Some(true),
        addRegimeTotalisation = Some(false),
        addLockInformation = Some(true),
        addPenaltyDetails = Some(true),
        addPostedInterestDetails = Some(true),
        addAccruingInterestDetails = Some(true))(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
      verify(mockAuditService, times(1)).audit(any())(any(), any(), any())
    }

    s"return the status from EIS when the call returns a non 200 or 404 status (auditing the response)" in new Setup(true) {
      when(mockGetFinancialDetailsConnector.getFinancialDetailsForAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")))

      val result = controller.getFinancialDetails(vrn = "123456789",
        searchType = Some("CHGREF"),
        searchItem = Some("XC00178236592"),
        dateType = Some("BILLING"),
        dateFrom = Some("2020-10-03"),
        dateTo = Some("2021-07-12"),
        includeClearedItems = Some(false),
        includeStatisticalItems = Some(true),
        includePaymentOnAccount = Some(true),
        addRegimeTotalisation = Some(false),
        addLockInformation = Some(true),
        addPenaltyDetails = Some(true),
        addPostedInterestDetails = Some(true),
        addAccruingInterestDetails = Some(true))(fakeRequest)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockAuditService, times(1)).audit(any())(any(), any(), any())
    }
  }

  "getPenaltyDetails" should {
    s"return OK (${Status.OK}) when a JSON payload is received from EIS (auditing the response)" in new Setup(isFSEnabled = true) {
      val sampleAPI1812Response = Json.parse(
        """
          |{
          | "totalisations": {
          |   "LSPTotalValue": 200,
          |   "penalisedPrincipalTotal": 2000,
          |   "LPPPostedTotal": 165.25,
          |   "LPPEstimatedTotal": 15.26
          | },
          | "lateSubmissionPenalty": {
          |   "summary": {
          |     "activePenaltyPoints": 10,
          |     "inactivePenaltyPoints": 12,
          |     "regimeThreshold": 10,
          |     "penaltyChargeAmount": 684.25,
          |     "PoCAchievementDate": "2022-10-30"
          |   },
          |   "details": [
          |     {
          |       "penaltyNumber": "12345678901234",
          |       "penaltyOrder": "01",
          |       "penaltyCategory": "P",
          |       "penaltyStatus": "ACTIVE",
          |       "penaltyCreationDate": "2022-10-30",
          |       "penaltyExpiryDate": "2022-10-30",
          |       "communicationsDate": "2022-10-30",
          |       "FAPIndicator": "X",
          |       "lateSubmissions": [
          |         {
          |           "lateSubmissionID": "001",
          |           "taxPeriod":  "23AA",
          |           "taxPeriodStartDate": "2022-01-01",
          |           "taxPeriodEndDate": "2022-12-31",
          |           "taxPeriodDueDate": "2023-02-07",
          |           "returnReceiptDate": "2023-02-01",
          |           "taxReturnStatus": "Fulfilled"
          |         }
          |       ],
          |       "expiryReason": "FAP",
          |       "appealInformation": [
          |         {
          |           "appealStatus": "99",
          |           "appealLevel": "01",
          |         "appealDescription": "Some value"
          |         }
          |       ],
          |       "chargeDueDate": "2022-10-30",
          |       "chargeOutstandingAmount": 200,
          |       "chargeAmount": 200,
          |       "triggeringProcess": "P123",
          |       "chargeReference": "CHARGEREF1"
          |   }]
          | },
          | "latePaymentPenalty": {
          |     "details": [{
          |       "penaltyCategory": "LPP1",
          |       "penaltyChargeReference": "1234567890",
          |       "principalChargeReference":"1234567890",
          |       "penaltyChargeCreationDate":"2022-10-30",
          |       "penaltyStatus": "A",
          |       "appealInformation":
          |       [{
          |         "appealStatus": "99",
          |         "appealLevel": "01",
          |         "appealDescription": "Some value"
          |       }],
          |       "principalChargeBillingFrom": "2022-10-30",
          |       "principalChargeBillingTo": "2022-10-30",
          |       "principalChargeDueDate": "2022-10-30",
          |       "communicationsDate": "2022-10-30",
          |       "penaltyAmountAccruing": 1.11,
          |       "principalChargeMainTransaction": "4700",
          |       "penaltyAmountOutstanding": 99.99,
          |       "penaltyAmountPosted": 0.00,
          |       "penaltyAmountPaid": 1001.45,
          |       "LPP1LRDays": "15",
          |       "LPP1HRDays": "31",
          |       "LPP2Days": "31",
          |       "LPP1HRCalculationAmount": 99.99,
          |       "LPP1LRCalculationAmount": 99.99,
          |       "LPP2Percentage": 4.00,
          |       "LPP1LRPercentage": 2.00,
          |       "LPP1HRPercentage": 2.00,
          |       "penaltyChargeDueDate": "2022-10-30",
          |       "principalChargeDocNumber": "DOC1",
          |       "principalChargeSubTransaction": "SUB1"
          |   }]
          | }
          |}
          |""".stripMargin)
      when(mockGetPenaltyDetailsConnector.getPenaltyDetailsForAPI(any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, sampleAPI1812Response.toString)))
      val result = controller.getPenaltyDetails(vrn = "123456789", dateLimit = Some("02"))(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe sampleAPI1812Response
      verify(mockAuditService, times(1)).audit(any())(any(), any(), any())
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data (auditing the response)" in new Setup(true) {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetailsForAPI(any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, "NOT_FOUND")))

      val result = controller.getPenaltyDetails(vrn = "123456789", dateLimit = None)(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
      verify(mockAuditService, times(1)).audit(any())(any(), any(), any())
    }

    s"return the status from EIS when the call returns a non 200 or 404 status (auditing the response)" in new Setup(true) {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetailsForAPI(any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")))

      val result = controller.getPenaltyDetails(vrn = "123456789", dateLimit = None)(fakeRequest)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      verify(mockAuditService, times(1)).audit(any())(any(), any(), any())
    }
  }
}
