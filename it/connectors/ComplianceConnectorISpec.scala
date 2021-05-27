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

package connectors

import connectors.parsers.ComplianceParser.{CompliancePayloadResponse, GetCompliancePayloadFailureResponse, GetCompliancePayloadSuccessResponse}
import featureSwitches.{CallETMP, FeatureSwitching}
import play.api.http.Status
import play.api.test.Helpers._
import utils.{ComplianceWiremock, IntegrationSpecCommonBase}

import scala.concurrent.ExecutionContext

class ComplianceConnectorISpec extends IntegrationSpecCommonBase with ComplianceWiremock with FeatureSwitching {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val connector: ComplianceConnector = injector.instanceOf[ComplianceConnector]
  }

  "getPastReturnsForEnrolmentKey" should {
    "call ETMP when the feature switch is enabled and handle a successful response" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForPastReturnPayload(Status.OK, "123456789")
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse].jsValue shouldBe pastReturnPayloadAsJson
    }

    "call the stub when the feature switch is disabled and handle a successful response" in new Setup {
      disableFeatureSwitch(CallETMP)
      mockResponseForStubPastReturnPayload(Status.OK, "123456789")
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse].jsValue shouldBe pastReturnPayloadAsJson
    }

    //TODO: Implement GetCompliancePayloadMalformed test
    //TODO: Implement GetCompliancePayloadNoContent test

    s"return a $GetCompliancePayloadFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForPastReturnPayload(Status.INTERNAL_SERVER_ERROR, "123456789")
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)
    }

    s"return a $GetCompliancePayloadFailureResponse when the response status is unmatched i.e. Gateway Timeout (${Status.GATEWAY_TIMEOUT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForPastReturnPayload(Status.GATEWAY_TIMEOUT, "123456789")
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadFailureResponse(Status.GATEWAY_TIMEOUT)
    }
  }

  "getComplianceSummaryForEnrolmentKey" should {
    "call ETMP when the feature switch is enabled and handle a successful response" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForComplianceSummaryPayload(Status.OK, "123456789")
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse].jsValue shouldBe complianceSummaryPayloadAsJson
    }

    "call the stub when the feature switch is disabled and handle a successful response" in new Setup {
      disableFeatureSwitch(CallETMP)
      mockResponseForStubComplianceSummaryPayload(Status.OK, "123456789")
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse].jsValue shouldBe complianceSummaryPayloadAsJson
    }

    //TODO: Implement GetCompliancePayloadMalformed test
    //TODO: Implement GetCompliancePayloadNoContent test

    s"return a $GetCompliancePayloadFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForComplianceSummaryPayload(Status.INTERNAL_SERVER_ERROR, "123456789")
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)
    }

    s"return a $GetCompliancePayloadFailureResponse when the response status is unmatched i.e. Gateway Timeout (${Status.GATEWAY_TIMEOUT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForComplianceSummaryPayload(Status.GATEWAY_TIMEOUT, "123456789")
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadFailureResponse(Status.GATEWAY_TIMEOUT)
    }
  }
}
