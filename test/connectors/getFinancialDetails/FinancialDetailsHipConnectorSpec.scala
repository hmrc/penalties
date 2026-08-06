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
import connectors.parsers.getFinancialDetails.HIPFinancialDetailsParser.{
  HIPFinancialDetailsFailureResponse,
  HIPFinancialDetailsResponse,
  HIPFinancialDetailsSuccessResponse
}
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, FinancialDetailsRequestModel, LineItemDetails}
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import utils.DateHelper
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsHipConnectorSpec extends SpecBase with LogCapturing {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val mockDateHelper: DateHelper = mock(classOf[DateHelper])

  val regime = Regime("VATC") 
  val idType = IdType("VRN")
  val id = Id("123456789")


  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    regime,
    idType,
    id
  )


  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)

    val connector = new FinancialDetailsHipConnector(mockHttpClient, mockAppConfig, mockDateHelper)
  
    when(mockAppConfig.getFinancialDetailsHipUrl).thenReturn("/etmp/RESTAdapter/cross-regime/taxpayer/financial-data/query")
    when(mockAppConfig.baseFinancialDetailsRequestModel).thenReturn(FinancialDetailsRequestModel.emptyModel)

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
      regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
      interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
    ))
  )

  private val expectedApiUrl = "/etmp/RESTAdapter/cross-regime/taxpayer/financial-data/query"

  "getFinancialDetails" should {
    "return a 200 when the call succeeds" when {
      "the 'includeClearedItems' body parameter is 'true'" in new Setup {
        when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(HIPFinancialDetailsSuccessResponse(mockGetFinancialDetailsModelAPI1811))))

        val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = true)(HeaderCarrier()))
        result.isRight shouldBe true
      }

      "the 'includeClearedItems' body parameter is 'false'" in new Setup {
        when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
          ArgumentMatchers.any(),
          ArgumentMatchers.any())
          (ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(HIPFinancialDetailsSuccessResponse(mockGetFinancialDetailsModelAPI1811))))

        val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
        result.isRight shouldBe true
      }
    }

    s"return a 404 when the call fails for Not Found" in new Setup {
      when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(HIPFinancialDetailsFailureResponse(Status.NOT_FOUND))))

      val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 400 when the call fails for Bad Request" in new Setup {
      when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(HIPFinancialDetailsFailureResponse(Status.BAD_REQUEST))))

      val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 409 when the call fails for Conflict" in new Setup {
      when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(HIPFinancialDetailsFailureResponse(Status.CONFLICT))))

      val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 422 when the call fails for Unprocessable Entity" in new Setup {
      when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(HIPFinancialDetailsFailureResponse(Status.UNPROCESSABLE_ENTITY))))

      val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 500 when the call fails for Internal Server Error" in new Setup {
      when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(HIPFinancialDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))

      val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 403 when the call fails" in new Setup {
      when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(HIPFinancialDetailsFailureResponse(Status.FORBIDDEN))))

      val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 503 when the call fails" in new Setup {
      when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(HIPFinancialDetailsFailureResponse(Status.SERVICE_UNAVAILABLE))))

      val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    "return a 500 when the call fails due to an UpstreamErrorResponse(5xx) exception" in new Setup {
      when(mockHttpClient.POST[JsObject, HttpResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("Something weird happened", INTERNAL_SERVER_ERROR)))

      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }

    "return a 400 when the call fails due to an UpstreamErrorResponse(4xx) exception" in new Setup {
      when(mockHttpClient.POST[JsObject, HttpResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("Something weird happened", BAD_REQUEST)))

      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }

    "return a 500 when the call fails due to an unexpected exception" in new Setup {
      when(mockHttpClient.POST[JsObject, HIPFinancialDetailsResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("Something weird happened")))

      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: HIPFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789, includeClearedItems = false)(HeaderCarrier()))
          logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }
  }

  "getFinancialDetailsForAPI" should {
    "return a 200 when the call succeeds" in new Setup {
      when(mockHttpClient.POST[JsObject, HttpResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse.apply(status = Status.OK, json = Json.toJson(mockGetFinancialDetailsModelAPI1811), headers = Map.empty)))

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
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe Json.toJson(mockGetFinancialDetailsModelAPI1811)
    }

    s"return a 403 when the call fails for Not Found (for 4xx errors)" in new Setup {
      when(mockHttpClient.POST[JsObject, HttpResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("You shall not pass", Status.FORBIDDEN)))

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
      result.status shouldBe Status.FORBIDDEN
    }

    s"return a 500 when the call fails for Internal Server Error (for 5xx errors)" in new Setup {
      when(mockHttpClient.POST[JsObject, HttpResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply("Oops :(", Status.INTERNAL_SERVER_ERROR)))

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
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return a 500 when the call fails due to an unexpected exception" in new Setup {
      when(mockHttpClient.POST[JsObject, HttpResponse](ArgumentMatchers.eq(expectedApiUrl),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
        (ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("Something weird happened")))

      withCaptureOfLoggingFrom(logger) {
        logs => {
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
          logs.exists(_.getMessage.contains(PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1811_API.toString)) shouldBe true
          result.status shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }

}
