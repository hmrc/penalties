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

import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class FinancialDetailsConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock {

  class Setup {
    val connector: FinancialDetailsConnector = injector.instanceOf[FinancialDetailsConnector]
  }

  "getFinancialDetails" should {
    "return a successful response when called" in new Setup {
      mockResponseForNewETMPPayloadFinancialDetails(Status.OK, "VRN/123456789/VATC")
      val result: HttpResponse = await(connector.getFinancialDetails("/VRN/123456789/VATC"))
      result.status shouldBe Status.OK
    }
  }
}
