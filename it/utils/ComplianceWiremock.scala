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

package utils

import java.time.LocalDateTime

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}

trait ComplianceWiremock {
  val pastReturnPayloadAsJson: JsValue = Json.parse(
    """
      |{
      |   "regime":"VAT",
      |   "VRN":"10045678976543",
      |   "noOfMissingReturns":"2",
      |   "missingReturns":[
      |      {
      |         "startDate":"2020-10-01T00:00:00.000Z",
      |         "endDate":"2020-12-31T23:59:59.000Z"
      |      }
      |   ]
      |}
      |""".stripMargin
  )
  val complianceSummaryPayloadAsJson: JsValue = Json.parse(
    """
      |{
      |   "regime":"VAT",
      |   "VRN":"10045678976543",
      |   "expiryDateOfAllPenaltyPoints":"2023-03-31T00:00:00.000Z",
      |   "noOfSubmissionsReqForCompliance":"4",
      |   "returns":[
      |      {
      |         "startDate":"2020-10-01T00:00:00.000Z",
      |         "endDate":"2020-12-31T23:59:59.000Z",
      |         "dueDate":"2021-05-07T23:59:59.000Z",
      |         "status":"Submitted"
      |      },
      |      {
      |         "startDate":"2021-04-01T00:00:00.000Z",
      |         "endDate":"2021-06-30T23:59:59.000Z",
      |         "dueDate":"2021-08-07T23:59:59.000Z"
      |      }
      |   ]
      |}
      |""".stripMargin
  )

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
      |					"inboundCorrespondenceFromDate": "1920-02-29",
      |					"inboundCorrespondenceToDate": "1920-02-29",
      |					"inboundCorrespondenceDateReceived": "1920-02-29",
      |					"inboundCorrespondenceDueDate": "1920-02-29",
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
      |					"inboundCorrespondenceFromDate": "1920-02-29",
      |					"inboundCorrespondenceToDate": "1920-02-29",
      |					"inboundCorrespondenceDateReceived": "1920-02-29",
      |					"inboundCorrespondenceDueDate": "1920-02-29",
      |					"periodKey": "#001"
      |				}
      |			]
      |}
      |""".stripMargin)

  def mockResponseForStubPastReturnPayload(status: Int, identifier: String, startDate: LocalDateTime, endDate: LocalDateTime,
                                           regime: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlPathEqualTo(s"/penalties-stub/compliance/previous-data/$regime/$identifier"))
      .willReturn(
        aResponse()
          .withBody(body.fold(pastReturnPayloadAsJson.toString())(identity))
          .withStatus(status)))
  }

  def mockResponseForPastReturnPayload(status: Int, identifier: String, startDate: LocalDateTime, endDate: LocalDateTime,
                                       body: Option[String] = None): StubMapping = {
    stubFor(get(urlPathEqualTo(s"/$identifier"))
    .willReturn(
      aResponse()
        .withBody(body.fold(pastReturnPayloadAsJson.toString())(identity))
        .withStatus(status)))
  }

  def mockResponseForStubComplianceSummaryPayload(status: Int, identifier: String, regime: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/compliance/summary-data/$regime/$identifier"))
    .willReturn(
      aResponse()
        .withBody(body.fold(complianceSummaryPayloadAsJson.toString())(identity))
        .withStatus(status)))
  }

  def mockResponseForComplianceSummaryPayload(status: Int, identifier: String, regime: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/$identifier"))
    .willReturn(
      aResponse()
        .withBody(body.fold(complianceSummaryPayloadAsJson.toString())(identity))
        .withStatus(status)
    ))
  }

  def mockResponseForComplianceDataFromDES(status: Int, vrn: String, fromDate: String, toDate: String,
                                           hasBody: Boolean = false, invalidBody: Boolean = false): StubMapping = {
    stubFor(get(urlEqualTo(s"/enterprise/obligation-data/vrn/$vrn/VATC?from=$fromDate&to=$toDate"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(if(invalidBody) "{}" else if(hasBody) complianceSeqPayloadAsJson.toString() else "")
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
