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

package models.appeals

import base.SpecBase
import play.api.http.Status.OK
import play.api.libs.json.Json

class AppealSubmissionResponseModelSpec extends SpecBase {
  "AppealSubmissionResponseModel" should {
    "be writable to JSON" in {
      val model = AppealSubmissionResponseModel(Some("REV-1234"), OK, Some("No error, just testing"))
      val modelAsJson = Json.parse(
        """
          |{
          | "caseId": "REV-1234",
          | "status": 200,
          | "error": "No error, just testing"
          |}
          |""".stripMargin)
      val result = Json.toJson(model)(AppealSubmissionResponseModel.format)
      result shouldBe modelAsJson
    }
  }
}
