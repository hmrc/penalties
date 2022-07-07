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

package services

import base.SpecBase
import config.featureSwitches.FeatureSwitching
import connectors.getFinancialDetails.GetFinancialDetailsConnector
import org.mockito.Mockito.{mock, reset, when}

import java.time.LocalDate
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import models.getFinancialDetails._
import models.mainTransaction.MainTransactionEnum
import org.mockito.Matchers
import org.mockito.Matchers.any
import play.api.Configuration
import play.api.http.Status.IM_A_TEAPOT
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsServiceSpec extends SpecBase with FeatureSwitching {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val config: Configuration = mock(classOf[Configuration])
  val mockGetFinancialDetailsConnector: GetFinancialDetailsConnector = mock(classOf[GetFinancialDetailsConnector])
  class Setup {
    val service = new GetFinancialDetailsService(mockGetFinancialDetailsConnector)
    reset(mockGetFinancialDetailsConnector, config)
    sys.props -= TIME_MACHINE_NOW
  }

  "getDataFromFinancialServiceForVATVCN" should {
    val mockGetFinancialDetailsResponseAsModel: GetFinancialDetails = GetFinancialDetails(
      documentDetails = Seq(DocumentDetails(
        documentId = "0001",
        accruingInterestAmount = Some(123.45),
        interestOutstandingAmount = Some(543.21),
        metadata = DocumentDetailsMetadata(
          taxYear = "2022",
          documentDate = LocalDate.of(2022, 10, 30),
          documentText = "Document Text",
          documentDueDate = LocalDate.of(2022, 10, 30),
          documentDescription = Some("Document Description"),
          formBundleNumber = Some("1234"),
          totalAmount = 123.45,
          documentOutstandingAmount = 543.21,
          lastClearingDate = Some(LocalDate.of(2022, 10, 30)),
          lastClearingReason = Some("last Clearing Reason"),
          lastClearedAmount = Some(123.45),
          statisticalFlag = true,
          informationCode = Some("A"),
          paymentLot = Some("81203010024"),
          paymentLotItem = Some("000001"),
          interestRate = Some(543.21),
          interestFromDate = Some(LocalDate.of(2022, 10, 30)),
          interestEndDate = Some(LocalDate.of(2022, 10, 30)),
          latePaymentInterestID = Some("1234567890123456"),
          latePaymentInterestAmount = Some(123.45),
          lpiWithDunningBlock = Some(543.21),
          accruingPenaltyLPP1 = Some("Interest Rate")
        ))
      ),
      financialDetails = Seq(FinancialDetails(
        documentId = "0001",
        taxPeriodFrom = Some(LocalDate.of(2022, 10 ,30)),
        taxPeriodTo = Some(LocalDate.of(2022,10,30)),
        items = Seq(FinancialItem(
          dueDate = Some(LocalDate.of(2022, 10 ,30)),
          clearingDate = Some(LocalDate.of(2022, 10 ,30)),
          metadata = FinancialItemMetadata(
            subItem = Some("001"),
            amount = Some(123.45),
            clearingReason = Some("01"),
            outgoingPaymentMethod = Some("outgoing Payment"),
            paymentLock = Some("paymentLock"),
            clearingLock = Some("clearingLock"),
            interestLock = Some("interestLock"),
            dunningLock = Some("dunningLock"),
            returnFlag = Some(true),
            paymentReference = Some("Ab12453535"),
            paymentAmount = Some(543.21),
            paymentMethod = Some("Payment"),
            paymentLot = Some("81203010024"),
            paymentLotItem = Some("000001"),
            clearingSAPDocument = Some("3350000253"),
            codingInitiationDate = Some(LocalDate.of(2022, 10 ,30)),
            statisticalDocument = Some("S"),
            DDCollectionInProgress = Some(true),
            returnReason = Some("ABCA"),
            promisetoPay = Some("promisetoPay")
          )
        )),
        originalAmount = Some(123.45),
        outstandingAmount = Some(543.21),
        mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
        chargeReference = Some("1"),
        metadata = FinancialDetailsMetadata(
          taxYear = "2022",
          chargeType = Some("PAYE"),
          mainType = Some("2100"),
          periodKey = Some("13RL"),
          periodKeyDescription = Some("abcde"),
          businessPartner = Some("6622334455"),
          contractAccountCategory = Some("02"),
          contractAccount = Some("X"),
          contractObjectType = Some("ABCD"),
          contractObject = Some("00000003000000002757"),
          sapDocumentNumber = Some("1040000872"),
          sapDocumentNumberItem = Some("XM00"),
          subTransaction = Some("5678"),
          clearedAmount = Some(123.45),
          accruedInterest = Some(543.21)
        )
      ))
    )

    s"call the connector and return a $GetFinancialDetailsSuccessResponse when the request is successful" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"),
        Matchers.eq(LocalDate.now().minusYears(2)),
        Matchers.eq(LocalDate.now()))(any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))
      val result: GetFinancialDetailsResponse = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isRight shouldBe true
      result.right.get shouldBe GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
    }

    s"call the connector and return a $GetFinancialDetailsSuccessResponse when the request is successful (with the time machine date)" in new Setup {
      setTimeMachineDate(Some(LocalDate.of(2024, 1, 1)))
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"),
        Matchers.eq(LocalDate.of(2022, 1, 1)),
        Matchers.eq(LocalDate.of(2024, 1, 1)))(any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))
      val result: GetFinancialDetailsResponse = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isRight shouldBe true
      result.right.get shouldBe GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
    }

    s"call the connector and return $GetFinancialDetailsNoContent when the response body contains NO_DATA_FOUND" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"),
        Matchers.eq(LocalDate.now().minusYears(2)),
        Matchers.eq(LocalDate.now()))(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      val result: GetFinancialDetailsResponse = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetFinancialDetailsNoContent
    }

    s"call the connector and return $GetFinancialDetailsMalformed when the response body is malformed" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"),
        Matchers.eq(LocalDate.now().minusYears(2)),
        Matchers.eq(LocalDate.now()))(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsMalformed)))
      val result: GetFinancialDetailsResponse = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetFinancialDetailsMalformed
    }

    s"call the connector and return $GetFinancialDetailsFailureResponse when an unknown status is returned" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"),
        Matchers.eq(LocalDate.now().minusYears(2)),
        Matchers.eq(LocalDate.now()))(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(IM_A_TEAPOT))))
      val result: GetFinancialDetailsResponse = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetFinancialDetailsFailureResponse(IM_A_TEAPOT)
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"),
        Matchers.eq(LocalDate.now().minusYears(2)),
        Matchers.eq(LocalDate.now()))(any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))
      val result: Exception = intercept[Exception](await(service.getDataFromFinancialServiceForVATVCN("123456789")))
      result.getMessage shouldBe "Something has gone wrong."
    }
  }
}
