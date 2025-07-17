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

package connectors.getFinancialDetails

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.http.Fault
import config.featureSwitches.{CallAPI1811HIP, FeatureSwitching}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.getFinancialDetails.FinancialDetailsRequestModel
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import play.api.http.Status._
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate

class FinancialDetailsHipConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {

  val connector: FinancialDetailsHipConnector = injector.instanceOf[FinancialDetailsHipConnector]

  val vatcEnrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("VATC"), IdType("VRN"), Id("123456789"))
  val itsaEnrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("ITSA"), IdType("NINO"), Id("AA000000A"))

  val dateNow: LocalDate = LocalDate.now()
  val financialDetailsRequestWithoutTargetedSearch: FinancialDetailsRequestModel = FinancialDetailsRequestModel(
    searchType = None,
    searchItem = None,
    dateType = Some("POSTING"),
    dateFrom = Some(dateNow.minusYears(2).toString),
    dateTo = Some(dateNow.toString),
    includeClearedItems = Some(true),
    includeStatisticalItems = Some(true),
    includePaymentOnAccount = Some(true),
    addRegimeTotalisation = Some(true),
    addLockInformation = Some(true),
    addPenaltyDetails = Some(true),
    addPostedInterestDetails = Some(true),
    addAccruingInterestDetails = Some(true)
  )
  val financialDetailsRequestMaxModel: FinancialDetailsRequestModel =
    financialDetailsRequestWithoutTargetedSearch.copy(searchType = Some("CHGREF"), searchItem = Some("XC00178236592"))

  enableFeatureSwitch(CallAPI1811HIP)

  "getFinancialDetails" when {
    Seq(vatcEnrolmentKey, itsaEnrolmentKey).foreach { enrolmentKey =>
      s"calling HIP for ${enrolmentKey.regime.value} regime" should {
        "return a successful response" when {
          val successResponseBody: String = getFinancialDetailsHipResponseWithoutTotalisations.toString()
          "'includeClearedItems' query parameter is 'true'" in {
            val requestBody: String =
              financialDetailsRequestWithoutTargetedSearch.copy(includeClearedItems = Some(true)).toJsonRequest(enrolmentKey).toString()
            mockGetFinancialDetailsHIP(CREATED, requestBody, successResponseBody)

            val result: FinancialDetailsResponse = await(connector.getFinancialDetails(enrolmentKey, includeClearedItems = true)(hc))

            result.isRight shouldBe true
            result.getOrElse(FinancialDetailsNoContent) shouldBe a[FinancialDetailsHipSuccessResponse]
          }
          "'includeClearedItems' query parameter is 'false'" in {
            val requestBody: String =
              financialDetailsRequestWithoutTargetedSearch.copy(includeClearedItems = Some(false)).toJsonRequest(enrolmentKey).toString()
            mockGetFinancialDetailsHIP(CREATED, requestBody, successResponseBody)

            val result: FinancialDetailsResponse = await(connector.getFinancialDetails(enrolmentKey, includeClearedItems = false)(hc))

            result.isRight shouldBe true
            result.getOrElse(FinancialDetailsNoContent) shouldBe a[FinancialDetailsHipSuccessResponse]
          }
        }

        "return a failure response" which {
          val requestBody: String = financialDetailsRequestWithoutTargetedSearch.toJsonRequest(enrolmentKey).toString()
          s"is a $FinancialDetailsMalformed when a malformed response body is returned" in {
            val malformedResponseBody: String = """{"documentDetails": [{ "documentOutstandingAmount": "xyz"}]}"""
            mockGetFinancialDetailsHIP(CREATED, requestBody, malformedResponseBody)

            val result: FinancialDetailsResponse = await(connector.getFinancialDetails(enrolmentKey, includeClearedItems = true)(hc))

            result shouldBe Left(FinancialDetailsMalformed)
          }
          Seq(BAD_REQUEST, NOT_FOUND, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { errorStatus =>
            s"is a $FinancialDetailsFailureResponse when a $errorStatus response body is returned" in {
              mockGetFinancialDetailsHIP(errorStatus, requestBody, "{}")
              val result: FinancialDetailsResponse = await(connector.getFinancialDetails(enrolmentKey, includeClearedItems = true)(hc))

              result shouldBe Left(FinancialDetailsFailureResponse(errorStatus))
            }
          }
        }
      }
    }
  }

  "getFinancialDetailsForAPI" when {
    Seq(vatcEnrolmentKey, itsaEnrolmentKey).foreach { enrolmentKey =>
      s"calling HIP for ${enrolmentKey.regime.value} regime" should {
        "return a success response with returned 201 status replaced with a 200 status" when {
          val successResponseBody: String = getFinancialDetailsHipResponseWithoutTotalisations.toString()
          "no extra query parameters are given" in {
            val requestBody: String = FinancialDetailsRequestModel.emptyModel.toJsonRequest(enrolmentKey).toString()
            mockGetFinancialDetailsHIP(CREATED, requestBody, successResponseBody)

            val result: HttpResponse = await(
              connector.getFinancialDetailsForAPI(enrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None)(hc))

            result.status shouldBe 200
          }
          "extra query parameters are given" in {
            val requestBody: String = financialDetailsRequestMaxModel.toJsonRequest(enrolmentKey).toString()
            mockGetFinancialDetailsHIP(CREATED, requestBody, successResponseBody)

            val result: HttpResponse = await(
              connector.getFinancialDetailsForAPI(
                enrolmentKey,
                searchType = Some("CHGREF"),
                searchItem = Some("XC00178236592"),
                dateType = Some("POSTING"),
                dateFrom = Some(dateNow.minusYears(2).toString),
                dateTo = Some(dateNow.toString),
                includeClearedItems = Some(true),
                includeStatisticalItems = Some(true),
                includePaymentOnAccount = Some(true),
                addRegimeTotalisation = Some(true),
                addLockInformation = Some(true),
                addPenaltyDetails = Some(true),
                addPostedInterestDetails = Some(true),
                addAccruingInterestDetails = Some(true)
              )(hc))

            result.status shouldBe 200
          }
        }

        "return a failure response" which {
          "has a 500 status when an unknown exception is returned" in {
            val stubCall: MappingBuilder = post(urlEqualTo("/etmp/RESTAdapter/cross-regime/taxpayer/financial-data/query"))
            stubFor(stubCall.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

            val result: HttpResponse = await(
              connector.getFinancialDetailsForAPI(enrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None)(hc))

            result.status shouldBe INTERNAL_SERVER_ERROR
          }
          Seq(BAD_REQUEST, NOT_FOUND, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { errorStatus =>
            s"returns the $errorStatus error status response that is returned" in {
              val requestBody: String = FinancialDetailsRequestModel.emptyModel.toJsonRequest(enrolmentKey).toString()
              mockGetFinancialDetailsHIP(errorStatus, requestBody, "{}")

              val result: HttpResponse = await(
                connector.getFinancialDetailsForAPI(enrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None)(hc))

              result.status shouldBe errorStatus
            }
          }
        }
      }
    }
  }
}
