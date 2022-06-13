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

package connectors.v3.getFinancialDetails

import java.time.LocalDate

import base.SpecBase
import config.AppConfig
import connectors.parsers.v3.getFinancialDetails.GetFinancialDetailsParser._
import models.v3.getFinancialDetails._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.DateHelper

import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsConnectorSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val mockDateHelper: DateHelper = mock(classOf[DateHelper])
  val sampleDate: LocalDate = LocalDate.of(2023, 1, 1)

  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)

    val connector = new GetFinancialDetailsConnector(mockHttpClient, mockAppConfig)
    when(mockAppConfig.getFinancialDetailsUrl).thenReturn("/")
    when(mockAppConfig.eiOutboundBearerToken).thenReturn("1234")
    when(mockAppConfig.eisEnvironment).thenReturn("asdf")
    when(mockAppConfig.queryParametersForGetFinancialDetail(Matchers.any(), Matchers.any())).thenReturn("?foo=bar")
  }
  
  val mockGetFinancialDetailsModelAPI1811: GetFinancialDetails = GetFinancialDetails(
    documentDetails = Seq(DocumentDetails(
      documentId = "1234",
      accruingInterestAmount = None,
      interestOutstandingAmount = None,
      metadata = DocumentDetailsMetadata(
        taxYear = "2022",
        documentDate = LocalDate.now,
        documentText = "asdf",
        documentDueDate = LocalDate.now,
        documentDescription = None,
        formBundleNumber = None,
        totalAmount = 0.00,
        documentOutstandingAmount = 0.00,
        lastClearingDate = None,
        lastClearingReason = None,
        lastClearedAmount = None,
        statisticalFlag = true,
        informationCode = None,
        paymentLot = None,
        paymentLotItem = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        latePaymentInterestID = None,
        latePaymentInterestAmount = None,
        lpiWithDunningBlock = None,
        accruingPenaltyLPP1 = None
      ))
    ),
    financialDetails = Seq(FinancialDetails(
      documentId = "asdf",
      taxPeriodFrom = None,
      taxPeriodTo = None,
      items = Seq(FinancialItem(
        dueDate = None, clearingDate = None, metadata = FinancialItemMetadata(
          subItem = None,
          amount = None,
          clearingReason = None,
          outgoingPaymentMethod = None,
          paymentLock = None,
          clearingLock = None,
          interestLock = None,
          dunningLock = None,
          returnFlag = None,
          paymentReference = None,
          paymentAmount = None,
          paymentMethod = None,
          paymentLot = None,
          paymentLotItem = None,
          clearingSAPDocument = None,
          codingInitiationDate = None,
          statisticalDocument = None,
          DDCollectionInProgress = None,
          returnReason = None,
          promisetoPay = None
        )
      )),
      originalAmount = None,
      outstandingAmount = None,
      metadata = FinancialDetailsMetadata(
        taxYear = "2022",
        chargeType = None,
        mainType = None,
        periodKey = None,
        periodKeyDescription = None,
        businessPartner = None,
        contractAccountCategory = None,
        contractAccount = None,
        contractObjectType = None,
        contractObject = None,
        sapDocumentNumber = None,
        sapDocumentNumberItem = None,
        chargeReference = None,
        mainTransaction = None,
        subTransaction = None,
        clearedAmount = None,
        accruedInterest = None
      )
    ))
  )

  "getFinancialDetails" should {
    "return a 200 when the call succeeds" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/VRN/123456789/VATC?foo=bar"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsModelAPI1811))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("VRN/123456789/VATC", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1))(HeaderCarrier()))
      result.isRight shouldBe true
    }

    s"return a 404 when the call fails for Not Found" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/FOO/BAR/123456789?foo=bar"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.NOT_FOUND))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1))(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 400 when the call fails for Bad Request" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/FOO/BAR/123456789?foo=bar"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.BAD_REQUEST))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1))(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 409 when the call fails for Conflict" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/FOO/BAR/123456789?foo=bar"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.CONFLICT))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1))(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 422 when the call fails for Unprocessable Entity" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/FOO/BAR/123456789?foo=bar"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.UNPROCESSABLE_ENTITY))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1))(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 500 when the call fails for Internal Server Error" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/FOO/BAR/123456789?foo=bar"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1))(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 403 when the call fails" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/FOO/BAR/123456789?foo=bar"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.FORBIDDEN))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1))(HeaderCarrier()))
      result.isLeft shouldBe true
    }

    s"return a 503 when the call fails" in new Setup {
      when(mockHttpClient.GET[GetFinancialDetailsResponse](Matchers.eq("/FOO/BAR/123456789?foo=bar"),
        Matchers.any(),
        Matchers.any())
        (Matchers.any(),
          Matchers.any(),
          Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.SERVICE_UNAVAILABLE))))

      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("FOO/BAR/123456789", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1))(HeaderCarrier()))
      result.isLeft shouldBe true
    }
  }
}
