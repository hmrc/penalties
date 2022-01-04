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

package utils

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait AppealWiremock {

  def mockResponseForAppealSubmissionPEGA(status: Int, penaltyId: String): StubMapping = {
    stubFor(post(urlEqualTo(s"/penalty/first-stage-appeal/$penaltyId"))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

  def mockResponseForAppealSubmissionStub(status: Int, enrolmentKey: String, penaltyId: String, isLPP: Boolean = false): StubMapping = {
    stubFor(post(urlEqualTo(s"/penalties-stub/appeals/submit?enrolmentKey=$enrolmentKey&isLPP=$isLPP&penaltyId=$penaltyId"))
      .willReturn(
        aResponse()
          .withStatus(status)
      ))
  }

  def mockResponseForAppealSubmissionStubFault(enrolmentKey: String, penaltyId: String, isLPP: Boolean = false): StubMapping = {
    stubFor(post(urlEqualTo(s"/penalties-stub/appeals/submit?enrolmentKey=$enrolmentKey&isLPP=$isLPP&penaltyId=$penaltyId"))
      .willReturn(
        aResponse()
          .withFault(Fault.CONNECTION_RESET_BY_PEER)
      ))
  }
}
