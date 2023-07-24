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

package models.auditing

import base.{LogCapturing, SpecBase}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}

class ThirdParty1812APIRetrievalAuditModelSpec extends SpecBase with LogCapturing {
  val sampleJsonResponse: JsValue = Json.toJson(
    "foo" -> "bar"
  )
  val sampleJsonResponseAsString: String = Json.stringify(sampleJsonResponse)

  val modelWithJsonifiedBody = ThirdParty1812APIRetrievalAuditModel("123456789", Status.OK, sampleJsonResponse)

  "UserHasPenaltyAuditModel" should {
    "have the correct audit type" in {
      modelWithJsonifiedBody.auditType shouldBe "Penalties3rdPartyPenaltyDetailsDataRetrieval"
    }

    "have the correct transaction name" in {
      modelWithJsonifiedBody.transactionName shouldBe "penalty-penalty-data-retrieval"
    }

    "show the correct audit details" when {
      "the correct detail information is present (when the audit has a JSON body)" in {
        (modelWithJsonifiedBody.detail \ "vrn").validate[String].get shouldBe "123456789"
        (modelWithJsonifiedBody.detail \ "responseCodeSentAPIService").validate[Int].get shouldBe Status.OK
        (modelWithJsonifiedBody.detail \ "etmp-response").validate[JsValue].get shouldBe sampleJsonResponse
      }
    }
  }
}