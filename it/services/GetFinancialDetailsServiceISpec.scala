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

import config.featureSwitches.FeatureSwitching
import java.time.LocalDate
import connectors.parsers.v3.getFinancialDetails.GetFinancialDetailsParser._
import models.v3.getFinancialDetails._
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class GetFinancialDetailsServiceISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {
  val service: GetFinancialDetailsService = injector.instanceOf[GetFinancialDetailsService]

  "getDataFromFinancialServiceForVATVCN" when {
    val getFinancialDetailsModel: GetFinancialDetails = GetFinancialDetails(
      documentDetails = Seq(DocumentDetails(
        documentId = "0002",
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
          chargeReference = Some("XM002610011594"),
          mainTransaction = Some("1234"),
          subTransaction = Some("5678"),
          clearedAmount = Some(123.45),
          accruedInterest = Some(543.21)
        )
      ))
    )

    "call the connector and return a successful result" in {
      mockStubReponseForGetFinancialDetailsv3(Status.OK, s"VRN/123456789/VATC?dateFrom=${LocalDate.now().minusYears(2)}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isRight shouldBe true
      result.right.get shouldBe GetFinancialDetailsSuccessResponse(getFinancialDetailsModel)
    }

    "call the connector and return a successful result (using time machine date)" in {
      setTimeMachineDate(Some(LocalDate.of(2024, 1, 1)))
      mockStubReponseForGetFinancialDetailsv3(Status.OK, s"VRN/123456789/VATC?dateFrom=${LocalDate.of(2022, 1, 1)}&dateTo=${LocalDate.of(2024, 1, 1)}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isRight shouldBe true
      result.right.get shouldBe GetFinancialDetailsSuccessResponse(getFinancialDetailsModel)
      setTimeMachineDate(None)
    }

    s"the response body is not well formed: $GetFinancialDetailsMalformed" in {
      mockStubReponseForGetFinancialDetailsv3(Status.OK, s"VRN/123456789/VATC?dateFrom=${LocalDate.now().minusYears(2)}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", body = Some(
        """
          {
           "documentDetails": [
            {
              "taxyear": "2022"
            }
           ]
          }
          """))
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetFinancialDetailsMalformed
    }

    s"the response body contains NO_DATA_FOUND for 404 response - returning $GetFinancialDetailsNoContent" in {
      val noDataFoundBody =
        """
          |{
          | "failures":[
          |   {
          |     "code": "NO_DATA_FOUND",
          |     "reason": "This is a reason"
          |   }
          | ]
          |}
          |""".stripMargin
      mockStubReponseForGetFinancialDetailsv3(Status.NOT_FOUND, s"VRN/123456789/VATC?dateFrom=${LocalDate.now().minusYears(2)}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", body = Some(noDataFoundBody))
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetFinancialDetailsNoContent
    }

    s"an unknown response is returned from the connector - $GetFinancialDetailsFailureResponse" in {
      mockStubReponseForGetFinancialDetailsv3(Status.IM_A_TEAPOT, s"VRN/123456789/VATC?dateFrom=${LocalDate.now().minusYears(2)}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetFinancialDetailsFailureResponse(Status.IM_A_TEAPOT)
    }
  }
}
