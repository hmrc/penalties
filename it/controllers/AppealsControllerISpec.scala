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

package controllers

import scala.jdk.CollectionConverters._
import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo}
import config.featureSwitches.FeatureSwitching
import models.appeals.MultiplePenaltiesData
import org.scalatest.concurrent.Eventually.eventually
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{AppealWiremock, ETMPWiremock, FileNotificationOrchestratorWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate

class AppealsControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock with AppealWiremock with FileNotificationOrchestratorWiremock with FeatureSwitching {
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

  val getPenaltyDetailsJson: JsValue = Json.parse(
    """
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
      |   "details": [
      |     {
      |       "penaltyNumber": "123456789",
      |       "penaltyOrder": "01",
      |       "penaltyCategory": "P",
      |       "penaltyStatus": "ACTIVE",
      |       "FAPIndicator": "X",
      |       "penaltyCreationDate": "2022-10-30",
      |       "penaltyExpiryDate": "2022-10-30",
      |       "triggeringProcess": "XYZ",
      |       "expiryReason": "FAP",
      |       "chargeReference": "CHARGE123",
      |       "communicationsDate": "2024-02-08",
      |       "lateSubmissions": [
      |         {
      |           "taxPeriodStartDate": "2023-01-01",
      |           "taxPeriodEndDate": "2023-12-31",
      |           "taxPeriodDueDate": "2024-02-07",
      |           "returnReceiptDate": "2024-02-01",
      |           "taxReturnStatus": "Fulfilled"
      |         }
      |       ],
      |       "appealInformation": [
      |         {
      |         "appealStatus": "99",
      |         "appealLevel": "01"
      |         }
      |       ],
      |       "chargeDueDate": "2022-10-30",
      |       "chargeOutstandingAmount": 200,
      |       "chargeAmount": 200
      |     },
      |     {
      |       "penaltyNumber": "123456788",
      |       "penaltyOrder": "01",
      |       "penaltyCategory": "P",
      |       "penaltyStatus": "ACTIVE",
      |       "FAPIndicator": "X",
      |       "penaltyCreationDate": "2022-10-30",
      |       "penaltyExpiryDate": "2022-10-30",
      |       "triggeringProcess": "XYZ",
      |       "expiryReason": "FAP",
      |       "chargeReference": "CHARGE123",
      |       "communicationsDate": "2022-10-30",
      |       "lateSubmissions": [
      |         {
      |           "taxPeriodStartDate": "2022-01-01",
      |           "taxPeriodEndDate": "2022-12-31",
      |           "taxPeriodDueDate": "2023-02-07",
      |           "returnReceiptDate": "2023-02-01",
      |           "taxReturnStatus": "Fulfilled"
      |         }
      |       ],
      |       "appealInformation": [
      |         {
      |         "appealStatus": "99",
      |         "appealLevel": "01"
      |         }
      |       ],
      |       "chargeDueDate": "2022-10-30",
      |       "chargeOutstandingAmount": 200,
      |       "chargeAmount": 200
      |     }
      |   ]
      | },
      | "latePaymentPenalty": {
      |     "details": [
      |       {
      |          "penaltyChargeReference": "1234567890",
      |          "penaltyCategory": "LPP2",
      |          "penaltyStatus": "A",
      |          "penaltyAmountPaid": 44.21,
      |          "penaltyAmountOutstanding": 100,
      |          "LPP1LRCalculationAmount": 99.99,
      |          "LPP1LRDays": "15",
      |          "LPP1LRPercentage": 2.00,
      |          "LPP1HRCalculationAmount": 99.99,
      |          "LPP1HRDays": "31",
      |          "LPP1HRPercentage": 2.00,
      |          "LPP2Days": "31",
      |          "LPP2Percentage": 4.00,
      |          "penaltyChargeCreationDate": "2022-10-30",
      |          "communicationsDate": "2026-02-08",
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "1234567893",
      |          "principalChargeBillingFrom": "2025-01-01",
      |          "principalChargeBillingTo": "2025-12-31",
      |          "principalChargeDueDate": "2026-02-07"
      |       },
      |       {
      |          "penaltyChargeReference": "1234567889",
      |          "penaltyCategory": "LPP2",
      |          "penaltyStatus": "A",
      |          "penaltyAmountPaid": 100.00,
      |          "penaltyAmountOutstanding": 23.45,
      |          "LPP1LRCalculationAmount": 99.99,
      |          "LPP1LRDays": "15",
      |          "LPP1LRPercentage": 2.00,
      |          "LPP1HRCalculationAmount": 99.99,
      |          "LPP1HRDays": "31",
      |          "LPP1HRPercentage": 2.00,
      |          "LPP2Days": "31",
      |          "LPP2Percentage": 4.00,
      |          "penaltyChargeCreationDate": "2022-10-30",
      |          "communicationsDate": "2025-02-08",
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "1234567892",
      |          "principalChargeBillingFrom": "2024-01-01",
      |          "principalChargeBillingTo": "2024-12-31",
      |          "principalChargeDueDate": "2025-02-07"
      |       },
      |       {
      |          "penaltyChargeReference": "1234567888",
      |          "penaltyCategory": "LPP1",
      |          "penaltyStatus": "P",
      |          "penaltyAmountPaid": 0,
      |          "penaltyAmountOutstanding": 144.00,
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
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "1234567891",
      |          "principalChargeBillingFrom": "2023-01-01",
      |          "principalChargeBillingTo": "2023-12-31",
      |          "principalChargeDueDate": "2024-02-07"
      |       },
      |       {
      |          "penaltyChargeReference": "1234567887",
      |          "penaltyCategory": "LPP1",
      |          "penaltyStatus": "P",
      |          "penaltyAmountPaid": 0,
      |          "penaltyAmountOutstanding": 144.00,
      |          "LPP1LRCalculationAmount": 99.99,
      |          "LPP1LRDays": "15",
      |          "LPP1LRPercentage": 2.00,
      |          "LPP1HRCalculationAmount": 99.99,
      |          "LPP1HRDays": "31",
      |          "LPP1HRPercentage": 2.00,
      |          "LPP2Days": "31",
      |          "LPP2Percentage": 4.00,
      |          "penaltyChargeCreationDate": "2022-10-30",
      |          "communicationsDate": "2023-02-08",
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "1234567890",
      |          "principalChargeBillingFrom": "2022-01-01",
      |          "principalChargeBillingTo": "2022-12-31",
      |          "principalChargeDueDate": "2023-02-07"
      |       }
      |   ]
      | }
      |}
      |""".stripMargin)

  "getAppealsDataForLateSubmissionPenalty" should {
    "call ETMP and compare the penalty ID provided and the penalty ID in the payload - return OK if there is a match" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", Some(getPenaltyDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-submissions?penaltyId=123456789&enrolmentKey=HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe Status.OK
      result.body shouldBe appealV2Json.toString()
    }

    "return NOT_FOUND when the penalty ID given does not match the penalty ID in the payload" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", Some(getPenaltyDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-submissions?penaltyId=0001&enrolmentKey=HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe Status.NOT_FOUND
    }

    "return an ISE when the call to ETMP fails" in {
      mockStubResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, "123456789", Some(""))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-submissions?penaltyId=0001&enrolmentKey=HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "getAppealsDataForLatePaymentPenalty" should {
    "call ETMP and compare the penalty ID provided and the penalty ID in the payload - return OK if there is a match" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", Some(getPenaltyDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-payments?penaltyId=1234567887&enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isAdditional=false").get())
      result.status shouldBe Status.OK
      result.body shouldBe appealV2JsonLPP.toString()
    }

    "call ETMP and compare the penalty ID provided and the penalty ID in the payload for Additional - return OK if there is a match" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", Some(getPenaltyDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-payments?penaltyId=1234567889&enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isAdditional=true").get())
      result.status shouldBe Status.OK
      result.body shouldBe appealV2JsonLPPAdditional.toString()
    }

    "return NOT_FOUND when the penalty ID given does not match the penalty ID in the payload" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", Some(getPenaltyDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-payments?penaltyId=0001&enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isAdditional=false").get())
      result.status shouldBe Status.NOT_FOUND
    }

    "return an ISE when the call to ETMP fails" in {
      mockStubResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, "123456789", Some(""))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/late-payments?penaltyId=0001&enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isAdditional=false").get())
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "getReasonableExcuses" should {
    "return all active reasonable excuses" in {
      val jsonExpectedToReturn: JsValue = Json.parse(
        """
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

  "submitAppeal" should {
    "call the connector and send the appeal data received in the request body - returns OK when successful for bereavement" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "sourceSystem": "MDTP",
          |    "taxRegime": "VAT",
          |    "customerReferenceNo": "123456789",
          |    "dateOfAppeal": "2020-01-01T00:00:00",
          |    "isLPP": false,
          |    "appealSubmittedBy": "client",
          |    "appealInformation": {
          |						"reasonableExcuse": "bereavement",
          |           "honestyDeclaration": true,
          |           "startDateOfEvent": "2021-04-23T00:00",
          |						"statement": "This is a statement",
          |           "lateAppeal": false
          |		}
          |}
          |""".stripMargin
      )
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }
    "call the connector and send the appeal data received in the request body - returns OK when successful for crime" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "sourceSystem": "MDTP",
          |    "taxRegime": "VAT",
          |    "customerReferenceNo": "123456789",
          |    "dateOfAppeal": "2020-01-01T00:00:00",
          |    "isLPP": false,
          |    "appealSubmittedBy": "client",
          |    "appealInformation": {
          |						 "reasonableExcuse": "crime",
          |            "honestyDeclaration": true,
          |            "startDateOfEvent": "2021-04-23T00:00",
          |            "reportedIssueToPolice": true,
          |						 "statement": "This is a statement",
          |            "lateAppeal": false
          |		}
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for fire or flood" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "sourceSystem": "MDTP",
          |    "taxRegime": "VAT",
          |    "customerReferenceNo": "123456789",
          |    "dateOfAppeal": "2020-01-01T00:00:00",
          |    "isLPP": false,
          |    "appealSubmittedBy": "client",
          |    "appealInformation": {
          |          "reasonableExcuse": "fireOrFlood",
          |          "honestyDeclaration": true,
          |          "startDateOfEvent": "2021-04-23T00:00",
          |          "statement": "This is a statement",
          |          "lateAppeal": false
          |    }
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for loss of staff" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "sourceSystem": "MDTP",
          |    "taxRegime": "VAT",
          |    "customerReferenceNo": "123456789",
          |    "dateOfAppeal": "2020-01-01T00:00:00",
          |    "isLPP": false,
          |    "appealSubmittedBy": "client",
          |    "appealInformation": {
          |						 "reasonableExcuse": "lossOfStaff",
          |            "honestyDeclaration": true,
          |            "startDateOfEvent": "2021-04-23T00:00",
          |						 "statement": "This is a statement",
          |            "lateAppeal": false
          |		}
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for technical issues" in {
      mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
      val jsonToSubmit: JsValue = Json.parse(
        """
          |{
          |    "sourceSystem": "MDTP",
          |    "taxRegime": "VAT",
          |    "customerReferenceNo": "123456789",
          |    "dateOfAppeal": "2020-01-01T00:00:00",
          |    "isLPP": false,
          |    "appealSubmittedBy": "client",
          |    "appealInformation": {
          |					 	 "reasonableExcuse": "technicalIssues",
          |            "honestyDeclaration": true,
          |            "startDateOfEvent": "2021-04-23T00:00",
          |            "endDateOfEvent": "2021-04-24T23:59:59.999999999",
          |						 "statement": "This is a statement",
          |            "lateAppeal": false
          |		}
          |}
          |""".stripMargin)
      val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
        jsonToSubmit
      ))
      result.status shouldBe OK
    }

    "call the connector and send the appeal data received in the request body - returns OK when successful for health" when {
      "there has been no hospital stay" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": false,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "health",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T00:00",
            |            "hospitalStayInvolved": false,
            |            "eventOngoing": false,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "there is an ongoing hospital stay" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": false,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "health",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T00:00",
            |            "hospitalStayInvolved": true,
            |            "eventOngoing": true,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "there has been a hospital stay" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": false,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "health",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T00:00",
            |            "endDateOfEvent": "2021-04-23T18:25:43.511Z",
            |            "hospitalStayInvolved": true,
            |            "eventOngoing": false,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "call the connector and send the appeal data received in the request body - returns OK when successful for other with file upload" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
        mockResponseForFileNotificationOrchestrator(OK)
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": false,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "other",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T00:00",
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
            |                     "SHA-256": "md5"
            |                 },
            |                 "lastUpdated":"2018-04-24T09:30:00"
            |               }
            |            ]
            |		}
            |}
            |""".stripMargin
        )
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }

      "call the connector and send the appeal data received in the request body - returns OK when successful for other with file upload (audit storage failure)" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
        mockResponseForFileNotificationOrchestrator(INTERNAL_SERVER_ERROR)
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": false,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "other",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T00:00",
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
            |                     "SHA-256": "md5"
            |                 },
            |                 "lastUpdated":"2018-04-24T09:30:00"
            |               }
            |            ]
            |		}
            |}
            |""".stripMargin
        )
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
        eventually {
          wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("PenaltyAppealFileNotificationStorageFailure")) shouldBe true
        }
      }

      "call the connector and send the appeal data received in the request body - returns OK when successful for LPP" in {
        mockResponseForAppealSubmissionStub(OK, "HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyNumber = "123456789")
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": true,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "crime",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T00:00",
            |            "reportedIssueToPolice": true,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true&penaltyNumber=123456789&correlationId=correlationId").post(
          jsonToSubmit
        ))
        result.status shouldBe OK
      }
    }

    "return BAD_REQUEST (400)" when {
      "no JSON body is in the request" in {
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true&penaltyNumber=123456789&correlationId=correlationId").post(
          ""
        ))
        result.status shouldBe BAD_REQUEST
      }

      "JSON body is present but it can not be parsed to a model" in {
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true&penaltyNumber=123456789&correlationId=correlationId").post(
          Json.parse("{}")
        ))
        result.status shouldBe BAD_REQUEST
      }
    }

    "return error status code" when {
      "the call to PEGA/stub fails" in {
        mockResponseForAppealSubmissionStub(GATEWAY_TIMEOUT, "HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": false,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "crime",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T00:00",
            |            "reportedIssueToPolice": true,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
          jsonToSubmit
        ))
        result.status shouldBe GATEWAY_TIMEOUT
      }

      "the call to PEGA/stub has a fault" in {
        mockResponseForAppealSubmissionStubFault("HMRC-MTD-VAT~VRN~123456789", penaltyNumber = "123456789")
        val jsonToSubmit: JsValue = Json.parse(
          """
            |{
            |    "sourceSystem": "MDTP",
            |    "taxRegime": "VAT",
            |    "customerReferenceNo": "123456789",
            |    "dateOfAppeal": "2020-01-01T00:00:00",
            |    "isLPP": false,
            |    "appealSubmittedBy": "client",
            |    "appealInformation": {
            |						 "reasonableExcuse": "crime",
            |            "honestyDeclaration": true,
            |            "startDateOfEvent": "2021-04-23T00:00",
            |            "reportedIssueToPolice": true,
            |						 "statement": "This is a statement",
            |            "lateAppeal": false
            |		}
            |}
            |""".stripMargin)
        val result = await(buildClientForRequestToApp(uri = "/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=123456789&correlationId=correlationId").post(
          jsonToSubmit
        ))
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "getMultiplePenaltyData" should {
    val getPenaltyDetailsOneLPPJson: JsValue = Json.parse(
      """
        |{
        | "totalisations": {
        |   "LSPTotalValue": 200,
        |   "penalisedPrincipalTotal": 2000,
        |   "LPPPostedTotal": 165.25,
        |   "LPPEstimatedTotal": 15.26
        | },
        | "latePaymentPenalty": {
        |     "details": [
        |       {
        |          "penaltyChargeReference": "1234567887",
        |          "penaltyCategory": "LPP1",
        |          "penaltyStatus": "P",
        |          "penaltyAmountPaid": 0,
        |          "penaltyAmountOutstanding": 144.00,
        |          "LPP1LRCalculationAmount": 99.99,
        |          "LPP1LRDays": "15",
        |          "LPP1LRPercentage": 2.00,
        |          "LPP1HRCalculationAmount": 99.99,
        |          "LPP1HRDays": "31",
        |          "LPP1HRPercentage": 2.00,
        |          "LPP2Days": "31",
        |          "LPP2Percentage": 4.00,
        |          "penaltyChargeCreationDate": "2022-10-30",
        |          "communicationsDate": "2023-02-08",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-01-01",
        |          "principalChargeBillingTo": "2022-12-31",
        |          "principalChargeDueDate": "2023-02-07"
        |       }
        |   ]
        | }
        |}
        |""".stripMargin
    )

    val getPenaltyDetailsTwoLPPsJson: JsValue = Json.parse(
      """
        |{
        | "totalisations": {
        |   "LSPTotalValue": 200,
        |   "penalisedPrincipalTotal": 2000,
        |   "LPPPostedTotal": 165.25,
        |   "LPPEstimatedTotal": 15.26
        | },
        | "latePaymentPenalty": {
        |     "details": [
        |       {
        |          "penaltyChargeReference": "1234567888",
        |          "penaltyCategory": "LPP2",
        |          "penaltyStatus": "P",
        |          "penaltyAmountPaid": 0,
        |          "penaltyAmountOutstanding": 144.00,
        |          "LPP1LRCalculationAmount": 99.99,
        |          "LPP1LRDays": "15",
        |          "LPP1LRPercentage": 2.00,
        |          "LPP1HRCalculationAmount": 99.99,
        |          "LPP1HRDays": "31",
        |          "LPP1HRPercentage": 2.00,
        |          "LPP2Days": "31",
        |          "LPP2Percentage": 4.00,
        |          "penaltyChargeCreationDate": "2022-10-30",
        |          "communicationsDate": "2023-02-08",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-01-01",
        |          "principalChargeBillingTo": "2022-12-31",
        |          "principalChargeDueDate": "2023-02-07"
        |       },
        |       {
        |          "penaltyChargeReference": "1234567887",
        |          "penaltyCategory": "LPP1",
        |          "penaltyStatus": "P",
        |          "penaltyAmountPaid": 0,
        |          "penaltyAmountOutstanding": 144.01,
        |          "LPP1LRCalculationAmount": 99.99,
        |          "LPP1LRDays": "15",
        |          "LPP1LRPercentage": 2.00,
        |          "LPP1HRCalculationAmount": 99.99,
        |          "LPP1HRDays": "31",
        |          "LPP1HRPercentage": 2.00,
        |          "LPP2Days": "31",
        |          "LPP2Percentage": 4.00,
        |          "penaltyChargeCreationDate": "2022-10-30",
        |          "communicationsDate": "2023-01-08",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-01-01",
        |          "principalChargeBillingTo": "2022-12-31",
        |          "principalChargeDueDate": "2023-02-07"
        |       }
        |   ]
        | }
        |}
        |""".stripMargin
    )

    val getPenaltyDetailsTwoLPPsWithAppealsJson: JsValue = Json.parse(
      """
        |{
        | "totalisations": {
        |   "LSPTotalValue": 200,
        |   "penalisedPrincipalTotal": 2000,
        |   "LPPPostedTotal": 165.25,
        |   "LPPEstimatedTotal": 15.26
        | },
        | "latePaymentPenalty": {
        |     "details": [
        |       {
        |          "penaltyChargeReference": "1234567888",
        |          "penaltyCategory": "LPP2",
        |          "penaltyStatus": "P",
        |          "penaltyAmountPaid": 0,
        |          "penaltyAmountOutstanding": 144.00,
        |          "LPP1LRCalculationAmount": 99.99,
        |          "LPP1LRDays": "15",
        |          "LPP1LRPercentage": 2.00,
        |          "LPP1HRCalculationAmount": 99.99,
        |          "LPP1HRDays": "31",
        |          "LPP1HRPercentage": 2.00,
        |          "LPP2Days": "31",
        |          "LPP2Percentage": 4.00,
        |          "penaltyChargeCreationDate": "2022-10-30",
        |          "communicationsDate": "2023-02-08",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-01-01",
        |          "principalChargeBillingTo": "2022-12-31",
        |          "principalChargeDueDate": "2023-02-07",
        |          "appealInformation": [
        |           {
        |             "appealStatus": "A",
        |             "appealLevel": "01"
        |           }
        |           ]
        |       },
        |       {
        |          "penaltyChargeReference": "1234567887",
        |          "penaltyCategory": "LPP1",
        |          "penaltyStatus": "P",
        |          "penaltyAmountPaid": 0,
        |          "penaltyAmountOutstanding": 144.01,
        |          "LPP1LRCalculationAmount": 99.99,
        |          "LPP1LRDays": "15",
        |          "LPP1LRPercentage": 2.00,
        |          "LPP1HRCalculationAmount": 99.99,
        |          "LPP1HRDays": "31",
        |          "LPP1HRPercentage": 2.00,
        |          "LPP2Days": "31",
        |          "LPP2Percentage": 4.00,
        |          "penaltyChargeCreationDate": "2022-10-30",
        |          "communicationsDate": "2023-02-08",
        |          "penaltyChargeDueDate": "2022-10-30",
        |          "principalChargeReference": "1234567890",
        |          "principalChargeBillingFrom": "2022-01-01",
        |          "principalChargeBillingTo": "2022-12-31",
        |          "principalChargeDueDate": "2023-02-07"
        |       }
        |   ]
        | }
        |}
        |""".stripMargin
    )

    "call ETMP and return NO_CONTENT" when {
      "there is only one penalty related to the charge" in {
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", Some(getPenaltyDetailsOneLPPJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/appeals-data/multiple-penalties?penaltyId=1234567887&enrolmentKey=HMRC-MTD-VAT~VRN~123456789").get())
        result.status shouldBe Status.NO_CONTENT
      }

      "either penalty under the principal charge has appeal in any state" in {
        mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", Some(getPenaltyDetailsTwoLPPsWithAppealsJson.toString()))
        val result = await(buildClientForRequestToApp(uri = "/appeals-data/multiple-penalties?penaltyId=1234567887&enrolmentKey=HMRC-MTD-VAT~VRN~123456789").get())
        result.status shouldBe Status.NO_CONTENT
      }
    }

    "call ETMP and return OK when there is two penalties related to the charge" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", Some(getPenaltyDetailsTwoLPPsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/multiple-penalties?penaltyId=1234567887&enrolmentKey=HMRC-MTD-VAT~VRN~123456789").get())
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
      mockStubResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, "123456789", Some(""))
      val result = await(buildClientForRequestToApp(uri = "/appeals-data/multiple-penalties?penaltyId=0001&enrolmentKey=HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }
}
