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

package connectors.getFinancialDetails

import base.{LogCapturing, SpecBase}
import config.AppConfig
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, LineItemDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsConnectorSpec extends SpecBase with LogCapturing {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])

  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)

    val connector = new GetFinancialDetailsConnector(mockHttpClient, mockAppConfig)
    when(mockAppConfig.getFinancialDetailsUrl(Matchers.any())).thenReturn("/VRN/123456789/VATC")
    when(mockAppConfig.eiOutboundBearerToken).thenReturn("1234")
    when(mockAppConfig.eisEnvironment).thenReturn("asdf")
    when(mockAppConfig.queryParametersForGetFinancialDetails).thenReturn("?foo=bar")
    when(mockAppConfig.addDateRangeQueryParameters()).thenReturn("&bar=wizz")
  }

  val mockGetFinancialDetailsModelAPI1811: FinancialDetails = FinancialDetails(
    documentDetails = Some(Seq(DocumentDetails(
      chargeReferenceNumber = None,
      documentOutstandingAmount = Some(0.00),
      lineItemDetails = Some(Seq(LineItemDetails(None))),
      documentTotalAmount = Some(100.00),
      issueDate = Some(LocalDate.of(2023, 1, 1)))
    )),
    totalisation = Some(FinancialDetailsTotalisation(
      regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
      interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
    ))
  )

  "getFinancialDetails" should {
    "return a 200 when the call succeeds" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsModelAPI1811))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
      result.isRight shouldBe true
    }

    "pass custom parameters when provided" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?custom=value&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsModelAPI1811))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", Some("?custom=value"))(HeaderCarrier()))
      result.isRight shouldBe true
    }

    s"return a 404 when the call fails for Not Found" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.NOT_FOUND))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 400 when the call fails for Bad Request" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.BAD_REQUEST))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 409 when the call fails for Conflict" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.CONFLICT))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 422 when the call fails for Unprocessable Entity" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.UNPROCESSABLE_ENTITY))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 500 when the call fails for Internal Server Error" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 403 when the call fails" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.FORBIDDEN))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 503 when the call fails" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.SERVICE_UNAVAILABLE))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    "return a 500 when the call fails due to an UpstreamErrorResponse(5xx) exception" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("Something weird happened", INTERNAL_SERVER_ERROR)))

      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }

    "return a 400 when the call fails due to an UpstreamErrorResponse(4xx) exception" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("Something weird happened", BAD_REQUEST)))

      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }

    "return a 500 when the call fails due to an unexpected exception" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar&bar=wizz"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(new Exception("Something weird happened")))

      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", None)(HeaderCarrier()))
          logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }
  }

  "getFinancialDetailsForAPI" should {
    val queryParams = s"?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false" +
      s"&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true" +
      s"&addPostedInterestDetails=true&addAccruingInterestDetails=true"
    val queryParamsSomeMissingFields = s"?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12" +
      s"&includeStatisticalItems=true&includePaymentOnAccount=true&addLockInformation=true&addPenaltyDetails=true" +
      s"&addPostedInterestDetails=true"
    "return a 200 when the call succeeds" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/VRN/123456789/VATC$queryParams"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(status = Status.OK, json = Json.toJson(mockGetFinancialDetailsModelAPI1811), headers = Map.empty)))

      val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
        vrn = "123456789",
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
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe Json.toJson(mockGetFinancialDetailsModelAPI1811)
    }

    "return a 200 when the call succeeds - with some missing fields" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/VRN/123456789/VATC$queryParamsSomeMissingFields"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(status = Status.OK, json = Json.toJson(mockGetFinancialDetailsModelAPI1811), headers =  Map.empty)))

      val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
        vrn = "123456789",
        searchType = Some("CHGREF"),
        searchItem = Some("XC00178236592"),
        dateType = Some("BILLING"),
        dateFrom = Some("2020-10-03"),
        dateTo = Some("2021-07-12"),
        includeClearedItems = None,
        includeStatisticalItems = Some(true),
        includePaymentOnAccount = Some(true),
        addRegimeTotalisation = None,
        addLockInformation = Some(true),
        addPenaltyDetails = Some(true),
        addPostedInterestDetails = Some(true),
        addAccruingInterestDetails = None
      )(HeaderCarrier()))
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe Json.toJson(mockGetFinancialDetailsModelAPI1811)
    }

    s"return a 403 when the call fails for Not Found (for 4xx errors)" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/VRN/123456789/VATC$queryParams"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("You shall not pass", Status.FORBIDDEN)))

      val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
        vrn = "123456789",
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
      result.status shouldBe Status.FORBIDDEN
    }

    s"return a 500 when the call fails for Internal Server Error (for 5xx errors)" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/VRN/123456789/VATC$queryParams"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("Oops :(", Status.INTERNAL_SERVER_ERROR)))

      val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
        vrn = "123456789",
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
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return a 500 when the call fails due to an unexpected exception" in new Setup {
      when(mockHttpClient.GET[HttpResponse](Matchers.eq(s"/VRN/123456789/VATC$queryParams"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.failed(new Exception("Something weird happened")))

      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
            vrn = "123456789",
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
          logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1811_API.toString)) shouldBe true
          result.status shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
