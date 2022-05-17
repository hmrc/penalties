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
import connectors.FileNotificationOrchestratorConnector
import connectors.parsers.AppealsParser.UnexpectedFailure
import connectors.parsers.ETMPPayloadParser.{GetETMPPayloadMalformed, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsSuccessResponse}
import featureSwitches.FeatureSwitching
import models.appeals.AppealData
import models.appeals.AppealTypeEnum.{Additional, Late_Payment, Late_Submission}
import models.appeals.v2.{AppealData => V2AppealData}
import models.notification._
import models.upload.{UploadDetails, UploadJourney, UploadStatusEnum}
import models.v3.getPenaltyDetails.GetPenaltyDetails
import models.v3.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import models.v3.getPenaltyDetails.lateSubmission._
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{ETMPService, GetPenaltyDetailsService}
import utils.{PenaltyPeriodHelper, UUIDGenerator}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AppealsControllerSpec extends SpecBase with FeatureSwitching {
  val mockETMPService: ETMPService = mock(classOf[ETMPService])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val mockUUIDGenerator: UUIDGenerator = mock(classOf[UUIDGenerator])
  val mockGetPenaltyDetailsService: GetPenaltyDetailsService = mock(classOf[GetPenaltyDetailsService])
  val correlationId = "id-1234567890"
  val connector: FileNotificationOrchestratorConnector = injector.instanceOf[FileNotificationOrchestratorConnector]

  class Setup(withRealAppConfig: Boolean = true) {
    reset(mockAppConfig, mockETMPService, mockGetPenaltyDetailsService)
    val controller = new AppealsController(if (withRealAppConfig) appConfig
    else mockAppConfig, mockETMPService, mockGetPenaltyDetailsService, mockUUIDGenerator, connector, stubControllerComponents())
  }

  "getAppealsDataForLateSubmissionPenalty" should {
    "when the 1812 feature switch is disabled" must {
      s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP can not find the data for the given enrolment key" in new Setup {
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadNoContent))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty("1", sampleEnrolmentKey, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe s"Could not retrieve ETMP penalty data for $sampleEnrolmentKey"
      }

      s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP returns data but the given penaltyId is wrong" in new Setup {
        val samplePenaltyId: String = "1234"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty(samplePenaltyId, sampleEnrolmentKey, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe "Penalty ID was not found in users penalties."
      }

      s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call to ETMP fails for some reason" in new Setup {
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadMalformed))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty("1", sampleEnrolmentKey, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      s"return OK (${Status.OK}) when the call to ETMP succeeds and the penalty ID matches" in new Setup {
        val samplePenaltyId: String = "123456789"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty(samplePenaltyId, sampleEnrolmentKey, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.OK
        val appealDataToReturn: AppealData = AppealData(
          Late_Submission,
          mockETMPPayloadResponseAsModel.penaltyPoints.head.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.startDate,
          mockETMPPayloadResponseAsModel.penaltyPoints.head.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.endDate,
          mockETMPPayloadResponseAsModel.penaltyPoints.head.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.submission.dueDate,
          mockETMPPayloadResponseAsModel.penaltyPoints.head.communications.head.dateSent
        )
        contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
      }

      s"return OK (${Status.OK}) when the call to ETMP succeeds and the correct model for the specified penalty ID" in new Setup {
        val samplePenaltyId: String = "123456789"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModelMultiplePoints),
            Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelMultiplePoints)))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty(samplePenaltyId, sampleEnrolmentKey, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.OK
        val appealDataToReturn: AppealData = AppealData(
          Late_Submission,
          mockETMPPayloadResponseAsModelMultiplePoints.penaltyPoints.last.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.startDate,
          mockETMPPayloadResponseAsModelMultiplePoints.penaltyPoints.last.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.endDate,
          mockETMPPayloadResponseAsModelMultiplePoints.penaltyPoints.head.period.get.sortWith(PenaltyPeriodHelper.sortByPenaltyStartDate(_, _) < 0).head.submission.dueDate,
          mockETMPPayloadResponseAsModelMultiplePoints.penaltyPoints.head.communications.head.dateSent
        )
        contentAsJson(result) shouldBe Json.toJson(appealDataToReturn)
      }
    }

    "when the 1812 feature switch is enabled" must {
      val getPenaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = Some(
          LateSubmissionPenalty(
            summary = LSPSummary(
              activePenaltyPoints = 2, inactivePenaltyPoints = 0, regimeThreshold = 5, penaltyChargeAmount = 200
            ),
            details = Seq(
              LSPDetails(
                penaltyNumber = "123456789",
                penaltyOrder = "1",
                penaltyCategory = LSPPenaltyCategoryEnum.Point,
                penaltyStatus = LSPPenaltyStatusEnum.Active,
                penaltyCreationDate = LocalDate.of(2022, 4, 1),
                penaltyExpiryDate = LocalDate.of(2022, 4, 1),
                communicationsDate = LocalDate.of(2022, 5, 8),
                FAPIndicator = None,
                lateSubmissions = Some(
                  Seq(
                    LateSubmission(
                      taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                      taxPeriodEndDate = Some(LocalDate.of(2022, 3, 31)),
                      taxPeriodDueDate = Some(LocalDate.of(2022, 5, 7)),
                      returnReceiptDate = Some(LocalDate.of(2022, 5, 9))
                    )
                  )
                ),
                expiryReason = None,
                appealInformation = None,
                chargeDueDate = None,
                chargeOutstandingAmount = None,
                chargeAmount = None
              ),
              LSPDetails(
                penaltyNumber = "123456788",
                penaltyOrder = "2",
                penaltyCategory = LSPPenaltyCategoryEnum.Point,
                penaltyStatus = LSPPenaltyStatusEnum.Active,
                penaltyCreationDate = LocalDate.of(2022, 4, 1),
                penaltyExpiryDate = LocalDate.of(2022, 4, 1),
                communicationsDate = LocalDate.of(2022, 4, 1),
                FAPIndicator = None,
                lateSubmissions = Some(
                  Seq(
                    LateSubmission(
                      taxPeriodStartDate = Some(LocalDate.of(2022, 4, 1)),
                      taxPeriodEndDate = Some(LocalDate.of(2022, 6, 30)),
                      taxPeriodDueDate = Some(LocalDate.of(2022, 8, 7)),
                      returnReceiptDate = Some(LocalDate.of(2022, 8, 9))
                    )
                  )
                ),
                expiryReason = None,
                appealInformation = None,
                chargeDueDate = None,
                chargeOutstandingAmount = None,
                chargeAmount = None
              )
            )
          )
        ),
        latePaymentPenalty = None
      )
      s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP can not find the data for the given enrolment key" in new Setup {
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(NOT_FOUND))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty("1", sampleEnrolmentKey, useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe s"A downstream call returned 404 for VRN: $vrn"
      }

      s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP returns data but the given penaltyId is wrong" in new Setup {
        val samplePenaltyId: String = "1234"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty(samplePenaltyId, sampleEnrolmentKey, useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe "Penalty ID was not found in users penalties."
      }

      s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call to ETMP fails for some reason" in new Setup {
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty("1", sampleEnrolmentKey, useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      s"return OK (${Status.OK}) when the call to ETMP succeeds and the penalty ID matches" in new Setup {
        val samplePenaltyId: String = "123456789"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))))

        val result: Future[Result] = controller.getAppealsDataForLateSubmissionPenalty(samplePenaltyId, sampleEnrolmentKey,
          useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.OK
        val appealDataToReturn: V2AppealData = V2AppealData(
          Late_Submission,
          startDate = LocalDate.of(2022, 1, 1),
          endDate = LocalDate.of(2022, 3, 31),
          dueDate = LocalDate.of(2022, 5, 7),
          dateCommunicationSent = LocalDate.of(2022, 5, 8)
        )
        contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
      }
    }
  }

  "getAppealsDataForLatePaymentPenalty" should {
    "when the 1812 feature switch is disabled" must {
      s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP can not find the data for the given enrolment key" in new Setup {
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadNoContent))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty("1", sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe s"Could not retrieve ETMP penalty data for $sampleEnrolmentKey"
      }

      s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP returns data but the given penaltyId is wrong" in new Setup {
        val samplePenaltyId: String = "1234"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty(samplePenaltyId, sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe "Penalty ID was not found in users penalties."
      }

      s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call to ETMP fails for some reason" in new Setup {
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadMalformed))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty("1", sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      s"return OK (${Status.OK}) when the call to ETMP succeeds and the penalty ID matches" in new Setup {
        val samplePenaltyId: String = "123456800"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModelForLPP), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelForLPP)))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty(samplePenaltyId, sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.OK
        val appealDataToReturn: AppealData = AppealData(
          Late_Payment,
          mockETMPPayloadResponseAsModelForLPP.latePaymentPenalties.get.head.period.startDate,
          mockETMPPayloadResponseAsModelForLPP.latePaymentPenalties.get.head.period.endDate,
          mockETMPPayloadResponseAsModelForLPP.latePaymentPenalties.get.head.period.dueDate,
          mockETMPPayloadResponseAsModelForLPP.latePaymentPenalties.get.head.communications.head.dateSent
        )
        contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
      }

      s"return OK (${Status.OK}) when the call to ETMP succeeds and the penalty ID matches for Additional penalty" in new Setup {
        val samplePenaltyId: String = "123456801"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModelForLPPWithAdditionalPenalties),
            Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelForLPPWithAdditionalPenalties)))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty(samplePenaltyId, sampleEnrolmentKey,
          isAdditional = true, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.OK
        val appealDataToReturn: AppealData = AppealData(
          Additional,
          mockETMPPayloadResponseAsModelForLPP.latePaymentPenalties.get.head.period.startDate,
          mockETMPPayloadResponseAsModelForLPP.latePaymentPenalties.get.head.period.endDate,
          mockETMPPayloadResponseAsModelForLPP.latePaymentPenalties.get.head.period.dueDate,
          mockETMPPayloadResponseAsModelForLPP.latePaymentPenalties.get.head.communications.head.dateSent
        )
        contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
      }

      s"return OK (${Status.OK}) when the call to ETMP succeeds and the correct model for the specified penalty ID" in new Setup {
        val samplePenaltyId: String = "123456800"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModelMultipleSubmissionAndPaymentPoints),
            Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelMultipleSubmissionAndPaymentPoints)))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty(samplePenaltyId, sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = false)(fakeRequest)
        status(result) shouldBe Status.OK
        val appealDataToReturn: AppealData = AppealData(
          Late_Payment,
          mockETMPPayloadResponseAsModelMultipleSubmissionAndPaymentPoints.latePaymentPenalties.get.head.period.startDate,
          mockETMPPayloadResponseAsModelMultipleSubmissionAndPaymentPoints.latePaymentPenalties.get.head.period.endDate,
          mockETMPPayloadResponseAsModelMultipleSubmissionAndPaymentPoints.latePaymentPenalties.get.head.period.dueDate,
          mockETMPPayloadResponseAsModelMultipleSubmissionAndPaymentPoints.latePaymentPenalties.get.head.communications.head.dateSent
        )
        contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
      }
    }

    "when the 1812 feature switch is enabled" must {
      val getPenaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(
          LatePaymentPenalty(
            Some(
              Seq(
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                  principalChargeReference = "123456801",
                  penaltyChargeReference = Some("1234567891"),
                  penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
                  penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                  appealInformation = None,
                  principalChargeBillingFrom = LocalDate.of(2022, 4, 1),
                  principalChargeBillingTo = LocalDate.of(2022, 6, 30),
                  principalChargeDueDate = LocalDate.of(2022, 8, 7),
                  communicationsDate = LocalDate.of(2022, 8, 8),
                  penaltyAmountOutstanding = Some(100),
                  penaltyAmountPaid = Some(13.45),
                  LPP1LRDays = None,
                  LPP1HRDays = None,
                  LPP2Days = None,
                  LPP1HRCalculationAmount = None,
                  LPP1LRCalculationAmount = None,
                  LPP2Percentage = None,
                  LPP1LRPercentage = None,
                  LPP1HRPercentage = None,
                  penaltyChargeDueDate = LocalDate.of(2022, 8, 7)
                ),
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                  principalChargeReference = "123456800",
                  penaltyChargeReference = Some("1234567890"),
                  penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
                  penaltyStatus = LPPPenaltyStatusEnum.Posted,
                  appealInformation = None,
                  principalChargeBillingFrom = LocalDate.of(2022, 1, 1),
                  principalChargeBillingTo = LocalDate.of(2022, 3, 31),
                  principalChargeDueDate = LocalDate.of(2022, 5, 7),
                  communicationsDate = LocalDate.of(2022, 5, 8),
                  penaltyAmountOutstanding = Some(100),
                  penaltyAmountPaid = Some(13.45),
                  LPP1LRDays = None,
                  LPP1HRDays = None,
                  LPP2Days = None,
                  LPP1HRCalculationAmount = None,
                  LPP1LRCalculationAmount = None,
                  LPP2Percentage = None,
                  LPP1LRPercentage = None,
                  LPP1HRPercentage = None,
                  penaltyChargeDueDate = LocalDate.of(2022, 8, 7)
                )
              )
            )
          )
        )
      )
      s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP can not find the data for the given enrolment key" in new Setup {
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(NOT_FOUND))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty("1", sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe s"A downstream call returned 404 for VRN: $vrn"
      }

      s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP returns data but the given penaltyId is wrong" in new Setup {
        val samplePenaltyId: String = "1234"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty(samplePenaltyId, sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe "Penalty ID was not found in users penalties."
      }

      s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call to ETMP fails for some reason" in new Setup {
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty("1", sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      s"return OK (${Status.OK}) when the call to ETMP succeeds and the penalty ID matches" in new Setup {
        val samplePenaltyId: String = "1234567890"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty(samplePenaltyId, sampleEnrolmentKey,
          isAdditional = false, useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.OK
        val appealDataToReturn: V2AppealData = V2AppealData(
          `type` = Late_Payment,
          startDate = LocalDate.of(2022, 1, 1),
          endDate = LocalDate.of(2022, 3, 31),
          dueDate = LocalDate.of(2022, 5, 7),
          dateCommunicationSent = LocalDate.of(2022, 5, 8)
        )
        contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
      }

      s"return OK (${Status.OK}) when the call to ETMP succeeds and the penalty ID matches for Additional penalty" in new Setup {
        val samplePenaltyId: String = "1234567891"
        val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        val vrn: String = "123456789"
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.eq(vrn))(Matchers.any()))
          .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))))

        val result: Future[Result] = controller.getAppealsDataForLatePaymentPenalty(samplePenaltyId, sampleEnrolmentKey,
          isAdditional = true, useNewApiModel = true)(fakeRequest)
        status(result) shouldBe Status.OK
        val appealDataToReturn: V2AppealData = V2AppealData(
          `type` = Additional,
          startDate = LocalDate.of(2022, 4, 1),
          endDate = LocalDate.of(2022, 6, 30),
          dueDate = LocalDate.of(2022, 8, 7),
          dateCommunicationSent = LocalDate.of(2022, 8, 8)
        )
        contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
      }
    }

  }

  "getReasonableExcuses" should {
    "return all the excuses that are stored in the ReasonableExcuse model" in new Setup {
      val jsonExpectedToReturn: JsValue = Json.parse(
        """
          |{
          |  "excuses": [
          |    {
          |      "type": "bereavement",
          |      "descriptionKey": "reasonableExcuses.bereavementReason"
          |    },
          |    {
          |      "type": "crime",
          |      "descriptionKey": "reasonableExcuses.crimeReason"
          |    },
          |    {
          |      "type": "fireOrFlood",
          |      "descriptionKey": "reasonableExcuses.fireOrFloodReason"
          |    },
          |    {
          |      "type": "health",
          |      "descriptionKey": "reasonableExcuses.healthReason"
          |    },
          |    {
          |      "type": "lossOfStaff",
          |      "descriptionKey": "reasonableExcuses.lossOfStaffReason"
          |    },
          |    {
          |      "type": "technicalIssues",
          |      "descriptionKey": "reasonableExcuses.technicalIssuesReason"
          |    },
          |    {
          |      "type": "other",
          |      "descriptionKey": "reasonableExcuses.otherReason"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val result: Future[Result] = controller.getReasonableExcuses()(fakeRequest)
      status(result) shouldBe OK
      contentAsJson(result) shouldBe jsonExpectedToReturn
    }

    "return only those reasonable excuses that are active based on config" in new Setup(withRealAppConfig = false) {
      val jsonExpectedToReturn: JsValue = Json.parse(
        """
          |{
          |  "excuses": [
          |    {
          |      "type": "bereavement",
          |      "descriptionKey": "reasonableExcuses.bereavementReason"
          |    },
          |    {
          |      "type": "crime",
          |      "descriptionKey": "reasonableExcuses.crimeReason"
          |    },
          |    {
          |      "type": "fireOrFlood",
          |      "descriptionKey": "reasonableExcuses.fireOrFloodReason"
          |    },
          |    {
          |      "type": "health",
          |      "descriptionKey": "reasonableExcuses.healthReason"
          |    },
          |    {
          |      "type": "lossOfStaff",
          |      "descriptionKey": "reasonableExcuses.lossOfStaffReason"
          |    },
          |    {
          |      "type": "technicalIssues",
          |      "descriptionKey": "reasonableExcuses.technicalIssuesReason"
          |    }
          |  ]
          |}
          |""".stripMargin
      )
      when(mockAppConfig.isReasonableExcuseEnabled(Matchers.any()))
        .thenReturn(true)
      when(mockAppConfig.isReasonableExcuseEnabled(Matchers.eq("other")))
        .thenReturn(false)
      val result: Future[Result] = controller.getReasonableExcuses()(fakeRequest)
      status(result) shouldBe OK
      contentAsJson(result) shouldBe jsonExpectedToReturn
    }
  }

  "submitAppeal" should {
    "return BAD_REQUEST (400)" when {
      "the request body is not valid JSON" in new Setup {
        val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe "Invalid body received i.e. could not be parsed to JSON"
      }

      "the request body is valid JSON but can not be serialised to a model" in new Setup {
        val appealsJson: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": true,
            |    "appealSubmittedBy": "client"
            |}
            |""".stripMargin)

        val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe "Failed to parse to model"
      }
    }

    "return the error status code" when {
      "the connector calls fails" in new Setup {

        when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
          .thenReturn(Future.successful(Left(UnexpectedFailure(GATEWAY_TIMEOUT, s"Unexpected response, status $GATEWAY_TIMEOUT returned"))))
        val appealsJson: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": true,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "crime",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "reportedIssueToPolice": true,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
        status(result) shouldBe GATEWAY_TIMEOUT
      }
    }

    "return OK (200)" when {
      "the JSON request body can be parsed and the connector returns a successful response for crime" in new Setup {
        when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
          .thenReturn(Future.successful(Right(appealResponseModel)))
        val appealsJson: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": true,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "crime",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "reportedIssueToPolice": true,
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
        status(result) shouldBe OK
      }

      "the JSON request body can be parsed and the connector returns a successful response for loss of staff" in new Setup {
        when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
          .thenReturn(Future.successful(Right(appealResponseModel)))
        val appealsJson: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": true,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "lossOfStaff",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
        status(result) shouldBe OK
      }

      "the Json request body can be parsed and the connector returns a successful response for fire or flood" in new Setup {
        when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
          .thenReturn(Future.successful(Right(appealResponseModel)))
        val appealsJson: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": true,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "fireOrFlood",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
        status(result) shouldBe OK
      }

      "the Json request body can be parsed and the connector returns a successful response for technical issues" in new Setup {
        when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
          .thenReturn(Future.successful(Right(appealResponseModel)))
        val appealsJson: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": true,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "technicalIssues",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "endDateOfEvent": "2021-04-24T18:25:43.511Z",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
        status(result) shouldBe OK
      }

      "the Json request body can be parsed and the connector returns a successful response for health" when {
        "there was no hospital stay" in new Setup {
          when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
          .thenReturn(Future.successful(Right(appealResponseModel)))
          val appealsJson: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": true,
              |    "appealSubmittedBy": "client",
              |    "appealInformation": {
              |						 "reasonableExcuse": "health",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
              |            "hospitalStayInvolved": false,
              |            "eventOngoing": false,
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
          status(result) shouldBe OK
        }

        "there is an ongoing hospital stay" in new Setup {
          when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
            .thenReturn(Future.successful(Right(appealResponseModel)))
          val appealsJson: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": true,
              |    "appealSubmittedBy": "client",
              |    "appealInformation": {
              |						 "reasonableExcuse": "health",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
              |            "hospitalStayInvolved": true,
              |            "eventOngoing": true,
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
          status(result) shouldBe OK
        }

        "there was a hospital stay that has ended" in new Setup {
          when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
            .thenReturn(Future.successful(Right(appealResponseModel)))
          val appealsJson: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": true,
              |    "appealSubmittedBy": "client",
              |    "appealInformation": {
              |						 "reasonableExcuse": "health",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
              |            "endDateOfEvent": "2021-04-23T18:25:43.511Z",
              |            "hospitalStayInvolved": true,
              |            "eventOngoing": false,
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
          status(result) shouldBe OK
        }

        "the JSON request body can be parsed and the appeal is a LPP" in new Setup {
          when(mockETMPService.submitAppeal(any(), any(), any(), any(), any()))
            .thenReturn(Future.successful(Right(appealResponseModel)))
          val appealsJson: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": true,
              |    "appealSubmittedBy": "client",
              |    "appealInformation": {
              |						"reasonableExcuse": "crime",
              |           "honestyDeclaration": true,
              |           "startDateOfEvent": "2021-04-23T18:25:43.511Z",
              |           "reportedIssueToPolice": true,
              |           "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: Future[Result] = controller.submitAppeal("HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyNumber = "123456789", correlationId = correlationId)(fakeRequest.withJsonBody(appealsJson))
          status(result) shouldBe OK
        }
      }

    }
  }

  "createSDESNotification" should {
    "return an empty Seq" when {
      "None is passed to the uploadJourney" in new Setup {
        val result = controller.createSDESNotifications(None, "")
        result shouldBe Seq.empty
      }
    }

    "return a Seq of SDES notifications" when {
      "Some uploadJourneys are passed in" in new Setup {
        val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0 , 0, 0)
        val uploads = Seq(
          UploadJourney(reference = "ref-123",
            fileStatus = UploadStatusEnum.READY,
            downloadUrl = Some("/"),
            uploadDetails = Some(UploadDetails(
              fileName = "file1",
              fileMimeType = "text/plain",
              uploadTimestamp = LocalDateTime.of(2018,4,24,9,30,0),
              checksum = "check123456789",
              size = 1
            )),
            lastUpdated = mockDateTime,
            uploadFields = Some(Map(
              "key" -> "abcxyz",
              "x-amz-algorithm" -> "md5"
            ))
          )
        )

        val expectedResult = Seq(
          SDESNotification(
            informationType = "S18",
            file = SDESNotificationFile(
              recipientOrSender = "123456789012",
              name = "file1",
              location = "/",
              checksum = SDESChecksum(algorithm = "md5", value = "check123456789"),
              size = 1,
              properties = Seq(
                SDESProperties(name = "CaseId", value = "PR-1234"),
                SDESProperties(name = "SourceFileUploadDate", value = "2018-04-24T09:30")
              )
            ),
            audit = SDESAudit(
              correlationID = correlationId
            )
          )
        )

        when(mockUUIDGenerator.generateUUID).thenReturn(correlationId)
        val result = controller.createSDESNotifications(Some(uploads), caseID = "PR-1234")
        result shouldBe expectedResult
      }
    }
  }
}
