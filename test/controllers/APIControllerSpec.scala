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
import connectors.parsers.ETMPPayloadParser._
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser._
import featureSwitches.{FeatureSwitching, UseAPI1812Model}
import models.v3.getPenaltyDetails.GetPenaltyDetails
import models.v3.getPenaltyDetails.latePayment._
import models.v3.getPenaltyDetails.lateSubmission.{LSPSummary, LateSubmissionPenalty}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.auditing.AuditService
import services.v2.APIService
import services.{ETMPService, GetPenaltyDetailsService}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIControllerSpec extends SpecBase with FeatureSwitching {
  val mockETMPService: ETMPService = mock(classOf[ETMPService])
  val mockAuditService: AuditService = mock(classOf[AuditService])
  val mockAPIService: APIService = mock(classOf[APIService])
  val mockGetPenaltyDetailsService: GetPenaltyDetailsService = mock(classOf[GetPenaltyDetailsService])

  class Setup(isFSEnabled: Boolean = false) {
    reset(mockETMPService, mockAuditService, mockAPIService)
    val controller = new APIController(mockETMPService, mockAuditService, mockAPIService,
      mockGetPenaltyDetailsService, stubControllerComponents())
    if(isFSEnabled) enableFeatureSwitch(UseAPI1812Model) else disableFeatureSwitch(UseAPI1812Model)
  }

  "getSummaryDataForVRN" should {
    "call stub data when call 1812 feature is disabled" must {
      s"return NOT_FOUND (${Status.NOT_FOUND}) when the call to ETMP fails" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)))))
        val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }

      s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadNoContent))))
        val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }

      s"return BAD_REQUEST (${Status.BAD_REQUEST}) when the user supplies an invalid VRN" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))
        val result = controller.getSummaryDataForVRN("1234567891234567890")(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        //TODO: change data based on implementation
        contentAsString(result) shouldBe "VRN: 1234567891234567890 was not in a valid format."
      }

      s"return OK (${Status.OK}) when the call returns some data and can be parsed to the correct response" in new Setup {
        when(mockETMPService.checkIfHasAnyPenaltyData(Matchers.any())).thenReturn(true)
        when(mockETMPService.getNumberOfEstimatedPenalties(Matchers.any())).thenReturn(2)
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadForAPIResponseData), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadForAPIResponseData)))))
        when(mockETMPService.findEstimatedPenaltiesAmount(Matchers.any()))
          .thenReturn(BigDecimal(123.45))
        when(mockETMPService.getNumberOfCrystalizedPenalties(Matchers.any())).thenReturn(2)
        when(mockETMPService.getCrystalisedPenaltyTotal(Matchers.any())).thenReturn(BigDecimal(288))
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
        verify(mockAuditService, times(1)).audit(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      s"return OK (${Status.OK}) when there are no LSP or LPP estimated penalties in etmpPayload" in new Setup {
        when(mockETMPService.checkIfHasAnyPenaltyData(Matchers.any())).thenReturn(false)
        when(mockETMPService.getNumberOfEstimatedPenalties(Matchers.any())).thenReturn(0)
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadWithNoEstimatedPenaltiesForAPIResponseData), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadWithNoEstimatedPenaltiesForAPIResponseData)))))
        when(mockETMPService.findEstimatedPenaltiesAmount(Matchers.any()))
          .thenReturn(BigDecimal(0))
        when(mockETMPService.getNumberOfCrystalizedPenalties(Matchers.any())).thenReturn(0)
        when(mockETMPService.getCrystalisedPenaltyTotal(Matchers.any())).thenReturn(BigDecimal(0))
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
            |  "hasAnyPenaltyData": false
            |}
            |""".stripMargin
        )
      }
    }

    "call API 1812 when call 1812 feature is enabled" must {
      val getPenaltyDetailsNoEstimatedLPPs: GetPenaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = Some(
          LateSubmissionPenalty(
            summary = LSPSummary(
              activePenaltyPoints = 4,
              inactivePenaltyPoints = 0,
              regimeThreshold = 5,
              penaltyChargeAmount = 200
            ),
            details = Seq() //omitted
          )
        ),
        latePaymentPenalty = None
      )

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
                  penaltyChargeDueDate = LocalDate.of(2022, 1, 1)
                ),
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                  principalChargeReference = "12345677",
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
                  penaltyChargeDueDate = LocalDate.of(2022, 1, 1)
                ),
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                  principalChargeReference = "12345676",
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
                  penaltyChargeDueDate = LocalDate.of(2022, 1, 1)
                ),
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                  principalChargeReference = "12345675",
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
                  penaltyChargeDueDate = LocalDate.of(2022, 1, 1)
                )
              )
            )
          )
        )
      )

      s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call fails" in new Setup(isFSEnabled = true) {
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))
        val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data" in new Setup(isFSEnabled = true) {
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.NOT_FOUND))))
        val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }

      s"return BAD_REQUEST (${Status.BAD_REQUEST}) when the user supplies an invalid VRN" in new Setup(isFSEnabled = true) {
        val result = controller.getSummaryDataForVRN("1234567891234567890")(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe "VRN: 1234567891234567890 was not in a valid format."
      }

      s"return OK (${Status.OK}) when the call returns some data and can be parsed to the correct response" in new Setup(isFSEnabled = true) {
        when(mockAPIService.checkIfHasAnyPenaltyData(Matchers.any())).thenReturn(true)
        when(mockAPIService.getNumberOfEstimatedPenalties(Matchers.any())).thenReturn(2)
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
        when(mockAPIService.findEstimatedPenaltiesAmount(Matchers.any()))
          .thenReturn(BigDecimal(123.45))
        when(mockAPIService.getNumberOfCrystallisedPenalties(Matchers.any())).thenReturn(2)
        when(mockAPIService.getCrystallisedPenaltyTotal(Matchers.any())).thenReturn(BigDecimal(288))
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
        verify(mockAuditService, times(1)).audit(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      s"return OK (${Status.OK}) when there are no estimated LPPs in penalty details" in new Setup(isFSEnabled = true) {
        when(mockAPIService.checkIfHasAnyPenaltyData(Matchers.any())).thenReturn(true)
        when(mockAPIService.getNumberOfEstimatedPenalties(Matchers.any())).thenReturn(0)
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsNoEstimatedLPPs))))
        when(mockAPIService.findEstimatedPenaltiesAmount(Matchers.any()))
          .thenReturn(BigDecimal(0))
        when(mockAPIService.getNumberOfCrystallisedPenalties(Matchers.any())).thenReturn(0)
        when(mockAPIService.getCrystallisedPenaltyTotal(Matchers.any())).thenReturn(BigDecimal(0))
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
    }
  }
}
