/*
 * Copyright 2021 HM Revenue & Customs
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

package models.compliance

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

//TODO: rename when we switch to new API
class CompliancePayloadObligationAPISpec extends AnyWordSpec with Matchers {
  val compliancePayloadAsJson: JsValue = Json.parse(
    """
      |		{
      |			"identification": {
      |				"referenceNumber": "123456789",
      |				"referenceType": "VRN"
      |			},
      |			"obligationDetails": [
      |				{
      |					"status": "O",
      |					"inboundCorrespondenceFromDate": "1920-02-29",
      |					"inboundCorrespondenceToDate": "1920-02-29",
      |					"inboundCorrespondenceDueDate": "1920-02-29",
      |					"periodKey": "#001"
      |				},
      |				{
      |					"status": "F",
      |					"inboundCorrespondenceFromDate": "1920-02-29",
      |					"inboundCorrespondenceToDate": "1920-02-29",
      |					"inboundCorrespondenceDateReceived": "1920-02-29",
      |					"inboundCorrespondenceDueDate": "1920-02-29",
      |					"periodKey": "#001"
      |				}
      |			]
      |		}
      |""".stripMargin)
  
  val compliancePayloadAsModel: CompliancePayloadObligationAPI = CompliancePayloadObligationAPI(
    identification = ObligationIdentification(
      incomeSourceType = None,
      referenceNumber = "123456789",
      referenceType = "VRN"
    ),
    obligationDetails = Seq(
      ObligationDetail(
        status = "O",
        inboundCorrespondenceFromDate = "1920-02-29",
        inboundCorrespondenceToDate = "1920-02-29",
        inboundCorrespondenceDateReceived = None,
        inboundCorrespondenceDueDate = "1920-02-29",
        periodKey = "#001"
      ),
      ObligationDetail(
        status = "F",
        inboundCorrespondenceFromDate = "1920-02-29",
        inboundCorrespondenceToDate = "1920-02-29",
        inboundCorrespondenceDateReceived = Some("1920-02-29"),
        inboundCorrespondenceDueDate = "1920-02-29",
        periodKey = "#001"
      )
    )
  )

  "CompliancePayloadObligationAPI" should {
    "parse the model from JSON" in {
      val result = Json.fromJson(compliancePayloadAsJson)(CompliancePayloadObligationAPI.format)
      result.isSuccess shouldBe true
      result.get shouldBe compliancePayloadAsModel
    }

    "parse the model to JSON" in {
      val result = Json.toJson(compliancePayloadAsModel)(CompliancePayloadObligationAPI.format)
      result shouldBe compliancePayloadAsJson
    }
  }
}
