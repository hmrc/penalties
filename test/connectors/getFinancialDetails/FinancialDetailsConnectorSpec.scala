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

package connectors.getFinancialDetails

import base.{LogCapturing, SpecBase}
import config.AppConfig
import config.featureSwitches.{CallAPI1811HIP, CallAPI1811Stub}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, FinancialDetailsHIP, LineItemDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.OngoingStubbing
import uk.gov.hmrc.http

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsConnectorSpec extends SpecBase with LogCapturing {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient    = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig      = mock(classOf[AppConfig])

  val regime  = "VATC"
  val idType  = "VRN"
  val idValue = "123456789"

  val urlIF  = s"/penalty/financial-data/VRN/$idValue/VATC"
  val urlHIP = "/RESTAdapter/cross-regime/taxpayer/financial-data/query"

  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    regime = Regime(regime),
    idType = IdType(idType),
    id = Id(idValue)
  )
  val financialDetailsBody: JsObject = Json.obj(
    "taxRegime" -> regime,
    "taxpayerInformation" -> Json.obj(
      "idType" -> idType,
      "idNumber" -> idValue
    )
  )

  when(mockAppConfig.queryParametersForGetFinancialDetails).thenReturn("")
  when(mockAppConfig.addDateRangeQueryParameters()).thenReturn("")

  val financialDetailsModelAPI1811: FinancialDetails = FinancialDetails(
    documentDetails = Some(
      Seq(DocumentDetails(
        chargeReferenceNumber = None,
        documentOutstandingAmount = Some(0.00),
        lineItemDetails = Some(Seq(LineItemDetails(None))),
        documentTotalAmount = Some(100.00),
        issueDate = Some(LocalDate.of(2023, 1, 1))
      ))),
    totalisation = Some(
      FinancialDetailsTotalisation(
        regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
      ))
  )
  val financialDetailsHipModelAPI1811: FinancialDetailsHIP =
    FinancialDetailsHIP(processingDate = "2025-05-06", financialData = financialDetailsModelAPI1811)

  trait SetupHIP {
    val connector                            = new FinancialDetailsConnector(mockHttpClient, mockAppConfig)
    when(mockAppConfig.isEnabled(ArgumentMatchers.eq(CallAPI1811HIP))).thenReturn(true)
    when(mockAppConfig.getRegimeFinancialDetailsUrl(Id(idValue))).thenReturn(urlHIP)

    def buildErrorResponseMock(errorResponse: Exception): OngoingStubbing[Future[GetFinancialDetailsResponse]] = {
      when(mockHttpClient.POST[JsObject, GetFinancialDetailsResponse](
        ArgumentMatchers.eq(urlHIP),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(errorResponse))
    }
  }
  trait SetupIF {
    val connector                            = new FinancialDetailsConnector(mockHttpClient, mockAppConfig)
    when(mockAppConfig.isEnabled(ArgumentMatchers.eq(CallAPI1811HIP))).thenReturn(false)
    when(mockAppConfig.getRegimeFinancialDetailsUrl(Id(idValue))).thenReturn(urlIF)

    def buildErrorResponseMock(errorResponse: Exception): OngoingStubbing[Future[GetFinancialDetailsResponse]] = {
      when(
        mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq(urlIF), ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())).thenReturn(Future.failed(errorResponse))
    }
  }

