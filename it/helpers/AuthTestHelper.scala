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

package helpers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsValue
import play.api.libs.ws.WSRequest
import play.api.test.Helpers._
import utils.AuthWiremock

trait AuthTestHelper extends AuthWiremock {
  _: AnyWordSpec with Matchers =>
    def buildClientForRequestToApp(baseUrl: String = "/penalties", uri: String): WSRequest

    def testAuthorisationOnEndpoint(endpointUri: String, isPost: Boolean = false, postData: Option[JsValue] = None): Unit = {
      val request = buildClientForRequestToApp(uri = endpointUri)
      "the caller is unauthorised" should {
        "return UNAUTHORIZED (401)" when {
          "the user has provided no authentication" in {
            val result = await(if (isPost) request.withHttpHeaders().post(postData.get) else request.withHttpHeaders().get())
            result.status shouldBe UNAUTHORIZED
          }
        }

        "return FORBIDDEN (403)" when {
          "the user can't access this service" in {
            mockForbidden()
            val result = await(if (isPost) request.post(postData.get) else request.get())
            result.status shouldBe FORBIDDEN
          }
        }
      }
    }
}
