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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class PenaltiesFrontendControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val controller: PenaltiesFrontendController = injector.instanceOf[PenaltiesFrontendController]
  val financialDataQueryParamWithClearedItems: String = {
      s"includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true" +
      s"&addRegimeTotalisation=true&addLockInformation=true&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true" +
      s"&dateType=POSTING&dateFrom=${LocalDate.now().minusYears(2).toString}&dateTo=${LocalDate.now().toString}"
  }
  val financialDataQueryParamWithoutClearedItems: String = {
    s"includeClearedItems=false&includeStatisticalItems=true&includePaymentOnAccount=true" +
      s"&addRegimeTotalisation=true&addLockInformation=true&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true" +
      s"&dateType=POSTING&dateFrom=${LocalDate.now().minusYears(2).toString}&dateTo=${LocalDate.now().toString}"
  }

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
      |     "activePenaltyPoints": 0,
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
      |          "penaltyCategory": "LPP1",
      |          "penaltyStatus": "A",
      |          "penaltyAmountPosted": 0,
      |          "LPP1LRCalculationAmount": 99.99,
      |          "LPP1LRDays": "15",
      |          "LPP1LRPercentage": 2.00,
      |          "LPP1HRCalculationAmount": 99.99,
      |          "LPP1HRDays": "31",
      |          "LPP1HRPercentage": 2.00,
      |          "LPP2Days": "31",
      |          "LPP2Percentage": 4.00,
      |          "penaltyChargeCreationDate": "2022-10-30",
      |          "penaltyChargeDueDate": "2022-10-30",
      |          "principalChargeReference": "XM002610011594",
      |          "principalChargeDocNumber": "DOC1",
      |          "principalChargeSubTransaction": "SUB1",
      |          "principalChargeBillingFrom": "2022-10-30",
      |          "principalChargeBillingTo": "2022-10-30",
      |          "principalChargeDueDate": "2022-10-30",
      |          "timeToPay": [
      |             {
      |               "TTPStartDate": "2022-01-01",
      |               "TTPEndDate": "2022-12-31"
      |             }
      |          ],
      |          "principalChargeMainTransaction": "4700",
      |          "penaltyAmountAccruing": 99.99
      |       }
      |   ]
      | }
      |}
      |""".stripMargin)

  val getPenaltyDetailsJsonWithBlankAppealLevel: JsValue = Json.parse(
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
      |     "activePenaltyPoints": 1,
      |     "inactivePenaltyPoints": 0,
      |     "regimeThreshold": 5,
      |     "penaltyChargeAmount": 200.00,
      |     "PoCAchievementDate": "2022-01-01"
      |   },
      |   "details": [
      |     {
      |       "penaltyNumber": "12345678901234",
      |       "penaltyOrder": "01",
      |       "penaltyCategory": "P",
      |       "penaltyStatus": "ACTIVE",
      |       "penaltyCreationDate": "2022-10-30",
      |       "penaltyExpiryDate": "2022-10-30",
      |       "communicationsDate": "2022-10-30",
      |       "triggeringProcess": "P123",
      |       "lateSubmissions": [
      |         {
      |           "lateSubmissionID": "001",
      |           "taxPeriod":  "23AA",
      |           "taxPeriodStartDate": "2022-01-01",
      |           "taxPeriodEndDate": "2022-12-31",
      |           "taxPeriodDueDate": "2023-02-07",
      |           "returnReceiptDate": "2023-02-01",
      |           "taxReturnStatus": "Fulfilled"
      |         }
      |       ],
      |       "appealInformation": [
      |         {
      |           "appealStatus": "99",
      |           "appealDescription": "Some value"
      |         }
      |       ]
      |     }
      |   ]
      | },
      | "latePaymentPenalty": {
      |     "details": [
      |       {
      |         "principalChargeDueDate": "2022-10-30",
      |         "principalChargeBillingTo": "2022-10-30",
      |         "LPP1LRPercentage": 2,
      |         "LPP1HRDays": "31",
      |         "penaltyChargeDueDate": "2022-10-30",
      |         "LPP2Days": "31",
      |         "penaltyAmountPosted": 0,
      |         "penaltyChargeCreationDate": "2022-10-30",
      |         "LPP1HRPercentage": 2,
      |         "LPP1LRDays": "15",
      |         "timeToPay": [
      |             {
      |                 "TTPStartDate": "2022-01-01",
      |                 "TTPEndDate": "2022-12-31"
      |             }
      |         ],
      |         "LPP1HRCalculationAmount": 99.99,
      |         "penaltyCategory": "LPP1",
      |         "principalChargeReference": "XM002610011594",
      |         "principalChargeBillingFrom": "2022-10-30",
      |         "penaltyStatus": "A",
      |         "mainTransaction": "4703",
      |         "LPP2Percentage": 4,
      |         "LPP1LRCalculationAmount": 99.99,
      |         "principalChargeDocNumber": "DOC1",
      |         "principalChargeSubTransaction": "SUB1",
      |         "appealInformation": [
      |           {
      |             "appealStatus": "99",
      |             "appealDescription": "Some value"
      |           }
      |         ],
      |         "principalChargeMainTransaction": "4700",
      |         "penaltyAmountAccruing": 99.99
      |       }
      |   ]
      | }
      |}
      |""".stripMargin)

  val getPenaltyDetailsJsonWithRemovedExpiryReasonAndDefaultedAppealLevel: JsValue = Json.parse(
    """
      |{
      | "totalisations": {
      |   "LSPTotalValue": 200,
      |   "penalisedPrincipalTotal": 2000,
      |   "LPPPostedTotal": 165.25,
      |   "LPPEstimatedTotal": 15.26,
      |   "totalAccountOverdue": 1000.0,
      |   "totalAccountPostedInterest": 12.34,
      |   "totalAccountAccruingInterest": 43.21
      | },
      | "lateSubmissionPenalty": {
      |   "summary": {
      |     "activePenaltyPoints": 1,
      |     "inactivePenaltyPoints": 0,
      |     "regimeThreshold": 5,
      |     "penaltyChargeAmount": 200.00,
      |     "PoCAchievementDate": "2022-01-01"
      |   },
      |   "details": [
      |     {
      |       "penaltyNumber": "12345678901234",
      |       "penaltyOrder": "01",
      |       "penaltyCategory": "P",
      |       "penaltyStatus": "ACTIVE",
      |       "penaltyCreationDate": "2022-10-30",
      |       "penaltyExpiryDate": "2022-10-30",
      |       "communicationsDate": "2022-10-30",
      |       "triggeringProcess": "P123",
      |       "lateSubmissions": [
      |         {
      |           "lateSubmissionID": "001",
      |           "taxPeriod":  "23AA",
      |           "taxPeriodStartDate": "2022-01-01",
      |           "taxPeriodEndDate": "2022-12-31",
      |           "taxPeriodDueDate": "2023-02-07",
      |           "returnReceiptDate": "2023-02-01",
      |           "taxReturnStatus": "Fulfilled"
      |         }
      |       ],
      |       "appealInformation": [
      |         {
      |           "appealStatus": "99",
      |           "appealLevel": "01",
      |           "appealDescription": "Some value"
      |         }
      |       ]
      |     }
      |   ]
      | },
      | "latePaymentPenalty": {
      |     "details": [
      |       {
      |         "principalChargeDueDate": "2022-10-30",
      |         "principalChargeBillingTo": "2022-10-30",
      |         "penaltyAmountPosted": 0,
      |         "LPP1LRPercentage": 2,
      |         "LPP1HRDays": "31",
      |         "penaltyChargeDueDate": "2022-10-30",
      |         "LPP2Days": "31",
      |         "penaltyChargeCreationDate": "2022-10-30",
      |         "LPP1HRPercentage": 2,
      |         "LPP1LRDays": "15",
      |         "LPP1HRCalculationAmount": 99.99,
      |         "penaltyCategory": "LPP1",
      |         "principalChargeReference": "XM002610011594",
      |         "principalChargeBillingFrom": "2022-10-30",
      |         "principalChargeDocNumber": "DOC1",
      |         "principalChargeSubTransaction": "SUB1",
      |         "penaltyStatus": "A",
      |         "mainTransaction": "4700",
      |         "LPP2Percentage": 4,
      |         "LPP1LRCalculationAmount": 99.99,
      |         "appealInformation": [
      |           {
      |             "appealStatus": "99",
      |             "appealLevel": "01",
      |             "appealDescription": "Some value"
      |           }
      |         ],
      |         "timeToPay": [
      |             {
      |                 "TTPStartDate": "2022-01-01",
      |                 "TTPEndDate": "2022-12-31"
      |             }
      |         ],
      |         "principalChargeMainTransaction": "4700",
      |         "penaltyAmountAccruing": 99.99,
      |         "vatOutstandingAmount": 543.21
      |       }
      |   ]
      | }
      |}
      |""".stripMargin)

  val combinedPenaltyAndFinancialData: JsValue = Json.parse(
    """
      |{
      |    "totalisations": {
      |        "LSPTotalValue": 200,
      |        "penalisedPrincipalTotal": 2000,
      |        "LPPPostedTotal": 165.25,
      |        "LPPEstimatedTotal": 15.26,
      |        "totalAccountOverdue": 1000.0,
      |        "totalAccountPostedInterest": 12.34,
      |        "totalAccountAccruingInterest": 43.21
      |    },
      |    "lateSubmissionPenalty": {
      |        "summary": {
      |            "activePenaltyPoints": 0,
      |            "inactivePenaltyPoints": 0,
      |            "regimeThreshold": 5,
      |            "penaltyChargeAmount": 200,
      |            "PoCAchievementDate": "2022-01-01"
      |        },
      |        "details": []
      |    },
      |    "latePaymentPenalty": {
      |        "details": [
      |            {
      |                "principalChargeDueDate": "2022-10-30",
      |                "principalChargeBillingTo": "2022-10-30",
      |                "penaltyAmountPosted": 0,
      |                "LPP1LRPercentage": 2,
      |                "LPP1HRDays": "31",
      |                "penaltyChargeDueDate": "2022-10-30",
      |                "LPP2Days": "31",
      |                "penaltyChargeCreationDate": "2022-10-30",
      |                "LPP1HRPercentage": 2,
      |                "LPP1LRDays": "15",
      |                "LPP1HRCalculationAmount": 99.99,
      |                "penaltyCategory": "LPP1",
      |                "principalChargeReference": "XM002610011594",
      |                "principalChargeBillingFrom": "2022-10-30",
      |                "penaltyStatus": "A",
      |                "mainTransaction": "4700",
      |                "LPP2Percentage": 4,
      |                "LPP1LRCalculationAmount": 99.99,
      |                "principalChargeMainTransaction": "4700",
      |                "principalChargeDocNumber": "DOC1",
      |                "principalChargeSubTransaction": "SUB1",
      |                "timeToPay": [
      |                 {
      |                   "TTPStartDate": "2022-01-01",
      |                   "TTPEndDate": "2022-12-31"
      |                 }
      |                ],
      |                "penaltyAmountAccruing": 99.99,
      |                "vatOutstandingAmount": 543.21
      |            }
      |        ]
      |    }
      |}
      |""".stripMargin
  )

  val combinedPenaltyAndFinancialDataWithout1811Totalisations: JsValue = Json.parse(
    """
      |{
      |    "totalisations": {
      |        "LSPTotalValue": 200,
      |        "penalisedPrincipalTotal": 2000,
      |        "LPPPostedTotal": 165.25,
      |        "LPPEstimatedTotal": 15.26
      |    },
      |    "lateSubmissionPenalty": {
      |        "summary": {
      |            "activePenaltyPoints": 0,
      |            "inactivePenaltyPoints": 0,
      |            "regimeThreshold": 5,
      |            "penaltyChargeAmount": 200,
      |            "PoCAchievementDate": "2022-01-01"
      |        },
      |        "details": []
      |    },
      |    "latePaymentPenalty": {
      |        "details": [
      |            {
      |                "principalChargeDueDate": "2022-10-30",
      |                "principalChargeBillingTo": "2022-10-30",
      |                "penaltyAmountPosted": 0,
      |                "LPP1LRPercentage": 2,
      |                "LPP1HRDays": "31",
      |                "penaltyChargeDueDate": "2022-10-30",
      |                "LPP2Days": "31",
      |                "penaltyChargeCreationDate": "2022-10-30",
      |                "LPP1HRPercentage": 2,
      |                "LPP1LRDays": "15",
      |                "LPP1HRCalculationAmount": 99.99,
      |                "penaltyCategory": "LPP1",
      |                "principalChargeReference": "XM002610011594",
      |                "principalChargeBillingFrom": "2022-10-30",
      |                "penaltyStatus": "A",
      |                "mainTransaction": "4700",
      |                "LPP2Percentage": 4,
      |                "LPP1LRCalculationAmount": 99.99,
      |                "principalChargeMainTransaction": "4700",
      |                "principalChargeDocNumber": "DOC1",
      |                "principalChargeSubTransaction": "SUB1",
      |                "timeToPay": [
      |                 {
      |                   "TTPStartDate": "2022-01-01",
      |                   "TTPEndDate": "2022-12-31"
      |                 }
      |                ],
      |                "penaltyAmountAccruing": 99.99,
      |                "vatOutstandingAmount": 543.21
      |            }
      |        ]
      |    }
      |}
      |""".stripMargin
  )

  val combinedPenaltyAndFinancialDataWithManualLPP: JsValue = Json.parse(
    """
      |{
      |    "totalisations": {
      |        "LSPTotalValue": 200,
      |        "penalisedPrincipalTotal": 2000,
      |        "LPPPostedTotal": 220.25,
      |        "LPPEstimatedTotal": 15.26
      |    },
      |    "lateSubmissionPenalty": {
      |        "summary": {
      |            "activePenaltyPoints": 0,
      |            "inactivePenaltyPoints": 0,
      |            "regimeThreshold": 5,
      |            "penaltyChargeAmount": 200,
      |            "PoCAchievementDate": "2022-01-01"
      |        },
      |        "details": []
      |    },
      |    "latePaymentPenalty": {
      |        "details": [
      |            {
      |               "principalChargeReference" : "PENALTY1234",
      |		            "penaltyCategory": "MANUAL",
      |		            "penaltyStatus": "P",
      |		            "penaltyAmountAccruing": 0,
      |		            "penaltyAmountPosted": 100.00,
      |		            "penaltyAmountPaid": 45.00,
      |		            "penaltyAmountOutstanding": 55.00,
      |		            "penaltyChargeCreationDate": "2023-04-01",
      |               "principalChargeDueDate": "2023-04-01",
      |               "principalChargeBillingTo": "2023-04-01",
      |               "principalChargeBillingFrom": "2023-04-01",
      |               "mainTransaction": "4787",
      |               "principalChargeMainTransaction": "4787"
      |            }
      |        ]
      |    }
      |}
      |""".stripMargin
  )

  s"return OK (${Status.OK})" when {

    "the get penalty details call succeeds and the get financial details call succeeds (combining the data together)" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.OK,
        s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems", Some(getFinancialDetailsWithoutTotalisationsAsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.OK,
        s"VRN/123456789/VATC?$financialDataQueryParamWithoutClearedItems", Some(getFinancialDetailsTotalisationsAsJson.toString()))

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe OK
      Json.parse(result.body) shouldBe combinedPenaltyAndFinancialData
    }

    "the get penalty details call includes blank appealLevel fields" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJsonWithBlankAppealLevel.toString()))
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems")
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParamWithoutClearedItems", Some(getFinancialDetailsTotalisationsAsJson.toString()))

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe OK
      Json.parse(result.body) shouldBe getPenaltyDetailsJsonWithRemovedExpiryReasonAndDefaultedAppealLevel
    }

    "the get penalty details call succeeds and the get financial details call succeeds (combining the data together - second 1811 call returns NO_CONTENT)" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.OK,
        s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems")
      mockStubResponseForGetFinancialDetails(Status.NO_CONTENT,
        s"VRN/123456789/VATC?$financialDataQueryParamWithoutClearedItems")

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe OK
      Json.parse(result.body) shouldBe combinedPenaltyAndFinancialDataWithout1811Totalisations
    }

    "the get penalty details call succeeds and the get financial details call succeeds (combining the data together - for manual LPP)" in {
      val getFinancialDetailsWithManualLPP: JsValue = Json.parse(
        """
          |{
          | "getFinancialData": {
          |   "financialDetails":{
          |     "documentDetails": [
          |     {
          |      "chargeReferenceNumber": "PENALTY1234",
          |      "issueDate": "2023-04-01",
          |      "documentTotalAmount": "100.00",
          |      "documentOutstandingAmount": "55.00",
          |      "lineItemDetails": [
          |        {
          |          "mainTransaction": "4787"
          |      }]
          |    }
          |  ]
          |}
          |}
          |}
          |""".stripMargin
      )
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
          |     "activePenaltyPoints": 0,
          |     "inactivePenaltyPoints": 0,
          |     "regimeThreshold": 5,
          |     "penaltyChargeAmount": 200.00,
          |     "PoCAchievementDate": "2022-01-01"
          |   },
          |   "details": []
          | },
          | "latePaymentPenalty": {
          |    "details": []
          | }
          |}
          |""".stripMargin)
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.OK,
        s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems", body = Some(getFinancialDetailsWithManualLPP.toString()))
      mockStubResponseForGetFinancialDetails(Status.NO_CONTENT,
        s"VRN/123456789/VATC?$financialDataQueryParamWithoutClearedItems")

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe OK
      Json.parse(result.body) shouldBe combinedPenaltyAndFinancialDataWithManualLPP
    }

    s"the get penalty details call succeeds and the get financial details call returns NO_CONTENT (${Status.NO_CONTENT}) (returning penalty details unaltered)" in {
      val getPenaltyDetailsNoLPPJson: JsValue = Json.parse(
        """
          |{
          |}
          |""".stripMargin)
      val noDataFoundBody =
        """
          |{
          | "failures": [
          |   {
          |     "code": "NO_DATA_FOUND",
          |     "reason": "This is a reason"
          |   }
          | ]
          |}
          |""".stripMargin
      mockStubResponseForGetFinancialDetails(Status.NOT_FOUND,
        s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems", Some(noDataFoundBody))
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsNoLPPJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe OK
      Json.parse(result.body) shouldBe getPenaltyDetailsNoLPPJson
    }
  }

  s"return NOT_FOUND (${Status.NOT_FOUND})" when {
    "the user supplies an invalid VRN" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789123456789").get())
      result.status shouldBe NOT_FOUND
    }
  }

  s"return NO_CONTENT (${Status.NO_CONTENT})" when {
    "the get penalty details call returns 404 with NO_DATA_FOUND in body" in {
      val noDataFoundBody =
        """
          |{
          | "failures": [
          |   {
          |     "code": "NO_DATA_FOUND",
          |     "reason": "This is a reason"
          |   }
          | ]
          |}
          |""".stripMargin
      mockStubResponseForGetPenaltyDetails(Status.NOT_FOUND, "123456789", body = Some(noDataFoundBody))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe NO_CONTENT
    }

    "the get financial details call returns 404 with NO_DATA_FOUND in body" in {
      val noDataFoundBody =
        """
          |{
          | "failures": [
          |   {
          |     "code": "NO_DATA_FOUND",
          |     "reason": "This is a reason"
          |   }
          | ]
          |}
          |""".stripMargin
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.NOT_FOUND, s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems", Some(noDataFoundBody))

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe NO_CONTENT
    }
  }

  s"return ISE (${Status.INTERNAL_SERVER_ERROR})" when {
    "the get penalty details call fails" in {
      mockStubResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, "123456789", body = Some(""))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "the first get financial details call fails" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.INTERNAL_SERVER_ERROR, s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems")

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "the second get financial details call fails" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems")
      mockStubResponseForGetFinancialDetails(Status.INTERNAL_SERVER_ERROR, s"VRN/123456789/VATC?$financialDataQueryParamWithoutClearedItems")

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
      result.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "audit the response when the user has > 0 penalties" in {
    val penaltyDetailsWithLSPAndLPPs: JsValue = Json.parse(
      """
        |{
        |   "totalisations":{
        |      "LSPTotalValue":200,
        |      "penalisedPrincipalTotal":2000,
        |      "LPPPostedTotal":165.25,
        |      "LPPEstimatedTotal":15.26
        |   },
        |   "lateSubmissionPenalty":{
        |      "summary":{
        |         "activePenaltyPoints": 1,
        |         "inactivePenaltyPoints": 0,
        |         "regimeThreshold": 5,
        |         "penaltyChargeAmount": 200,
        |         "PoCAchievementDate": "2022-01-01"
        |      },
        |      "details":[
        |         {
        |            "penaltyNumber":"12345678901234",
        |            "penaltyOrder":"01",
        |            "penaltyCategory":"P",
        |            "penaltyStatus":"ACTIVE",
        |            "penaltyCreationDate":"2022-10-30",
        |            "penaltyExpiryDate":"2022-10-30",
        |            "communicationsDate":"2022-10-30",
        |            "lateSubmissions":[
        |               {
        |                  "lateSubmissionID": "001",
        |                  "taxPeriod":  "23AA",
        |                  "taxPeriodStartDate":"2022-01-01",
        |                  "taxPeriodEndDate":"2022-12-31",
        |                  "taxPeriodDueDate":"2023-02-07",
        |                  "returnReceiptDate":"2023-02-01",
        |                  "taxReturnStatus":"Fulfilled"
        |               }
        |            ],
        |            "appealInformation":[
        |               {
        |                  "appealStatus":"99",
        |                  "appealLevel":"01",
        |                  "appealDescription": "Some value"
        |               }
        |            ],
        |            "chargeDueDate":"2022-10-30",
        |            "chargeOutstandingAmount":200,
        |            "chargeAmount":200,
        |            "triggeringProcess": "P123",
        |            "chargeReference": "CHARGEREF1"
        |         }
        |      ]
        |   },
        |   "latePaymentPenalty":{
        |      "details":[
        |         {
        |            "principalChargeDueDate":"2022-10-30",
        |            "principalChargeBillingTo":"2022-10-30",
        |            "penaltyAmountPosted": 0,
        |            "LPP1LRPercentage":2,
        |            "LPP1HRDays":"31",
        |            "penaltyChargeDueDate":"2022-10-30",
        |            "communicationsDate":"2022-10-30",
        |            "LPP2Days":"31",
        |            "penaltyChargeCreationDate":"2022-10-30",
        |            "LPP1HRPercentage":2,
        |            "LPP1LRDays":"15",
        |            "LPP1HRCalculationAmount":99.99,
        |            "penaltyChargeReference":"1234567890",
        |            "penaltyCategory":"LPP1",
        |            "principalChargeReference":"XM002610011594",
        |            "penaltyStatus":"A",
        |            "penaltyAmountAccruing": 99.99,
        |            "principalChargeBillingFrom":"2022-10-30",
        |            "LPP2Percentage":4,
        |            "appealInformation":[
        |               {
        |                  "appealStatus":"99",
        |                  "appealLevel":"01",
        |                  "appealDescription": "Some value"
        |               }
        |            ],
        |            "LPP1LRCalculationAmount":99.99,
        |            "principalChargeMainTransaction": "4700",
        |            "penaltyAmountOutstanding":0,
        |            "principalChargeDocNumber": "DOC1",
        |            "principalChargeSubTransaction": "SUB1"
        |         }
        |      ]
        |   }
        |}
        |""".stripMargin)

    val penaltyDetailsWithLSPAndLPPAndFinancialDetails: JsValue = Json.parse(
      """
        |{
        |   "totalisations":{
        |      "LSPTotalValue":200,
        |      "penalisedPrincipalTotal":2000,
        |      "LPPPostedTotal":165.25,
        |      "LPPEstimatedTotal":15.26,
        |      "totalAccountOverdue": 1000.0,
        |      "totalAccountPostedInterest": 12.34,
        |      "totalAccountAccruingInterest": 43.21
        |   },
        |   "lateSubmissionPenalty":{
        |      "summary":{
        |         "activePenaltyPoints": 1,
        |         "inactivePenaltyPoints": 0,
        |         "regimeThreshold": 5,
        |         "penaltyChargeAmount": 200,
        |         "PoCAchievementDate": "2022-01-01"
        |      },
        |      "details":[
        |         {
        |            "penaltyNumber":"12345678901234",
        |            "penaltyOrder":"01",
        |            "penaltyCategory":"P",
        |            "penaltyStatus":"ACTIVE",
        |            "penaltyCreationDate":"2022-10-30",
        |            "penaltyExpiryDate":"2022-10-30",
        |            "communicationsDate":"2022-10-30",
        |            "lateSubmissions":[
        |               {
        |                  "lateSubmissionID": "001",
        |                  "taxPeriod":  "23AA",
        |                  "taxPeriodStartDate":"2022-01-01",
        |                  "taxPeriodEndDate":"2022-12-31",
        |                  "taxPeriodDueDate":"2023-02-07",
        |                  "returnReceiptDate":"2023-02-01",
        |                  "taxReturnStatus":"Fulfilled"
        |               }
        |            ],
        |            "appealInformation":[
        |               {
        |                  "appealStatus":"99",
        |                  "appealLevel":"01",
        |                  "appealDescription": "Some value"
        |               }
        |            ],
        |            "chargeDueDate":"2022-10-30",
        |            "chargeOutstandingAmount":200,
        |            "chargeAmount":200,
        |            "triggeringProcess": "P123",
        |            "chargeReference": "CHARGEREF1"
        |         }
        |      ]
        |   },
        |   "latePaymentPenalty":{
        |      "details":[
        |         {
        |            "principalChargeDueDate":"2022-10-30",
        |            "principalChargeBillingTo":"2022-10-30",
        |            "penaltyAmountPosted": 0,
        |            "LPP1LRPercentage":2,
        |            "LPP1HRDays":"31",
        |            "penaltyChargeDueDate":"2022-10-30",
        |            "communicationsDate":"2022-10-30",
        |            "LPP2Days":"31",
        |            "penaltyChargeCreationDate":"2022-10-30",
        |            "LPP1HRPercentage":2,
        |            "LPP1LRDays":"15",
        |            "LPP1HRCalculationAmount":99.99,
        |            "penaltyChargeReference":"1234567890",
        |            "penaltyCategory":"LPP1",
        |            "principalChargeReference":"XM002610011594",
        |            "penaltyStatus":"A",
        |            "penaltyAmountAccruing": 99.99,
        |            "principalChargeMainTransaction": "4700",
        |            "principalChargeBillingFrom":"2022-10-30",
        |            "principalChargeDocNumber": "DOC1",
        |            "principalChargeSubTransaction": "SUB1",
        |            "mainTransaction":"4700",
        |            "LPP2Percentage":4,
        |            "appealInformation":[
        |               {
        |                  "appealStatus":"99",
        |                  "appealLevel":"01",
        |                  "appealDescription": "Some value"
        |               }
        |            ],
        |            "LPP1LRCalculationAmount":99.99,
        |            "penaltyAmountOutstanding":0,
        |            "vatOutstandingAmount": 543.21
        |         }
        |      ]
        |   }
        |}
        |""".stripMargin)
    mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(penaltyDetailsWithLSPAndLPPs.toString()))
    mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems")
    mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParamWithoutClearedItems", Some(getFinancialDetailsTotalisationsAsJson.toString))
    val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
    result.status shouldBe Status.OK
    Json.parse(result.body) shouldBe penaltyDetailsWithLSPAndLPPAndFinancialDetails
    wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe true
  }

  "NOT audit the response when the user has 0 LSPs and 0 LPPs" in {
    val getPenaltyDetailsWithNoPointsAsJson: JsValue = Json.parse(
      """
        |{
        | "totalisations": {
        |   "LSPTotalValue": 0,
        |   "penalisedPrincipalTotal": 0,
        |   "LPPPostedTotal": 0.00,
        |   "totalAccountOverdue": 1000,
        |   "totalAccountPostedInterest": 12.34,
        |   "totalAccountAccruingInterest": 43.21
        | },
        | "lateSubmissionPenalty": {
        |   "summary": {
        |     "activePenaltyPoints": 0,
        |     "inactivePenaltyPoints": 0,
        |     "regimeThreshold": 0,
        |     "penaltyChargeAmount": 0.00,
        |     "PoCAchievementDate": "2022-01-01"
        |   },
        |   "details": []
        | },
        | "latePaymentPenalty": {
        |   "details": []
        | }
        |}
        |""".stripMargin)
    mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsWithNoPointsAsJson.toString()))
    mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParamWithClearedItems")
    mockStubResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?$financialDataQueryParamWithoutClearedItems", Some(getFinancialDetailsTotalisationsAsJson.toString))
    val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
    result.status shouldBe Status.OK
    result.body shouldBe getPenaltyDetailsWithNoPointsAsJson.toString()
    wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe false
  }
}