//  "getFinancialDetails" when {
//    "calling the HIP endpoint" should {
//      "return the financial details in a Right when the call succeeds" in new SetupHIP {
//        val successResponse: GetFinancialDetailsSuccess = GetFinancialDetailsHipSuccessResponse(financialDetailsHipModelAPI1811)
//        when(mockHttpClient.POST[JsObject, GetFinancialDetailsResponse](
//          ArgumentMatchers.eq(urlHIP),
//          ArgumentMatchers.eq(financialDetailsBody),
//          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Right(successResponse)))
//
//        val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))
//        result shouldBe Right(GetFinancialDetailsHipSuccessResponse(financialDetailsHipModelAPI1811))
//      }
//      "return a GetFinancialDetailsFailureResponse with upstream error status and log error message" when {
//        "a 4XX is returned from upstream" in new SetupHIP {
//          val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse.apply("", BAD_REQUEST)
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
//            result shouldBe Left(GetFinancialDetailsFailureResponse(BAD_REQUEST))
//          }
//        }
//        "a 5XX is returned from upstream" in new SetupHIP {
//          val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse.apply("", BAD_GATEWAY)
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1811_API.toString)) shouldBe true
//            result shouldBe Left(GetFinancialDetailsFailureResponse(BAD_GATEWAY))
//          }
//        }
//        "an unknown exception is returned from upstream" in new SetupHIP {
//          val errorResponse: Exception = new Exception("An unknown error")
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1811_API.toString)) shouldBe true
//            result shouldBe Left(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))
//          }
//        }
//      }
//    }
//
//    "calling the IF endpoint" should {
//      "return the financial details in a Right when the call succeeds" in new SetupIF {
//        val successResponse: GetFinancialDetailsSuccess = GetFinancialDetailsSuccessResponse(financialDetailsModelAPI1811)
//        when(
//          mockHttpClient.GET[GetFinancialDetailsResponse](ArgumentMatchers.eq(urlIF), ArgumentMatchers.any(), ArgumentMatchers.any())(
//            ArgumentMatchers.any(),
//            ArgumentMatchers.any(),
//            ArgumentMatchers.any())).thenReturn(Future.successful(Right(successResponse)))
//
//        val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))
//        result shouldBe Right(GetFinancialDetailsSuccessResponse(financialDetailsModelAPI1811))
//      }
//      "return a GetFinancialDetailsFailureResponse with upstream error status and log error message" when {
//        "a 4XX is returned from upstream" in new SetupIF {
//          val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse.apply("", BAD_REQUEST)
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
//            result shouldBe Left(GetFinancialDetailsFailureResponse(BAD_REQUEST))
//          }
//        }
//        "a 5XX is returned from upstream" in new SetupIF {
//          val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse.apply("", BAD_GATEWAY)
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1811_API.toString)) shouldBe true
//            result shouldBe Left(GetFinancialDetailsFailureResponse(BAD_GATEWAY))
//          }
//        }
//        "an unknown exception is returned from upstream" in new SetupIF {
//          val errorResponse: Exception = new Exception("An unknown error")
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1811_API.toString)) shouldBe true
//            result shouldBe Left(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))
//          }
//        }
//      }
//    }
//  }

  "getFinancialDetailsForAPI" when {
    "calling the HIP endpoint" should {
      "return a 200 response when the call succeeds" when {
        "there are no extra parameters are passed in the request body" in new SetupHIP {
          val successResponse: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(financialDetailsHipModelAPI1811), headers = Map.empty)
          when(mockHttpClient.POST[JsObject, HttpResponse](
            ArgumentMatchers.eq(urlHIP),
            ArgumentMatchers.any(),
            //            ArgumentMatchers.eq(financialDetailsBody),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(successResponse))

          val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
          result.isInstanceOf[HttpResponse] shouldBe true
          result.status shouldBe OK
        }
        "all extra parameters are in use in the request body" in new SetupHIP {
          val successResponse: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(financialDetailsHipModelAPI1811), headers = Map.empty)
          when(mockHttpClient.POST[JsObject, HttpResponse](
            ArgumentMatchers.eq(urlHIP),
            ArgumentMatchers.any(),
//            ArgumentMatchers.eq(financialDetailsBody),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(successResponse))

          val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
            enrolmentKey = vrn123456789,
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
            addAccruingInterestDetails = Some(true)
          )(HeaderCarrier()))
          result.isInstanceOf[HttpResponse] shouldBe true
          result.status shouldBe OK
        }
      }
      "return a HttpResponse with upstream error status and log error message" when {
        "a 4XX is returned from upstream" in new SetupHIP {
          val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse.apply("", BAD_REQUEST)
          buildErrorResponseMock(errorResponse)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
            logs.exists(_.getMessage.contains(s"Received $BAD_REQUEST status from API 1811 call")) shouldBe true
            result.isInstanceOf[HttpResponse] shouldBe true
            result.status shouldBe BAD_REQUEST
          }
        }
        "a 5XX is returned from upstream" in new SetupHIP {
          val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse.apply("", BAD_GATEWAY)
          buildErrorResponseMock(errorResponse)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
            logs.exists(_.getMessage.contains(s"Received $BAD_GATEWAY status from API 1811 call")) shouldBe true
            result.isInstanceOf[HttpResponse] shouldBe true
            result.status shouldBe BAD_GATEWAY
          }
        }
        "an unknown exception is returned from upstream" in new SetupHIP {
          val errorResponse: Exception = new Exception("An unknown error")
          buildErrorResponseMock(errorResponse)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
            logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1811_API.toString)) shouldBe true
            result.isInstanceOf[HttpResponse] shouldBe true
            result.status shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }

