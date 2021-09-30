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

package controllers

import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class APIControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val controller: APIController = injector.instanceOf[APIController]

  val apiDataJson: JsValue = Json.parse(
  """
     |{
     | "noOfPoints": 1
     |}
     |""".stripMargin)

  "getSummaryDataForVRN" should {
    s"return OK (${Status.OK})" when {
      "the ETMP call succeeds" in {
        mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789")
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
        result.status shouldBe OK
        //TODO: change data based on implementation
        result.body shouldBe apiDataJson.toString()
      }
    }

    s"return OK (${Status.NOT_FOUND})" when {
      "the ETMP call fails" in {
        mockResponseForStubETMPPayload(Status.INTERNAL_SERVER_ERROR, "HMRC-MTD-VAT~VRN~123456789", body = Some(""))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
        result.status shouldBe NOT_FOUND
      }

      "the ETMP call returns nothing" in {
        mockResponseForStubETMPPayload(Status.OK, "HMRC-MTD-VAT~VRN~123456789", body = Some("{}"))
        val result = await(buildClientForRequestToApp(uri = "/vat/penalties/summary/123456789").get)
        result.status shouldBe NOT_FOUND
      }
    }
  }
}
