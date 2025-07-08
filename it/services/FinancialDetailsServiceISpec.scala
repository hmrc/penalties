/*
 * Copyright 2025 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.featureSwitches.{CallAPI1811Stub, FeatureSwitching}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.getFinancialDetails._
import models.getFinancialDetails.totalisation._
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status._
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.{IntegrationSpecCommonBase, RegimeETMPWiremock}

import java.time.LocalDate

class FinancialDetailsServiceISpec extends IntegrationSpecCommonBase with RegimeETMPWiremock with FeatureSwitching with TableDrivenPropertyChecks {
//  setEnabledFeatureSwitches(CallAPI1811Stub)
  setEnabledFeatureSwitches()
  val service: FinancialDetailsService = injector.instanceOf[FinancialDetailsService]

  Table(
    ("Regime", "IdType", "Id"),
    (Regime("VATC"), IdType("VRN"), Id("123456789")),
    (Regime("ITSA"), IdType("NINO"), Id("AB123456C"))
  ).forEvery { (regime, idType, id) =>
    val aKey = AgnosticEnrolmentKey(regime, idType, id)
    val financialData: FinancialDetails = FinancialDetails(
      documentDetails = Some(
        Seq(DocumentDetails(
          chargeReferenceNumber = Some("XM002610011594"),
          documentOutstandingAmount = Some(543.21),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))),
          documentTotalAmount = Some(100.00),
          issueDate = Some(LocalDate.of(2022, 1, 1))
        ))),
      totalisation = Some(
        FinancialDetailsTotalisation(
          regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = Some(43.21)))
        ))
    )
    val financialDetailsHip: FinancialDetailsHIP = FinancialDetailsHIP(processingDate = "2025-05-06", financialData = financialData)

    s"getFinancialDetails for $regime" when {

      Seq("HIP", "IF").foreach { upstream =>
        val isHip = upstream == "HIP"
        val successResponse =
          if (isHip) GetFinancialDetailsHipSuccessResponse(financialDetailsHip)
          else GetFinancialDetailsSuccessResponse(financialDetailsHip.financialData)

        def stubCall(status: Int, body: Option[String]): StubMapping =
          if (isHip) mockStubResponseForGetFinancialDetails(status, body)
          else mockStubResponseForGetFinancialDetailsIF(regime, idType, id, status)

        s"calling $upstream endpoint" should {

          "call the connector and return a successful result" in {
//            stubCall(OK, Some(getFinancialDetailsAsJson.toString()))
            mockStubResponseForGetFinancialDetails(OK, Some(getFinancialDetailsAsJson.toString()))
            val result = await(service.getFinancialDetails(aKey))

            result shouldBe Right(successResponse)
          }

          "call the connector and return a successful result - passing custom parameters when defined" in {
            stubCall(OK, Some(getFinancialDetailsAsJson.toString()))
            val result = await(service.getFinancialDetails(aKey))

            result shouldBe Right(successResponse)
          }

          s"the response body is not well formed: $GetFinancialDetailsMalformed" in {
            stubCall(
              OK,
              Some("""
          {
           "documentDetails": [
            {
              "documentOutstandingAmount": "xyz"
            }
           ]
          }
          """)
            )
            val result = await(service.getFinancialDetails(aKey))

            result shouldBe Left(GetFinancialDetailsMalformed)
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
            stubCall(NOT_FOUND, Some(noDataFoundBody))
            val result = await(service.getFinancialDetails(aKey))

            result shouldBe Left(GetFinancialDetailsNoContent)
          }

          s"an unknown response is returned from the connector - $GetFinancialDetailsFailureResponse" in {
            stubCall(IM_A_TEAPOT, None)
            val result = await(service.getFinancialDetails(aKey))

            result shouldBe Left(GetFinancialDetailsFailureResponse(IM_A_TEAPOT))
          }
        }
      }
    }
  }
}
