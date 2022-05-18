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
import connectors.parsers.ETMPPayloadParser.{GetETMPPayloadMalformed, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsSuccessResponse}
import models.ETMPPayload
import models.v3.getPenaltyDetails.GetPenaltyDetails
import models.v3.getPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import models.v3.getPenaltyDetails.lateSubmission.{LSPSummary, LateSubmissionPenalty}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import services.auditing.AuditService
import services.{ETMPService, GetPenaltyDetailsService}

import java.time.LocalDate
import scala.concurrent.Future

class PenaltiesFrontendControllerSpec extends SpecBase {
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val mockETMPService: ETMPService = mock(classOf[ETMPService])
  val mockAuditService: AuditService = mock(classOf[AuditService])
  val mockGetPenaltyDetailsService: GetPenaltyDetailsService = mock(classOf[GetPenaltyDetailsService])
  val etmpPayloadWithNoPenalties: ETMPPayload = ETMPPayload(
    pointsTotal = 0, lateSubmissions = 0, adjustmentPointsTotal = 0, fixedPenaltyAmount = 0, penaltyAmountsTotal = 0, otherPenalties = None, vatOverview = None, penaltyPointsThreshold = 4, penaltyPoints = Seq.empty, latePaymentPenalties = None
  )

  class Setup(isFSEnabled: Boolean = false) {
    reset(mockAppConfig, mockETMPService, mockAuditService)
    val controller: PenaltiesFrontendController = new PenaltiesFrontendController(
      mockETMPService,
      mockAuditService,
      mockGetPenaltyDetailsService,
      stubControllerComponents()
    )
  }

