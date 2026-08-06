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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.featureSwitches._
import controllers.APIControllerISpec.financialDetailsQueryParams
import models.getFinancialDetails.FinancialDetailsRequestModel
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{AuthMock, ETMPWiremock, HIPPenaltiesWiremock, IntegrationSpecCommonBase}

import scala.jdk.CollectionConverters._

class APIControllerISpec
    extends IntegrationSpecCommonBase
    with ETMPWiremock
    with HIPPenaltiesWiremock
    with FeatureSwitching
    with TableDrivenPropertyChecks
    with AuthMock {
  val controller: APIController = injector.instanceOf[APIController]

  val getHIPPenaltyDetailsJson: JsValue = Json.parse("""
      |{
      |  "success": {
      |    "processingDate": "2025-04-24T12:00:00Z",
      |    "penaltyData": {
      |      "totalisations": {
      |        "lspTotalValue": 200,
      |        "penalisedPrincipalTotal": 2000,
      |        "lppPostedTotal": 165.25,
      |        "lppEstimatedTotal": 15.26
      |      },
      |      "lsp": {
      |        "lspSummary": {
      |          "activePenaltyPoints": 2,
      |          "inactivePenaltyPoints": 0,
      |          "regimeThreshold": 5,
      |          "penaltyChargeAmount": 200.00,
      |          "pocAchievementDate": "2022-01-01"
      |        },
      |        "lspDetails": []
      |      },
      |      "lpp": {
      |        "lppDetails": [
      |          {
      |            "principalChargeReference": "1234567890",
      |            "penaltyCategory": "LPP2",
      |            "penaltyStatus": "A",
      |            "penaltyAmountAccruing": 246.9,
      |            "penaltyAmountPosted": 0,
      |            "penaltyAmountPaid": null,
      |            "penaltyAmountOutstanding": null,
      |            "lpp1LRCalculationAmt": 123.45,
      |            "lpp1LRDays": "15",
      |            "lpp1LRPercentage": 2.0,
      |            "lpp1HRCalculationAmt": 123.45,
      |            "lpp1HRDays": "31",
      |            "lpp1HRPercentage": 2.0,
      |            "lpp2Days": "31",
      |            "lpp2Percentage": 4.0,
      |            "penaltyChargeCreationDate": "2022-10-30",
      |            "communicationsDate": "2022-10-30",
      |            "penaltyChargeReference": null,
      |            "penaltyChargeDueDate": "2022-10-30",
      |            "appealInformation": null,
      |            "principalChargeDocNumber": null,
      |            "principalChargeMainTr": "4700",
      |            "principalChargeSubTr": null,
      |            "principalChargeBillingFrom": "2022-10-30",
      |            "principalChargeBillingTo": "2022-10-30",
      |            "principalChargeDueDate": "2022-10-30",
      |            "principalChargeLatestClearing": null,
      |            "timeToPay": null,
      |            "supplement": false
      |          },
      |          {
      |            "principalChargeReference": "1234567891",
      |            "penaltyCategory": "LPP1",
      |            "penaltyStatus": "P",
      |            "penaltyAmountAccruing": 0,
      |            "penaltyAmountPosted": 144.0,
      |            "penaltyAmountPaid": 0,
      |            "penaltyAmountOutstanding": 144.00,
      |            "lpp1LRCalculationAmt": 99.99,
      |            "lpp1LRDays": "15",
      |            "lpp1LRPercentage": 2.0,
      |            "lpp1HRCalculationAmt": 99.99,
      |            "lpp1HRDays": "31",
      |            "lpp1HRPercentage": 2.0,
      |            "lpp2Days": "31",
      |            "lpp2Percentage": 4.0,
      |            "penaltyChargeCreationDate": "2022-10-30",
      |            "communicationsDate": "2022-10-30",
      |            "penaltyChargeReference": null,
      |            "penaltyChargeDueDate": "2022-10-30",
      |            "appealInformation": null,
      |            "principalChargeDocNumber": null,
      |            "principalChargeMainTr": "4700",
      |            "principalChargeSubTr": null,
      |            "principalChargeBillingFrom": "2022-10-30",
      |            "principalChargeBillingTo": "2022-10-30",
      |            "principalChargeDueDate": "2022-10-30",
      |            "principalChargeLatestClearing": null,
      |            "timeToPay": null,
      |            "supplement": false
      |          }
      |        ],
      |        "manualLPPIndicator": true
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)

  val getPenaltyDetailsJson: JsValue = Json.parse("""
      |{
      | "totalisations": {
      |   "LSPTotalValue": 200,
      |   "penalisedPrincipalTotal": 2000,
      |   "LPPPostedTotal": 165.25,
      |   "LPPEstimatedTotal": 15.26
      | },
      | "lateSubmissionPenalty": {
      |   "summary": {
      |     "activePenaltyPoints": 2,
      |     "inactivePenaltyPoints": 0,
      |     "regimeThreshold": 5,
      |     "penaltyChargeAmount": 200.00,
      |     "PoCAchievementDate": "2022-01-01"
      |   },
      |   "details": []
      | },
      | "latePaymentPenalty": {
      |     "details": [
      |       {
      |          "penaltyCategory": "LPP2",
      |          "penaltyStatus": "A",
      |          "penaltyAmountPosted": 0,
      |          "LPP1LRCalculationAmount": 123.45,
      |          "LPP1LRDays": "15",
      |          "LPP1LRPercentage": 2.00,
      |          "LPP1HRCalculationAmount": 123.45,
      |          "LPP1HRDays": "31",
      |          "LPP1HRPercentage": 2.00,
      |          "LPP2Days": "31",
      |          "LPP2Percentage": 4.00,
      |          "penaltyChargeCreationDate": "2022-10-30",
      |          "communicationsDate": "2022-10-30",
      |          "penaltyAmountAccruing": 246.9,
      |          "principalChargeMainTransaction" : "4700",
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "1234567890",
      |          "principalChargeBillingFrom": "2022-10-30",
      |          "principalChargeBillingTo": "2022-10-30",
      |          "principalChargeMainTransaction": "4700",
      |          "principalChargeDueDate": "2022-10-30",
      |          "supplement": false
      |       },
      |       {
      |          "penaltyCategory": "LPP2",
      |          "penaltyStatus": "A",
      |          "penaltyAmountPosted": 0,
      |          "penaltyAmountAccruing": 123.45,
      |          "LPP1LRCalculationAmount": 123.45,
      |          "LPP1LRDays": "15",
      |          "LPP1LRPercentage": 2.00,
      |          "LPP1HRCalculationAmount": 123.45,
      |          "LPP1HRDays": "31",
      |          "LPP1HRPercentage": 2.00,
      |          "LPP2Days": "31",
      |          "LPP2Percentage": 4.00,
      |          "penaltyChargeCreationDate": "2022-10-30",
      |          "communicationsDate": "2022-10-30",
      |          "penaltyAmountAccruing": 0.00,
      |          "principalChargeMainTransaction" : "4700",
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "1234567890",
      |          "principalChargeBillingFrom": "2022-10-30",
      |          "principalChargeBillingTo": "2022-10-30",
      |          "principalChargeMainTransaction": "4700",
      |          "principalChargeDueDate": "2022-10-30",
      |          "supplement": false
      |       },
      |       {
      |          "penaltyCategory": "LPP1",
      |          "penaltyStatus": "P",
      |          "penaltyAmountPaid": 0,
      |          "penaltyAmountPosted": 144.0,
      |          "penaltyAmountOutstanding": 144.00,
      |          "penaltyAmountAccruing": 0,
      |          "LPP1LRCalculationAmount": 99.99,
      |          "LPP1LRDays": "15",
      |          "LPP1LRPercentage": 2.00,
      |          "LPP1HRCalculationAmount": 99.99,
      |          "LPP1HRDays": "31",
      |          "LPP1HRPercentage": 2.00,
      |          "LPP2Days": "31",
      |          "LPP2Percentage": 4.00,
      |          "penaltyChargeCreationDate": "2022-10-30",
      |          "communicationsDate": "2022-10-30",
      |          "penaltyAmountAccruing": 0.00,
      |          "principalChargeMainTransaction" : "4700",
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "1234567890",
      |          "principalChargeBillingFrom": "2022-10-30",
      |          "principalChargeBillingTo": "2022-10-30",
      |          "principalChargeMainTransaction": "4700",
      |          "principalChargeDueDate": "2022-10-30",
      |          "supplement": false
      |       },
      |       {
      |          "penaltyCategory": "LPP1",
      |          "penaltyStatus": "P",
      |          "penaltyAmountPaid": 0,
      |          "penaltyAmountPosted": 144.00,
      |          "penaltyAmountOutstanding": 144.00,
      |          "penaltyAmountAccruing": 0,
      |          "LPP1LRCalculationAmount": 99.99,
      |          "LPP1LRDays": "15",
      |          "LPP1LRPercentage": 2.00,
      |          "LPP1HRCalculationAmount": 99.99,
      |          "LPP1HRDays": "31",
      |          "LPP1HRPercentage": 2.00,
      |          "LPP2Days": "31",
      |          "LPP2Percentage": 4.00,
      |          "penaltyChargeCreationDate": "2022-10-30",
      |          "communicationsDate": "2022-10-30",
      |          "penaltyAmountAccruing": 0,
      |          "principalChargeMainTransaction" : "4700",
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "1234567890",
      |          "principalChargeBillingFrom": "2022-10-30",
      |          "principalChargeBillingTo": "2022-10-30",
      |          "principalChargeMainTransaction": "4700",
      |          "principalChargeDueDate": "2022-10-30",
      |          "supplement": false
      |       }
      |   ]
      | }
      |}
      |""".stripMargin)

  val hipRequestBody: FinancialDetailsRequestModel = FinancialDetailsRequestModel(
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

  Table(
    ("Regime", "IdType", "Id"),
    (Regime("VATC"), IdType("VRN"), Id("123456789")),
    (Regime("ITSA"), IdType("NINO"), Id("AB123456C"))
  ).forEvery { (regime, idType, id) =>
    val enrolmentKey = AgnosticEnrolmentKey(regime, idType, id)

    s"getSummaryData for $regime" should {

        def mockHIPSummary(responseStatus: Int): StubMapping =
          mockResponseForHIPPenaltyDetails(responseStatus, regime, idType, id, body = Some(getHIPPenaltyDetailsJson.toString()))

        val expectedSummaryResponse = Json.parse("""
            |{
            |  "noOfPoints": 2,
            |  "noOfEstimatedPenalties": 1,
            |  "noOfCrystalisedPenalties": 1,
            |  "estimatedPenaltyAmount": 246.9,
            |  "crystalisedPenaltyAmountDue": 144,
            |  "hasAnyPenaltyData": true
            |}
            |""".stripMargin)

        val uriToSummaryController = s"/${regime.value}/summary/${idType.value}/${id.value}"

          s"return OK (${Status.OK})" when {
            "the get penalty summary call succeeds" in {
              mockStubResponseForAuthorisedUser
              mockHIPSummary(OK)

              val result = await(buildClientForRequestToApp(uri = uriToSummaryController).get())

              result.status shouldBe OK
              result.json shouldBe expectedSummaryResponse
            }
          }

          s"return a NO_CONTENT 204" when {
            "a 422 response with 'Invalid ID Number' is returned" in {
              mockStubResponseForAuthorisedUser

              val notFoundResponseBody = """{ "errors": { "processingDate": "2025-03-03", "code": "016", "text": "Invalid ID Number" } }"""

              mockResponseForHIPPenaltyDetails(UNPROCESSABLE_ENTITY, regime, idType, id, body = Some(notFoundResponseBody))


              val result = await(buildClientForRequestToApp(uri = uriToSummaryController).get())
              result.status shouldBe NO_CONTENT
            }
          }

          "return the status from HIP" when {
            "a 404 response is returned" in {
              mockStubResponseForAuthorisedUser

                mockHIPSummary(NOT_FOUND)

              val result = await(buildClientForRequestToApp(uri = uriToSummaryController).get())
              result.status shouldBe NOT_FOUND
            }

            "an error response is returned" in {
              mockStubResponseForAuthorisedUser

                mockHIPSummary(INTERNAL_SERVER_ERROR)

              val result = await(buildClientForRequestToApp(uri = uriToSummaryController).get())
              result.status shouldBe INTERNAL_SERVER_ERROR
            }

            "a 200 response with empty body is returned" in {
              mockStubResponseForAuthorisedUser

              val emptyResponseBody = """{
                  "success": {
                    "processingDate": "2025-04-24T12:00:00Z",
                    "penaltyData": {}
                  }
                }"""

                mockResponseForHIPPenaltyDetails(OK, regime, idType, id, body = Some(emptyResponseBody))

              val result = await(buildClientForRequestToApp(uri = uriToSummaryController).get())
              result.status shouldBe NO_CONTENT
            }

              "a 422 response with 'No Data Identified' is returned" in {
                mockStubResponseForAuthorisedUser

                val noDataResponseBody = """{ "errors": { "processingDate": "2025-03-03", "code": "018", "text": "No Data Identified" } }"""

                mockResponseForHIPPenaltyDetails(UNPROCESSABLE_ENTITY, regime, idType, id, body = Some(noDataResponseBody))

                val result = await(buildClientForRequestToApp(uri = uriToSummaryController).get())
                result.status shouldBe NO_CONTENT
            }
          }
        }

    s"getFinancialDetails for $regime" should {
        def buildMockApiCall(responseStatus: Int): StubMapping = mockResponseForGetFinancialDetails(
          responseStatus,
          hipRequestBody.toJsonRequest(enrolmentKey).toString(),
          getFinancialDetailsHipResponseAsJson.toString())

        val expectedResponse = getFinancialDetailsHipResponseAsJson
        val uriToController  = s"/${regime.value}/penalty/financial-data/${idType.value}/${id.value}$financialDetailsQueryParams"

          s"return OK (${Status.OK})" when {
            "the get Financial Details call succeeds" in {
              mockStubResponseForAuthorisedUser
              buildMockApiCall(OK)

              val result = await(buildClientForRequestToApp(uri = uriToController).get())

              result.status shouldBe OK
              result.json shouldBe expectedResponse
              wireMockServer
                .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                .asScala
                .toList
                .exists(_.getBodyAsString.contains("Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval")) shouldBe true
            }
          }

          "return the status from HIP" when {
            "a 404 response is returned" in {
              mockStubResponseForAuthorisedUser
              buildMockApiCall(NOT_FOUND)

              val result = await(buildClientForRequestToApp(uri = uriToController).get())
              result.status shouldBe NOT_FOUND
              wireMockServer
                .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                .asScala
                .toList
                .exists(_.getBodyAsString.contains("Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval")) shouldBe true
            }

            "an error response is returned" in {
              mockStubResponseForAuthorisedUser
              buildMockApiCall(BAD_REQUEST)

              val result = await(buildClientForRequestToApp(uri = uriToController).get())
              result.status shouldBe BAD_REQUEST
              wireMockServer
                .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                .asScala
                .toList
                .exists(_.getBodyAsString.contains("Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval")) shouldBe true
            }

              "a 422-016 response (Invalid ID Number) is returned" in {
                mockStubResponseForAuthorisedUser

                val hipInvalidIdError = """{ "errors": { "processingDate": "2025-03-03", "code": "016", "text": "Invalid ID Number" } }"""
                mockResponseForGetFinancialDetails(UNPROCESSABLE_ENTITY, hipRequestBody.toJsonRequest(enrolmentKey).toString(), hipInvalidIdError)

                val result = await(buildClientForRequestToApp(uri = uriToController).get())
                result.status shouldBe NOT_FOUND
                wireMockServer
                  .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                  .asScala
                  .toList
                  .exists(_.getBodyAsString.contains("Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval")) shouldBe true
              }

              "a 422-018 response (No Data Identified) is returned" in {
                mockStubResponseForAuthorisedUser

                val hipNoDataError = """{ "errors": { "processingDate": "2025-03-03", "code": "018", "text": "No Data Identified" } }"""
                mockResponseForGetFinancialDetails(UNPROCESSABLE_ENTITY, hipRequestBody.toJsonRequest(enrolmentKey).toString(), hipNoDataError)

                val result = await(buildClientForRequestToApp(uri = uriToController).get())
                result.status shouldBe NOT_FOUND
                wireMockServer
                  .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                  .asScala
                  .toList
                  .exists(_.getBodyAsString.contains("Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval")) shouldBe true
            }
          }
    }

    s"getPenaltyDetails for $regime" should {

        def mockHIPPenaltyDetails(responseStatus: Int): StubMapping =
          mockResponseForHIPPenaltyDetails(responseStatus, regime, idType, id, body = Some(getHIPPenaltyDetailsJson.toString()), dateLimit = Some("09"))

        val expectedPenaltyResponse = Json.parse("""
            |{
            | "totalisations": {
            |   "LSPTotalValue": 200,
            |   "penalisedPrincipalTotal": 2000,
            |   "LPPPostedTotal": 165.25,
            |   "LPPEstimatedTotal": 15.26
            | },
            | "lateSubmissionPenalty": {
            |   "summary": {
            |     "activePenaltyPoints": 2,
            |     "inactivePenaltyPoints": 0,
            |     "regimeThreshold": 5,
            |     "penaltyChargeAmount": 200,
            |     "PoCAchievementDate": "2022-01-01"
            |   },
            |   "details": []
            | },
            | "latePaymentPenalty": {
            |   "details": [
            |     {
            |       "penaltyCategory": "LPP2",
            |       "principalChargeReference": "1234567890",
            |       "penaltyChargeCreationDate": "2022-10-30",
            |       "penaltyStatus": "A",
            |       "principalChargeBillingFrom": "2022-10-30",
            |       "principalChargeBillingTo": "2022-10-30",
            |       "principalChargeDueDate": "2022-10-30",
            |       "communicationsDate": "2022-10-30",
            |       "penaltyAmountPosted": 0,
            |       "LPP1LRDays": "15",
            |       "LPP1HRDays": "31",
            |       "LPP2Days": "31",
            |       "LPP1HRCalculationAmount": 123.45,
            |       "LPP1LRCalculationAmount": 123.45,
            |       "LPP2Percentage": 4,
            |       "LPP1LRPercentage": 2,
            |       "LPP1HRPercentage": 2,
            |       "penaltyChargeDueDate": "2022-10-30",
            |       "penaltyAmountAccruing": 246.9,
            |       "principalChargeMainTransaction": "4700",
            |       "supplement": false
            |     },
            |     {
            |       "penaltyCategory": "LPP1",
            |       "principalChargeReference": "1234567891",
            |       "penaltyChargeCreationDate": "2022-10-30",
            |       "penaltyStatus": "P",
            |       "principalChargeBillingFrom": "2022-10-30",
            |       "principalChargeBillingTo": "2022-10-30",
            |       "principalChargeDueDate": "2022-10-30",
            |       "communicationsDate": "2022-10-30",
            |       "penaltyAmountOutstanding": 144,
            |       "penaltyAmountPosted": 144,
            |       "penaltyAmountPaid": 0,
            |       "LPP1LRDays": "15",
            |       "LPP1HRDays": "31",
            |       "LPP2Days": "31",
            |       "LPP1HRCalculationAmount": 99.99,
            |       "LPP1LRCalculationAmount": 99.99,
            |       "LPP2Percentage": 4,
            |       "LPP1LRPercentage": 2,
            |       "LPP1HRPercentage": 2,
            |       "penaltyChargeDueDate": "2022-10-30",
            |       "penaltyAmountAccruing": 0,
            |       "principalChargeMainTransaction": "4700",
            |       "supplement": false
            |     }
            |   ],
            |   "ManualLPPIndicator": true
            | }
            |}
            |""".stripMargin)

        val uriToPenaltyController = s"/${regime.value}/penalty-details/${idType.value}/${id.value}?dateLimit=09"

          s"return OK (${Status.OK})" when {
            "the get Penalty Details call succeeds" in {
              mockStubResponseForAuthorisedUser

                mockHIPPenaltyDetails(OK)

              val result = await(buildClientForRequestToApp(uri = uriToPenaltyController).get())

              result.status shouldBe OK
              result.json shouldBe expectedPenaltyResponse
              wireMockServer
                .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                .asScala
                .toList
                .exists(_.getBodyAsString.contains("Penalties3rdPartyPenaltyDetailsDataRetrieval")) shouldBe true
            }
          }

          s"return NOT_FOUND (${Status.NOT_FOUND})" when {
            "a 404 response is returned" in {
              mockStubResponseForAuthorisedUser

              mockHIPPenaltyDetails(NOT_FOUND)

              val result = await(buildClientForRequestToApp(uri = uriToPenaltyController).get())
              result.status shouldBe NOT_FOUND
              wireMockServer
                .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                .asScala
                .toList
                .exists(_.getBodyAsString.contains("Penalties3rdPartyPenaltyDetailsDataRetrieval")) shouldBe true
            }

              "a 422-016 response (Invalid ID Number) is returned" in {
                mockStubResponseForAuthorisedUser

                val hipInvalidIdError = """{ "errors": { "processingDate": "2025-03-03", "code": "016", "text": "Invalid ID Number" } }"""
                mockResponseForHIPPenaltyDetails(UNPROCESSABLE_ENTITY, regime, idType, id, dateLimit = Some("09"), body = Some(hipInvalidIdError))

                val result = await(buildClientForRequestToApp(uri = uriToPenaltyController).get())
                result.status shouldBe NOT_FOUND
                wireMockServer
                  .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                  .asScala
                  .toList
                  .exists(_.getBodyAsString.contains("Penalties3rdPartyPenaltyDetailsDataRetrieval")) shouldBe true
            }
          }

          "return the status from HIP" when {
            "an error response is returned" in {
              mockStubResponseForAuthorisedUser

              mockHIPPenaltyDetails(INTERNAL_SERVER_ERROR)

              val result = await(buildClientForRequestToApp(uri = uriToPenaltyController).get())
              result.status shouldBe INTERNAL_SERVER_ERROR
              wireMockServer
                .findAll(postRequestedFor(urlEqualTo("/write/audit")))
                .asScala
                .toList
                .exists(_.getBodyAsString.contains("Penalties3rdPartyPenaltyDetailsDataRetrieval")) shouldBe true
            }
          }
    }
  }
}

object APIControllerISpec {

  val financialDetailsQueryParams: String =
    "?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false" +
      s"&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true" +
      s"&addPostedInterestDetails=true&addAccruingInterestDetails=true"
}
