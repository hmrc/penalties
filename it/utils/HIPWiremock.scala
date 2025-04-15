/*
 * Copyright 2025 HM Revenue & Customs
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

package utils

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, JsValue, Json}

trait HIPWiremock {

  val appealResponse: JsValue = Json.parse(
    """
      |{
      | "caseID": "PR-1234567889"
      |}
      |""".stripMargin
  )

  val appealInvalidPayloadError: JsValue = Json.parse(
    """
      |{
      |  "failures": [
      |    {
      |      "dependentSystemHTTPCode": "",
      |      "originatedFrom": "pegacms",
      |      "code": 400,
      |      "reason": "Bad Request or Invalid payload"
      |    }
      |  ]
      |}
      |""".stripMargin
  )

  val appealDuplicateSubmissionError: JsValue = Json.parse(
    """
      |{
      |  "failures": [
      |    {
      |      "dependentSystemHTTPCode": "",
      |      "origniatedFrom": "pegacms",
      |      "code": "ER001",
      |      "reason": "The duplicate case has been found for the given penalty number."
      |    }
      |  ]
      |}
      |""".stripMargin
  )

  val appealETMPError: JsValue = Json.parse(
    """
      |{
      |  "failures": [
      |    {
      |      "dependentSystemHTTPCode": 422,
      |      "originatedFrom": "etmp",
      |      "code": 301,
      |      "reason": "No valid agent relationship"
      |    }
      |  ]
      |}
      |""".stripMargin
  )

  val appealServerError: JsValue = Json.parse(
    """
      |{
      |  "failures": [
      |    {
      |      "dependentSystemHTTPCode": "500",
      |      "originatedFrom": "etmp",
      |      "code": "",
      |      "reason": ""
      |    }
      |  ]
      |}
      |""".stripMargin
  )

  def mockSuccessfulResponse(): StubMapping = mockResponse(201, appealResponse)
  def mockInvalidPayloadResponse(): StubMapping = mockResponse(400, appealInvalidPayloadError)
  def mockUnauthorisedResponse(): StubMapping = mockResponse(401, JsObject.empty)
  def mockForbiddenResponse(): StubMapping = mockResponse(403, JsObject.empty)
  def mockNotFoundResponse(): StubMapping = mockResponse(404, appealInvalidPayloadError)
  def mockDuplicateSubmissionResponse(): StubMapping = mockResponse(409, appealDuplicateSubmissionError)
  def mockUnsupportedMediaTypeResponse(): StubMapping = mockResponse(415, JsObject.empty)
  def mockEMTPErrorResponse(): StubMapping = mockResponse(422, appealETMPError)
  def mockInternalServerErrorResponse(): StubMapping = mockResponse(500, JsObject.empty)
  def mockBadGatewayResponse(): StubMapping = mockResponse(502, appealServerError)
  def mockServiceUnavailableResponse(): StubMapping = mockResponse(503, JsObject.empty)

  private def mockResponse(status:Int, responseModel: JsValue): StubMapping = {
    stubFor(post(urlEqualTo(s"/v1/penalty/appeal"))
      .willReturn(
        aResponse()
          .withBody(responseModel.toString())
          .withStatus(status)
      ))
  }





}
