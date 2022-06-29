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

import config.featureSwitches.{CallAPI1811ETMP, CallAPI1812ETMP, FeatureSwitching, UseAPI1812Model}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class APIControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {
  val controller: APIController = injector.instanceOf[APIController]

  val etmpPayloadAsJsonWithEstimatedLPP: JsValue = Json.parse(
    """
        {
      |	"pointsTotal": 2,
      |	"lateSubmissions": 2,
      |	"adjustmentPointsTotal": 1,
      |	"fixedPenaltyAmount": 200,
      |	"penaltyAmountsTotal": 400.00,
      |	"penaltyPointsThreshold": 4,
      |	"penaltyPoints": [
      |		{
      |			"type": "financial",
      |			"number": "3",
      |     "appealStatus": "UNDER_REVIEW",
      |     "id": "123456791",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "DUE",
      |			"period": [{
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"status": "OVERDUE"
      |				}
      |			}],
      |			"communications": [
      |				{
      |					"type": "secureMessage",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			],
      |     "financial": {
      |        "amountDue": 400.00,
      |        "outstandingAmountDue": 400.00,
      |        "dueDate": "2021-04-23T18:25:43.511"
      |     }
      |		},
      |  {
      |			"type": "financial",
      |			"number": "2",
      |     "appealStatus": "UNDER_REVIEW",
      |     "id": "123456790",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |			"period": [{
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			}],
      |			"communications": [
      |				{
      |					"type": "secureMessage",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			],
      |     "financial": {
      |        "amountDue": 400.00,
      |        "outstandingAmountDue": 400.00,
      |        "dueDate": "2021-04-23T18:25:43.511"
      |     }
      |		},
      |		{
      |			"type": "point",
      |			"number": "1",
      |     "id": "123456789",
      |			"dateCreated": "2021-04-23T18:25:43.511",
      |			"dateExpired": "2021-04-23T18:25:43.511",
      |			"status": "ACTIVE",
      |     "reason": "reason",
      |			"period": [{
      |				"startDate": "2021-04-23T18:25:43.511",
      |				"endDate": "2021-04-23T18:25:43.511",
      |				"submission": {
      |					"dueDate": "2021-04-23T18:25:43.511",
      |					"submittedDate": "2021-04-23T18:25:43.511",
      |					"status": "SUBMITTED"
      |				}
      |			}],
      |			"communications": [
      |				{
      |					"type": "letter",
      |					"dateSent": "2021-04-23T18:25:43.511",
      |					"documentId": "1234567890"
      |				}
      |			]
      |		}
      |	],
      | "latePaymentPenalties": [
      |     {
      |       "type": "financial",
      |       "reason": "VAT_NOT_PAID_AFTER_30_DAYS",
      |       "id": "1234567893",
      |       "dateCreated": "2021-04-23T18:25:43.511",
      |       "status": "DUE",
      |       "period": {
      |         "startDate": "2021-04-23T18:25:43.511",
      |         "endDate": "2021-04-23T18:25:43.511",
      |         "dueDate": "2021-04-23T18:25:43.511",
      |	        "paymentStatus": "PAID"
      |       },
      |       "communications": [
      |         {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |         }
      |       ],
      |       "financial": {
      |         "amountDue": 400.00,
      |         "outstandingAmountDue": 2.00,
      |         "dueDate": "2021-04-23T18:25:43.511"
      |       }
      |     },
      |     {
      |       "type": "additional",
      |       "reason": "VAT_NOT_PAID_AFTER_30_DAYS",
      |       "id": "1234567892",
      |       "dateCreated": "2021-04-23T18:25:43.511",
      |       "status": "ESTIMATED",
      |       "period": {
      |         "startDate": "2021-04-23T18:25:43.511",
      |         "endDate": "2021-04-23T18:25:43.511",
      |         "dueDate": "2021-04-23T18:25:43.511",
      |	        "paymentStatus": "PAID"
      |       },
      |       "communications": [
      |         {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |         }
      |       ],
      |       "financial": {
      |         "amountDue": 23.45,
      |         "outstandingAmountDue": 12.00,
      |         "dueDate": "2021-04-23T18:25:43.511"
      |       }
      |     },
      |     {
      |       "type": "financial",
      |       "reason": "VAT_NOT_PAID_WITHIN_30_DAYS",
      |       "id": "1234567891",
      |       "dateCreated": "2021-04-23T18:25:43.511",
      |       "status": "ACTIVE",
      |       "period": {
      |         "startDate": "2021-04-23T18:25:43.511",
      |         "endDate": "2021-04-23T18:25:43.511",
      |         "dueDate": "2021-04-23T18:25:43.511",
      |	        "paymentStatus": "PAID"
      |       },
      |       "communications": [
      |       {
      |          "type": "letter",
      |          "dateSent": "2021-04-23T18:25:43.511",
      |          "documentId": "1234567890"
      |        }
      |       ],
      |       "financial": {
      |         "amountDue": 400.00,
      |         "outstandingAmountDue": 2.00,
      |         "dueDate": "2021-04-23T18:25:43.511"
      |       }
      |    }
      |]
      |}
      |""".stripMargin)

  "getSummaryDataForVRN" should {
    disableFeatureSwitch(UseAPI1812Model)
    "call stub data when 1812 feature is disabled" must {
      s"return OK (${Status.OK})" when {
        "the ETMP call succeeds" in {
          mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", body = Some(etmpPayloadAsJsonWithEstimatedLPP.toString()))
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
          result.status shouldBe OK
          Json.parse(result.body) shouldBe Json.parse(
            """
              |{
              |  "noOfPoints": 2,
              |  "noOfEstimatedPenalties": 1,
              |  "noOfCrystalisedPenalties": 2,
              |  "estimatedPenaltyAmount": 12,
              |  "crystalisedPenaltyAmountDue": 402,
              |  "hasAnyPenaltyData": true
              |}
              |""".stripMargin
          )
        }
      }

      s"return BAD_REQUEST (${Status.BAD_REQUEST})" when {
        "the user supplies an invalid VRN" in {
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789123456789").get)
          result.status shouldBe BAD_REQUEST
        }
      }

      s"return NOT_FOUND (${Status.NOT_FOUND})" when {
        "the ETMP call fails" in {
          mockResponseForStubETMPPayload(Status.INTERNAL_SERVER_ERROR, "HMRC-MTD-VAT~VRN~123456789", body = Some(""))
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
          result.status shouldBe NOT_FOUND
        }

        "the ETMP call returns nothing" in {
          mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", body = Some("{}"))
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
          result.status shouldBe NOT_FOUND
        }
      }
    }

    "call API 1812 when call 1812 feature is enabled" must {
      val getPenaltyDetailsJson: JsValue = Json.parse(
        """
          |{
          | "totalisations": {
          |   "LSPTotalValue": 200,
          |   "penalisedPrincipalTotal": 2000,
          |   "LPPPostedTotal": 165.25,
          |   "LPPEstimatedTotal": 15.26,
          |   "LPIPostedTotal": 1968.2,
          |   "LPIEstimatedTotal": 7
          | },
          | "lateSubmissionPenalty": {
          |   "summary": {
          |     "activePenaltyPoints": 2,
          |     "inactivePenaltyPoints": 0,
          |     "regimeThreshold": 5,
          |     "penaltyChargeAmount": 200.00
          |   },
          |   "details": []
          | },
          | "latePaymentPenalty": {
          |     "details": [
          |       {
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
          |          "communicationsDate": "2022-10-30",
          |          "penaltyChargeDueDate": "2022-10-30",
          |          "principalChargeReference": "1234567890",
          |          "principalChargeBillingFrom": "2022-10-30",
          |          "principalChargeBillingTo": "2022-10-30",
          |          "principalChargeDueDate": "2022-10-30"
          |       },
          |       {
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
          |          "communicationsDate": "2022-10-30",
          |          "penaltyChargeDueDate": "2022-10-30",
          |          "principalChargeReference": "1234567890",
          |          "principalChargeBillingFrom": "2022-10-30",
          |          "principalChargeBillingTo": "2022-10-30",
          |          "principalChargeDueDate": "2022-10-30"
          |       },
          |       {
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
          |          "principalChargeReference": "1234567890",
          |          "principalChargeBillingFrom": "2022-10-30",
          |          "principalChargeBillingTo": "2022-10-30",
          |          "principalChargeDueDate": "2022-10-30"
          |       },
          |       {
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
          |          "principalChargeReference": "1234567890",
          |          "principalChargeBillingFrom": "2022-10-30",
          |          "principalChargeBillingTo": "2022-10-30",
          |          "principalChargeDueDate": "2022-10-30"
          |       }
          |   ]
          | }
          |}
          |""".stripMargin)

      s"return OK (${Status.OK})" when {
        "the get penalty details call succeeds" in {
          enableFeatureSwitch(UseAPI1812Model)
          mockStubResponseForGetPenaltyDetailsv3(Status.OK, "123456789", body = Some(getPenaltyDetailsJson.toString()))
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
          result.status shouldBe OK
          Json.parse(result.body) shouldBe Json.parse(
            """
              |{
              |  "noOfPoints": 2,
              |  "noOfEstimatedPenalties": 2,
              |  "noOfCrystalisedPenalties": 2,
              |  "estimatedPenaltyAmount": 123.45,
              |  "crystalisedPenaltyAmountDue": 288,
              |  "hasAnyPenaltyData": true
              |}
              |""".stripMargin
          )
        }
      }

      s"return BAD_REQUEST (${Status.BAD_REQUEST})" when {
        "the user supplies an invalid VRN" in {
          enableFeatureSwitch(UseAPI1812Model)
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789123456789").get)
          result.status shouldBe BAD_REQUEST
        }
      }

      s"return ISE (${Status.INTERNAL_SERVER_ERROR})" when {
        "the get penalty details call fails" in {
          enableFeatureSwitch(UseAPI1812Model)
          mockStubResponseForGetPenaltyDetailsv3(Status.INTERNAL_SERVER_ERROR, "123456789", body = Some(""))
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }

      s"return NOT_FOUND (${Status.NOT_FOUND})" when {
        "the get penalty details call returns 404" in {
          enableFeatureSwitch(UseAPI1812Model)
          mockStubResponseForGetPenaltyDetailsv3(Status.NOT_FOUND, "123456789", body = Some(""))
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
          result.status shouldBe NOT_FOUND
        }

        "the get penalty details call returns 204" in {
          enableFeatureSwitch(UseAPI1812Model)
          mockStubResponseForGetPenaltyDetailsv3(Status.NO_CONTENT, "123456789", body = Some(""))
          val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
          result.status shouldBe NOT_FOUND
        }
      }
    }
  }

  "getFinancialDetails" should {
    s"return OK (${Status.OK})" when {
      "the get Financial Details call succeeds" in {
        val sampleAPI1911Response = Json.parse(
          """
            |{
            |            "taxPayerDetails": {
            |              "idType": "VRN",
            |              "idNumber": 123456789,
            |              "regimeType": "VATC"
            |            },
            |            "balanceDetails": {
            |              "balanceDueWithin30Days": -99999999999.99,
            |              "nextPaymentDateForChargesDueIn30Days": "1920-02-29",
            |              "balanceNotDueIn30Days": -99999999999.99,
            |              "nextPaymentDateBalanceNotDue": "1920-02-29",
            |              "overDueAmount": -99999999999.99,
            |              "earliestPaymentDateOverDue": "1920-02-29",
            |              "totalBalance": -99999999999.99,
            |              "amountCodedOut": 3456.67
            |            },
            |            "codingDetails": [
            |              {
            |                "taxYearReturn": "2017",
            |                "totalReturnAmount": 2234.56,
            |                "amountNotCoded": 234.56,
            |                "amountNotCodedDueDate": "2021-07-29",
            |                "amountCodedOut": 2634.56,
            |                "taxYearCoding": "2018",
            |                "documentText": "document coding details"
            |              }
            |            ],
            |            "documentDetails": [
            |              {
            |                "taxYear": "2017",
            |                "documentId": "1455",
            |                "documentDate": "2018-03-29",
            |                "documentText": "ITSA- Bal Charge",
            |                "documentDueDate": "2020-04-15",
            |                "documentDescription": "document Description",
            |                "totalAmount": 45552768.79,
            |                "documentOutstandingAmount": 297873.46,
            |                "lastClearingDate": "2018-04-15",
            |                "lastClearingReason": "last Clearing Reason",
            |                "lastClearedAmount": 589958.83,
            |                "statisticalFlag": false,
            |                "paymentLot": 81203010024,
            |                "paymentLotItem": "000001",
            |                "accruingInterestAmount": 1000.9,
            |                "interestRate": 1000.9,
            |                "interestFromDate": "2021-01-11",
            |                "interestEndDate": "2021-04-11",
            |                "latePaymentInterestID": "1234567890123456",
            |                "latePaymentInterestAmount": 1000.67,
            |                "lpiWithDunningBlock": 1000.23,
            |                "interestOutstandingAmount": 1000.34
            |              }
            |            ],
            |            "financialDetails": [
            |              {
            |                "taxYear": "2017",
            |                "documentId": 1.2345678901234568e+28,
            |                "chargeType": "PAYE",
            |                "mainType": "2100",
            |                "periodKey": "13RL",
            |                "periodKeyDescription": "abcde",
            |                "taxPeriodFrom": "2018-08-13",
            |                "taxPeriodTo": "2018-08-14",
            |                "businessPartner": "6622334455",
            |                "contractAccountCategory": "02",
            |                "contractAccount": "X",
            |                "contractObjectType": "ABCD",
            |                "contractObject": "00000003000000002757",
            |                "sapDocumentNumber": "1040000872",
            |                "sapDocumentNumberItem": "XM00",
            |                "chargeReference": "XM002610011594",
            |                "mainTransaction": "1234",
            |                "subTransaction": "5678",
            |                "originalAmount": 10000,
            |                "outstandingAmount": 10000,
            |                "clearedAmount": 10000,
            |                "accruedInterest": 10000,
            |                "items": [
            |                  {
            |                    "subItem": "001",
            |                    "dueDate": "2018-08-13",
            |                    "amount": 10000,
            |                    "clearingDate": "2018-08-13",
            |                    "clearingReason": "01",
            |                    "outgoingPaymentMethod": "outgoing Payment",
            |                    "paymentLock": "paymentLock",
            |                    "clearingLock": "clearingLock",
            |                    "interestLock": "interestLock",
            |                    "dunningLock": "dunningLock",
            |                    "returnFlag": true,
            |                    "paymentReference": "Ab12453535",
            |                    "paymentAmount": 10000,
            |                    "paymentMethod": "Payment",
            |                    "paymentLot": 81203010024,
            |                    "paymentLotItem": "000001",
            |                    "clearingSAPDocument": "3350000253",
            |                    "codingInitiationDate": "2021-01-11",
            |                    "statisticalDocument": "S",
            |                    "DDCollectionInProgress": true,
            |                    "returnReason": "ABCA"
            |                  }
            |                ]
            |              }
            |            ]
            |          }""".stripMargin)

        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetailsv3(Status.OK, s"VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true", Some(sampleAPI1911Response.toString))
        val result = await(buildClientForRequestToApp(uri = s"/penalty/financial-data/VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true").get)
        result.status shouldBe OK
        result.json shouldBe sampleAPI1911Response
      }
    }

    "return the status from EIS" when {
      "404 response received " in {

        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetailsv3(Status.NOT_FOUND, s"VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true", Some(""))

        val result = await(buildClientForRequestToApp(uri = s"/penalty/financial-data/VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true").get)
        result.status shouldBe NOT_FOUND
      }

      "Non 200 response received " in {
        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetailsv3(Status.BAD_REQUEST, s"VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true", Some(""))
        val result = await(buildClientForRequestToApp(uri = s"/penalty/financial-data/VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true").get)
        result.status shouldBe BAD_REQUEST
      }
    }
  }

  "getPenaltyDetails" should {
    s"return OK (${Status.OK})" when {
      "the get Penalty Details call succeeds" in {
        val sampleAPI1812Response = Json.parse(
          """
            |{
            | "totalisations": {
            |   "LSPTotalValue": 200,
            |   "penalisedPrincipalTotal": 2000,
            |   "LPPPostedTotal": 165.25,
            |   "LPPEstimatedTotal": 15.26,
            |   "LPIPostedTotal": 1968.2,
            |   "LPIEstimatedTotal": 7
            | },
            | "lateSubmissionPenalty": {
            |   "summary": {
            |     "activePenaltyPoints": 10,
            |     "inactivePenaltyPoints": 12,
            |     "regimeThreshold": 10,
            |     "penaltyChargeAmount": 684.25
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
            |       "FAPIndicator": "X",
            |       "lateSubmissions": [
            |         {
            |           "taxPeriodStartDate": "2022-01-01",
            |           "taxPeriodEndDate": "2022-12-31",
            |           "taxPeriodDueDate": "2023-02-07",
            |           "returnReceiptDate": "2023-02-01",
            |           "taxReturnStatus": "Fulfilled"
            |         }
            |       ],
            |       "expiryReason": "FAP",
            |       "appealInformation": [
            |         {
            |           "appealStatus": "99",
            |           "appealLevel": "01"
            |         }
            |       ],
            |       "chargeDueDate": "2022-10-30",
            |       "chargeOutstandingAmount": 200,
            |       "chargeAmount": 200
            |   }]
            | },
            | "latePaymentPenalty": {
            |     "details": [{
            |       "penaltyCategory": "LPP1",
            |       "penaltyChargeReference": "1234567890",
            |       "principalChargeReference":"1234567890",
            |       "penaltyChargeCreationDate":"2022-10-30",
            |       "penaltyStatus": "A",
            |       "appealInformation":
            |       [{
            |         "appealStatus": "99",
            |         "appealLevel": "01"
            |       }],
            |       "principalChargeBillingFrom": "2022-10-30",
            |       "principalChargeBillingTo": "2022-10-30",
            |       "principalChargeDueDate": "2022-10-30",
            |       "communicationsDate": "2022-10-30",
            |       "penaltyAmountOutstanding": 99.99,
            |       "penaltyAmountPaid": 1001.45,
            |       "LPP1LRDays": "15",
            |       "LPP1HRDays": "31",
            |       "LPP2Days": "31",
            |       "LPP1HRCalculationAmount": 99.99,
            |       "LPP1LRCalculationAmount": 99.99,
            |       "LPP2Percentage": 4.00,
            |       "LPP1LRPercentage": 2.00,
            |       "LPP1HRPercentage": 2.00,
            |       "penaltyChargeDueDate": "2022-10-30"
            |   }]
            | }
            |}
            |""".stripMargin)
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetailsv3(Status.OK, s"123456789?dateLimit=09", Some(sampleAPI1812Response.toString))
        val result = await(buildClientForRequestToApp(uri = s"/penalty-details/VAT/VRN/123456789?dateLimit=09").get)
        result.status shouldBe OK
        result.json shouldBe sampleAPI1812Response
      }
    }

    "return the status from EIS" when {
      "404 response received " in {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetailsv3(Status.NOT_FOUND, s"123456789?dateLimit=09", Some(""))
        val result = await(buildClientForRequestToApp(uri = s"/penalty-details/VAT/VRN/123456789?dateLimit=09").get)
        result.status shouldBe NOT_FOUND
      }

      "Non 200 response received " in {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetailsv3(Status.BAD_REQUEST, s"123456789?dateLimit=09", Some(""))
        val result = await(buildClientForRequestToApp(uri = s"/penalty-details/VAT/VRN/123456789?dateLimit=09").get)
        result.status shouldBe BAD_REQUEST
      }
    }
  }
}
