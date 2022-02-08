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

package connectors.parsers.v2

import connectors.parsers.v2.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsSuccessResponse}
import models.v2.GetPenaltyDetails
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class GetPenaltyDetailsParserSpec extends AnyWordSpec with Matchers {

  val mockGetPenaltyDetailsModel: GetPenaltyDetails = GetPenaltyDetails(
        lateSubmissionPenalty = None,
        latePaymentPenalty = None
      )

  val mockOKHttpResponseWithValidBody: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(mockGetPenaltyDetailsModel), headers = Map.empty)

  val mockOKHttpResponseWithInvalidBody: HttpResponse =
    HttpResponse.apply(status = Status.OK, json = Json.parse(
      """
        |{
        | "lateSubmissionPenalty": {
        |   "summary": {
        |     "activePenaltyPoints": 1
        |     }
        |   }
        | }
        |""".stripMargin
    ), headers = Map.empty)

  val mockNoContentHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NO_CONTENT, body = "")

  val mockISEHttpResponse: HttpResponse = HttpResponse.apply(status = Status.INTERNAL_SERVER_ERROR, body = "Something went wrong.")

  val mockImATeapotHttpResponse: HttpResponse = HttpResponse.apply(status = Status.IM_A_TEAPOT, body = "I'm a teapot.")

  "GetPenaltyDetailsReads" should {
    s"parse an OK (${Status.OK}) response" when {
      s"the body of the response is valid" in {
        val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockOKHttpResponseWithValidBody)
        result.isRight shouldBe true
        result.right.get.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails shouldBe mockGetPenaltyDetailsModel
      }

      s"the body is malformed - returning a $Left $GetPenaltyDetailsMalformed" in {
        val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockOKHttpResponseWithInvalidBody)
        result.isLeft shouldBe true
      }
    }

    s"parse a NO CONTENT (${Status.NO_CONTENT}) response" in {
      val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockNoContentHttpResponse)
      result.isLeft shouldBe true
    }

    s"parse an INTERNAL SERVER ERROR (${Status.INTERNAL_SERVER_ERROR}) response" in {
      val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockISEHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"parse an unknown error (e.g. IM A TEAPOT - ${Status.IM_A_TEAPOT})" in {
      val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockImATeapotHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.IM_A_TEAPOT
    }
  }
}

