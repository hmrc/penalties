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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}

trait ComplianceWiremock {
  val pastReturnPayloadAsJson: JsValue = Json.parse(
    "{}"
  )
  val complianceSummaryPayloadAsJson: JsValue = Json.parse(
    "{}"
  )

  def mockResponseForStubPastReturnPayload(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/compliance/previous-data/mtd-vat/$enrolmentKey"))
      .willReturn(
        aResponse()
          .withBody(body.fold(pastReturnPayloadAsJson.toString())(identity))
          .withStatus(status)))
  }

  def mockResponseForPastReturnPayload(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/$enrolmentKey"))
    .willReturn(
      aResponse()
        .withBody(body.fold(pastReturnPayloadAsJson.toString())(identity))
        .withStatus(status)))
  }

  def mockResponseForStubComplianceSummaryPayload(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/penalties-stub/compliance/summary-data/mtd-vat/$enrolmentKey"))
    .willReturn(
      aResponse()
        .withBody(body.fold(complianceSummaryPayloadAsJson.toString())(identity))
        .withStatus(status)))
  }

  def mockResponseForComplianceSummaryPayload(status: Int, enrolmentKey: String, body: Option[String] = None): StubMapping = {
    stubFor(get(urlEqualTo(s"/$enrolmentKey"))
    .willReturn(
      aResponse()
        .withBody(body.fold(complianceSummaryPayloadAsJson.toString())(identity))
        .withStatus(status)
    ))
  }
}
