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

package utils

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}

trait ComplianceWiremock {
  val complianceSeqPayloadAsJson: JsValue = Json.parse(
    """
      |{
      |   "obligations": [
      |     {
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
      |					"inboundCorrespondenceFromDate": "1920-03-29",
      |					"inboundCorrespondenceToDate": "1920-03-29",
      |					"inboundCorrespondenceDateReceived": "1920-03-29",
      |					"inboundCorrespondenceDueDate": "1920-03-29",
      |					"periodKey": "#001"
      |				}
      |			]
      |		}
      |  ]
      |}
      |""".stripMargin)

  val compliancePayloadAsJson: JsValue = Json.parse(
    """
      |{
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
      |					"inboundCorrespondenceFromDate": "1920-03-29",
      |					"inboundCorrespondenceToDate": "1920-03-29",
      |					"inboundCorrespondenceDateReceived": "1920-03-29",
      |					"inboundCorrespondenceDueDate": "1920-03-29",
      |					"periodKey": "#001"
      |				}
      |			]
      |}
      |""".stripMargin)


  def mockResponseForComplianceDataFromDES(status: Int, vrn: String, fromDate: String, toDate: String,
                                           hasBody: Boolean = false, invalidBody: Boolean = false, optBody: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/enterprise/obligation-data/vrn/$vrn/VATC?from=$fromDate&to=$toDate"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(if(invalidBody) "{}" else if(optBody.isDefined) optBody.get else if(hasBody) complianceSeqPayloadAsJson.toString() else "")
      ))
  }

  def mockResponseForComplianceDataFromStub(status: Int, vrn: String, fromDate: String, toDate: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/enterprise/obligation-data/vrn/$vrn/VATC?from=$fromDate&to=$toDate"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(complianceSeqPayloadAsJson.toString())
      ))
  }
}
