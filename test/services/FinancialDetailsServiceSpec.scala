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

import base.{LogCapturing, SpecBase}
import config.featureSwitches.{CallAPI1811HIP, FeatureSwitching}
import connectors.getFinancialDetails.{FinancialDetailsConnector, FinancialDetailsHipConnector}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models._
import models.getFinancialDetails.FinancialDetails
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.Configuration
import play.api.http.Status.IM_A_TEAPOT
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Logger.logger

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsServiceSpec extends SpecBase with FeatureSwitching with LogCapturing {
  implicit val ec: ExecutionContext  = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier     = HeaderCarrier()
  implicit val config: Configuration = mock(classOf[Configuration])

  val vatcEnrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("VATC"), IdType("VRN"), Id("123456789"))
  val itsaEnrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("ITSA"), IdType("NINO"), Id("AA000000A"))

  val mockGetFinancialDetailsConnector: FinancialDetailsConnector       = mock(classOf[FinancialDetailsConnector])
  val mockGetFinancialDetailsHipConnector: FinancialDetailsHipConnector = mock(classOf[FinancialDetailsHipConnector])

  def buildGetFinancialDetailsMock(enrolmentKey: AgnosticEnrolmentKey,
                                   upstreamService: String,
                                   mockedResponse: Future[FinancialDetailsResponse]): OngoingStubbing[Future[FinancialDetailsResponse]] =
    if (upstreamService == "HIP") {
      when(mockGetFinancialDetailsHipConnector.getFinancialDetails(ArgumentMatchers.eq(enrolmentKey), ArgumentMatchers.any())(any()))
        .thenReturn(mockedResponse)
    } else {
      when(mockGetFinancialDetailsConnector.getFinancialDetails(ArgumentMatchers.eq(enrolmentKey), ArgumentMatchers.any())(any()))
        .thenReturn(mockedResponse)
    }
  def buildGetFinancialDetailsForApiMock(enrolmentKey: AgnosticEnrolmentKey,
                                         upstreamService: String,
                                         mockedResponse: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] =
    if (upstreamService == "HIP") {
      when(
        mockGetFinancialDetailsHipConnector.getFinancialDetailsForAPI(
          ArgumentMatchers.eq(enrolmentKey),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(any()))
        .thenReturn(mockedResponse)
    } else {
      when(
        mockGetFinancialDetailsConnector.getFinancialDetailsForAPI(
          ArgumentMatchers.eq(enrolmentKey),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(any()))
        .thenReturn(mockedResponse)
    }

  class Setup(upstreamService: String) {
    val service = new FinancialDetailsService(mockGetFinancialDetailsConnector, mockGetFinancialDetailsHipConnector)
    reset(mockGetFinancialDetailsConnector)
    reset(config)
    sys.props -= TIME_MACHINE_NOW

    if (upstreamService == "HIP") enableFeatureSwitch(CallAPI1811HIP) else disableFeatureSwitch(CallAPI1811HIP)
  }

  val mockGetFinancialDetailsResponseAsModel: FinancialDetails = FinancialDetails(
    documentDetails = Some(
      Seq(getFinancialDetails.DocumentDetails(
        chargeReferenceNumber = None,
        documentOutstandingAmount = Some(0.00),
        lineItemDetails = Some(Seq(getFinancialDetails.LineItemDetails(None))),
        documentTotalAmount = Some(100.00),
        issueDate = Some(LocalDate.of(2023, 1, 1))
      ))),
    totalisation = Some(
      FinancialDetailsTotalisation(
        regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
      ))
  )

  val getFinancialDetailsIFResponseBody: String =
    """
      |{
      | "getFinancialData": {
      |   "financialDetails":{
      |     "totalisation": {
      |       "regimeTotalisation": {
      |         "totalAccountOverdue": 1000.0,
      |         "totalAccountNotYetDue": 250.0,
      |         "totalAccountCredit": 40.0,
      |         "totalAccountBalance": 1210
      |       },
      |       "targetedSearch_SelectionCriteriaTotalisation": {
      |         "totalOverdue": 100.0,
      |         "totalNotYetDue": 0.0,
      |         "totalBalance": 100.0,
      |         "totalCredit": 10.0,
      |         "totalCleared": 50
      |       },
      |       "additionalReceivableTotalisations": {
      |         "totalAccountPostedInterest": 12.34,
      |         "totalAccountAccruingInterest": 43.21
      |       }
      |     },
      |     "documentDetails": [
      |     {
      |      "documentNumber": "187346702498",
      |      "documentType": "TRM New Charge",
      |      "chargeReferenceNumber": "XM002610011594",
      |      "businessPartnerNumber": "100893731",
      |      "contractAccountNumber": "900726630",
      |      "contractAccountCategory": "VAT",
      |      "contractObjectNumber": "104920928302302",
      |      "contractObjectType": "ZVAT",
      |      "postingDate": "2022-01-01",
      |      "issueDate": "2022-01-01",
      |      "documentTotalAmount": "100.0",
      |      "documentClearedAmount": "100.0",
      |      "documentOutstandingAmount": "543.21",
      |      "documentLockDetails": {
      |        "lockType": "Payment",
      |        "lockStartDate": "2022-01-01",
      |        "lockEndDate": "2022-01-01"
      |      },
      |      "documentInterestTotals": {
      |        "interestPostedAmount": "13.12",
      |        "interestPostedChargeRef": "XB001286323438",
      |        "interestAccruingAmount": 12.1
      |      },
      |      "documentPenaltyTotals": [
      |        {
      |          "penaltyType": "LPP1",
      |          "penaltyStatus": "POSTED",
      |          "penaltyAmount": "10.01",
      |          "postedChargeReference": "XR00123933492"
      |        }
      |      ],
      |      "lineItemDetails": [
      |        {
      |          "itemNumber": "0001",
      |          "subItemNumber": "003",
      |          "mainTransaction": "4703",
      |          "subTransaction": "1000",
      |          "chargeDescription": "VAT Return",
      |          "periodFromDate": "2022-01-01",
      |          "periodToDate": "2022-01-31",
      |          "periodKey": "22A1",
      |          "netDueDate": "2022-02-08",
      |          "formBundleNumber": "125435934761",
      |          "statisticalKey": "1",
      |          "amount": "3420.0",
      |          "clearingDate": "2022-02-09",
      |          "clearingReason": "Payment at External Payment Collector Reported",
      |          "clearingDocument": "719283701921",
      |          "outgoingPaymentMethod": "B",
      |          "ddCollectionInProgress": "true",
      |          "lineItemLockDetails": [
      |            {
      |              "lockType": "Payment",
      |              "lockStartDate": "2022-01-01",
      |              "lockEndDate": "2022-01-01"
      |            }
      |          ],
      |          "lineItemInterestDetails": {
      |            "interestKey": "String",
      |            "currentInterestRate": "-999.999999",
      |            "interestStartDate": "1920-02-29",
      |            "interestPostedAmount": "-99999999999.99",
      |            "interestAccruingAmount": -99999999999.99
      |          }
      |      }]
      |    }
      |  ]
      |}
      |}
      |}
      |""".stripMargin
  val getFinancialDetailsHIPResponseBody: String =
    """
      |{
      | "success": {
      |   "processingDate": "2023-11-28T10:15:10Z",
      |   "financialData": {
      |     "totalisation": {
      |       "regimeTotalisation": {
      |         "totalAccountOverdue": 1000.0,
      |         "totalAccountNotYetDue": 250.0,
      |         "totalAccountCredit": 40.0,
      |         "totalAccountBalance": 1210
      |       },
      |       "targetedSearch_SelectionCriteriaTotalisation": {
      |         "totalOverdue": 100.0,
      |         "totalNotYetDue": 0.0,
      |         "totalBalance": 100.0,
      |         "totalCredit": 10.0,
      |         "totalCleared": 50
      |       },
      |       "additionalReceivableTotalisations": {
      |         "totalAccountPostedInterest": 12.34,
      |         "totalAccountAccruingInterest": 43.21
      |       }
      |     },
      |     "documentDetails": [
      |     {
      |      "documentNumber": "187346702498",
      |      "documentType": "TRM New Charge",
      |      "chargeReferenceNumber": "XM002610011594",
      |      "businessPartnerNumber": "100893731",
      |      "contractAccountNumber": "900726630",
      |      "contractAccountCategory": "VAT",
      |      "contractObjectNumber": "104920928302302",
      |      "contractObjectType": "ZVAT",
      |      "postingDate": "2022-01-01",
      |      "issueDate": "2022-01-01",
      |      "documentTotalAmount": "100.0",
      |      "documentClearedAmount": "100.0",
      |      "documentOutstandingAmount": "543.21",
      |      "documentLockDetails": {
      |        "lockType": "Payment",
      |        "lockStartDate": "2022-01-01",
      |        "lockEndDate": "2022-01-01"
      |      },
      |      "documentInterestTotals": {
      |        "interestPostedAmount": "13.12",
      |        "interestPostedChargeRef": "XB001286323438",
      |        "interestAccruingAmount": 12.1
      |      },
      |      "documentPenaltyTotals": [
      |        {
      |          "penaltyType": "LPP1",
      |          "penaltyStatus": "POSTED",
      |          "penaltyAmount": "10.01",
      |          "postedChargeReference": "XR00123933492"
      |        }
      |      ],
      |      "lineItemDetails": [
      |        {
      |          "itemNumber": "0001",
      |          "subItemNumber": "003",
      |          "mainTransaction": "4703",
      |          "subTransaction": "1000",
      |          "chargeDescription": "VAT Return",
      |          "periodFromDate": "2022-01-01",
      |          "periodToDate": "2022-01-31",
      |          "periodKey": "22A1",
      |          "netDueDate": "2022-02-08",
      |          "formBundleNumber": "125435934761",
      |          "statisticalKey": "1",
      |          "amount": "3420.0",
      |          "clearingDate": "2022-02-09",
      |          "clearingReason": "Payment at External Payment Collector Reported",
      |          "clearingDocument": "719283701921",
      |          "outgoingPaymentMethod": "B",
      |          "ddCollectionInProgress": "true",
      |          "lineItemLockDetails": [
      |            {
      |              "lockType": "Payment",
      |              "lockStartDate": "2022-01-01",
      |              "lockEndDate": "2022-01-01"
      |            }
      |          ],
      |          "lineItemInterestDetails": {
      |            "interestKey": "String",
      |            "currentInterestRate": "-999.999999",
      |            "interestStartDate": "1920-02-29",
      |            "interestPostedAmount": "-99999999999.99",
      |            "interestAccruingAmount": -99999999999.99
      |          }
      |      }]
      |    }
      |  ]
      |}
      |}
      |}
      |""".stripMargin
  val getFinancialDetailsInvalidResponseBody: String =
    """
      |{
      | "success": {
      |   "processingDate": "2023-11-28T10:15:10Z",
      |   "getFinancialData": {
      |     "totalisation": {
      |       "regimeTotalisation": {
      |         "totalAccountOverdue": 1000.0,
      |         "totalAccountNotYetDue": 250.0,
      |         "totalAccountCredit": 40.0,
      |         "totalAccountBalance": 1210
      |       },
      |       "targetedSearch_SelectionCriteriaTotalisation": {
      |         "totalOverdue": 100.0,
      |         "totalNotYetDue": 0.0,
      |         "totalBalance": 100.0,
      |         "totalCredit": 10.0,
      |         "totalCleared": 50
      |       },
      |       "additionalReceivableTotalisations": {
      |         "totalAccountPostedInterest": 12.34,
      |         "totalAccountAccruingInterest": 43.21
      |       }
      |     },
      |     "documentDetails": [
      |     {
      |      "documentNumber": "187346702498",
      |      "documentType": "TRM New Charge",
      |      "chargeReferenceNumber": "XM002610011594",
      |      "businessPartnerNumber": "100893731",
      |      "contractAccountNumber": "900726630",
      |      "contractAccountCategory": "VAT",
      |      "contractObjectNumber": "104920928302302",
      |      "contractObjectType": "ZVAT",
      |      "postingDate": "2022-01-01",
      |      "issueDate": "2022-01-01",
      |      "documentTotalAmount": "100.0",
      |      "documentClearedAmount": "100.0",
      |      "documentOutstandingAmount": "543.21",
      |      "documentLockDetails": {
      |        "lockType": "Payment",
      |        "lockStartDate": "2022-01-01",
      |        "lockEndDate": "2022-01-01"
      |      },
      |      "documentInterestTotals": {
      |        "interestPostedAmount": "13.12",
      |        "interestPostedChargeRef": "XB001286323438",
      |        "interestAccruingAmount": 12.1
      |      },
      |      "documentPenaltyTotals": [
      |        {
      |          "penaltyType": "LPP1",
      |          "penaltyStatus": "POSTED",
      |          "penaltyAmount": "10.01",
      |          "postedChargeReference": "XR00123933492"
      |        }
      |      ],
      |      "lineItemDetails": [
      |        {
      |          "itemNumber": "0001",
      |          "subItemNumber": "003",
      |          "mainTransaction": "4703",
      |          "subTransaction": "1000",
      |          "chargeDescription": "VAT Return",
      |          "periodFromDate": "2022-01-01",
      |          "periodToDate": "2022-01-31",
      |          "periodKey": "22A1",
      |          "netDueDate": "2022-02-08",
      |          "formBundleNumber": "125435934761",
      |          "statisticalKey": "1",
      |          "amount": "3420.0",
      |          "clearingDate": "2022-02-09",
      |          "clearingReason": "Payment at External Payment Collector Reported",
      |          "clearingDocument": "719283701921",
      |          "outgoingPaymentMethod": "B",
      |          "ddCollectionInProgress": "true",
      |          "lineItemLockDetails": [
      |            {
      |              "lockType": "Payment",
      |              "lockStartDate": "2022-01-01",
      |              "lockEndDate": "2022-01-01"
      |            }
      |          ],
      |          "lineItemInterestDetails": {
      |            "interestKey": "String",
      |            "currentInterestRate": "-999.999999",
      |            "interestStartDate": "1920-02-29",
      |            "interestPostedAmount": "-99999999999.99",
      |            "interestAccruingAmount": -99999999999.99
      |          }
      |      }]
      |    }
      |  ]
      |}
      |}
      |}
      |""".stripMargin
  val financialDetailsForVatApi: JsValue = Json.parse(
    """
      |{
      |  "totalisation": {
      |    "regimeTotalisation": {
      |      "totalAccountOverdue": 1000.0
      |    },
      |    "interestTotalisations": {
      |      "totalAccountPostedInterest": 12.34,
      |      "totalAccountAccruingInterest": 43.21
      |    }
      |  },
      |  "documentDetails": [
      |    {
      |      "chargeReferenceNumber": "XM002610011594",
      |      "documentOutstandingAmount": 543.21,
      |      "documentTotalAmount": 100,
      |      "lineItemDetails": [
      |        {
      |          "mainTransaction": "4703"
      |        }
      |      ],
      |      "issueDate": "2022-01-01"
      |    }
      |  ]
      |}
      |""".stripMargin)

  private val testCases = Seq(
    ("IF", vatcEnrolmentKey),
    ("HIP", vatcEnrolmentKey),
    ("HIP", itsaEnrolmentKey)
  )

  "getFinancialDetails" when {
    testCases.foreach { case (upstreamService, enrolmentKey) =>
      val errorLogPrefix = s"[FinancialDetailsService][getFinancialDetails][$enrolmentKey]"

      s"calling the $upstreamService service for ${enrolmentKey.regime} regime" should {
        s"return a $FinancialDetailsSuccessResponse from the connector when successful" in new Setup(upstreamService) {
          buildGetFinancialDetailsMock(
            enrolmentKey,
            upstreamService,
            Future.successful(Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))

          val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

          result shouldBe Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))
        }

        s"return a $FinancialDetailsSuccessResponse from the connector when successful (with the time machine date)" in new Setup(upstreamService) {
          setTimeMachineDate(Some(LocalDateTime.of(2024, 1, 1, 0, 0, 0)))
          buildGetFinancialDetailsMock(
            enrolmentKey,
            upstreamService,
            Future.successful(Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))

          val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

          result.isRight shouldBe true
          result.toOption.get shouldBe FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
        }

        s"return $FinancialDetailsNoContent from the connector when the response body contains NO_DATA_FOUND" in new Setup(upstreamService) {
          buildGetFinancialDetailsMock(enrolmentKey, upstreamService, Future.successful(Left(FinancialDetailsNoContent)))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsNoContent)
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Got a 404 response and no data was found for GetFinancialDetails call")
          }
        }

        s"return $FinancialDetailsMalformed from the connector when the response body is malformed" in new Setup(upstreamService) {
          buildGetFinancialDetailsMock(enrolmentKey, upstreamService, Future.successful(Left(FinancialDetailsMalformed)))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsMalformed)
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Failed to parse HTTP response into model for $enrolmentKey")
          }
        }

        s"return $FinancialDetailsFailureResponse from the connector when an unknown status is returned" in new Setup(upstreamService) {
          buildGetFinancialDetailsMock(enrolmentKey, upstreamService, Future.successful(Left(FinancialDetailsFailureResponse(IM_A_TEAPOT))))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsFailureResponse(IM_A_TEAPOT))
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Unknown status returned from connector for $enrolmentKey")
          }
        }

        s"throw an exception from the connector when something unknown has happened" in new Setup(upstreamService) {
          buildGetFinancialDetailsMock(enrolmentKey, upstreamService, Future.failed(new Exception("Something has gone wrong.")))

          val result: Exception = intercept[Exception](await(service.getFinancialDetails(enrolmentKey, None)))

          result.getMessage shouldBe "Something has gone wrong."
        }
      }
    }
  }

  "getFinancialDetailsForAPI" when {
    testCases.foreach { case (upstreamService, enrolmentKey) =>
      val errorLogPrefix = s"[FinancialDetailsService][getFinancialDetailsForAPI][$enrolmentKey]"

      s"calling the $upstreamService service for ${enrolmentKey.regime} regime" when {
        "a 200 HttpResponse is returned from the connector" should {
          s"return the HttpResponse with the FinancialDetails model extracted from its $upstreamService wrapper" in new Setup(upstreamService) {
            private val successResponseBody = if (upstreamService == "HIP") getFinancialDetailsHIPResponseBody else getFinancialDetailsIFResponseBody
            buildGetFinancialDetailsForApiMock(enrolmentKey, upstreamService, Future.successful(HttpResponse(200, successResponseBody)))

            val result: HttpResponse =
              await(service.getFinancialDetailsForAPI(enrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None))

            result.status shouldBe 200
            result.json shouldBe financialDetailsForVatApi
          }
          s"return a 500 HttpResponse when the response body is not parsable as the expected format from $upstreamService" in new Setup(upstreamService) {
            buildGetFinancialDetailsForApiMock(enrolmentKey, upstreamService, Future.successful(HttpResponse(200, getFinancialDetailsInvalidResponseBody)))

            val result: HttpResponse =
              await(service.getFinancialDetailsForAPI(enrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None))

            result.status shouldBe 500
            result.json shouldBe Json.parse("""{"jsonValidationError":"FinancialDetailsMalformed"}""")
          }
        }

        Seq(BAD_REQUEST, UNAUTHORIZED, NOT_FOUND, INTERNAL_SERVER_ERROR).foreach { errorStatus =>
          s"$errorStatus error is returned from the connector, should return error in HttpResponse" in new Setup(upstreamService) {
            buildGetFinancialDetailsForApiMock(enrolmentKey, upstreamService, Future.successful(HttpResponse(errorStatus, "{}")))

            val result: HttpResponse =
              await(service.getFinancialDetailsForAPI(enrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None))

            result.status shouldBe errorStatus
          }
        }

        "something unknown has happened, throw an exception" in new Setup(upstreamService) {
          buildGetFinancialDetailsForApiMock(enrolmentKey, upstreamService, Future.failed(new Exception("Something has gone wrong.")))

          val result: Exception = intercept[Exception](
            await(service.getFinancialDetailsForAPI(enrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None)))

          result.getMessage shouldBe "Something has gone wrong."
        }
      }
    }
  }
}
