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

package connectors.parsers

import base.{LogCapturing, SpecBase}
import connectors.parsers.AppealsParser.{BadRequest, DuplicateAppeal, InvalidJson, UnexpectedFailure}
import models.appeals.AppealResponseModel
import play.api.http.Status
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

class AppealsParserSpec extends SpecBase with LogCapturing {

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
      withCaptureOfLoggingFrom(logger) {
        logs => {
          readResponse shouldBe Left(InvalidJson)
          logs.exists(_.getMessage.contains(PagerDutyKeys.INVALID_JSON_RECEIVED_FROM_1808_API.toString)) shouldBe true
        }
      }
    }

    s"return $BadRequest if ${Status.BAD_REQUEST} returned" in new Setup(Status.BAD_REQUEST) {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          readResponse shouldBe Left(BadRequest)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1808_API.toString)) shouldBe true
        }
      }
    }

    s"return $DuplicateAppeal if ${Status.CONFLICT} returned (not logging a PagerDuty)" in new Setup(Status.CONFLICT) {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          readResponse shouldBe Left(DuplicateAppeal)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1808_API.toString)) shouldBe false
        }
      }
    }

    s"return $UnexpectedFailure if random non Success status code returned" in new Setup(Status.INTERNAL_SERVER_ERROR) {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          readResponse shouldBe Left(UnexpectedFailure(500, "Unexpected response, status 500 returned on submission to PEGA"))
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1808_API.toString)) shouldBe true
        }
      }
    }
  }
}
