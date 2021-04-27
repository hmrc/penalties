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

import config.AppConfig
import featureSwitches.{CallETMP, FeatureSwitch, FeatureSwitching}
import play.api.http.Status
import play.api.test.Helpers._
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class ETMPControllerISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val controller = injector.instanceOf[ETMPController]

  "getPenaltiesData" should {
    s"call out to ETMP and return OK (${Status.OK}) when successful" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789")
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789").get())
      result.status shouldBe Status.OK
      result.body shouldBe etmpPayloadAsJson.toString()
    }

    s"call out to ETMP and return a Not Found (${Status.NOT_FOUND}) when NoContent is returned from the connector" in {
      mockResponseForStubETMPPayload(Status.NO_CONTENT, "123456789", body = Some(""))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789").get())
      result.status shouldBe Status.NOT_FOUND
    }

    s"call out to ETMP and return a ISE (${Status.INTERNAL_SERVER_ERROR}) when an issue has occurred i.e. invalid json response" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some("{}"))
      val result = await(buildClientForRequestToApp(uri = "/etmp/penalties/123456789").get())
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
      result.body shouldBe "Something went wrong."
    }
  }
}
