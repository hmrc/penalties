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

import connectors.parsers.ETMPPayloadParser._
import models.ETMPPayload
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class ETMPPayloadParserSpec extends AnyWordSpec with Matchers {

  val mockETMPPayloadModel: ETMPPayload = ETMPPayload(
    pointsTotal = 0,
    lateSubmissions = 0,
    adjustmentPointsTotal = 0,
    fixedPenaltyAmount = 0,
    penaltyAmountsTotal = 0,
    penaltyPointsThreshold = 0,
    penaltyPoints = Seq.empty
  )
  val mockOKHttpResponseWithValidBody: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(mockETMPPayloadModel), headers = Map.empty)
  val mockOKHttpResponseWithInvalidBody: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)

  val mockNoContentHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NO_CONTENT, body = "")

  val mockISEHttpResponse: HttpResponse = HttpResponse.apply(status = Status.INTERNAL_SERVER_ERROR, body = "Something went wrong.")

  val mockImATeapotHttpResponse: HttpResponse = HttpResponse.apply(status = Status.IM_A_TEAPOT, body = "I'm a teapot.")

  "ETMPPayloadReads" should {
    s"parse an OK (${Status.OK}) response" when {
      s"the body of the response is valid" in {
        val result = ETMPPayloadParser.ETMPPayloadReads.read("GET", "/", mockOKHttpResponseWithValidBody)
        result.isRight shouldBe true
        result.right.get.asInstanceOf[GetETMPPayloadSuccessResponse].etmpPayload shouldBe mockETMPPayloadModel
      }

      s"the body is malformed - returning a $Left $GetETMPPayloadMalformed" in {
        val result = ETMPPayloadParser.ETMPPayloadReads.read("GET", "/", mockOKHttpResponseWithInvalidBody)
        result.isLeft shouldBe true
      }
    }

    s"parse a NO CONTENT (${Status.NO_CONTENT}) response" in {
      val result = ETMPPayloadParser.ETMPPayloadReads.read("GET", "/", mockNoContentHttpResponse)
      result.isLeft shouldBe true
    }

    s"parse an INTERNAL SERVER ERROR (${Status.INTERNAL_SERVER_ERROR}) response" in {
      val result = ETMPPayloadParser.ETMPPayloadReads.read("GET", "/", mockISEHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetETMPPayloadFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"parse an unknown error (e.g. IM A TEAPOT - ${Status.IM_A_TEAPOT})" in {
      val result = ETMPPayloadParser.ETMPPayloadReads.read("GET", "/", mockImATeapotHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetETMPPayloadFailureResponse].status shouldBe Status.IM_A_TEAPOT
    }
  }
}
