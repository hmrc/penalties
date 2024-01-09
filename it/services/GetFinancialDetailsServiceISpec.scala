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

import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import models.getFinancialDetails._
import models.getFinancialDetails.totalisation._
import play.api.http.Status
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate

class GetFinancialDetailsServiceISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val service: GetFinancialDetailsService = injector.instanceOf[GetFinancialDetailsService]
  val financialDataQueryParam: String = {
      s"includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true" +
      s"&addRegimeTotalisation=true&addLockInformation=true&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true" +
      s"&dateType=POSTING&dateFrom=${LocalDate.now().minusYears(2).toString}&dateTo=${LocalDate.now().toString}"
  }

  "getFinancialDetails" when {

    val getFinancialDetailsModel: FinancialDetails = FinancialDetails(
      documentDetails = Some(Seq(DocumentDetails(
        chargeReferenceNumber = Some("XM002610011594"),
        documentOutstandingAmount = Some(543.21),
        lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))),
        documentTotalAmount = Some(100.00),
        issueDate = Some(LocalDate.of(2022, 1, 1)))
      )),
      totalisation = Some(FinancialDetailsTotalisation(
        regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = Some(43.21)))
      ))
    )

    "call the connector and return a successful result" in {
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParam", Some(getFinancialDetailsAsJson.toString()))
      val result = await(service.getFinancialDetails("123456789", None))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetFinancialDetailsSuccessResponse(getFinancialDetailsModel)
    }

    "call the connector and return a successful result - passing custom parameters when defined" in {
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?foo=bar&dateType=POSTING&dateFrom=${LocalDate.now().minusYears(2).toString}&dateTo=${LocalDate.now().toString}",
        Some(getFinancialDetailsAsJson.toString()))
      val result = await(service.getFinancialDetails("123456789", Some("?foo=bar")))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetFinancialDetailsSuccessResponse(getFinancialDetailsModel)
    }

    s"the response body is not well formed: $GetFinancialDetailsMalformed" in {
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParam", Some(
        """
          {
           "documentDetails": [
            {
              "documentOutstandingAmount": "xyz"
            }
           ]
          }
          """))
      val result = await(service.getFinancialDetails("123456789", None))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetFinancialDetailsMalformed
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
      mockStubResponseForGetFinancialDetails(Status.NOT_FOUND, s"VRN/123456789/VATC?$financialDataQueryParam", Some(noDataFoundBody))
      val result = await(service.getFinancialDetails("123456789", None))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetFinancialDetailsNoContent
    }

    s"an unknown response is returned from the connector - $GetFinancialDetailsFailureResponse" in {
      mockStubResponseForGetFinancialDetails(Status.IM_A_TEAPOT, s"VRN/123456789/VATC?$financialDataQueryParam")
      val result = await(service.getFinancialDetails("123456789", None))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetFinancialDetailsFailureResponse(Status.IM_A_TEAPOT)
    }
  }
}
