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

package connectors.parsers

import base.SpecBase
import connectors.parsers.AppealsParser.{BadRequest, InvalidJson, UnexpectedFailure}
import models.appeals.AppealResponseModel
import play.api.http.Status
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.http.HttpResponse

class AppealsParserSpec extends SpecBase {

  class Setup(status: Int, optJson: Option[JsValue] = None, responseHeaders: Map[String, Seq[String]] = Map.empty) {

    private val httpMethod = "POST"
    private val url = "/"
    val httpResponse: AnyRef with HttpResponse = HttpResponse.apply(status, optJson.getOrElse(JsString("")), responseHeaders)

    def readResponse: AppealsParser.AppealSubmissionResponse = AppealsParser.AppealSubmissionResponseReads.read(httpMethod, url, httpResponse)

  }

  val validModel: AppealResponseModel = AppealResponseModel("PR-1234")

  val responseJson: JsValue = Json.parse(
    s"""
      |{
      | "caseID": "PR-1234"
      |}
      |""".stripMargin
  )

  "reads" should {
    s"return the response model if status is ${Status.OK}" in new Setup(Status.OK, optJson = Some(responseJson)) {
      readResponse shouldBe Right(validModel)
    }

    s"return invalid Json if ${Status.OK} and json is invalid" in new Setup(Status.OK, optJson = Some(Json.obj())){
      readResponse shouldBe Left(InvalidJson)
    }

    s"return $BadRequest if ${Status.BAD_REQUEST} returned" in new Setup(Status.BAD_REQUEST) {
      readResponse shouldBe Left(BadRequest)
    }

    s"return $UnexpectedFailure if random non Success status code returned" in new Setup(Status.INTERNAL_SERVER_ERROR) {
      readResponse shouldBe Left(UnexpectedFailure(500, "Unexpected response, status 500 returned"))
    }
  }
}
