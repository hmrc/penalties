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

package models.v3.getPenaltyDetails

import base.SpecBase
import play.api.libs.json.{JsResult, JsValue, Json}

class TotalisationsSpec extends SpecBase {
  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      |   "LSPTotalValue": 200,
      |   "penalisedPrincipalTotal": 2000,
      |   "LPPPostedTotal": 165.25,
      |   "LPPEstimatedTotal": 15.26,
      |   "LPIPostedTotal": 1968.2,
      |   "LPIEstimatedTotal": 7
      |}
      |""".stripMargin)

  val model: Totalisations = Totalisations(
    LSPTotalValue = Some(200),
    penalisedPrincipalTotal = Some(2000),
    LPPPostedTotal = Some(165.25),
    LPPEstimatedTotal = Some(15.26),
    LPIPostedTotal = Some(1968.2),
    LPIEstimatedTotal = Some(7)
  )

  "be readable to JSON" in {
    val result: JsResult[Totalisations] = Json.fromJson(jsonRepresentingModel)(Totalisations.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result: JsValue = Json.toJson(model)(Totalisations.format)
    result shouldBe jsonRepresentingModel
  }

}