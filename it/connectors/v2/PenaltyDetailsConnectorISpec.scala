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

package connectors.v2


import featureSwitches.{CallAPI1812ETMP, FeatureSwitching}
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class PenaltyDetailsConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {

  class Setup {
    val connector: PenaltyDetailsConnector = injector.instanceOf[PenaltyDetailsConnector]
  }

  "getPenaltyDetails" should {
    "return a successful response when called" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForNewETMPPayload(Status.OK, "VATC/VRN/123456789")
      val result: HttpResponse = await(connector.getPenaltyDetails("VATC/VRN/123456789"))
      result.status shouldBe Status.OK
    }
  }
}
