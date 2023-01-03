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

package controllers

import config.featureSwitches.{CallDES, FeatureSwitching}
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.{ComplianceWiremock, IntegrationSpecCommonBase}

class ComplianceControllerISpec extends IntegrationSpecCommonBase with ComplianceWiremock with FeatureSwitching {
  val enrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"

  class Setup {
    enableFeatureSwitch(CallDES)
  }

  "getComplianceData" should {
    "return 200 with the associated model when the call succeeds" in new Setup {
      mockResponseForComplianceDataFromDES(OK, "123456789", "2020-01-31", "2020-12-31", hasBody = true)
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe OK
      Json.parse(result.body) shouldBe compliancePayloadAsJson
    }

    "return 400 when the downstream service returns 400" in new Setup {
      mockResponseForComplianceDataFromDES(BAD_REQUEST, "123456789", "2020-01-31", "2020-12-31")
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe BAD_REQUEST
    }

    "return 404 when the downstream service has no data for the VRN" in new Setup {
      mockResponseForComplianceDataFromDES(NOT_FOUND, "123456789", "2020-01-31", "2020-12-31")
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe NOT_FOUND
    }

    "return 500 when the downstream service has returns 500" in new Setup {
      mockResponseForComplianceDataFromDES(INTERNAL_SERVER_ERROR, "123456789", "2020-01-31", "2020-12-31")
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return 503 when the downstream service has returns 503" in new Setup {
      mockResponseForComplianceDataFromDES(SERVICE_UNAVAILABLE, "123456789", "2020-01-31", "2020-12-31")
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe SERVICE_UNAVAILABLE
    }
  }
}
