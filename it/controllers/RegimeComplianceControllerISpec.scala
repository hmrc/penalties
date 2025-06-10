/*
 * Copyright 2025 HM Revenue & Customs
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
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.{AuthMock, IntegrationSpecCommonBase, RegimeComplianceWiremock}

class RegimeComplianceControllerISpec extends IntegrationSpecCommonBase with RegimeComplianceWiremock with FeatureSwitching with TableDrivenPropertyChecks with AuthMock{
  val enrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"

  class Setup {
    enableFeatureSwitch(CallDES)
  }

    Table(
    ("Regime", "IdType", "Id"),
    (Regime("VATC"), IdType("VRN"), Id("123456789")),
    (Regime("ITSA"), IdType("NINO"), Id("AB123456C")),
  ).forEvery { (regime, idType, id) =>

    val enrolmentKey = AgnosticEnrolmentKey(regime, idType, id) 

    val apiPath = s"/${regime.value}/compliance/data/${idType.value}/${id.value}?"

    s"getComplianceData $regime" should {
      "return 200 with the associated model when the call succeeds" in new Setup {
        mockStubResponseForAuthorisedUser
        mockResponseForComplianceDataFromDES(OK, regime, idType, id, "2020-01-31", "2020-12-31", hasBody = true)
        val result = await(buildClientForRequestToApp(uri = s"${apiPath}fromDate=2020-01-31&toDate=2020-12-31").get())
        result.status shouldBe OK
        Json.parse(result.body) shouldBe compliancePayloadAsJson(idType, id)
      }

      "return 400 when the downstream service returns 400" in new Setup {
        mockStubResponseForAuthorisedUser
        mockResponseForComplianceDataFromDES(BAD_REQUEST, regime, idType, id, "2020-01-31", "2020-12-31")
        val result = await(buildClientForRequestToApp(uri = s"${apiPath}fromDate=2020-01-31&toDate=2020-12-31").get())
        result.status shouldBe BAD_REQUEST
      }

      "return 404 when the downstream service has no data for the VRN" in new Setup {
        mockStubResponseForAuthorisedUser
        mockResponseForComplianceDataFromDES(NOT_FOUND, regime, idType, id, "2020-01-31", "2020-12-31")
        val result = await(buildClientForRequestToApp(uri = s"${apiPath}fromDate=2020-01-31&toDate=2020-12-31").get())
        result.status shouldBe NOT_FOUND
      }

      "return 500 when the downstream service has returns 500" in new Setup {
        mockStubResponseForAuthorisedUser
        mockResponseForComplianceDataFromDES(INTERNAL_SERVER_ERROR, regime, idType, id, "2020-01-31", "2020-12-31")
        val result = await(buildClientForRequestToApp(uri = s"${apiPath}fromDate=2020-01-31&toDate=2020-12-31").get())
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "return 503 when the downstream service has returns 503" in new Setup {
        mockStubResponseForAuthorisedUser
        mockResponseForComplianceDataFromDES(SERVICE_UNAVAILABLE, regime, idType, id, "2020-01-31", "2020-12-31")
        val result = await(buildClientForRequestToApp(uri = s"${apiPath}fromDate=2020-01-31&toDate=2020-12-31").get())
        result.status shouldBe SERVICE_UNAVAILABLE
      }
    }
  }
}
