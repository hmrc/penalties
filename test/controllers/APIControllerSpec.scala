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
import config.featureSwitches.{FeatureSwitching, UseAPI1812Model}
import connectors.parsers.ETMPPayloadParser._
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser._
import connectors.v3.getFinancialDetails.GetFinancialDetailsConnector
import connectors.v3.getPenaltyDetails.GetPenaltyDetailsConnector
import models.v3.getPenaltyDetails.GetPenaltyDetails
import models.v3.getPenaltyDetails.latePayment._
import models.v3.getPenaltyDetails.lateSubmission.{LSPSummary, LateSubmissionPenalty}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.auditing.AuditService
import services.v2.APIService
import services.{ETMPService, GetPenaltyDetailsService}
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIControllerSpec extends SpecBase with FeatureSwitching {
  val mockETMPService: ETMPService = mock(classOf[ETMPService])
  val mockAuditService: AuditService = mock(classOf[AuditService])
  val mockAPIService: APIService = mock(classOf[APIService])
  val mockGetPenaltyDetailsService: GetPenaltyDetailsService = mock(classOf[GetPenaltyDetailsService])
  val mockGetFinancialDetailsConnector:GetFinancialDetailsConnector = mock(classOf[GetFinancialDetailsConnector])
  val mockGetPenaltyDetailsConnector: GetPenaltyDetailsConnector = mock(classOf[GetPenaltyDetailsConnector])
  implicit val config: Configuration = appConfig.config

  class Setup(isFSEnabled: Boolean = false) {
    reset(mockETMPService, mockAuditService, mockAPIService)
    val controller = new APIController(mockETMPService, mockAuditService, mockAPIService,
      mockGetPenaltyDetailsService, mockGetFinancialDetailsConnector, mockGetPenaltyDetailsConnector, stubControllerComponents())
    if(isFSEnabled) enableFeatureSwitch(UseAPI1812Model) else disableFeatureSwitch(UseAPI1812Model)
  }

  "getSummaryDataForVRN" should {
    "call stub data when call 1812 feature is disabled" must {
      s"return NOT_FOUND (${Status.NOT_FOUND}) when the call to ETMP fails" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(any())(any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)))))
        val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }

      s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(any())(any()))
          .thenReturn(Future.successful((None, Left(GetETMPPayloadNoContent))))
        val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }

      s"return BAD_REQUEST (${Status.BAD_REQUEST}) when the user supplies an invalid VRN" in new Setup {
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(any())(any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))
        val result = controller.getSummaryDataForVRN("1234567891234567890")(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        //TODO: change data based on implementation
        contentAsString(result) shouldBe "VRN: 1234567891234567890 was not in a valid format."
      }

      s"return OK (${Status.OK}) when the call returns some data and can be parsed to the correct response" in new Setup {
        when(mockETMPService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
        when(mockETMPService.getNumberOfEstimatedPenalties(any())).thenReturn(2)
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(any())(any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadForAPIResponseData), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadForAPIResponseData)))))
        when(mockETMPService.findEstimatedPenaltiesAmount(any()))
          .thenReturn(BigDecimal(123.45))
        when(mockETMPService.getNumberOfCrystalizedPenalties(any())).thenReturn(2)
        when(mockETMPService.getCrystalisedPenaltyTotal(any())).thenReturn(BigDecimal(288))
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

      s"return OK (${Status.OK}) when there are no LSP or LPP estimated penalties in etmpPayload" in new Setup {
        when(mockETMPService.checkIfHasAnyPenaltyData(any())).thenReturn(false)
        when(mockETMPService.getNumberOfEstimatedPenalties(any())).thenReturn(0)
        when(mockETMPService.getPenaltyDataFromETMPForEnrolment(any())(any()))
          .thenReturn(Future.successful((Some(mockETMPPayloadWithNoEstimatedPenaltiesForAPIResponseData), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadWithNoEstimatedPenaltiesForAPIResponseData)))))
        when(mockETMPService.findEstimatedPenaltiesAmount(any()))
          .thenReturn(BigDecimal(0))
        when(mockETMPService.getNumberOfCrystalizedPenalties(any())).thenReturn(0)
        when(mockETMPService.getCrystalisedPenaltyTotal(any())).thenReturn(BigDecimal(0))
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
                  penaltyChargeReference = Some("1234567893"),
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
                  penaltyChargeReference = Some("1234567892"),
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
                  penaltyChargeReference = Some("1234567891"),
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
                )
              )
            )
          )
        )
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

      s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data" in new Setup(isFSEnabled = true) {
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
          .thenReturn(Future.successful(Left(GetPenaltyDetailsFailureResponse(Status.NO_CONTENT))))
        val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }

      s"return BAD_REQUEST (${Status.BAD_REQUEST}) when the user supplies an invalid VRN" in new Setup(isFSEnabled = true) {
        val result = controller.getSummaryDataForVRN("1234567891234567890")(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe "VRN: 1234567891234567890 was not in a valid format."
      }

      s"return OK (${Status.OK}) when the call returns some data and can be parsed to the correct response" in new Setup(isFSEnabled = true) {
        when(mockAPIService.checkIfHasAnyPenaltyData(any())).thenReturn(true)
        when(mockAPIService.getNumberOfEstimatedPenalties(any())).thenReturn(2)
        when(mockGetPenaltyDetailsService.getDataFromPenaltyServiceForVATCVRN(any())(any()))
          .thenReturn(Future.successful(Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetailsFullAPIResponse))))
        when(mockAPIService.findEstimatedPenaltiesAmount(any()))
          .thenReturn(BigDecimal(123.45))
        when(mockAPIService.getNumberOfCrystallisedPenalties(any())).thenReturn(2)
        when(mockAPIService.getCrystallisedPenaltyTotal(any())).thenReturn(BigDecimal(288))
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
        when(mockAPIService.getNumberOfCrystallisedPenalties(any())).thenReturn(0)
        when(mockAPIService.getCrystallisedPenaltyTotal(any())).thenReturn(BigDecimal(0))
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

  "getFinancialDetails" should {
    s"return OK (${Status.OK}) when a JSON payload is received from EIS" in new Setup(isFSEnabled = true) {
      val sampleAPI1911Response = Json.parse(
        """
          |{
          |            "taxPayerDetails": {
          |              "idType": "VRN",
          |              "idNumber": 123456789,
          |              "regimeType": "VATC"
          |            },
          |            "balanceDetails": {
          |              "balanceDueWithin30Days": -99999999999.99,
          |              "nextPaymentDateForChargesDueIn30Days": "1920-02-29",
          |              "balanceNotDueIn30Days": -99999999999.99,
          |              "nextPaymentDateBalanceNotDue": "1920-02-29",
          |              "overDueAmount": -99999999999.99,
          |              "earliestPaymentDateOverDue": "1920-02-29",
          |              "totalBalance": -99999999999.99,
          |              "amountCodedOut": 3456.67
          |            },
          |            "codingDetails": [
          |              {
          |                "taxYearReturn": "2017",
          |                "totalReturnAmount": 2234.56,
          |                "amountNotCoded": 234.56,
          |                "amountNotCodedDueDate": "2021-07-29",
          |                "amountCodedOut": 2634.56,
          |                "taxYearCoding": "2018",
          |                "documentText": "document coding details"
          |              }
          |            ],
          |            "documentDetails": [
          |              {
          |                "taxYear": "2017",
          |                "documentId": "1455",
          |                "documentDate": "2018-03-29",
          |                "documentText": "ITSA- Bal Charge",
          |                "documentDueDate": "2020-04-15",
          |                "documentDescription": "document Description",
          |                "totalAmount": 45552768.79,
          |                "documentOutstandingAmount": 297873.46,
          |                "lastClearingDate": "2018-04-15",
          |                "lastClearingReason": "last Clearing Reason",
          |                "lastClearedAmount": 589958.83,
          |                "statisticalFlag": false,
          |                "paymentLot": 81203010024,
          |                "paymentLotItem": "000001",
          |                "accruingInterestAmount": 1000.9,
          |                "interestRate": 1000.9,
          |                "interestFromDate": "2021-01-11",
          |                "interestEndDate": "2021-04-11",
          |                "latePaymentInterestID": "1234567890123456",
          |                "latePaymentInterestAmount": 1000.67,
          |                "lpiWithDunningBlock": 1000.23,
          |                "interestOutstandingAmount": 1000.34
          |              }
          |            ],
          |            "financialDetails": [
          |              {
          |                "taxYear": "2017",
          |                "documentId": 1.2345678901234568e+28,
          |                "chargeType": "PAYE",
          |                "mainType": "2100",
          |                "periodKey": "13RL",
          |                "periodKeyDescription": "abcde",
          |                "taxPeriodFrom": "2018-08-13",
          |                "taxPeriodTo": "2018-08-14",
          |                "businessPartner": "6622334455",
          |                "contractAccountCategory": "02",
          |                "contractAccount": "X",
          |                "contractObjectType": "ABCD",
          |                "contractObject": "00000003000000002757",
          |                "sapDocumentNumber": "1040000872",
          |                "sapDocumentNumberItem": "XM00",
          |                "chargeReference": "XM002610011594",
          |                "mainTransaction": "1234",
          |                "subTransaction": "5678",
          |                "originalAmount": 10000,
          |                "outstandingAmount": 10000,
          |                "clearedAmount": 10000,
          |                "accruedInterest": 10000,
          |                "items": [
          |                  {
          |                    "subItem": "001",
          |                    "dueDate": "2018-08-13",
          |                    "amount": 10000,
          |                    "clearingDate": "2018-08-13",
          |                    "clearingReason": "01",
          |                    "outgoingPaymentMethod": "outgoing Payment",
          |                    "paymentLock": "paymentLock",
          |                    "clearingLock": "clearingLock",
          |                    "interestLock": "interestLock",
          |                    "dunningLock": "dunningLock",
          |                    "returnFlag": true,
          |                    "paymentReference": "Ab12453535",
          |                    "paymentAmount": 10000,
          |                    "paymentMethod": "Payment",
          |                    "paymentLot": 81203010024,
          |                    "paymentLotItem": "000001",
          |                    "clearingSAPDocument": "3350000253",
          |                    "codingInitiationDate": "2021-01-11",
          |                    "statisticalDocument": "S",
          |                    "DDCollectionInProgress": true,
          |                    "returnReason": "ABCA"
          |                  }
          |                ]
          |              }
          |            ]
          |          }""".stripMargin)


      when(mockGetFinancialDetailsConnector.getFinancialDetailsForAPI(any(), any(), any(),any(),any(),any(),any(),any(),any(),any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, sampleAPI1911Response.toString)))
      val result = controller.getFinancialDetails(vrn ="123456789",
        docNumber = None,
        dateFrom = None,
        dateTo = None,
        onlyOpenItems = true,
        includeStatistical = false,
        includeLocks = false,
        calculateAccruedInterest = false,
        removePOA = false,
        customerPaymentInformation = false)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe sampleAPI1911Response
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data" in new Setup(true) {
      when(mockGetFinancialDetailsConnector.getFinancialDetailsForAPI(any(), any(), any(),any(),any(),any(),any(),any(),any(),any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, "NOT_FOUND")))

      val result = controller.getFinancialDetails(vrn ="123456789",
        docNumber = None,
        dateFrom = None,
        dateTo = None,
        onlyOpenItems = true,
        includeStatistical = false,
        includeLocks = false,
        calculateAccruedInterest = false,
        removePOA = false,
        customerPaymentInformation = false)(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
    }

    s"return the status from EIS when the call returns a non 200 or 404 status" in new Setup(true) {
      when(mockGetFinancialDetailsConnector.getFinancialDetailsForAPI(any(), any(), any(),any(),any(),any(),any(),any(),any(),any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")))

      val result = controller.getFinancialDetails(vrn ="123456789",
        docNumber = None,
        dateFrom = None,
        dateTo = None,
        onlyOpenItems = true,
        includeStatistical = false,
        includeLocks = false,
        calculateAccruedInterest = false,
        removePOA = false,
        customerPaymentInformation = false)(fakeRequest)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "getPenaltyDetails" should {
    s"return OK (${Status.OK}) when a JSON payload is received from EIS" in new Setup(isFSEnabled = true) {
      val sampleAPI1812Response = Json.parse(
        """
          |{
          | "totalisations": {
          |   "LSPTotalValue": 200,
          |   "penalisedPrincipalTotal": 2000,
          |   "LPPPostedTotal": 165.25,
          |   "LPPEstimatedTotal": 15.26,
          |   "LPIPostedTotal": 1968.2,
          |   "LPIEstimatedTotal": 7
          | },
          | "lateSubmissionPenalty": {
          |   "summary": {
          |     "activePenaltyPoints": 10,
          |     "inactivePenaltyPoints": 12,
          |     "regimeThreshold": 10,
          |     "penaltyChargeAmount": 684.25
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
          |           "appealLevel": "01"
          |         }
          |       ],
          |       "chargeDueDate": "2022-10-30",
          |       "chargeOutstandingAmount": 200,
          |       "chargeAmount": 200
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
          |         "appealLevel": "01"
          |       }],
          |       "principalChargeBillingFrom": "2022-10-30",
          |       "principalChargeBillingTo": "2022-10-30",
          |       "principalChargeDueDate": "2022-10-30",
          |       "communicationsDate": "2022-10-30",
          |       "penaltyAmountOutstanding": 99.99,
          |       "penaltyAmountPaid": 1001.45,
          |       "LPP1LRDays": "15",
          |       "LPP1HRDays": "31",
          |       "LPP2Days": "31",
          |       "LPP1HRCalculationAmount": 99.99,
          |       "LPP1LRCalculationAmount": 99.99,
          |       "LPP2Percentage": 4.00,
          |       "LPP1LRPercentage": 2.00,
          |       "LPP1HRPercentage": 2.00,
          |       "penaltyChargeDueDate": "2022-10-30"
          |   }]
          | }
          |}
          |""".stripMargin)
      when(mockGetPenaltyDetailsConnector.getPenaltyDetailsForAPI(any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, sampleAPI1812Response.toString)))
      val result = controller.getPenaltyDetails(vrn = "123456789", dateLimit = Some("02"))(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe sampleAPI1812Response
    }
    s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data" in new Setup(true) {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetailsForAPI(any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(NOT_FOUND, "NOT_FOUND")))

      val result = controller.getPenaltyDetails(vrn = "123456789", dateLimit = None)(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
    }

    s"return the status from EIS when the call returns a non 200 or 404 status" in new Setup(true) {
      when(mockGetPenaltyDetailsConnector.getPenaltyDetailsForAPI(any(), any())(any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")))

      val result = controller.getPenaltyDetails(vrn = "123456789", dateLimit = None)(fakeRequest)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

  }
}
