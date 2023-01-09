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

package services

import config.featureSwitches.FeatureSwitching
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import models.getFinancialDetails
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{FinancialDetails, MainTransactionEnum}
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate

class GetFinancialDetailsServiceISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {
  val service: GetFinancialDetailsService = injector.instanceOf[GetFinancialDetailsService]

  "getDataFromFinancialServiceForVATVCN" when {

    val getFinancialDetailsModel: FinancialDetails = FinancialDetails(
      documentDetails = Some(Seq(getFinancialDetails.DocumentDetails(
        chargeReferenceNumber = Some("XM002610011594"),
        documentOutstandingAmount = Some(543.21),
        lineItemDetails = Some(Seq(getFinancialDetails.LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))))
      )),
      totalisation = Some(FinancialDetailsTotalisation(
        regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = Some(43.21)))
      ))
    )

    "call the connector and return a successful result" in {
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?dateFrom=${LocalDate.now().minusYears(2)}&dateTo=${LocalDate.now()}&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true")
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetFinancialDetailsSuccessResponse(getFinancialDetailsModel)
    }

    "call the connector and return a successful result (using time machine date)" in {
      setTimeMachineDate(Some(LocalDate.of(2024, 1, 1)))
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?dateFrom=${LocalDate.of(2022, 1, 1)}&dateTo=${LocalDate.of(2024, 1, 1)}&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true")
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetFinancialDetailsSuccessResponse(getFinancialDetailsModel)
      setTimeMachineDate(None)
    }

    s"the response body is not well formed: $GetFinancialDetailsMalformed" in {
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?dateFrom=${LocalDate.now().minusYears(2)}&dateTo=${LocalDate.now()}&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true", body = Some(
        """
          {
           "documentDetails": [
            {
              "documentOutstandingAmount": "xyz"
            }
           ]
          }
          """))
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.getOrElse(false) shouldBe GetFinancialDetailsMalformed
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
      mockStubResponseForGetFinancialDetails(Status.NOT_FOUND, s"VRN/123456789/VATC?dateFrom=${LocalDate.now().minusYears(2)}&dateTo=${LocalDate.now()}&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true", body = Some(noDataFoundBody))
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.getOrElse(false) shouldBe GetFinancialDetailsNoContent
    }

    s"an unknown response is returned from the connector - $GetFinancialDetailsFailureResponse" in {
      mockStubResponseForGetFinancialDetails(Status.IM_A_TEAPOT, s"VRN/123456789/VATC?dateFrom=${LocalDate.now().minusYears(2)}&dateTo=${LocalDate.now()}&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true")
      val result = await(service.getDataFromFinancialServiceForVATVCN("123456789"))
      result.isLeft shouldBe true
      result.left.getOrElse(false) shouldBe GetFinancialDetailsFailureResponse(Status.IM_A_TEAPOT)
    }
  }
}
