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

package controllers

import featureSwitches.CallDES
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{ComplianceWiremock, IntegrationSpecCommonBase}

import scala.concurrent.Future

class ComplianceControllerISpec extends IntegrationSpecCommonBase with ComplianceWiremock {
  val enrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"

  class Setup {
    val localApp: Application = new GuiceApplicationBuilder()
      .configure(configForApp + (CallDES.name -> true))
      .build()
    val controller: ComplianceController = localApp.injector.instanceOf[ComplianceController]
  }

  "getComplianceData" should {
    "return 200 with the associated model when the call succeeds" in new Setup {
      mockResponseForComplianceDataFromDES(OK, "123456789", "2020-01-31", "2020-12-31", hasBody = true)
      val result: Future[Result] = controller.getComplianceData(vrn = "123456789", fromDate = "2020-01-31", toDate = "2020-12-31")(FakeRequest())
      await(result).header.status shouldBe OK
      Json.parse(contentAsString(result)) shouldBe compliancePayloadAsJson
    }

    "return 400 when the downstream service returns 400" in new Setup {
      mockResponseForComplianceDataFromDES(BAD_REQUEST, "123456789", "2020-01-31", "2020-12-31")
      val result: Result = await(controller.getComplianceData(vrn = "123456789", fromDate = "2020-01-31", toDate = "2020-12-31")(FakeRequest()))
      result.header.status shouldBe BAD_REQUEST
    }

    "return 404 when the downstream service has no data for the VRN" in new Setup {
      mockResponseForComplianceDataFromDES(NOT_FOUND, "123456789", "2020-01-31", "2020-12-31")
      val result: Result = await(controller.getComplianceData(vrn = "123456789", fromDate = "2020-01-31", toDate = "2020-12-31")(FakeRequest()))
      result.header.status shouldBe NOT_FOUND
    }

    "return 500 when the downstream service has returns 500" in new Setup {
      mockResponseForComplianceDataFromDES(INTERNAL_SERVER_ERROR, "123456789", "2020-01-31", "2020-12-31")
      val result: Result = await(controller.getComplianceData(vrn = "123456789", fromDate = "2020-01-31", toDate = "2020-12-31")(FakeRequest()))
      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return 503 when the downstream service has returns 503" in new Setup {
      mockResponseForComplianceDataFromDES(SERVICE_UNAVAILABLE, "123456789", "2020-01-31", "2020-12-31")
      val result: Result = await(controller.getComplianceData(vrn = "123456789", fromDate = "2020-01-31", toDate = "2020-12-31")(FakeRequest()))
      result.header.status shouldBe SERVICE_UNAVAILABLE
    }
  }
}
