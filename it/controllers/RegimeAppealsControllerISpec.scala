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
import config.featureSwitches.{CallAPI1808HIP, FeatureSwitching}
import models.appeals.MultiplePenaltiesData
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import utils.{AuthMock, FileNotificationOrchestratorWiremock, HIPWiremock, IntegrationSpecCommonBase, RegimeAppealWiremock, RegimeETMPWiremock}
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import play.api.libs.ws.WSResponse

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class RegimeAppealsControllerISpec
    extends IntegrationSpecCommonBase
    with RegimeETMPWiremock
    with HIPWiremock
    with RegimeAppealWiremock
    with FileNotificationOrchestratorWiremock
    with FeatureSwitching
    with TableDrivenPropertyChecks
    with AuthMock {

  setEnabledFeatureSwitches()

  val controller: RegimeAppealsController = injector.instanceOf[RegimeAppealsController]

  val appealJson: JsValue = Json.parse("""
      |{
      |  "type": "LATE_SUBMISSION",
      |  "startDate": "2021-04-23T18:25:43.511",
      |  "endDate": "2021-04-23T18:25:43.511",
      |  "dueDate": "2021-04-23T18:25:43.511",
      |  "dateCommunicationSent": "2021-04-23T18:25:43.511"
      |}
      |""".stripMargin)

  val appealJsonLPP: JsValue = Json.parse("""
      |{
      |  "type": "LATE_PAYMENT",
      |	 "startDate": "2023-01-01T18:25:43.511",
      |	 "endDate" : "2023-03-31T18:25:43.511",
      |	 "dueDate" : "2023-05-07T18:25:43.511",
      |  "dateCommunicationSent": "2021-05-08T18:25:43.511"
      |}
      |""".stripMargin)

  val appealJsonLPPAdditional: JsValue = Json.parse("""
      |{
      |  "type": "ADDITIONAL",
      |	 "startDate": "2023-01-01T18:25:43.511",
      |	 "endDate" : "2023-03-31T18:25:43.511",
      |	 "dueDate" : "2023-05-07T18:25:43.511",
      |  "dateCommunicationSent": "2021-05-08T18:25:43.511"
      |}
      |""".stripMargin)

  val appealV2Json: JsValue = Json.parse("""
      |{
      |  "type": "LATE_SUBMISSION",
      |  "startDate": "2023-01-01",
      |  "endDate": "2023-12-31",
      |  "dueDate": "2024-02-07",
      |  "dateCommunicationSent": "2024-02-08"
      |}
      |""".stripMargin)

  val appealV2JsonLPP: JsValue = Json.parse("""
      |{
      |  "type": "LATE_PAYMENT",
      |	 "startDate": "2022-01-01",
      |	 "endDate" : "2022-12-31",
      |	 "dueDate" : "2023-02-07",
      |  "dateCommunicationSent": "2023-02-08"
      |}
      |""".stripMargin)

  val appealV2JsonLPPAdditional: JsValue = Json.parse("""
      |{
      |  "type": "ADDITIONAL",
      |	 "startDate": "2024-01-01",
      |	 "endDate" : "2024-12-31",
      |	 "dueDate" : "2025-02-07",
      |  "dateCommunicationSent": "2025-02-08"
      |}
      |""".stripMargin)

  val penaltyDetailsJson: JsValue = Json.parse(s"""
     |{
     |  "success": {
     |    "processingDate": "$mockInstant",
     |    "penaltyData": {
     |      "totalisations": {
     |        "LSPTotalValue": 200,
     |        "penalisedPrincipalTotal": 2000,
     |        "LPPPostedTotal": 165.25,
     |        "LPPEstimatedTotal": 15.26
     |      },
     |      "lsp": {
     |        "lspSummary": {
     |          "activePenaltyPoints": 10,
     |          "inactivePenaltyPoints": 12,
     |          "regimeThreshold": 10,
     |          "penaltyChargeAmount": 684.25,
     |          "pocAchievementDate": "2022-01-01"
     |        },
     |        "lspDetails": [
     |          {
     |            "penaltyNumber": "123456789",
     |            "penaltyOrder": "01",
     |            "penaltyCategory": "P",
     |            "penaltyStatus": "ACTIVE",
     |            "penaltyCreationDate": "2023-01-01",
     |            "penaltyExpiryDate": "2023-12-31",
     |            "communicationsDate": "2024-02-08",
     |            "lateSubmissions": [
     |              {
     |                "lateSubmissionID": "001",
     |                "taxPeriod": "23AA",
     |                "taxPeriodStartDate": "2023-01-01",
     |                "taxPeriodEndDate": "2023-12-31",
     |                "taxPeriodDueDate": "2024-02-07",
     |                "returnReceiptDate": "2024-02-01",
     |                "taxReturnStatus": "Fulfilled"
     |              }
     |            ],
     |            "appealInformation": [
     |              {
     |                "appealStatus": "99",
     |                "appealDescription": "Some value"
     |              }
     |            ],
     |            "chargeDueDate": "2024-02-07",
     |            "chargeOutstandingAmount": 200,
     |            "chargeAmount": 200,
     |            "triggeringProcess": "P123",
     |            "chargeReference": "CHARGEREF1"
     |          }
     |        ]
     |      },
     |      "lpp": {
     |        "lppDetails": [
     |          {
     |            "penaltyCategory": "LPP1",
     |            "penaltyChargeReference": "1234567887",
     |            "principalChargeReference": "1234567890",
     |            "penaltyChargeCreationDate": "2022-01-01",
     |            "penaltyStatus": "A",
     |            "penaltyChargeAmount": 99.99,
     |            "penaltyAmountPosted": 0,
     |            "penaltyAmountOutstanding": null,
     |            "penaltyAmountPaid": null,
     |            "penaltyAmountAccruing": 99.99,
     |            "principalChargeMainTransaction": "4700",
     |            "principalChargeBillingFrom": "2022-01-01",
     |            "principalChargeBillingTo": "2022-12-31",
     |            "principalChargeDueDate": "2023-02-07",
     |            "lpp1LRDays": "15",
     |            "lpp1HRDays": "31",
     |            "lpp2Days": "31",
     |            "lpp1HRCalculationAmount": 99.99,
     |            "lpp1LRCalculationAmount": 99.99,
     |            "lpp2Percentage": 4.00,
     |            "lpp1LRPercentage": 2.00,
     |            "lpp1HRPercentage": 2.00,
     |            "communicationsDate": "2023-02-08",
     |            "penaltyChargeDueDate": "2023-02-07",
     |            "appealInformation": [
     |              {
     |                "appealStatus": "99",
     |                "appealLevel": "01",
     |                "appealDescription": "Some value"
     |              }
     |            ],
     |            "principalChargeLatestClearing": null,
     |            "vatOutstandingAmount": null,
     |            "timeToPay": [
     |              {
     |                "ttpStartDate": "2022-01-01",
     |                "ttpEndDate": "2022-12-31"
     |              }
     |            ],
     |            "principalChargeDocNumber": "DOC1",
     |            "principalChargeSubTransaction": "SUB1"
     |          },
     |          {
     |            "penaltyCategory": "LPP2",
     |            "penaltyChargeReference": "1234567889",
     |            "principalChargeReference": "1234567890",
     |            "penaltyChargeCreationDate": "2024-01-01",
     |            "penaltyStatus": "A",
     |            "penaltyChargeAmount": 0,
     |            "penaltyAmountPosted": 0,
     |            "penaltyAmountOutstanding": null,
     |            "penaltyAmountPaid": null,
     |            "penaltyAmountAccruing": 0,
     |            "principalChargeMainTransaction": "4700",
     |            "principalChargeBillingFrom": "2024-01-01",
     |            "principalChargeBillingTo": "2024-12-31",
     |            "principalChargeDueDate": "2025-02-07",
     |            "lpp1LRDays": "15",
     |            "lpp1HRDays": "31",
     |            "lpp2Days": "31",
     |            "lpp1HRCalculationAmount": 0,
     |            "lpp1LRCalculationAmount": 0,
     |            "lpp2Percentage": 0,
     |            "lpp1LRPercentage": 0,
     |            "lpp1HRPercentage": 0,
     |            "communicationsDate": "2025-02-08",
     |            "penaltyChargeDueDate": "2025-02-07",
     |            "appealInformation": [],
     |            "principalChargeLatestClearing": null,
     |            "vatOutstandingAmount": null,
     |            "timeToPay": [],
     |            "principalChargeDocNumber": "DOC3",
     |            "principalChargeSubTransaction": "SUB3"
     |          }
     |        ]
     |      },
     |      "breathingSpace": [
     |        {
     |          "bsStartDate": "2023-01-01",
     |          "bsEndDate": "2023-12-31"
     |        }
     |      ]
     |    }
     |  }
     |}
     |""".stripMargin)

  class SetUp(hipFeatureSwitch: Boolean = false) {
    if (hipFeatureSwitch) {
      setEnabledFeatureSwitches(CallAPI1808HIP)
    } else {
      disableFeatureSwitch(CallAPI1808HIP)
    }
    mockStubResponseForAuthorisedUser
    mockSuccessfulResponse()
  }

  Table(
    ("Regime", "IdType", "Id"),
    (Regime("VATC"), IdType("VRN"), Id("123456789")),
    (Regime("ITSA"), IdType("NINO"), Id("AB123456C"))
  ).forEvery { (regime, idType, id) =>
    val enrolmentKey    = AgnosticEnrolmentKey(regime, idType, id)
    val (r, it, i)      = (regime.value, idType.value, id.value)
    val submitAppealUri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=false&penaltyNumber=123456789&correlationId=uuid-1"

    s"getAppealsDataForLateSubmissionPenalty for $regime" should {
      "call ETMP and compare the penalty ID provided and the penalty ID in the payload - return OK if there is a match" in {

        mockStubResponseForAuthorisedUser
        mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsJson.toString()))

        val result = await(
          buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/late-submissions/${idType.value}/${id.value}?penaltyId=123456789").get())
        result.status shouldBe Status.OK
        result.body shouldBe appealV2Json.toString()
      }

      "return NOT_FOUND when the penalty ID given does not match the penalty ID in the payload" in {

        mockStubResponseForAuthorisedUser
        mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsJson.toString()))

        val result =
          await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/late-submissions/${idType.value}/${id.value}?penaltyId=0001").get())
        result.status shouldBe Status.NOT_FOUND
      }

      "return an ISE when the call to ETMP fails" in {

        mockStubResponseForAuthorisedUser
        mockStubResponseForPenaltyDetails(Status.INTERNAL_SERVER_ERROR, regime, idType, id, Some(""))

        val result = await(
          buildClientForRequestToApp(
            uri = s"/${regime.value}/appeals-data/late-submissions/${idType.value}/${id.value}?penaltyId=123456789"
          ).get())
        result.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    s"getAppealsDataForLatePaymentPenalty for $regime" should {
      "call ETMP and compare the penalty ID provided and the penalty ID in the payload - return OK if there is a match" in {

        mockStubResponseForAuthorisedUser
        mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsJson.toString()))

        val result = await(
          buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/late-payments/${idType.value}/${id.value}?penaltyId=1234567887&isAdditional=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe appealV2JsonLPP.toString()
      }

      "call ETMP and compare the penalty ID provided and the penalty ID in the payload for Additional - return OK if there is a match" in {

        mockStubResponseForAuthorisedUser
        mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsJson.toString()))

        val result = await(
          buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/late-payments/${idType.value}/${id.value}?penaltyId=1234567889&isAdditional=true").get())
        result.status shouldBe Status.OK
        result.body shouldBe appealV2JsonLPPAdditional.toString()
      }

      "return NOT_FOUND when the penalty ID given does not match the penalty ID in the payload" in {

        mockStubResponseForAuthorisedUser
        mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsJson.toString()))

        val result = await(
          buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/late-payments/${idType.value}/${id.value}?penaltyId=0001&isAdditional=false").get())
        result.status shouldBe Status.NOT_FOUND
      }

      "return an ISE when the call to ETMP fails" in {

        mockStubResponseForAuthorisedUser
        mockStubResponseForPenaltyDetails(Status.INTERNAL_SERVER_ERROR, regime, idType, id, Some(""))

        val result = await(
          buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/late-payments/${idType.value}/${id.value}?penaltyId=0001&isAdditional=false").get())
        result.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    s"getReasonableExcuses for $regime" should {
      "return all active reasonable excuses" in {
        val jsonExpectedToReturn: JsValue = Json.parse("""
            |{
            |  "excuses": [
            |    {
            |      "type": "bereavement",
            |      "descriptionKey": "reasonableExcuses.bereavementReason"
            |    },
            |    {
            |      "type": "crime",
            |      "descriptionKey": "reasonableExcuses.crimeReason"
            |    },
            |    {
            |      "type": "fireOrFlood",
            |      "descriptionKey": "reasonableExcuses.fireOrFloodReason"
            |    },
            |    {
            |      "type": "health",
            |      "descriptionKey": "reasonableExcuses.healthReason"
            |    },
            |    {
            |      "type": "lossOfStaff",
            |      "descriptionKey": "reasonableExcuses.lossOfStaffReason"
            |    },
            |    {
            |      "type": "technicalIssues",
            |      "descriptionKey": "reasonableExcuses.technicalIssuesReason"
            |    },
            |    {
            |      "type": "other",
            |      "descriptionKey": "reasonableExcuses.otherReason"
            |    }
            |  ]
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals-data/reasonable-excuses").get())
        result.status shouldBe OK
        Json.parse(result.body) shouldBe jsonExpectedToReturn
      }
    }

    s"submitAppeal for $regime" should {
      "call the connector and send the appeal data received in the request body" when {
        "returns OK when successful for bereavement" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						"reasonableExcuse": "bereavement",
              |           "honestyDeclaration": true,
              |           "startDateOfEvent": "2021-04-23T00:00:00",
              |						"statement": "This is a statement",
              |           "lateAppeal": false
              |		}
              |}
              |""".stripMargin
          )
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }

        "returns OK when successful for crime" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "crime",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "reportedIssueToPolice": "yes",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }

        "returns OK when successful for fire or flood" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |          "reasonableExcuse": "fireandflood",
              |          "honestyDeclaration": true,
              |          "startDateOfEvent": "2021-04-23T00:00:00",
              |          "statement": "This is a statement",
              |          "lateAppeal": false
              |    }
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }

        "returns OK when successful for loss of staff" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "lossOfEssentialStaff",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }

        "returns OK when successful for technical issues" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |					 	 "reasonableExcuse": "technicalIssue",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "endDateOfEvent": "2021-04-24T00:00:01",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }

        "returns OK when successful for health" when {
          "there has been no hospital stay" in new SetUp {
            mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")

            val jsonToSubmit: JsValue = Json.parse("""
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
                |    "customerReferenceNo": "123456789",
                |    "dateOfAppeal": "2020-01-01T00:00:00",
                |    "isLPP": false,
                |    "appealSubmittedBy": "customer",
                |    "appealInformation": {
                |						 "reasonableExcuse": "health",
                |            "honestyDeclaration": true,
                |            "startDateOfEvent": "2021-04-23T00:00:00",
                |            "hospitalStayInvolved": false,
                |            "eventOngoing": false,
                |						 "statement": "This is a statement",
                |            "lateAppeal": false
                |		}
                |}
                |""".stripMargin)
            val result: WSResponse = await(
              buildClientForRequestToApp(uri = submitAppealUri).post(
                jsonToSubmit
              ))
            result.status shouldBe OK
          }

          "there is an ongoing hospital stay" in new SetUp {
            mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")

            val jsonToSubmit: JsValue = Json.parse("""
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
                |    "customerReferenceNo": "123456789",
                |    "dateOfAppeal": "2020-01-01T00:00:00",
                |    "isLPP": false,
                |    "appealSubmittedBy": "customer",
                |    "appealInformation": {
                |						 "reasonableExcuse": "health",
                |            "honestyDeclaration": true,
                |            "startDateOfEvent": "2021-04-23T00:00:00",
                |            "hospitalStayInvolved": true,
                |            "eventOngoing": true,
                |						 "statement": "This is a statement",
                |            "lateAppeal": false
                |		}
                |}
                |""".stripMargin)
            val result: WSResponse = await(
              buildClientForRequestToApp(uri = submitAppealUri).post(
                jsonToSubmit
              ))
            result.status shouldBe OK
          }

          "there has been a hospital stay" in new SetUp {
            mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")

            val jsonToSubmit: JsValue = Json.parse("""
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
                |    "customerReferenceNo": "123456789",
                |    "dateOfAppeal": "2020-01-01T00:00:00",
                |    "isLPP": false,
                |    "appealSubmittedBy": "customer",
                |    "appealInformation": {
                |						 "reasonableExcuse": "health",
                |            "honestyDeclaration": true,
                |            "startDateOfEvent": "2021-04-23T00:00:00",
                |            "endDateOfEvent": "2021-04-23T00:00:01",
                |            "hospitalStayInvolved": true,
                |            "eventOngoing": false,
                |						 "statement": "This is a statement",
                |            "lateAppeal": false
                |		}
                |}
                |""".stripMargin)
            val result: WSResponse = await(
              buildClientForRequestToApp(uri = submitAppealUri).post(
                jsonToSubmit
              ))
            result.status shouldBe OK
          }
        }

        "returns OK when successful for other with file upload" in new SetUp {

          mockStubResponseForAuthorisedUser
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")
          mockResponseForFileNotificationOrchestrator(OK)

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "other",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false,
              |            "uploadedFiles": [
              |               {
              |                 "reference":"reference-3000",
              |                 "fileStatus":"READY",
              |                 "downloadUrl":"download.file",
              |                 "uploadDetails": {
              |                     "fileName":"file1.txt",
              |                     "fileMimeType":"text/plain",
              |                     "uploadTimestamp":"2018-04-24T09:30:00",
              |                     "checksum":"check12345678",
              |                     "size":987
              |                 },
              |                 "uploadFields": {
              |                     "key": "abcxyz",
              |                     "x-amz-algorithm" : "AWS4-HMAC-SHA256"
              |                 },
              |                 "lastUpdated":"2018-04-24T09:30:00"
              |               }
              |            ]
              |		}
              |}
              |""".stripMargin
          )
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }

        "returns OK when successful for other with file upload (audit storage failure) - single appeal" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")
          mockResponseForFileNotificationOrchestrator(INTERNAL_SERVER_ERROR)

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "other",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false,
              |            "uploadedFiles": [
              |               {
              |                 "reference":"reference-3000",
              |                 "fileStatus":"READY",
              |                 "downloadUrl":"download.file",
              |                 "uploadDetails": {
              |                     "fileName":"file1.txt",
              |                     "fileMimeType":"text/plain",
              |                     "uploadTimestamp":"2018-04-24T09:30:00",
              |                     "checksum":"check12345678",
              |                     "size":987
              |                 },
              |                 "uploadFields": {
              |                     "key": "abcxyz",
              |                     "x-amz-algorithm" : "AWS4-HMAC-SHA256"
              |                 },
              |                 "lastUpdated":"2018-04-24T09:30:00"
              |               }
              |            ]
              |		}
              |}
              |""".stripMargin
          )
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
          eventually {
            wireMockServer
              .findAll(postRequestedFor(urlEqualTo("/write/audit")))
              .asScala
              .toList
              .exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
          }
        }

        "returns OK when successful for LPP" in new SetUp {

          mockStubResponseForAuthorisedUser
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, isLPP = true, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": true,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "crime",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "reportedIssueToPolice": "yes",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=true&penaltyNumber=123456789&correlationId=uuid-1")
              .post(
                jsonToSubmit
              ))
          result.status shouldBe OK
        }

        "returns OK when successful for other with file upload (audit storage failure) - part of multi appeal" in new SetUp {
          mockStubResponseForAuthorisedUser
          mockResponseForAppealSubmissionStub(OK, enrolmentKey, penaltyNumber = "123456789")
          mockResponseForFileNotificationOrchestrator(INTERNAL_SERVER_ERROR)

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "other",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false,
              |            "uploadedFiles": [
              |               {
              |                 "reference":"reference-3000",
              |                 "fileStatus":"READY",
              |                 "downloadUrl":"download.file",
              |                 "uploadDetails": {
              |                     "fileName":"file1.txt",
              |                     "fileMimeType":"text/plain",
              |                     "uploadTimestamp":"2018-04-24T09:30:00",
              |                     "checksum":"check12345678",
              |                     "size":987
              |                 },
              |                 "uploadFields": {
              |                     "key": "abcxyz",
              |                     "x-amz-algorithm" : "AWS4-HMAC-SHA256"
              |                 },
              |                 "lastUpdated":"2018-04-24T09:30:00"
              |               }
              |            ]
              |		}
              |}
              |""".stripMargin
          )
          val expectedJsonResponse: JsObject = Json.obj(
            "caseId" -> "PR-1234567889",
            "status" -> MULTI_STATUS,
            "error" -> "Appeal submitted (case ID: PR-1234567889, correlation ID: uuid-1) but received 500 response from file notification orchestrator"
          )

          val result: WSResponse = await(
            buildClientForRequestToApp(
              uri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=false&penaltyNumber=123456789&correlationId=uuid-1&isMultiAppeal=true"
            ).post(
              jsonToSubmit
            ))

          result.status shouldBe MULTI_STATUS
          Json.parse(result.body) shouldBe expectedJsonResponse
          eventually {
            wireMockServer
              .findAll(postRequestedFor(urlEqualTo("/write/audit")))
              .asScala
              .toList
              .exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
          }
        }
      }

      "return BAD_REQUEST (400)" when {
        "no JSON body is in the request" in new SetUp {
          val result: WSResponse = await(
            buildClientForRequestToApp(
              uri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=true&penaltyNumber=123456789&correlationId=uuid-1"
            ).post(""))
          result.status shouldBe BAD_REQUEST
        }

        "JSON body is present but it can not be parsed to a model" in new SetUp {
          val result: WSResponse = await(
            buildClientForRequestToApp(
              uri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=true&penaltyNumber=123456789&correlationId=uuid-1"
            ).post(Json.parse("{}")))
          result.status shouldBe BAD_REQUEST
        }
      }

      "return error status code" when {
        "the call to PEGA/stub fails" in new SetUp {
          mockResponseForAppealSubmissionStub(GATEWAY_TIMEOUT, enrolmentKey, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "crime",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "reportedIssueToPolice": "yes",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe GATEWAY_TIMEOUT
        }

        "the call to PEGA/stub has a fault" in new SetUp {
          mockResponseForAppealSubmissionStubFault(enrolmentKey, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "crime",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "reportedIssueToPolice": "yes",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    s"submitAppeal for $regime calling HIP" should {
      "call the connector and send the appeal data received in the request body" when {
        "returns OK when successful for bereavement" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						"reasonableExcuse": "bereavement",
              |           "honestyDeclaration": true,
              |           "startDateOfEvent": "2021-04-23T00:00:00",
              |						"statement": "This is a statement",
              |           "lateAppeal": false
              |		}
              |}
              |""".stripMargin
          )
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }
        "returns OK when successful for crime" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "crime",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "reportedIssueToPolice": "yes",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }
        "returns OK when successful for fire or flood" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |          "reasonableExcuse": "fireandflood",
              |          "honestyDeclaration": true,
              |          "startDateOfEvent": "2021-04-23T00:00:00",
              |          "statement": "This is a statement",
              |          "lateAppeal": false
              |    }
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }
        "returns OK when successful for loss of staff" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "lossOfEssentialStaff",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }
        "returns OK when successful for technical issues" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |					 	 "reasonableExcuse": "technicalIssue",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "endDateOfEvent": "2021-04-24T00:00:01",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }
        "returns OK when successful for health" when {

          "there has been no hospital stay" in new SetUp(hipFeatureSwitch = true) {
            val jsonToSubmit: JsValue = Json.parse("""
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
                |    "customerReferenceNo": "123456789",
                |    "dateOfAppeal": "2020-01-01T00:00:00",
                |    "isLPP": false,
                |    "appealSubmittedBy": "customer",
                |    "appealInformation": {
                |						 "reasonableExcuse": "health",
                |            "honestyDeclaration": true,
                |            "startDateOfEvent": "2021-04-23T00:00:00",
                |            "hospitalStayInvolved": false,
                |            "eventOngoing": false,
                |						 "statement": "This is a statement",
                |            "lateAppeal": false
                |		}
                |}
                |""".stripMargin)
            val result: WSResponse = await(
              buildClientForRequestToApp(uri = submitAppealUri).post(
                jsonToSubmit
              ))
            result.status shouldBe OK
          }

          "there is an ongoing hospital stay" in new SetUp(hipFeatureSwitch = true) {
            val jsonToSubmit: JsValue = Json.parse("""
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
                |    "customerReferenceNo": "123456789",
                |    "dateOfAppeal": "2020-01-01T00:00:00",
                |    "isLPP": false,
                |    "appealSubmittedBy": "customer",
                |    "appealInformation": {
                |						 "reasonableExcuse": "health",
                |            "honestyDeclaration": true,
                |            "startDateOfEvent": "2021-04-23T00:00:00",
                |            "hospitalStayInvolved": true,
                |            "eventOngoing": true,
                |						 "statement": "This is a statement",
                |            "lateAppeal": false
                |		}
                |}
                |""".stripMargin)
            val result: WSResponse = await(
              buildClientForRequestToApp(uri = submitAppealUri).post(
                jsonToSubmit
              ))
            result.status shouldBe OK
          }

          "there has been a hospital stay" in new SetUp(hipFeatureSwitch = true) {
            val jsonToSubmit: JsValue = Json.parse("""
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
                |    "customerReferenceNo": "123456789",
                |    "dateOfAppeal": "2020-01-01T00:00:00",
                |    "isLPP": false,
                |    "appealSubmittedBy": "customer",
                |    "appealInformation": {
                |						 "reasonableExcuse": "health",
                |            "honestyDeclaration": true,
                |            "startDateOfEvent": "2021-04-23T00:00:00",
                |            "endDateOfEvent": "2021-04-23T00:00:01",
                |            "hospitalStayInvolved": true,
                |            "eventOngoing": false,
                |						 "statement": "This is a statement",
                |            "lateAppeal": false
                |		}
                |}
                |""".stripMargin)
            val result: WSResponse = await(
              buildClientForRequestToApp(uri = submitAppealUri).post(
                jsonToSubmit
              ))
            result.status shouldBe OK
          }
        }
        "returns OK when successful for other with file upload" in new SetUp(hipFeatureSwitch = true) {
          mockResponseForFileNotificationOrchestrator(OK)

          val jsonToSubmit: JsValue = Json.parse(
            """
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
                |    "customerReferenceNo": "123456789",
                |    "dateOfAppeal": "2020-01-01T00:00:00",
                |    "isLPP": false,
                |    "appealSubmittedBy": "customer",
                |    "appealInformation": {
                |						 "reasonableExcuse": "other",
                |            "honestyDeclaration": true,
                |            "startDateOfEvent": "2021-04-23T00:00:00",
                |						 "statement": "This is a statement",
                |            "lateAppeal": false,
                |            "uploadedFiles": [
                |               {
                |                 "reference":"reference-3000",
                |                 "fileStatus":"READY",
                |                 "downloadUrl":"download.file",
                |                 "uploadDetails": {
                |                     "fileName":"file1.txt",
                |                     "fileMimeType":"text/plain",
                |                     "uploadTimestamp":"2018-04-24T09:30:00",
                |                     "checksum":"check12345678",
                |                     "size":987
                |                 },
                |                 "uploadFields": {
                |                     "key": "abcxyz",
                |                     "x-amz-algorithm" : "AWS4-HMAC-SHA256"
                |                 },
                |                 "lastUpdated":"2018-04-24T09:30:00"
                |               }
                |            ]
                |		}
                |}
                |""".stripMargin
          )
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }

        "returns OK when successful for other with file upload (audit storage failure) - single appeal" in new SetUp(hipFeatureSwitch = true) {
          mockResponseForFileNotificationOrchestrator(INTERNAL_SERVER_ERROR)

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "other",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false,
              |            "uploadedFiles": [
              |               {
              |                 "reference":"reference-3000",
              |                 "fileStatus":"READY",
              |                 "downloadUrl":"download.file",
              |                 "uploadDetails": {
              |                     "fileName":"file1.txt",
              |                     "fileMimeType":"text/plain",
              |                     "uploadTimestamp":"2018-04-24T09:30:00",
              |                     "checksum":"check12345678",
              |                     "size":987
              |                 },
              |                 "uploadFields": {
              |                     "key": "abcxyz",
              |                     "x-amz-algorithm" : "AWS4-HMAC-SHA256"
              |                 },
              |                 "lastUpdated":"2018-04-24T09:30:00"
              |               }
              |            ]
              |		}
              |}
              |""".stripMargin
          )
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe OK
          eventually {
            wireMockServer
              .findAll(postRequestedFor(urlEqualTo("/write/audit")))
              .asScala
              .toList
              .exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
          }
        }

        "returns OK when successful for LPP" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": true,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "crime",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "reportedIssueToPolice": "yes",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=true&penaltyNumber=123456789&correlationId=uuid-1").post(
              jsonToSubmit
            ))
          result.status shouldBe OK
        }

        "returns OK when successful for other with file upload (audit storage failure) - part of multi appeal" in new SetUp(hipFeatureSwitch = true) {
          mockResponseForFileNotificationOrchestrator(INTERNAL_SERVER_ERROR)

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "other",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false,
              |            "uploadedFiles": [
              |               {
              |                 "reference":"reference-3000",
              |                 "fileStatus":"READY",
              |                 "downloadUrl":"download.file",
              |                 "uploadDetails": {
              |                     "fileName":"file1.txt",
              |                     "fileMimeType":"text/plain",
              |                     "uploadTimestamp":"2018-04-24T09:30:00",
              |                     "checksum":"check12345678",
              |                     "size":987
              |                 },
              |                 "uploadFields": {
              |                     "key": "abcxyz",
              |                     "x-amz-algorithm" : "AWS4-HMAC-SHA256"
              |                 },
              |                 "lastUpdated":"2018-04-24T09:30:00"
              |               }
              |            ]
              |		}
              |}
              |""".stripMargin
          )
          val expectedJsonResponse: JsObject = Json.obj(
            "caseId" -> "PR-1234567889",
            "status" -> MULTI_STATUS,
            "error" -> "Appeal submitted (case ID: PR-1234567889, correlation ID: uuid-1) but received 500 response from file notification orchestrator"
          )

          val result: WSResponse = await(
            buildClientForRequestToApp(
              uri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=false&penaltyNumber=123456789&correlationId=uuid-1&isMultiAppeal=true"
            ).post(
              jsonToSubmit
            ))

          result.status shouldBe MULTI_STATUS
          Json.parse(result.body) shouldBe expectedJsonResponse
          eventually {
            wireMockServer
              .findAll(postRequestedFor(urlEqualTo("/write/audit")))
              .asScala
              .toList
              .exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
          }
        }
      }

      "return BAD_REQUEST (400)" when {
        "no JSON body is in the request" in new SetUp(hipFeatureSwitch = true) {
          val result: WSResponse = await(
            buildClientForRequestToApp(
              uri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=true&penaltyNumber=123456789&correlationId=uuid-1"
            ).post(""))
          result.status shouldBe BAD_REQUEST
        }

        "JSON body is present but it can not be parsed to a model" in new SetUp(hipFeatureSwitch = true) {
          val result: WSResponse = await(
            buildClientForRequestToApp(
              uri = s"/$r/appeals/submit-appeal/$it/$i?isLPP=true&penaltyNumber=123456789&correlationId=uuid-1"
            ).post(Json.parse("{}")))
          result.status shouldBe BAD_REQUEST
        }
      }

      "return error status code" when {
        "the call to PEGA/stub fails" in new SetUp {
          mockResponseForAppealSubmissionStub(GATEWAY_TIMEOUT, enrolmentKey, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "crime",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "reportedIssueToPolice": "yes",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe GATEWAY_TIMEOUT
        }

        "the call to PEGA/stub has a fault" in new SetUp {
          mockResponseForAppealSubmissionStubFault(enrolmentKey, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse("""
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
              |    "customerReferenceNo": "123456789",
              |    "dateOfAppeal": "2020-01-01T00:00:00",
              |    "isLPP": false,
              |    "appealSubmittedBy": "customer",
              |    "appealInformation": {
              |						 "reasonableExcuse": "crime",
              |            "honestyDeclaration": true,
              |            "startDateOfEvent": "2021-04-23T00:00:00",
              |            "reportedIssueToPolice": "yes",
              |						 "statement": "This is a statement",
              |            "lateAppeal": false
              |		}
              |}
              |""".stripMargin)
          val result: WSResponse = await(
            buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    s"getMultiplePenaltyData for $regime" should {
      val penaltyDetailsOneLPPJson: JsValue = Json.parse(s"""
          |{
          |  "success": {
          |    "processingDate": "$mockInstant",
          |    "penaltyData": {
          |      "totalisations": {
          |        "LSPTotalValue": 200,
          |        "penalisedPrincipalTotal": 2000,
          |        "LPPPostedTotal": 165.25,
          |        "LPPEstimatedTotal": 15.26
          |      },
          |      "lpp": {
          |        "lppDetails": [
          |          {
          |            "penaltyChargeReference": "1234567887",
          |            "penaltyCategory": "LPP1",
          |            "penaltyStatus": "P",
          |            "penaltyAmountPosted": 144.00,
          |            "penaltyAmountAccruing": 0,
          |            "penaltyAmountOutstanding": 144.00,
          |            "penaltyAmountPaid": 0,
          |            "principalChargeMainTransaction": "4700",
          |            "principalChargeBillingFrom": "2022-01-01",
          |            "principalChargeBillingTo": "2022-12-31",
          |            "principalChargeDueDate": "2023-02-07",
          |            "lpp1LRDays": "15",
          |            "lpp1HRDays": "31",
          |            "lpp2Days": "31",
          |            "lpp1LRCalculationAmount": 99.99,
          |            "lpp1HRCalculationAmount": 99.99,
          |            "lpp2Percentage": 4.00,
          |            "lpp1LRPercentage": 2.00,
          |            "lpp1HRPercentage": 2.00,
          |            "communicationsDate": "2023-02-08",
          |            "penaltyChargeDueDate": "2022-10-30",
          |            "principalChargeLatestClearing": "2023-04-01",
          |            "vatOutstandingAmount": null,
          |            "timeToPay": [
          |              {
          |                "ttpStartDate": "2022-01-01",
          |                "ttpEndDate": "2022-12-31"
          |              }
          |            ],
          |            "principalChargeReference": "1234567890",
          |            "principalChargeDocNumber": "DOC1",
          |            "principalChargeSubTransaction": "SUB1"
          |          }
          |        ]
          |      }
          |    }
          |  }
          |}
          |""".stripMargin)

      val penaltyDetailsTwoLPPsJson: JsValue = Json.parse(s"""
     |{
     |  "success": {
     |    "processingDate": "$mockInstant",
     |    "penaltyData": {
     |      "totalisations": {
     |        "LSPTotalValue": 200,
     |        "penalisedPrincipalTotal": 2000,
     |        "LPPPostedTotal": 165.25,
     |        "LPPEstimatedTotal": 15.26
     |      },
     |      "lpp": {
     |        "lppDetails": [
     |          {
     |            "penaltyCategory": "LPP2",
     |            "penaltyChargeReference": "1234567888",
     |            "principalChargeReference": "1234567890",
     |            "penaltyChargeCreationDate": "2022-10-30",
     |            "penaltyStatus": "P",
     |            "penaltyAmountPosted": 144.00,
     |            "penaltyAmountAccruing": 0,
     |            "penaltyAmountOutstanding": 144.00,
     |            "penaltyAmountPaid": 0,
     |            "principalChargeMainTransaction": "4700",
     |            "principalChargeBillingFrom": "2022-01-01",
     |            "principalChargeBillingTo": "2022-12-31",
     |            "principalChargeDueDate": "2023-02-07",
     |            "lpp1LRDays": "15",
     |            "lpp1HRDays": "31",
     |            "lpp2Days": "31",
     |            "lpp1LRCalculationAmount": 99.99,
     |            "lpp1HRCalculationAmount": 99.99,
     |            "lpp2Percentage": 4.00,
     |            "lpp1LRPercentage": 2.00,
     |            "lpp1HRPercentage": 2.00,
     |            "communicationsDate": "2023-02-08",
     |            "penaltyChargeDueDate": "2022-10-30",
     |            "principalChargeLatestClearing": "2023-04-01",
     |            "vatOutstandingAmount": null,
     |            "timeToPay": [
     |              {
     |                "ttpStartDate": "2022-01-01",
     |                "ttpEndDate": "2022-12-31"
     |              }
     |            ],
     |            "principalChargeDocNumber": "DOC1",
     |            "principalChargeSubTransaction": "SUB1"
     |          },
     |          {
     |            "penaltyCategory": "LPP1",
     |            "penaltyChargeReference": "1234567887",
     |            "principalChargeReference": "1234567890",
     |            "penaltyChargeCreationDate": "2022-10-30",
     |            "penaltyStatus": "P",
     |            "penaltyAmountPosted": 144.01,
     |            "penaltyAmountAccruing": 0,
     |            "penaltyAmountOutstanding": 144.01,
     |            "penaltyAmountPaid": 0,
     |            "principalChargeMainTransaction": "4700",
     |            "principalChargeBillingFrom": "2022-01-01",
     |            "principalChargeBillingTo": "2022-12-31",
     |            "principalChargeDueDate": "2023-02-07",
     |            "lpp1LRDays": "15",
     |            "lpp1HRDays": "31",
     |            "lpp2Days": "31",
     |            "lpp1LRCalculationAmount": 99.99,
     |            "lpp1HRCalculationAmount": 99.99,
     |            "lpp2Percentage": 4.00,
     |            "lpp1LRPercentage": 2.00,
     |            "lpp1HRPercentage": 2.00,
     |            "communicationsDate": "2023-01-08",
     |            "penaltyChargeDueDate": "2022-10-30",
     |            "principalChargeLatestClearing": "2023-04-01",
     |            "vatOutstandingAmount": null,
     |            "timeToPay": [
     |              {
     |                "ttpStartDate": "2022-01-01",
     |                "ttpEndDate": "2022-12-31"
     |              }
     |            ],
     |            "principalChargeDocNumber": "DOC2",
     |            "principalChargeSubTransaction": "SUB2"
     |          }
     |        ]
     |      }
     |    }
     |  }
     |}
     |""".stripMargin)
      val penaltyDetailsTwoLPPsWithAppealsJson: JsValue = Json.parse(s"""
          |{
          |  "success": {
          |    "processingDate": "$mockInstant",
          |    "penaltyData": {
          |      "totalisations": {
          |        "LSPTotalValue": 200,
          |        "penalisedPrincipalTotal": 2000,
          |        "LPPPostedTotal": 165.25,
          |        "LPPEstimatedTotal": 15.26
          |      },
          |      "lpp": {
          |        "lppDetails": [
          |          {
          |            "penaltyChargeReference": "1234567888",
          |            "penaltyCategory": "LPP2",
          |            "penaltyStatus": "P",
          |            "penaltyAmountPaid": 0,
          |            "penaltyAmountPosted": 144.00,
          |            "penaltyAmountAccruing": 0,
          |            "penaltyAmountOutstanding": 144.00,
          |            "penaltyAmountPaid": 0,
          |            "principalChargeMainTransaction": "4700",
          |            "principalChargeBillingFrom": "2022-01-01",
          |            "principalChargeBillingTo": "2022-12-31",
          |            "principalChargeDueDate": "2023-02-07",
          |            "lpp1LRDays": "15",
          |            "lpp1HRDays": "31",
          |            "lpp2Days": "31",
          |            "lpp1LRCalculationAmount": 99.99,
          |            "lpp1HRCalculationAmount": 99.99,
          |            "lpp2Percentage": 4.00,
          |            "lpp1LRPercentage": 2.00,
          |            "lpp1HRPercentage": 2.00,
          |            "communicationsDate": "2023-02-08",
          |            "penaltyChargeDueDate": "2022-10-30",
          |            "principalChargeLatestClearing": "2023-04-01",
          |            "vatOutstandingAmount": null,
          |            "timeToPay": [
          |              {
          |                "ttpStartDate": "2022-01-01",
          |                "ttpEndDate": "2022-12-31"
          |              }
          |            ],
          |            "principalChargeDocNumber": "DOC1",
          |            "principalChargeSubTransaction": "SUB1",
          |            "principalChargeReference": "1234567890"
          |          },
          |          {
          |            "penaltyChargeReference": "1234567887",
          |            "penaltyCategory": "LPP1",
          |            "penaltyStatus": "P",
          |            "penaltyAmountPaid": 0,
          |            "penaltyAmountPosted": 144.01,
          |            "penaltyAmountAccruing": 0,
          |            "penaltyAmountOutstanding": 144.01,
          |            "penaltyAmountPaid": 0,
          |            "principalChargeMainTransaction": "4700",
          |            "principalChargeBillingFrom": "2022-01-01",
          |            "principalChargeBillingTo": "2022-12-31",
          |            "principalChargeDueDate": "2023-02-07",
          |            "lpp1LRDays": "15",
          |            "lpp1HRDays": "31",
          |            "lpp2Days": "31",
          |            "lpp1LRCalculationAmount": 99.99,
          |            "lpp1HRCalculationAmount": 99.99,
          |            "lpp2Percentage": 4.00,
          |            "lpp1LRPercentage": 2.00,
          |            "lpp1HRPercentage": 2.00,
          |            "communicationsDate": "2023-01-08",
          |            "penaltyChargeDueDate": "2022-10-30",
          |            "principalChargeLatestClearing": "2023-04-01",
          |            "vatOutstandingAmount": null,
          |            "timeToPay": [
          |              {
          |                "ttpStartDate": "2022-01-01",
          |                "ttpEndDate": "2022-12-31"
          |              }
          |            ],
          |            "principalChargeDocNumber": "DOC2",
          |            "principalChargeSubTransaction": "SUB2",
          |            "principalChargeReference": "1234567890",
          |            "appealInformation": [
          |              {
          |                "appealStatus": "99",
          |                "appealDescription": "Some value"
          |              }
          |            ]
          |          }
          |        ]
          |      }
          |    }
          |  }
          |}
          |""".stripMargin)

      val penaltyDetailsTwoLPPsLPP2AccruingJson: JsValue = Json.parse(s"""
     |{
     |  "success": {
     |    "processingDate": "$mockInstant",
     |    "penaltyData": {
     |      "totalisations": {
     |        "LSPTotalValue": 200,
     |        "penalisedPrincipalTotal": 2000,
     |        "LPPPostedTotal": 165.25,
     |        "LPPEstimatedTotal": 15.26
     |      },
     |      "lpp": {
     |        "lppDetails": [
     |          {
     |            "penaltyCategory": "LPP2",
     |            "penaltyChargeReference": "1234567890",
     |            "principalChargeReference": "1234567890",
     |            "penaltyChargeCreationDate": "2022-10-30",
     |            "penaltyStatus": "A",
     |            "penaltyChargeAmount": 99.99,
     |            "penaltyAmountPosted": 0,
     |            "penaltyAmountOutstanding": null,
     |            "penaltyAmountPaid": null,
     |            "penaltyAmountAccruing": 99.99,
     |            "principalChargeMainTransaction": "4700",
     |            "principalChargeBillingFrom": "2022-01-01",
     |            "principalChargeBillingTo": "2022-12-31",
     |            "principalChargeDueDate": "2023-02-07",
     |            "lpp1LRDays": "15",
     |            "lpp1HRDays": "31",
     |            "lpp2Days": "31",
     |            "lpp1HRCalculationAmount": 99.99,
     |            "lpp1LRCalculationAmount": 99.99,
     |            "lpp2Percentage": 4.00,
     |            "lpp1LRPercentage": 2.00,
     |            "lpp1HRPercentage": 2.00,
     |            "communicationsDate": "2023-02-08",
     |            "penaltyChargeDueDate": "2022-10-30",
     |            "appealInformation": [
     |              {
     |                "appealStatus": "99",
     |                "appealDescription": "Some value"
     |              }
     |            ],
     |            "principalChargeLatestClearing": null,
     |            "vatOutstandingAmount": null,
     |            "timeToPay": [
     |              {
     |                "ttpStartDate": "2022-01-01",
     |                "ttpEndDate": "2022-12-31"
     |              }
     |            ],
     |            "principalChargeDocNumber": "DOC1",
     |            "principalChargeSubTransaction": "SUB1"
     |          },
     |          {
     |            "penaltyCategory": "LPP1",
     |            "penaltyChargeReference": "1234567887",
     |            "principalChargeReference": "1234567890",
     |            "penaltyChargeCreationDate": "2022-01-01",
     |            "penaltyStatus": "P",
     |            "penaltyChargeAmount": 99.99,
     |            "penaltyAmountPosted": 0,
     |            "penaltyAmountOutstanding": null,
     |            "penaltyAmountPaid": null,
     |            "penaltyAmountAccruing": 99.99,
     |            "principalChargeMainTransaction": "4700",
     |            "principalChargeBillingFrom": "2022-01-01",
     |            "principalChargeBillingTo": "2022-12-31",
     |            "principalChargeDueDate": "2023-02-07",
     |            "lpp1LRDays": "15",
     |            "lpp1HRDays": "31",
     |            "lpp2Days": "31",
     |            "lpp1HRCalculationAmount": 99.99,
     |            "lpp1LRCalculationAmount": 99.99,
     |            "lpp2Percentage": 4.00,
     |            "lpp1LRPercentage": 2.00,
     |            "lpp1HRPercentage": 2.00,
     |            "communicationsDate": "2023-02-08",
     |            "penaltyChargeDueDate": "2023-02-07",
     |            "appealInformation": [
     |              {
     |                "appealStatus": "99",
     |                "appealLevel": "01",
     |                "appealDescription": "Some value"
     |              }
     |            ],
     |            "principalChargeLatestClearing": null,
     |            "vatOutstandingAmount": null,
     |            "timeToPay": [
     |              {
     |                "ttpStartDate": "2022-01-01",
     |                "ttpEndDate": "2022-12-31"
     |              }
     |            ],
     |            "principalChargeDocNumber": "DOC2",
     |            "principalChargeSubTransaction": "SUB2"
     |          }
     |        ]
     |      }
     |    }
     |  }
     |}
     |""".stripMargin)

      val penaltyDetailsTwoLPPsVATNotPaidJson: JsValue = Json.parse(s"""
          |{
          |  "success": {
          |    "processingDate": "$mockInstant",
          |    "penaltyData": {
          |      "totalisations": {
          |        "LSPTotalValue": 200,
          |        "penalisedPrincipalTotal": 2000,
          |        "LPPPostedTotal": 165.25,
          |        "LPPEstimatedTotal": 15.26
          |      },
          |      "lpp": {
          |        "lppDetails": [
          |          {
          |            "penaltyChargeReference": "1234567888",
          |            "penaltyCategory": "LPP2",
          |            "penaltyStatus": "P",
          |            "penaltyAmountPaid": 0,
          |            "penaltyAmountPosted": 144.00,
          |            "penaltyAmountAccruing": 0,
          |            "penaltyAmountOutstanding": 144.00,
          |            "LPP1LRCalculationAmount": 99.99,
          |            "LPP1LRDays": "15",
          |            "LPP1LRPercentage": 2.00,
          |            "LPP1HRCalculationAmount": 99.99,
          |            "LPP1HRDays": "31",
          |            "LPP1HRPercentage": 2.00,
          |            "LPP2Days": "31",
          |            "LPP2Percentage": 4.00,
          |            "penaltyChargeCreationDate": "2022-10-30",
          |            "communicationsDate": "2023-02-08",
          |            "penaltyChargeDueDate": "2022-10-30",
          |            "principalChargeReference": "1234567890",
          |            "principalChargeBillingFrom": "2022-01-01",
          |            "principalChargeBillingTo": "2022-12-31",
          |            "principalChargeMainTransaction": "4700",
          |            "principalChargeDueDate": "2023-02-07"
          |          },
          |          {
          |            "penaltyChargeReference": "1234567887",
          |            "penaltyCategory": "LPP1",
          |            "penaltyStatus": "P",
          |            "penaltyAmountPaid": 0,
          |            "penaltyAmountPosted": 144.01,
          |            "penaltyAmountOutstanding": 144.01,
          |            "penaltyAmountAccruing": 0,
          |            "LPP1LRCalculationAmount": 99.99,
          |            "LPP1LRDays": "15",
          |            "LPP1LRPercentage": 2.00,
          |            "LPP1HRCalculationAmount": 99.99,
          |            "LPP1HRDays": "31",
          |            "LPP1HRPercentage": 2.00,
          |            "LPP2Days": "31",
          |            "LPP2Percentage": 4.00,
          |            "penaltyChargeCreationDate": "2022-10-30",
          |            "communicationsDate": "2023-02-08",
          |            "penaltyChargeDueDate": "2022-10-30",
          |            "principalChargeReference": "1234567890",
          |            "principalChargeBillingFrom": "2022-01-01",
          |            "principalChargeBillingTo": "2022-12-31",
          |            "principalChargeMainTransaction": "4700",
          |            "principalChargeDueDate": "2023-02-07"
          |          }
          |        ]
          |      }
          |    }
          |  }
          |}
          |""".stripMargin)

      "call ETMP and return NO_CONTENT" when {
        "there is only one penalty related to the charge" in {
          mockStubResponseForAuthorisedUser
          mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsOneLPPJson.toString()))

          val result = await(buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          result.status shouldBe Status.NO_CONTENT
        }

        "either penalty under the principal charge has appeal in any state" in {
          mockStubResponseForAuthorisedUser
          mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsTwoLPPsWithAppealsJson.toString()))

          val result = await(buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          result.status shouldBe Status.NO_CONTENT
        }

        "either penalty is accruing (LPP2)" in {
          mockStubResponseForAuthorisedUser
          mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsTwoLPPsLPP2AccruingJson.toString()))

          val result = await(buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          result.status shouldBe Status.NO_CONTENT
        }

        "the VAT has not been paid" in {
          mockStubResponseForAuthorisedUser
          mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsTwoLPPsVATNotPaidJson.toString()))

          val result = await(buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          result.status shouldBe Status.NO_CONTENT
        }
      }

      "call ETMP and return OK when there is two penalties related to the charge and they are both posted" +
        " and the VAT has been paid" in {
          mockStubResponseForAuthorisedUser
          mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, Some(penaltyDetailsTwoLPPsJson.toString()))

          val result = await(buildClientForRequestToApp(uri =
            s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          val expectedModel = MultiplePenaltiesData(
            firstPenaltyChargeReference = "1234567887",
            firstPenaltyAmount = 144.01,
            secondPenaltyChargeReference = "1234567888",
            secondPenaltyAmount = 144.00,
            firstPenaltyCommunicationDate = LocalDate.of(2023, 1, 8),
            secondPenaltyCommunicationDate = LocalDate.of(2023, 2, 8)
          )
          result.status shouldBe Status.OK
          Json.parse(result.body) shouldBe Json.toJson(expectedModel)
        }

      "return an ISE when the call to ETMP fails" in {
        mockStubResponseForAuthorisedUser
        mockStubResponseForPenaltyDetails(Status.INTERNAL_SERVER_ERROR, regime, idType, id, Some(""))

        val result = await(buildClientForRequestToApp(uri =
          s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
        result.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
