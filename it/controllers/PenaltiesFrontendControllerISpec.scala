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

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo}

import scala.collection.JavaConverters._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate

class PenaltiesFrontendControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val controller: PenaltiesFrontendController = injector.instanceOf[PenaltiesFrontendController]

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
      |          "principalChargeReference": "XM002610011594",
      |          "principalChargeBillingFrom": "2022-10-30",
      |          "principalChargeBillingTo": "2022-10-30",
      |          "principalChargeDueDate": "2022-10-30",
      |          "timeToPay": [
      |             {
      |               "TTPStartDate": "2022-01-01",
      |               "TTPEndDate": "2022-12-31"
      |             }
      |          ]
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
      |                "penaltyAmountPaid": 0,
      |                "outstandingAmount": 543.21,
      |                "LPP1LRPercentage": 2,
      |                "LPP1HRDays": "31",
      |                "penaltyChargeDueDate": "2022-10-30",
      |                "communicationsDate": "2022-10-30",
      |                "LPP2Days": "31",
      |                "penaltyChargeCreationDate": "2022-10-30",
      |                "LPP1HRPercentage": 2,
      |                "LPP1LRDays": "15",
      |                "timeToPay": [
      |                    {
      |                        "TTPStartDate": "2022-01-01",
      |                        "TTPEndDate": "2022-12-31"
      |                    }
      |                ],
      |                "LPP1HRCalculationAmount": 99.99,
      |                "penaltyCategory": "LPP1",
      |                "principalChargeReference": "XM002610011594",
      |                "principalChargeBillingFrom": "2022-10-30",
      |                "penaltyStatus": "A",
      |                "mainTransaction": "4703",
      |                "LPP2Percentage": 4,
      |                "LPP1LRCalculationAmount": 99.99,
      |                "penaltyAmountOutstanding": 144
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
        s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
          s"&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true")

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
      result.status shouldBe OK
      Json.parse(result.body) shouldBe combinedPenaltyAndFinancialData
    }

    s"the get penalty details call succeeds and the get financial details call returns NO_CONTENT (${Status.NO_CONTENT}) (returning penalty details unaltered)" in {
      val getPenaltyDetailsNoLPPJson: JsValue = Json.parse(
        """
          |{
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
          |
          | }
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
        s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
          s"&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true", Some(noDataFoundBody))
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsNoLPPJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
      result.status shouldBe OK
      Json.parse(result.body) shouldBe getPenaltyDetailsNoLPPJson
    }
  }

  s"return NOT_FOUND (${Status.NOT_FOUND})" when {
    "the user supplies an invalid VRN" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789123456789").get)
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
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
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
      mockStubResponseForGetFinancialDetails(Status.NOT_FOUND,
        s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
          s"&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true", Some(noDataFoundBody))

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
      result.status shouldBe NO_CONTENT
    }
  }

  s"return ISE (${Status.INTERNAL_SERVER_ERROR})" when {
    "the get penalty details call fails" in {
      mockStubResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, "123456789", body = Some(""))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "the get financial details call fails" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
      mockStubResponseForGetFinancialDetails(Status.INTERNAL_SERVER_ERROR,
        s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
          s"&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true")

      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get)
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
        |      "LPPEstimatedTotal":15.26,
        |      "LPIPostedTotal":1968.2,
        |      "LPIEstimatedTotal":7
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
        |                  "appealLevel":"01"
        |               }
        |            ],
        |            "chargeDueDate":"2022-10-30",
        |            "chargeOutstandingAmount":200,
        |            "chargeAmount":200
        |         }
        |      ]
        |   },
        |   "latePaymentPenalty":{
        |      "details":[
        |         {
        |            "principalChargeDueDate":"2022-10-30",
        |            "principalChargeBillingTo":"2022-10-30",
        |            "penaltyAmountPaid":0,
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
        |            "principalChargeBillingFrom":"2022-10-30",
        |            "LPP2Percentage":4,
        |            "appealInformation":[
        |               {
        |                  "appealStatus":"99",
        |                  "appealLevel":"01"
        |               }
        |            ],
        |            "LPP1LRCalculationAmount":99.99,
        |            "penaltyAmountOutstanding":99.99,
        |            "timeToPay": [
        |             {
        |               "TTPStartDate": "2022-01-01",
        |               "TTPEndDate": "2022-12-31"
        |             }
        |          ]
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
        |                  "appealLevel":"01"
        |               }
        |            ],
        |            "chargeDueDate":"2022-10-30",
        |            "chargeOutstandingAmount":200,
        |            "chargeAmount":200
        |         }
        |      ]
        |   },
        |   "latePaymentPenalty":{
        |      "details":[
        |         {
        |            "principalChargeDueDate":"2022-10-30",
        |            "principalChargeBillingTo":"2022-10-30",
        |            "penaltyAmountPaid":0,
        |            "outstandingAmount":543.21,
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
        |            "principalChargeBillingFrom":"2022-10-30",
        |            "mainTransaction":"4703",
        |            "LPP2Percentage":4,
        |            "appealInformation":[
        |               {
        |                  "appealStatus":"99",
        |                  "appealLevel":"01"
        |               }
        |            ],
        |            "LPP1LRCalculationAmount":99.99,
        |            "penaltyAmountOutstanding":99.99,
        |            "timeToPay": [
        |             {
        |               "TTPStartDate": "2022-01-01",
        |               "TTPEndDate": "2022-12-31"
        |             }
        |          ]
        |         }
        |      ]
        |   }
        |}
        |""".stripMargin)
    mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(penaltyDetailsWithLSPAndLPPs.toString()))
    mockStubResponseForGetFinancialDetails(Status.OK,
      s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
        s"&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true")
    val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
    result.status shouldBe Status.OK
    Json.parse(result.body) shouldBe penaltyDetailsWithLSPAndLPPAndFinancialDetails
    wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe true
  }

  "NOT audit the response when the user has 0 LSPs and 0 LPPs" in {
    mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some(getPenaltyDetailsWithNoPointsAsJson.toString()))
    mockStubResponseForGetFinancialDetails(Status.OK,
      s"VRN/123456789/VATC?dateFrom=${LocalDate.now.minusYears(2)}&dateTo=${LocalDate.now}" +
        s"&includeClearedItems=true&includeStatisticalItems=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=false&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true")
    val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/HMRC-MTD-VAT~VRN~123456789").get())
    result.status shouldBe Status.OK
    result.body shouldBe getPenaltyDetailsWithNoPointsAsJson.toString()
    wireMockServer.findAll(postRequestedFor(urlEqualTo("/write/audit"))).asScala.toList.exists(_.getBodyAsString.contains("UserHasPenalty")) shouldBe false
  }
}