//    "calling the IF endpoint" should {
//      "return a 200 response when the call succeeds" when {
//        "there are no query parameters" in new SetupIF {
//          val successResponse: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(financialDetailsModelAPI1811), headers = Map.empty)
//
//          when(
//            mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq(urlIF), ArgumentMatchers.any(), ArgumentMatchers.any())(
//              ArgumentMatchers.any(),
//              ArgumentMatchers.any(),
//              ArgumentMatchers.any())).thenReturn(Future.successful(successResponse))
//
//          val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
//          result.isInstanceOf[HttpResponse] shouldBe true
//          result.status shouldBe OK
//        }
//        "all query parameters are in use" in new SetupIF {
//          val successResponse: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(financialDetailsModelAPI1811), headers = Map.empty)
//          val urlWithQueryParameters: String = urlIF + "?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12" +
//            s"&includeClearedItems=false&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true" +
//            s"&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true"
//          when(
//            mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq(urlWithQueryParameters), ArgumentMatchers.any(), ArgumentMatchers.any())(
//              ArgumentMatchers.any(),
//              ArgumentMatchers.any(),
//              ArgumentMatchers.any())).thenReturn(Future.successful(successResponse))
//
//          val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
//            enrolmentKey = vrn123456789,
//            searchType = Some("CHGREF"),
//            searchItem = Some("XC00178236592"),
//            dateType = Some("BILLING"),
//            dateFrom = Some("2020-10-03"),
//            dateTo = Some("2021-07-12"),
//            includeClearedItems = Some(false),
//            includeStatisticalItems = Some(true),
//            includePaymentOnAccount = Some(true),
//            addRegimeTotalisation = Some(false),
//            addLockInformation = Some(true),
//            addPenaltyDetails = Some(true),
//            addPostedInterestDetails = Some(true),
//            addAccruingInterestDetails = Some(true)
//          )(HeaderCarrier()))
//          result.isInstanceOf[HttpResponse] shouldBe true
//          result.status shouldBe OK
//        }
//      }
//      "return a 200 response when the call succeeds" in new SetupIF {
//        val successResponse: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(financialDetailsModelAPI1811), headers = Map.empty)
//        when(
//          mockHttpClient.GET[HttpResponse](ArgumentMatchers.eq(urlIF), ArgumentMatchers.any(), ArgumentMatchers.any())(
//            ArgumentMatchers.any(),
//            ArgumentMatchers.any(),
//            ArgumentMatchers.any())).thenReturn(Future.successful(successResponse))
//
//        val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
//        result.isInstanceOf[HttpResponse] shouldBe true
//        result.status shouldBe OK
//      }
//      "return a HttpResponse with upstream error status and log error message" when {
//        "a 4XX is returned from upstream" in new SetupIF {
//          val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse.apply("", BAD_REQUEST)
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(s"Received $BAD_REQUEST status from API 1811 call")) shouldBe true
//            result.isInstanceOf[HttpResponse] shouldBe true
//            result.status shouldBe BAD_REQUEST
//          }
//        }
//        "a 5XX is returned from upstream" in new SetupIF {
//          val errorResponse: UpstreamErrorResponse = UpstreamErrorResponse.apply("", BAD_GATEWAY)
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(s"Received $BAD_GATEWAY status from API 1811 call")) shouldBe true
//            result.isInstanceOf[HttpResponse] shouldBe true
//            result.status shouldBe BAD_GATEWAY
//          }
//        }
//        "an unknown exception is returned from upstream" in new SetupIF {
//          val errorResponse: Exception = new Exception("An unknown error")
//          buildErrorResponseMock(errorResponse)
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)(HeaderCarrier()))
//            logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1811_API.toString)) shouldBe true
//            result.isInstanceOf[HttpResponse] shouldBe true
//            result.status shouldBe INTERNAL_SERVER_ERROR
//          }
//        }
//      }
//    }
  }

  "buildHipRequestForApiBody" should {
    "build the Json request body" which {
      "only contains the base body" when {
        "no other parameters are given" in {
          val result: JsObject = FinancialDetailsConnector.buildHipRequestForApiBody(vrn123456789, None, None, None, None, None, None, None, None, None, None, None, None, None)
          val expectedResult = Json.obj(
            "taxRegime" -> vrn123456789.regime.value,
            "taxpayerInformation" -> Json.obj(
              "idType" -> vrn123456789.idType.value,
              "idNumber" -> vrn123456789.id.value
            )
          )

          result shouldBe expectedResult
        }
        "some but not all required targetedSearch parameters are given" in {
          val resultWithoutSearchItem: JsObject = FinancialDetailsConnector.buildHipRequestForApiBody(vrn123456789, searchType = Some("CHGREF"),
            searchItem = None, None, None, None, None, None, None, None, None, None, None, None)
          val resultWithoutSearchType: JsObject = FinancialDetailsConnector.buildHipRequestForApiBody(vrn123456789, searchType = None,
            searchItem = Some("XC00178236592"), None, None, None, None, None, None, None, None, None, None, None)
          val expectedResult = Json.obj(
            "taxRegime" -> vrn123456789.regime.value,
            "taxpayerInformation" -> Json.obj(
              "idType" -> vrn123456789.idType.value,
              "idNumber" -> vrn123456789.id.value
            )
          )

          resultWithoutSearchItem shouldBe expectedResult
          resultWithoutSearchType shouldBe expectedResult
        }
      }
      "contains the base body plus targetedSearch when the correct targetedSearch parameters are given" in {
        val result: JsObject = FinancialDetailsConnector.buildHipRequestForApiBody(vrn123456789,
          searchType = Some("CHGREF"),
          searchItem = Some("XC00178236592"), None, None, None, None, None, None, None, None, None, None, None)
        val expectedResult = Json.obj(
          "taxRegime" -> vrn123456789.regime.value,
          "taxpayerInformation" -> Json.obj(
            "idType"   -> vrn123456789.idType.value,
            "idNumber" -> vrn123456789.id.value
          ),
          "targetedSearch" -> Json.obj(
            "searchType"   -> "CHGREF",
            "searchItem" -> "XC00178236592"
          )
        )

        result shouldBe expectedResult
      }
    }
  }
}
