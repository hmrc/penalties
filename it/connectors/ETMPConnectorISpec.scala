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

import connectors.parsers.ETMPPayloadParser.{GetETMPPayloadFailureResponse, GetETMPPayloadMalformed, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import featureSwitches.{CallETMP, FeatureSwitching}
import play.api.http.Status
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import scala.concurrent.ExecutionContext

class ETMPConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val connector: ETMPConnector = injector.instanceOf[ETMPConnector]
  }


  "getPenaltiesDataForEnrolmentKey" should {
    "call ETMP when the feature switch is enabled and handle a successful response" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.OK, "123456789")
      val result = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetETMPPayloadSuccessResponse].etmpPayload shouldBe etmpPayloadModel
    }

    "call the stub when the feature switch is disabled and handle a successful response" in new Setup {
      disableFeatureSwitch(CallETMP)
      mockResponseForStubETMPPayload(Status.OK, "123456789")
      val result = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetETMPPayloadSuccessResponse].etmpPayload shouldBe etmpPayloadModel
    }

    s"return a $GetETMPPayloadMalformed when the JSON is malformed" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.OK, "123456789", body = Some("{}"))
      val result = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetETMPPayloadMalformed
    }

    s"return a $GetETMPPayloadNoContent when the response status is No Content (${Status.NO_CONTENT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.NO_CONTENT, "123456789", body = Some("{}"))
      val result = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetETMPPayloadNoContent
    }

    s"return a $GetETMPPayloadFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.INTERNAL_SERVER_ERROR, "123456789")
      val result = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetETMPPayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)
    }

    s"return a $GetETMPPayloadFailureResponse when the response status is unmatched i.e. Gateway Timeout (${Status.GATEWAY_TIMEOUT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForETMPPayload(Status.GATEWAY_TIMEOUT, "123456789")
      val result = await(connector.getPenaltiesDataForEnrolmentKey("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetETMPPayloadFailureResponse(Status.GATEWAY_TIMEOUT)
    }
  }
}
