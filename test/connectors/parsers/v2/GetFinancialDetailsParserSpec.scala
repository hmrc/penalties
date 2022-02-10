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

import connectors.parsers.v2.GetFinancialDetailsParser.{GetFinancialDetailsFailureResponse, GetFinancialDetailsMalformed, GetFinancialDetailsSuccessResponse}
import models.v2.financialDetails.GetFinancialDetails
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class GetFinancialDetailsParserSpec extends AnyWordSpec with Matchers {

  val mockGetFinancialDetailsModel: GetFinancialDetails = GetFinancialDetails(
    documentDetails = Seq(),
    financialDetails = Seq()
  )

  val mockOKHttpResponseWithValidBody: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(mockGetFinancialDetailsModel), headers = Map.empty)

  val mockOKHttpResponseWithInvalidBody: HttpResponse =
    HttpResponse.apply(status = Status.OK, json = Json.parse(
      """
        |{
        | "documentDetails": [{
        |   "summary": {
        |     }
        |   }]
        | }
        |""".stripMargin
    ), headers = Map.empty)

  val mockISEHttpResponse: HttpResponse = HttpResponse.apply(status = Status.INTERNAL_SERVER_ERROR, body = "Something went wrong.")
  val mockBadRequestHttpResponse: HttpResponse = HttpResponse.apply(status = Status.BAD_REQUEST, body = "Bad Request.")
  val mockForbiddenHttpResponse: HttpResponse = HttpResponse.apply(status = Status.FORBIDDEN, body = "Forbidden.")
  val mockNotFoundHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NOT_FOUND, body = "Not Found.")
  val mockConflictHttpResponse: HttpResponse = HttpResponse.apply(status = Status.CONFLICT, body = "Conflict.")
  val mockUnprocessableEnityHttpResponse: HttpResponse = HttpResponse.apply(status = Status.UNPROCESSABLE_ENTITY, body = "Unprocessable Entity.")
  val mockServiceUnavailableHttpResponse: HttpResponse = HttpResponse.apply(status = Status.SERVICE_UNAVAILABLE, body = "Service Unavailable.")

  val mockImATeapotHttpResponse: HttpResponse = HttpResponse.apply(status = Status.IM_A_TEAPOT, body = "I'm a teapot.")


  "GetFinancialDetailsReads" should {
    s"parse an OK (${Status.OK}) response" when {
      s"the body of the response is valid" in {
        val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockOKHttpResponseWithValidBody)
        result.isRight shouldBe true
        result.right.get.asInstanceOf[GetFinancialDetailsSuccessResponse].FinancialDetails shouldBe mockGetFinancialDetailsModel
      }

      s"the body is malformed - returning a $Left $GetFinancialDetailsMalformed" in {
        val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockOKHttpResponseWithInvalidBody)
        result.isLeft shouldBe true
      }
    }

    s"parse an BAD REQUEST (${Status.BAD_REQUEST}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockBadRequestHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.BAD_REQUEST
    }

    s"parse an FORBIDDEN (${Status.FORBIDDEN}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockForbiddenHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.FORBIDDEN
    }

    s"parse an NOT FOUND (${Status.NOT_FOUND}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockNotFoundHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.NOT_FOUND
    }

    s"parse an Conflict (${Status.CONFLICT}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockConflictHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.CONFLICT
    }

    s"parse an UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockUnprocessableEnityHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.UNPROCESSABLE_ENTITY
    }

    s"parse an INTERNAL SERVER ERROR (${Status.INTERNAL_SERVER_ERROR}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockISEHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"parse an SERVICE UNAVAILABLE (${Status.SERVICE_UNAVAILABLE}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockServiceUnavailableHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.SERVICE_UNAVAILABLE
    }

    s"parse an unknown error (e.g. IM A TEAPOT - ${Status.IM_A_TEAPOT})" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockImATeapotHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.IM_A_TEAPOT
    }
  }

}
