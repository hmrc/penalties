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

package models

import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

package object getFinancialDetails {

  val enrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("VATC"), IdType("VRN"), Id("123456789"))

  val jsonHipResponseWithoutSuccessResponse: JsValue = Json.parse("""
                                          |{
                                          | "processingDate": "2023-11-28T10:15:10Z",
                                          |   "financialData":{
                                          |     "documentDetails":[
                                          |       {
                                          |         "chargeReferenceNumber":"1234567890",
                                          |         "documentOutstandingAmount":123.45,
                                          |         "lineItemDetails":[
                                          |           {
                                          |             "mainTransaction":"4703"
                                          |           }
                                          |          ],
                                          |          "documentTotalAmount": 100.0,
                                          |          "issueDate": "2023-01-01"
                                          |       }
                                          |     ],
                                          |     "totalisation": {
                                          |     "regimeTotalisation": {
                                          |       "totalAccountOverdue": 1000.0,
                                          |       "totalAccountNotYetDue": 250.0,
                                          |       "totalAccountCredit": 40.0,
                                          |       "totalAccountBalance": 1210
                                          |     },
                                          |     "targetedSearch_SelectionCriteriaTotalisation": {
                                          |       "totalOverdue": 100.0,
                                          |       "totalNotYetDue": 0.0,
                                          |       "totalBalance": 100.0,
                                          |       "totalCredit": 10.0,
                                          |       "totalCleared": 50
                                          |     },
                                          |     "additionalReceivableTotalisations": {
                                          |       "totalAccountPostedInterest": 123.45,
                                          |       "totalAccountAccruingInterest": 23.45
                                          |     }
                                          |   }
                                          | }
                                          |}
                                          |""".stripMargin)
  val jsonHipResponse: JsValue = Json.parse(s"""
                                               |{
                                               | "success": $jsonHipResponseWithoutSuccessResponse
                                               |}
                                               |""".stripMargin)

  val jsonResponse: JsValue = Json.parse("""
                                                |{
                                                | "processingDate": "2023-11-28T10:15:10Z",
                                                | "financialData":{
                                                |   "documentDetails":[
                                                |      {
                                                |         "chargeReferenceNumber":"1234567890",
                                                |         "documentOutstandingAmount":123.45,
                                                |         "lineItemDetails":[
                                                |            {
                                                |               "mainTransaction":"4703"
                                                |            }
                                                |         ],
                                                |         "documentTotalAmount": 100.0,
                                                |         "issueDate": "2023-01-01"
                                                |      }
                                                |   ],
                                                |   "totalisation": {
                                                |    "regimeTotalisations": {
                                                |      "totalAccountOverdue": 1000.0
                                                |    },
                                                |    "interestTotalisations": {
                                                |      "totalAccountPostedInterest": 123.45,
                                                |      "totalAccountAccruingInterest": 23.45
                                                |    }
                                                |  }
                                                | }
                                                |}
                                                |""".stripMargin)

  val financialDetails: FinancialDetails = FinancialDetails(
    documentDetails = Some(
      Seq(
        DocumentDetails(
          chargeReferenceNumber = Some("1234567890"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(
            Seq(
              LineItemDetails(Some(VATReturnFirstLppMainTransaction))
            )
          ),
          documentTotalAmount = Some(100.0),
          issueDate = Some(LocalDate.of(2023, 1, 1))
        )
      )
    ),
    totalisation = Some(
      FinancialDetailsTotalisation(
        regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
      ))
  )

  val financialDetailsHip: FinancialDetailsHIP = FinancialDetailsHIP(processingDate = "2023-11-28T10:15:10Z", financialDetails)

  val financialDetailsRequestMaxModel: FinancialDetailsRequestModel = FinancialDetailsRequestModel(
    searchType = Some("CHGREF"),
    searchItem = Some("XC00178236592"),
    dateType = Some("BILLING"),
    dateFrom = Some("2020-10-03"),
    dateTo = Some("2021-07-12"),
    includeClearedItems = Some(true),
    includeStatisticalItems = Some(true),
    includePaymentOnAccount = Some(true),
    addRegimeTotalisation = Some(true),
    addLockInformation = Some(true),
    addPenaltyDetails = Some(true),
    addPostedInterestDetails = Some(true),
    addAccruingInterestDetails = Some(true)
  )

  val financialDetailsRequestMaxJson: JsValue = Json.parse("""
                                           |{
                                           | "searchType": "CHGREF",
                                           | "searchItem": "XC00178236592",
                                           | "dateType": "BILLING",
                                           | "dateFrom": "2020-10-03",
                                           | "dateTo": "2021-07-12",
                                           | "includeClearedItems": true,
                                           | "includeStatisticalItems": true,
                                           | "includePaymentOnAccount": true,
                                           | "addRegimeTotalisation": true,
                                           | "addLockInformation": true,
                                           | "addPenaltyDetails": true,
                                           | "addPostedInterestDetails": true,
                                           | "addAccruingInterestDetails": true
                                           |}
                                           |""".stripMargin)
}