  "getPenaltiesData" should {
    "call stub data when call 1812 feature is disabled" must {
      "call the service to retrieve data from ETMP and return OK with the body if successful" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleMTDVATEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))
        val result: Future[Result] = controller.getPenaltiesData(sampleMTDVATEnrolmentKey)(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsJson(result) shouldBe Json.toJson(mockETMPPayloadResponseAsModel)
        verify(mockAuditService)
          .audit(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "call the service and return an ISE if there is a failure to do with processing" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleMTDVATEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadMalformed))))

        val result: Future[Result] = controller.getPenaltiesData(sampleMTDVATEnrolmentKey)(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        contentAsString(result) shouldBe s"Something went wrong."
      }

      "call the service and return a NotFound if there is a NoContent is returned from the connector" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleMTDVATEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadNoContent))))

        val result: Future[Result] = controller.getPenaltiesData(sampleMTDVATEnrolmentKey)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsString(result) shouldBe s"Could not retrieve ETMP penalty data for $sampleMTDVATEnrolmentKey"
      }

      "audit the response when user has > 0 penalties" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleMTDVATEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))
        val result: Future[Result] = controller.getPenaltiesData(sampleMTDVATEnrolmentKey)(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsJson(result) shouldBe Json.toJson(mockETMPPayloadResponseAsModel)
        verify(mockAuditService)
          .audit(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "NOT audit the response when user has 0 penalties" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(Matchers.eq(sampleMTDVATEnrolmentKey))(Matchers.any()))
          .thenReturn(Future.successful((Some(etmpPayloadWithNoPenalties), Right(GetETMPPayloadSuccessResponse(etmpPayloadWithNoPenalties)))))
        val result: Future[Result] = controller.getPenaltiesData(sampleMTDVATEnrolmentKey)(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsJson(result) shouldBe Json.toJson(etmpPayloadWithNoPenalties)
        verify(mockAuditService, times(0))
          .audit(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }
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
          details = Seq()
        )
      ),
      latePaymentPenalty = Some(
        LatePaymentPenalty(
          Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "12345678",
                penaltyChargeReference = Some("1234567892"),
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
                penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
                principalChargeLatestClearing = None
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "12345677",
                penaltyChargeReference = Some("1234567891"),
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
                penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
                principalChargeLatestClearing = None
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "12345676",
                penaltyChargeReference = Some("1234567890"),
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
                penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
                principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1))
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "12345675",
                penaltyChargeReference = Some("1234567889"),
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
                penaltyChargeDueDate = LocalDate.of(2022, 1, 1),
                principalChargeLatestClearing = Some(LocalDate.of(2022, 1, 1))
              )
            )
          )
        )
      )
    )
    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call fails" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))
      val result = controller.getPenaltiesData("123456789", Some(""), true)(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data" in new Setup(isFSEnabled = true) {
      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.NOT_FOUND))))
      val result = controller.getPenaltiesData("123456789", Some(""), true)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return OK (${Status.OK}) when the call returns some data and can be parsed to the correct response" in new Setup(isFSEnabled = true) {

      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))

      val result = controller.getPenaltiesData("123456789", Some(""), true)(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          |{
          | "lateSubmissionPenalty":
          | {
          |   "summary":
          |     {
          |       "activePenaltyPoints": 2,
          |       "inactivePenaltyPoints": 0,
          |       "regimeThreshold": 4,
          |       "penaltyChargeAmount": 200
          |      },
          |     "details": []
          | },
          |  "latePaymentPenalty":
          |   {
          |     "details":[
          |       {
          |         "penaltyCategory": "LPP2",
          |         "penaltyChargeReference": "1234567892",
          |         "principalChargeReference": "12345678",
          |         "penaltyChargeCreationDate": "2022-01-01",
          |         "penaltyStatus": "A",
          |         "principalChargeBillingFrom": "2022-01-01",
          |         "principalChargeBillingTo": "2022-01-01",
          |         "principalChargeDueDate": "2022-01-01",
          |         "communicationsDate": "2022-01-01",
          |         "penaltyAmountOutstanding": 100,
          |         "penaltyAmountPaid": 44.21,
          |         "penaltyChargeDueDate": "2022-01-01"
          |       },
          |       {
          |         "penaltyCategory": "LPP2",
          |         "penaltyChargeReference": "1234567891",
          |         "principalChargeReference": "12345677",
          |         "penaltyChargeCreationDate": "2022-01-01",
          |         "penaltyStatus": "A",
          |         "principalChargeBillingFrom": "2022-01-01",
          |         "principalChargeBillingTo": "2022-01-01",
          |         "principalChargeDueDate": "2022-01-01",
          |         "communicationsDate": "2022-01-01",
          |         "penaltyAmountOutstanding": 23.45,
          |         "penaltyAmountPaid": 100,
          |         "penaltyChargeDueDate": "2022-01-01"
          |       },
          |       {
          |         "penaltyCategory": "LPP1",
          |         "penaltyChargeReference": "1234567890",
          |         "principalChargeReference": "12345676",
          |         "penaltyChargeCreationDate": "2022-01-01",
          |         "penaltyStatus": "P",
          |         "principalChargeBillingFrom": "2022-01-01",
          |         "principalChargeBillingTo": "2022-01-01",
          |         "principalChargeDueDate": "2022-01-01",
          |         "communicationsDate": "2022-01-01",
          |         "penaltyAmountOutstanding": 144,
          |         "penaltyAmountPaid": 0.21,
          |         "penaltyChargeDueDate": "2022-01-01",
          |         "principalChargeLatestClearing": "2022-01-01"
          |       },
          |       {
          |         "penaltyCategory": "LPP1",
          |         "penaltyChargeReference": "1234567889",
          |         "principalChargeReference": "12345675",
          |         "penaltyChargeCreationDate": "2022-01-01",
          |         "penaltyStatus": "P",
          |         "principalChargeBillingFrom": "2022-01-01",
          |         "principalChargeBillingTo": "2022-01-01",
          |         "principalChargeDueDate": "2022-01-01",
          |         "communicationsDate": "2022-01-01",
          |         "penaltyAmountOutstanding": 144,
          |         "penaltyAmountPaid": 0.21,
          |         "penaltyChargeDueDate": "2022-01-01",
          |         "principalChargeLatestClearing": "2022-01-01"
          |       }
          |    ]
          |  }
          |}
          |""".stripMargin
      )
      verify(mockAuditService, times(1)).audit(Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    s"return OK (${Status.OK}) when there are no estimated LPPs in penalty details" in new Setup(isFSEnabled = true) {

      when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsNoEstimatedLPPs))))

      val result = controller.getPenaltiesData("123456789", Some(""), true)(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.parse(
        """
          | {
          |   "lateSubmissionPenalty":
          |     {
          |       "summary":
          |         {
          |           "activePenaltyPoints":4,
          |           "inactivePenaltyPoints":0,
          |           "regimeThreshold":5,
          |           "penaltyChargeAmount":200
          |         },
          |       "details":[]
          |     }
          | }
          |""".stripMargin
      )
    }
  }
}
