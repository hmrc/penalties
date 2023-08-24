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
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class GetFinancialDataSpec extends SpecBase {
  val modelAsJson: JsValue = Json.parse(
    """
      |{
      | "getFinancialData": {
      |   "financialDetails":{
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

  val parsedModelAsJson: JsValue = Json.parse(
    """
      |{
      | "financialDetails":{
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

  val model: GetFinancialData = GetFinancialData(
    FinancialDetails(
      documentDetails = Some(
        Seq(
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(
              Seq(
                LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP))
              )
            ),
            documentTotalAmount = Some(100.0),
            issueDate = Some(LocalDate.of(2023, 1, 1))
          )
        )
      ),
      totalisation = Some(FinancialDetailsTotalisation(
        regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
      ))
    )
  )

  "be readable from JSON" in {
    val result = Json.fromJson(modelAsJson)(GetFinancialData.reads)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result = Json.toJson(model)(GetFinancialData.writes)
    result shouldBe parsedModelAsJson
  }
}