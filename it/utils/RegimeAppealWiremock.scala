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
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}
import models.{AgnosticEnrolmentKey}

trait RegimeAppealWiremock {

  val appealResponseModel: JsValue = Json.parse(
    """
      |{
      | "caseID": "PR-1234567889"
      |}
      |""".stripMargin
  )

  val errorResponse: JsValue = Json.parse(
    """
      |{
      | "error": "this is an error"
      |}
      |""".stripMargin
  )

  def mockResponseForAppealSubmissionPEGA(status: Int, penaltyNumber: String): StubMapping = {
    stubFor(post(urlEqualTo(s"/penalty/first-stage-appeal/$penaltyNumber"))
      .willReturn(
        aResponse()
          .withBody(appealResponseModel.toString())
          .withStatus(status)
      ))
  }

  def mockResponseForAppealSubmissionStub(status: Int, agnosticEnrolmenKey: AgnosticEnrolmentKey, penaltyNumber: String, isLPP: Boolean = false): StubMapping = {
    val regime = agnosticEnrolmenKey.regime.value;
    val idType = agnosticEnrolmenKey.idType.value;
    val idValue = agnosticEnrolmenKey.id.value;
    stubFor(post(urlEqualTo(s"/penalties-stub/appeals/submit?regime=$regime&idType=$idType&id=$idValue&isLPP=$isLPP&penaltyNumber=$penaltyNumber"))
      .willReturn(
        aResponse()
          .withBody(if(status == 200) appealResponseModel.toString() else errorResponse.toString())
          .withStatus(status)
      ))
  }

  def mockResponseForAppealSubmissionStubFault(agnosticEnrolmenKey: AgnosticEnrolmentKey, penaltyNumber: String, isLPP: Boolean = false): StubMapping = {
    val regime = agnosticEnrolmenKey.regime.value;
    val idType = agnosticEnrolmenKey.idType.value;
    val idValue = agnosticEnrolmenKey.id.value;
    stubFor(post(urlEqualTo(s"/penalties-stub/appeals/submit?regime=$regime&idType=$idType&id=$idValue&isLPP=$isLPP&penaltyNumber=$penaltyNumber"))
      .willReturn(
        aResponse()
          .withFault(Fault.CONNECTION_RESET_BY_PEER)
      ))
  }
}
