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

import base.LogCapturing
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser
import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsNoContent, GetPenaltyDetailsSuccessResponse}
import models.getPenaltyDetails.GetPenaltyDetails
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

class GetPenaltyDetailsParserSpec extends AnyWordSpec with Matchers with LogCapturing {

  val mockGetPenaltyDetailsModelv3: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = None
  )

  val mockOKHttpResponseWithValidBody: HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(mockGetPenaltyDetailsModelv3), headers = Map.empty)

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

  val mockNotFoundHttpResponseJsonBody: HttpResponse = HttpResponse.apply(status = Status.NOT_FOUND, json = Json.parse(
    """
      |{
      | "failures": [
      |   {
      |     "code": "NO_DATA_FOUND",
      |     "reason": "Some reason"
      |   }
      | ]
      |}
      |""".stripMargin
  ), headers = Map.empty)


  val mockISEHttpResponse: HttpResponse = HttpResponse.apply(status = Status.INTERNAL_SERVER_ERROR, body = "Something went wrong.")
  val mockBadRequestHttpResponse: HttpResponse = HttpResponse.apply(status = Status.BAD_REQUEST, body = "Bad Request.")
  val mockNotFoundHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NOT_FOUND, body = "Not Found.")
  val mockNotFoundHttpResponseNoBody: HttpResponse = HttpResponse.apply(status = Status.NOT_FOUND, body = "")
  val mockNoContentHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NO_CONTENT, body = "")
  val mockConflictHttpResponse: HttpResponse = HttpResponse.apply(status = Status.CONFLICT, body = "Conflict.")
  val mockUnprocessableEnityHttpResponse: HttpResponse = HttpResponse.apply(status = Status.UNPROCESSABLE_ENTITY, body = "Unprocessable Entity.")
  val mockServiceUnavailableHttpResponse: HttpResponse = HttpResponse.apply(status = Status.SERVICE_UNAVAILABLE, body = "Service Unavailable.")

  val mockImATeapotHttpResponse: HttpResponse = HttpResponse.apply(status = Status.IM_A_TEAPOT, body = "I'm a teapot.")

  "GetPenaltyDetailsReads" should {
    s"parse an OK (${Status.OK}) response" when {
      s"the body of the response is valid" in {
        val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockOKHttpResponseWithValidBody)
        result.isRight shouldBe true
        result.toOption.get.asInstanceOf[GetPenaltyDetailsSuccessResponse].penaltyDetails shouldBe mockGetPenaltyDetailsModelv3
      }

      s"the body is malformed - returning a $Left $GetPenaltyDetailsMalformed" in {
        val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockOKHttpResponseWithInvalidBody)
        result.isLeft shouldBe true
      }
    }

    s"parse a BAD REQUEST (${Status.BAD_REQUEST}) response - logging a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockBadRequestHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.BAD_REQUEST
        }
      }
    }

    s"parse a NOT FOUND (${Status.NOT_FOUND}) response with invalid JSON body" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockNotFoundHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.INVALID_JSON_RECEIVED_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.NOT_FOUND
        }
      }
    }

    s"parse a NOT FOUND (${Status.NOT_FOUND}) response with JSON body returned (return $GetPenaltyDetailsNoContent)" in {
      val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockNotFoundHttpResponseJsonBody)
      result.isLeft shouldBe true
      result.left.getOrElse(false) shouldBe GetPenaltyDetailsNoContent
    }

    s"parse a NOT FOUND (${Status.NOT_FOUND}) response with no body" in {
      val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockNotFoundHttpResponseNoBody)
      result.isLeft shouldBe true
      result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.NOT_FOUND
    }

    s"parse a NO CONTENT (${Status.NO_CONTENT}) response" in {
      val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockNoContentHttpResponse)
      result.isLeft shouldBe true
      result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.NO_CONTENT
    }

    s"parse a Conflict (${Status.CONFLICT}) response - logging PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockConflictHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.CONFLICT
        }
      }
    }

    s"parse an UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockUnprocessableEnityHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.UNPROCESSABLE_ENTITY
        }
      }

    }

    s"parse an INTERNAL SERVER ERROR (${Status.INTERNAL_SERVER_ERROR}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockISEHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    s"parse a SERVICE UNAVAILABLE (${Status.SERVICE_UNAVAILABLE}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockServiceUnavailableHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.SERVICE_UNAVAILABLE
        }
      }
    }

    s"parse an unknown error (e.g. IM A TEAPOT - ${Status.IM_A_TEAPOT}) - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockImATeapotHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1812_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(false).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.IM_A_TEAPOT
        }
      }
    }
  }
}


