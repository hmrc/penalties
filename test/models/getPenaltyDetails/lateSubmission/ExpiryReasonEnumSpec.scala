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

package models.getPenaltyDetails.lateSubmission

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class ExpiryReasonEnumSpec extends SpecBase {

  "ExpiryReasonEnum" should {
    "be writeable to JSON for appeal - APP" in {
      val result = Json.toJson(ExpiryReasonEnum.Appeal)
      result shouldBe JsString("APP")
    }

    "be writeable to JSON for adjusted point - FAP" in {
      val result = Json.toJson(ExpiryReasonEnum.Adjustment)
      result shouldBe JsString("FAP")
    }

    "be writeable to JSON for obligation reversed - ICR" in {
      val result = Json.toJson(ExpiryReasonEnum.Reversal)
      result shouldBe JsString("ICR")
    }

    "be writeable to JSON for manual removal - MAN" in {
      val result = Json.toJson(ExpiryReasonEnum.Manual)
      result shouldBe JsString("MAN")
    }

    "be writeable to JSON for natural expiration - NAT" in {
      val result = Json.toJson(ExpiryReasonEnum.NaturalExpiration)
      result shouldBe JsString("NAT")
    }

    "be writeable to JSON for on time submission - NLT" in {
      val result = Json.toJson(ExpiryReasonEnum.SubmissionOnTime)
      result shouldBe JsString("NLT")
    }

    "be writeable to JSON for compliance met - POC" in {
      val result = Json.toJson(ExpiryReasonEnum.Compliance)
      result shouldBe JsString("POC")
    }

    "be writeable to JSON for reset of point - RES" in {
      val result = Json.toJson(ExpiryReasonEnum.Reset)
      result shouldBe JsString("RES")
    }

    "be readable from JSON for appeal - APP" in {
      val result = Json.fromJson(JsString("APP"))(ExpiryReasonEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ExpiryReasonEnum.Appeal
    }

    "be readable from JSON for adjusted point - FAP" in {
      val result = Json.fromJson(JsString("FAP"))(ExpiryReasonEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ExpiryReasonEnum.Adjustment
    }

    "be readable from JSON for obligation reversed - ICR" in {
      val result = Json.fromJson(JsString("ICR"))(ExpiryReasonEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ExpiryReasonEnum.Reversal
    }

    "be readable from JSON for manual removal - MAN" in {
      val result = Json.fromJson(JsString("MAN"))(ExpiryReasonEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ExpiryReasonEnum.Manual
    }
    
    "be readable from JSON for natural expiration - NAT" in {
      val result = Json.fromJson(JsString("NAT"))(ExpiryReasonEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ExpiryReasonEnum.NaturalExpiration
    }

    "be readable from JSON for on time submission - NLT" in {
      val result = Json.fromJson(JsString("NLT"))(ExpiryReasonEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ExpiryReasonEnum.SubmissionOnTime
    }
    
    "be readable from JSON for compliance met - POC" in {
      val result = Json.fromJson(JsString("POC"))(ExpiryReasonEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ExpiryReasonEnum.Compliance
    }

    "be readable from JSON for reset of point - RES" in {
      val result = Json.fromJson(JsString("RES"))(ExpiryReasonEnum.format)
      result.isSuccess shouldBe true
      result.get shouldBe ExpiryReasonEnum.Reset
    }

    "return JsError when the enum is not readable" in {
      val result = Json.fromJson(JsString("error"))(ExpiryReasonEnum.format)
      result.isError shouldBe true
    }
  }

}
