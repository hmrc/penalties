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
import play.api.libs.json.{JsString, JsValue, Json}

class ThirdPartyAPI1811RetrievalAuditModelSpec extends SpecBase with LogCapturing {

  val sampledJsonResponse: JsValue = Json.obj(
    "foo" -> "bar",
    "bar" -> "foo"
  )

  val sampleJsonResponseString: String = Json.stringify(sampledJsonResponse)

  val modelWithstringifiedJson = ThirdPartyAPI1811RetrievalAuditModel("12345", Status.OK, sampleJsonResponseString)
  val modelWithStringBody = ThirdPartyAPI1811RetrievalAuditModel("12346", Status.INTERNAL_SERVER_ERROR, "An error occurred")

  "ThirdPartyAPI1811RetrievalAuditModel" should {
    "have the correct audit type" in {
      modelWithstringifiedJson.auditType shouldBe "Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval"
    }

    "have the correct transaction name" in {
      modelWithstringifiedJson.transactionName shouldBe "penalty-financial-penalty-data-retrieval"
    }

    "show the correct audit details" when {
      "the correct detail information is present (when the audit has a JSON body)" in {
        (modelWithstringifiedJson.detail \ "vrn").validate[String].get shouldBe "12345"
        (modelWithstringifiedJson.detail \ "responseCodeSentAPIService").validate[Int].get shouldBe Status.OK
        (modelWithstringifiedJson.detail \ "etmp-response").validate[JsValue].get shouldBe sampledJsonResponse
      }

      "the correct detail information is present (when the audit has a non-JSON body)" in {
        (modelWithStringBody.detail \ "vrn").validate[String].get shouldBe "12346"
        (modelWithStringBody.detail \ "responseCodeSentAPIService").validate[Int].get shouldBe Status.INTERNAL_SERVER_ERROR
        (modelWithStringBody.detail \ "etmp-response").validate[JsValue].get shouldBe JsString("An error occurred")
      }
    }

  }

}
