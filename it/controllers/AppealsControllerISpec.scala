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
import models.{Id, IdType, Regime}
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils._

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class AppealsControllerISpec extends IntegrationSpecCommonBase with HipPenaltiesWiremock with HipFinancialWiremock with HIPWiremock
  with AppealWiremock
  with FileNotificationOrchestratorWiremock
  with FeatureSwitching
  with TableDrivenPropertyChecks
  with AuthMock {

  setEnabledFeatureSwitches()

  val controller: AppealsController = injector.instanceOf[AppealsController]

  val appealJson: JsValue = Json.parse(
    """
      |{
      |  "type": "LATE_SUBMISSION",
      |  "startDate": "2021-04-23T18:25:43.511",
      |  "endDate": "2021-04-23T18:25:43.511",
      |  "dueDate": "2021-04-23T18:25:43.511",
      |  "dateCommunicationSent": "2021-04-23T18:25:43.511"
      |}
      |""".stripMargin)

  val appealJsonLPP: JsValue = Json.parse(
    """
      |{
      |  "type": "LATE_PAYMENT",
      |	 "startDate": "2023-01-01T18:25:43.511",
      |	 "endDate" : "2023-03-31T18:25:43.511",
      |	 "dueDate" : "2023-05-07T18:25:43.511",
      |  "dateCommunicationSent": "2021-05-08T18:25:43.511"
      |}
      |""".stripMargin)

  val appealJsonLPPAdditional: JsValue = Json.parse(
    """
      |{
      |  "type": "ADDITIONAL",
      |	 "startDate": "2023-01-01T18:25:43.511",
      |	 "endDate" : "2023-03-31T18:25:43.511",
      |	 "dueDate" : "2023-05-07T18:25:43.511",
      |  "dateCommunicationSent": "2021-05-08T18:25:43.511"
      |}
      |""".stripMargin)

  val appealV2Json: JsValue = Json.parse(
    """
      |{
      |  "type": "LATE_SUBMISSION",
      |  "startDate": "2023-01-01",
      |  "endDate": "2023-12-31",
      |  "dueDate": "2024-02-07",
      |  "dateCommunicationSent": "2024-02-08"
      |}
      |""".stripMargin)

  val appealV2JsonLPP: JsValue = Json.parse(
    """
      |{
      |  "type": "LATE_PAYMENT",
      |	 "startDate": "2022-01-01",
      |	 "endDate" : "2022-12-31",
      |	 "dueDate" : "2023-02-07",
      |  "dateCommunicationSent": "2023-02-08"
      |}
      |""".stripMargin)

  val appealV2JsonLPPAdditional: JsValue = Json.parse(
    """
      |{
      |  "type": "ADDITIONAL",
      |	 "startDate": "2024-01-01",
      |	 "endDate" : "2024-12-31",
      |	 "dueDate" : "2025-02-07",
      |  "dateCommunicationSent": "2025-02-08"
      |}
      |""".stripMargin)

  val getPenaltyDetailsJson = Json.parse("""
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
                                |"lsp": {
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
                                |            "penaltyCreationDate": "2022-10-30",
                                |            "penaltyExpiryDate": "2022-10-30",
                                |            "communicationsDate": "2024-02-08",
                                |            "lateSubmissions": [
                                |              {
                                |                "lateSubmissionID": "001",
                                |                "incomeSource": "IT",
                                |                "taxPeriod": "23AA",
                                |                "taxPeriodStartDate": "2023-01-01",
                                |                "taxPeriodEndDate": "2023-12-31",
                                |                "taxPeriodDueDate": "2024-02-07",
                                |                "returnReceiptDate": "2023-02-01",
                                |                "taxReturnStatus": "Fulfilled"
                                |              }
                                |            ],
                                |            "chargeDueDate": "2022-10-30",
                                |            "chargeOutstandingAmount": 200,
                                |            "chargeAmount": 200,
                                |            "triggeringProcess": "P123",
                                |            "chargeReference": "CHARGEREF1"
                                |          }
                                |        ]
                                |      },
                                |      "lpp": {
                                |        "manualLPPIndicator": false,
                                |        "lppDetails": [
                                |          {
                                |            "principalChargeReference": "1234567891",
                                |            "penaltyCategory": "LPP2",
                                |            "penaltyStatus": "P",
                                |            "penaltyAmountAccruing": 246.9,
                                |            "penaltyAmountPosted": 0,
                                |            "penaltyAmountPaid": null,
                                |            "penaltyAmountOutstanding": 144,
                                |            "lpp1LRCalculationAmt": 123.45,
                                |            "lpp1LRDays": "15",
                                |            "lpp1LRPercentage": 2.0,
                                |            "lpp1HRCalculationAmt": 123.45,
                                |            "lpp1HRDays": "31",
                                |            "lpp1HRPercentage": 2.0,
                                |            "lpp2Days": "31",
                                |            "lpp2Percentage": 4.0,
                                |            "penaltyChargeCreationDate": "2022-10-30",
                                |            "communicationsDate": "2023-02-08",
                                |            "penaltyChargeReference": "1234567887",
                                |            "penaltyChargeDueDate": "2022-10-30",
                                |            "appealInformation": null,
                                |            "principalChargeDocNumber": null,
                                |            "principalChargeMainTr": "4700",
                                |            "principalChargeSubTr": null,
                                |            "principalChargeBillingFrom": "2022-01-01",
                                |            "principalChargeBillingTo": "2022-12-31",
                                |            "principalChargeDueDate": "2023-02-07",
                                |            "principalChargeLatestClearing": "2027-07-20",
                                |            "timeToPay": null,
                                |            "supplement": false
                                |          },
                                |          {
                                |            "principalChargeReference": "1234567891",
                                |            "penaltyCategory": "LPP1",
                                |            "penaltyStatus": "P",
                                |            "penaltyAmountAccruing": 246.9,
                                |            "penaltyAmountPosted": 0,
                                |            "penaltyAmountPaid": null,
                                |            "penaltyAmountOutstanding": 144.01,
                                |            "lpp1LRCalculationAmt": 123.45,
                                |            "lpp1LRDays": "15",
                                |            "lpp1LRPercentage": 2.0,
                                |            "lpp1HRCalculationAmt": 123.45,
                                |            "lpp1HRDays": "31",
                                |            "lpp1HRPercentage": 2.0,
                                |            "lpp2Days": "31",
                                |            "lpp2Percentage": 4.0,
                                |            "penaltyChargeCreationDate": "2022-10-30",
                                |            "communicationsDate": "2025-02-08",
                                |            "penaltyChargeReference": "1234567889",
                                |            "penaltyChargeDueDate": "2022-10-30",
                                |            "appealInformation": null,
                                |            "principalChargeDocNumber": null,
                                |            "principalChargeMainTr": "4700",
                                |            "principalChargeSubTr": null,
                                |            "principalChargeBillingFrom": "2024-01-01",
                                |            "principalChargeBillingTo": "2024-12-31",
                                |            "principalChargeDueDate": "2025-02-07",
                                |            "principalChargeLatestClearing": "2027-07-20",
                                |            "timeToPay": null,
                                |            "supplement": false
                                |          }
                                |        ]
                                |      }
                                |    }
                                |  }
                                |}
                                |""".stripMargin)

  class SetUp(hipFeatureSwitch:Boolean = false) {
    if(hipFeatureSwitch) {
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
//    (Regime("ITSA"), IdType("NINO"), Id("AB123456C")),
//    (Regime("ITSA"), IdType("MTDITID"), Id("012345678912345")),
  ).forEvery { (regime, idType, id) =>

    val (r, it, i) = (regime.value, idType.value, id.value)

    val submitAppealUri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1"


    s"getAppealsDataForLateSubmissionPenalty for $regime with $idType" should {
    "call ETMP and compare the penalty ID provided and the penalty ID in the payload - return OK if there is a match" in {

      mockStubResponseForAuthorisedUser
      mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsJson.toString))

      val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/late-submissions/${idType.value}/${id.value}?penaltyId=123456789").get())
      result.status shouldBe Status.OK
      result.body shouldBe appealV2Json.toString()
    }

    "return NOT_FOUND when the penalty ID given does not match the penalty ID in the payload" in {

      mockStubResponseForAuthorisedUser
      mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsJson.toString()))

      val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/late-submissions/${idType.value}/${id.value}?penaltyId=0001").get())
      result.status shouldBe Status.NOT_FOUND
    }

    "return an ISE when the call to ETMP fails" in {

      mockStubResponseForAuthorisedUser
      mockEmptyResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, regime, idType, id)

      val result = await(buildClientForRequestToApp(
        uri = s"/${regime.value}/appeals-data/late-submissions/${idType.value}/${id.value}?penaltyId=123456789"
      ).get())
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

    s"getAppealsDataForLatePaymentPenalty for $regime with $idType" should {
      "call ETMP and compare the penalty ID provided and the penalty ID in the payload - return OK if there is a match" in {

        mockStubResponseForAuthorisedUser
        mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsJson.toString()))

        val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/late-payments/${idType.value}/${id.value}?penaltyId=1234567887&isAdditional=false").get())
        result.status shouldBe Status.OK
        result.body shouldBe appealV2JsonLPP.toString()
      }

      "call ETMP and compare the penalty ID provided and the penalty ID in the payload for Additional - return OK if there is a match" in {

        mockStubResponseForAuthorisedUser
        mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsJson.toString()))

        val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/late-payments/${idType.value}/${id.value}?penaltyId=1234567889&isAdditional=true").get())
        result.status shouldBe Status.OK
        result.body shouldBe appealV2JsonLPPAdditional.toString()
      }

      "return NOT_FOUND when the penalty ID given does not match the penalty ID in the payload" in {

        mockStubResponseForAuthorisedUser
        mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsJson.toString()))

        val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/late-payments/${idType.value}/${id.value}?penaltyId=0001&isAdditional=false").get())
        result.status shouldBe Status.NOT_FOUND
      }

      "return an ISE when the call to ETMP fails" in {

        mockStubResponseForAuthorisedUser
        mockEmptyResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, regime, idType, id)

        val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/late-payments/${idType.value}/${id.value}?penaltyId=0001&isAdditional=false").get())
        result.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    def reasonableExcuses(regime: Regime): JsValue = Json.parse(
      s"""
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
        |    ${if (regime.value == "VATC")"""{
        |      "type": "lossOfStaff",
        |      "descriptionKey": "reasonableExcuses.lossOfStaffReason"
        |    },""" else ""}
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

    s"getReasonableExcuses for $regime with $idType" should {
      "return all active reasonable excuses" in {
        val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/reasonable-excuses").get())
        result.status shouldBe OK
        Json.parse(result.body) shouldBe reasonableExcuses(regime)
      }
    }

    s"submitAppeal for $regime with $idType" should {
      "call the connector and send the appeal data received in the request body" when {
        "returns OK when successful for bereavement" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }

        "returns OK when successful for crime" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }

        "returns OK when successful for fire or flood" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }

        "returns OK when successful for loss of staff" in new SetUp{
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }

        "returns OK when successful for technical issues" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }

        "returns OK when successful for health" when {
          "there has been no hospital stay" in new SetUp {
            mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")

            val jsonToSubmit: JsValue = Json.parse(
              """
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
            val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
            result.status shouldBe OK
          }

          "there is an ongoing hospital stay" in new SetUp {
            mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")

            val jsonToSubmit: JsValue = Json.parse(
              """
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
            val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
            result.status shouldBe OK
          }

          "there has been a hospital stay" in new SetUp {
            mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")

            val jsonToSubmit: JsValue = Json.parse(
              """
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
            val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
            result.status shouldBe OK
          }
        }

        "returns OK when successful for other with file upload" in new SetUp {

          mockStubResponseForAuthorisedUser
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")
          mockResponseForFileNotificationOrchestrator(OK)

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }

        "returns OK when successful for other with file upload (audit storage failure) - single appeal" in new SetUp {
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")
          mockResponseForFileNotificationOrchestrator(INTERNAL_SERVER_ERROR)

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
          eventually {
            wireMockServer
              .findAll(postRequestedFor(urlEqualTo("/write/audit")))
              .asScala.toList
              .exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
          }
        }

        "returns OK when successful for LPP" in new SetUp {

          mockStubResponseForAuthorisedUser
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
            buildClientForRequestToApp(
              uri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1")
              .post(
                jsonToSubmit
              ))
          result.status shouldBe OK
        }

        "returns OK when successful for other with file upload (audit storage failure) - part of multi appeal" in new SetUp {
          mockStubResponseForAuthorisedUser
          mockResponseForAppealSubmissionStub(OK, penaltyNumber = "123456789")
          mockResponseForFileNotificationOrchestrator(INTERNAL_SERVER_ERROR)

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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

          val result: WSResponse = await(buildClientForRequestToApp(
            uri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1&isMultiAppeal=true"
          ).post(
            jsonToSubmit
          ))

          result.status shouldBe MULTI_STATUS
          Json.parse(result.body) shouldBe expectedJsonResponse
          eventually {
            wireMockServer
              .findAll(postRequestedFor(urlEqualTo("/write/audit")))
              .asScala.toList.exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
          }
        }
      }

      "return BAD_REQUEST (400)" when {
        "no JSON body is in the request" in new SetUp {
          val result: WSResponse = await(buildClientForRequestToApp(
            uri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1"
          ).post(""))
          result.status shouldBe BAD_REQUEST
        }

        "JSON body is present but it can not be parsed to a model" in new SetUp {
          val result: WSResponse = await(buildClientForRequestToApp(
            uri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1"
          ).post(Json.parse("{}")))
          result.status shouldBe BAD_REQUEST
        }
      }

      "return error status code" when {
        "the call to PEGA/stub fails" in new SetUp {
          mockResponseForAppealSubmissionStub(GATEWAY_TIMEOUT, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe GATEWAY_TIMEOUT
        }

        "the call to PEGA/stub has a fault" in new SetUp {
          mockResponseForAppealSubmissionStubFault(penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    s"submitAppeal for $regime with $idType when calling HIP" should {
      "call the connector and send the appeal data received in the request body" when {
        "returns OK when successful for bereavement" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }
        "returns OK when successful for crime" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }
        "returns OK when successful for fire or flood" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }
        "returns OK when successful for loss of staff" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }
        "returns OK when successful for technical issues" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
        }
        "returns OK when successful for health" when {

          "there has been no hospital stay" in new SetUp(hipFeatureSwitch = true) {
            val jsonToSubmit: JsValue = Json.parse(
              """
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
            val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
            result.status shouldBe OK
          }

          "there is an ongoing hospital stay" in new SetUp(hipFeatureSwitch = true) {
            val jsonToSubmit: JsValue = Json.parse(
              """
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
            val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
              jsonToSubmit
            ))
            result.status shouldBe OK
          }

          "there has been a hospital stay" in new SetUp(hipFeatureSwitch = true) {
            val jsonToSubmit: JsValue = Json.parse(
              """
                |{
                |    "sourceSystem": "MDTP",
                |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
            val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
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
       |   "appealLevel": "01",
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
            val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
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
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe OK
          eventually {
            wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
          }
        }

        "returns OK when successful for LPP" in new SetUp(hipFeatureSwitch = true) {
          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1").post(
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
       |   "appealLevel": "01",
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

          val result: WSResponse = await(buildClientForRequestToApp(
            uri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1&isMultiAppeal=true"
          ).post(
            jsonToSubmit
          ))

          result.status shouldBe MULTI_STATUS
          Json.parse(result.body) shouldBe expectedJsonResponse
          eventually {
            wireMockServer
              .findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList
              .exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
          }
        }
      }

      "return BAD_REQUEST (400)" when {
        "no JSON body is in the request" in new SetUp(hipFeatureSwitch = true) {
          val result: WSResponse = await(buildClientForRequestToApp(
            uri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1"
          ).post(""))
          result.status shouldBe BAD_REQUEST
        }

        "JSON body is present but it can not be parsed to a model" in new SetUp(hipFeatureSwitch = true) {
          val result: WSResponse = await(buildClientForRequestToApp(
            uri = s"/$r/appeals/submit-appeal/$it/$i?penaltyNumber=123456789&correlationId=uuid-1"
          ).post(Json.parse("{}")))
          result.status shouldBe BAD_REQUEST
        }
      }

      "return error status code" when {
        "the call to PEGA/stub fails" in new SetUp {
          mockResponseForAppealSubmissionStub(GATEWAY_TIMEOUT, penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe GATEWAY_TIMEOUT
        }

        "the call to PEGA/stub has a fault" in new SetUp {
          mockResponseForAppealSubmissionStubFault(penaltyNumber = "123456789")

          val jsonToSubmit: JsValue = Json.parse(
            """
              |{
              |    "sourceSystem": "MDTP",
              |    "taxRegime": "VAT",
       |   "appealLevel": "01",
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
          val result: WSResponse = await(buildClientForRequestToApp(uri = submitAppealUri).post(
            jsonToSubmit
          ))
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    s"getMultiplePenaltyData for $regime with $idType" should {
      val getPenaltyDetailsOneLPPJson: JsValue = Json.parse("""
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
                                                           |      "lpp": {
                                                           |        "manualLPPIndicator": false,
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
                                                           |          }
                                                           |        ]
                                                           |      }
                                                           |    }
                                                           |  }
                                                           |}
                                                           |""".stripMargin)
      val getPenaltyDetailsTwoLPPsJson: JsValue = Json.parse("""
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
                                                           |      "lpp": {
                                                           |        "manualLPPIndicator": false,
                                                           |        "lppDetails": [
                                                           |          {
                                                           |            "principalChargeReference": "1234567891",
                                                           |            "penaltyCategory": "LPP2",
                                                           |            "penaltyStatus": "P",
                                                           |            "penaltyAmountAccruing": 246.9,
                                                           |            "penaltyAmountPosted": 0,
                                                           |            "penaltyAmountPaid": null,
                                                           |            "penaltyAmountOutstanding": 144,
                                                           |            "lpp1LRCalculationAmt": 123.45,
                                                           |            "lpp1LRDays": "15",
                                                           |            "lpp1LRPercentage": 2.0,
                                                           |            "lpp1HRCalculationAmt": 123.45,
                                                           |            "lpp1HRDays": "31",
                                                           |            "lpp1HRPercentage": 2.0,
                                                           |            "lpp2Days": "31",
                                                           |            "lpp2Percentage": 4.0,
                                                           |            "penaltyChargeCreationDate": "2022-10-30",
                                                           |            "communicationsDate": "2023-02-08",
                                                           |            "penaltyChargeReference": "1234567888",
                                                           |            "penaltyChargeDueDate": "2022-10-30",
                                                           |            "appealInformation": null,
                                                           |            "principalChargeDocNumber": null,
                                                           |            "principalChargeMainTr": "4700",
                                                           |            "principalChargeSubTr": null,
                                                           |            "principalChargeBillingFrom": "2022-10-30",
                                                           |            "principalChargeBillingTo": "2022-10-30",
                                                           |            "principalChargeDueDate": "2022-10-30",
                                                           |            "principalChargeLatestClearing": "2027-07-20",
                                                           |            "timeToPay": null,
                                                           |            "supplement": false
                                                           |          },
                                                           |          {
                                                           |            "principalChargeReference": "1234567891",
                                                           |            "penaltyCategory": "LPP1",
                                                           |            "penaltyStatus": "P",
                                                           |            "penaltyAmountAccruing": 246.9,
                                                           |            "penaltyAmountPosted": 0,
                                                           |            "penaltyAmountPaid": null,
                                                           |            "penaltyAmountOutstanding": 144.01,
                                                           |            "lpp1LRCalculationAmt": 123.45,
                                                           |            "lpp1LRDays": "15",
                                                           |            "lpp1LRPercentage": 2.0,
                                                           |            "lpp1HRCalculationAmt": 123.45,
                                                           |            "lpp1HRDays": "31",
                                                           |            "lpp1HRPercentage": 2.0,
                                                           |            "lpp2Days": "31",
                                                           |            "lpp2Percentage": 4.0,
                                                           |            "penaltyChargeCreationDate": "2022-10-30",
                                                           |            "communicationsDate": "2023-01-08",
                                                           |            "penaltyChargeReference": "1234567888",
                                                           |            "penaltyChargeDueDate": "2022-10-30",
                                                           |            "appealInformation": null,
                                                           |            "principalChargeDocNumber": null,
                                                           |            "principalChargeMainTr": "4700",
                                                           |            "principalChargeSubTr": null,
                                                           |            "principalChargeBillingFrom": "2022-10-30",
                                                           |            "principalChargeBillingTo": "2022-10-30",
                                                           |            "principalChargeDueDate": "2022-10-30",
                                                           |            "principalChargeLatestClearing": "2027-07-20",
                                                           |            "timeToPay": null,
                                                           |            "supplement": false
                                                           |          }
                                                           |        ]
                                                           |      }
                                                           |    }
                                                           |  }
                                                           |}
                                                           |""".stripMargin)

      val getPenaltyDetailsTwoLPPsWithAppealsJson: JsValue = Json.parse("""
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
                                                           |      "lpp": {
                                                           |        "manualLPPIndicator": false,
                                                           |        "lppDetails": [
                                                           |          {
                                                           |            "principalChargeReference": "1234567890",
                                                           |            "penaltyCategory": "LPP2",
                                                           |            "penaltyStatus": "P",
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
                                                           |            "appealInformation": [
                                                           |              {
                                                           |                "appealStatus": "A",
                                                           |                "appealLevel": "01"
                                                           |              }
                                                           |            ],
                                                           |            "principalChargeLatestClearing": null,
                                                           |            "timeToPay": null,
                                                           |            "supplement": false
                                                           |          },
                                                           |          {
                                                           |            "principalChargeReference": "1234567891",
                                                           |            "penaltyCategory": "LPP1",
                                                           |            "penaltyStatus": "P",
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
                                                           |          }
                                                           |        ]
                                                           |      }
                                                           |    }
                                                           |  }
                                                           |}
                                                           |""".stripMargin)

      val getPenaltyDetailsTwoLPPsLPP2AccruingJson: JsValue = Json.parse("""
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
                                                               |      "lpp": {
                                                               |        "manualLPPIndicator": false,
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
                                                               |          }
                                                               |        ]
                                                               |      }
                                                               |    }
                                                               |  }
                                                               |}
                                                               |""".stripMargin)

      val getPenaltyDetailsTwoLPPsVATNotPaidJson: JsValue = Json.parse("""
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
                                                               |      "lpp": {
                                                               |        "manualLPPIndicator": false,
                                                               |        "lppDetails": [
                                                               |          {
                                                               |            "principalChargeReference": "1234567890",
                                                               |            "penaltyCategory": "LPP2",
                                                               |            "penaltyStatus": "P",
                                                               |            "penaltyAmountPaid": 0,
                                                               |            "penaltyAmountPosted": 144.00,
                                                               |            "penaltyAmountAccruing": 0,
                                                               |            "penaltyAmountOutstanding": 144.00,
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
                                                               |            "penaltyAmountPaid": 0,
                                                               |            "penaltyAmountPosted": 144.01,
                                                               |            "penaltyAmountOutstanding": 144.01,
                                                               |            "penaltyAmountAccruing": 0,
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
          mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsOneLPPJson.toString()))

          val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          result.status shouldBe Status.NO_CONTENT
        }

        "either penalty under the principal charge has appeal in any state" in {
          mockStubResponseForAuthorisedUser
          mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsTwoLPPsWithAppealsJson.toString()))

          val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          result.status shouldBe Status.NO_CONTENT
        }

        "either penalty is accruing (LPP2)" in {
          mockStubResponseForAuthorisedUser
          mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsTwoLPPsLPP2AccruingJson.toString()))

          val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          result.status shouldBe Status.NO_CONTENT
        }

        "the VAT has not been paid" in {
          mockStubResponseForAuthorisedUser
          mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsTwoLPPsVATNotPaidJson.toString()))

          val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
          result.status shouldBe Status.NO_CONTENT
        }
      }

      "call ETMP and return OK when there is two penalties related to the charge and they are both posted" +
        " and the VAT has been paid" in {
        mockStubResponseForAuthorisedUser
        mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getPenaltyDetailsTwoLPPsJson.toString()))

        val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567888").get())
        val expectedModel = MultiplePenaltiesData(
          firstPenaltyChargeReference = "1234567888",
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
        mockEmptyResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, regime, idType, id)

        val result = await(buildClientForRequestToApp(uri = s"/${regime.value}/appeals-data/multiple-penalties/${idType.value}/${id.value}?penaltyId=1234567887").get())
        result.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

  }

}
