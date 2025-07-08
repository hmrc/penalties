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

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.featureSwitches.{CallAPI1811HIP, FeatureSwitching}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser.{GetFinancialDetailsSuccess, _}
import models.getFinancialDetails.FinancialDetailsRequestModel
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import play.api.http.Status._
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate

class FinancialDetailsConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {

  private val regime = Regime("VATC")
  private val idType = IdType("VRN")
  private val id     = Id("123456789")

  private val vrn123456789 = AgnosticEnrolmentKey(regime, idType, id)

  private val financialDetails = FinancialDetailsRequestModel(
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
  )

  class Setup(upstreamService: String) {
    val connector: FinancialDetailsConnector = injector.instanceOf[FinancialDetailsConnector]
    val isHip: Boolean                       = upstreamService == "HIP"
    val successResponseBody: String =
      if (isHip) getFinancialDetailsWithoutTotalisationsAsJsonHIP.toString() else getFinancialDetailsWithoutTotalisationsAsJson.toString()

    if (isHip) setEnabledFeatureSwitches(CallAPI1811HIP) else setEnabledFeatureSwitches()

    def buildMockCall(status: Int, responseBody: String, queryParams: String = defaultQueryParams): StubMapping =
      if (isHip) mockResponseForGetFinancialDetailsHIP(status, Some(responseBody))
      else mockResponseForGetFinancialDetails(status, "VRN/123456789/VATC" + queryParams, Some(responseBody))
  }

  lazy val dateNow: LocalDate = LocalDate.now()
  private val defaultQueryParams = s"?includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=true" +
    s"&addLockInformation=true&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true&dateType=POSTING" +
    s"&dateFrom=${dateNow.minusYears(2)}&dateTo=$dateNow"

  "getFinancialDetails" should {
    Seq("HIP", "IF").foreach { upstream =>
      s"when calling $upstream API" should {
        "return a successful response when called" in new Setup(upstream) {
          buildMockCall(OK, successResponseBody)
          val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(hc))

          result.isRight shouldBe true
          result.getOrElse("wrong") shouldBe a[GetFinancialDetailsSuccess]
        }

        s"return a $GetFinancialDetailsMalformed response when upstream response body is not recognised" in new Setup(upstream) {
          val malformedResponseBody = """{"documentDetails": [{"documentOutstandingAmount": "xyz"}]}"""
          buildMockCall(status = OK, malformedResponseBody)
          val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))

          result shouldBe Left(GetFinancialDetailsMalformed)
        }

        s"return a $GetFinancialDetailsFailureResponse" when {
          "a 4XX response is returned from upstream" in new Setup(upstream) {
            buildMockCall(status = NOT_FOUND, "")
            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))

            result shouldBe Left(GetFinancialDetailsFailureResponse(NOT_FOUND))
          }
          "a 5XX response is returned from upstream" in new Setup(upstream) {
            buildMockCall(status = SERVICE_UNAVAILABLE, "")
            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))

            result shouldBe Left(GetFinancialDetailsFailureResponse(SERVICE_UNAVAILABLE))
          }
          "an unknown exception is returned from upstream" in new Setup(upstream) {
            val stubCall: MappingBuilder =
              if (isHip) post(urlEqualTo(s"/RESTAdapter/cross-regime/taxpayer/financial-data/query"))
              else get(urlEqualTo(s"/penalty/financial-data/VRN/123456789/VATC$defaultQueryParams"))

            stubFor(stubCall.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
            val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))

            result shouldBe Left(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))
          }
        }
      }
    }
  }

  "getFinancialDetailsForAPI" should {
    Seq("HIP", "IF").foreach { upstream =>
      s"when calling $upstream API" should {
        "return a 200 response" in new Setup(upstream) {
          buildMockCall(OK, successResponseBody, queryParams = financialDetails.toJsonRequestQueryParams)
          val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, financialDetails))

          result.status shouldBe OK
        }

        "handle a UpstreamErrorResponse" when {
          "a 4xx error is returned" in new Setup(upstream) {
            buildMockCall(FORBIDDEN, "", queryParams = financialDetails.toJsonRequestQueryParams)
            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, financialDetails))

            result.status shouldBe FORBIDDEN
          }

          "a 5xx error is returned" in new Setup(upstream) {
            buildMockCall(BAD_GATEWAY, "", queryParams = financialDetails.toJsonRequestQueryParams)
            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, financialDetails))

            result.status shouldBe BAD_GATEWAY
          }

          "an unknown exception is returned" in new Setup(upstream) {
            val stubCall: MappingBuilder =
              if (isHip) post(urlEqualTo(s"/RESTAdapter/cross-regime/taxpayer/financial-data/query"))
              else get(urlEqualTo(s"/penalty/financial-data/VRN/123456789/VATC${financialDetails.toJsonRequestQueryParams}"))

            stubFor(stubCall.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
            val result: HttpResponse = await(connector.getFinancialDetailsForAPI(vrn123456789, financialDetails))

            result.status shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }
  }
}
