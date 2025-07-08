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

package models.getFinancialDetails

import base.SpecBase
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import play.api.libs.json.{JsObject, JsValue, Json}

class FinancialDetailsRequestSpec extends SpecBase {

  val enrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("VATC"), IdType("VRN"), Id("123456789"))

  val modelAsJson: JsValue = Json.parse("""
      |{
      | "success": {
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
      |   }
      | }
      |}
      |""".stripMargin)
  val jsonMaxModel: JsValue = Json.parse("""
      |{
      | "searchType": "CHGREF",
      | "searchItem": "XC00178236592",
      | "dateType": "BILLING",
      | "dateFrom": "2020-10-03",
      | "dateTo": "2021-07-12",
      | "includeClearedItems": false,
      | "includeStatisticalItems": true,
      | "includePaymentOnAccount": true,
      | "addRegimeTotalisation": false,
      | "addLockInformation": true,
      | "addPenaltyDetails": true,
      | "addPostedInterestDetails": true,
      | "addAccruingInterestDetails": true
      |}
      |""".stripMargin)
  val maxModel: FinancialDetailsRequestModel = FinancialDetailsRequestModel(
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

  "be readable from JSON" in {
    val result = Json.fromJson(jsonMaxModel)(FinancialDetailsRequestModel.format)
    result.isSuccess shouldBe true
    result.get shouldBe maxModel
  }

  "be writable to JSON" in {
    val result = Json.toJson(maxModel)(FinancialDetailsRequestModel.format)
    result shouldBe jsonMaxModel
  }

  val jsonMinRequest: JsObject = Json.obj(
    "taxRegime" -> enrolmentKey.regime.value,
    "taxpayerInformation" -> Json.obj(
      "idType"   -> enrolmentKey.idType.value,
      "idNumber" -> enrolmentKey.id.value
    )
  )
  val targetedSearch: JsObject = Json.obj(
    "targetedSearch" -> Json.obj(
      "searchType"   -> "CHGREF",
      "searchItem" -> "XC00178236592"
    )
  )

  "toJsonRequest" should {
    "build the Json request body" which {
      "only contains the base body" when {
        "no other parameters are given" in {
          val result = FinancialDetailsRequestModel.emptyModel.toJsonRequest(enrolmentKey)

          result shouldBe jsonMinRequest
        }
        "some but not all required targetedSearch parameters are given" in {
          val resultWithoutSearchItem = FinancialDetailsRequestModel.emptyModel.copy(searchType = Some("CHGREF")).toJsonRequest(enrolmentKey)
          val resultWithoutSearchType = FinancialDetailsRequestModel.emptyModel.copy(searchItem = Some("XC00178236592")).toJsonRequest(enrolmentKey)

          resultWithoutSearchItem shouldBe jsonMinRequest
          resultWithoutSearchType shouldBe jsonMinRequest
        }
      }
      "contains the base body plus targetedSearch when the correct targetedSearch parameters are given" in {
        val result =
          FinancialDetailsRequestModel.emptyModel.copy(searchType = Some("CHGREF"), searchItem = Some("XC00178236592")).toJsonRequest(enrolmentKey)

        result shouldBe jsonMinRequest ++ targetedSearch
      }
    }
  }
}
