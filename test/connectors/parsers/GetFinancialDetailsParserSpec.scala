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

import base.LogCapturing
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, GetFinancialData, LineItemDetails}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDate

class GetFinancialDetailsParserSpec extends AnyWordSpec with Matchers with LogCapturing {

  val mockGetFinancialDetailsModelAPI1811: GetFinancialData = GetFinancialData(
    FinancialDetails(
      documentDetails = Some(Seq(DocumentDetails(
        chargeReferenceNumber = None,
        documentOutstandingAmount = Some(0.00),
        lineItemDetails = Some(Seq(LineItemDetails(None))),
        documentTotalAmount = Some(100.00),
        issueDate = Some(LocalDate.of(2023, 1, 1)))
      )),
      totalisation = Some(FinancialDetailsTotalisation(
        regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000.0))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = Some(43.21)))
      ))
    )
  )

  val getFinancialDetailsAsJson: JsValue = Json.parse(
    """
      |{
      | "getFinancialData": {
      |   "financialDetails":{
      |     "documentDetails": [
      |     {
      |       "documentOutstandingAmount": 0.0,
      |       "documentTotalAmount": 100.0,
      |       "issueDate": "2023-01-01",
      |       "lineItemDetails": [{}]
      |     }
      |   ],
      |   "totalisation": {
      |     "regimeTotalisation": {
      |      "totalAccountOverdue": 1000.0,
      |      "totalAccountNotYetDue": 250.0,
      |      "totalAccountCredit": 40.0,
      |      "totalAccountBalance": 1210
      |     },
      |     "targetedSearch_SelectionCriteriaTotalisation": {
      |      "totalOverdue": 100.0,
      |      "totalNotYetDue": 0.0,
      |      "totalBalance": 100.0,
      |      "totalCredit": 10.0,
      |      "totalCleared": 50
      |     },
      |     "additionalReceivableTotalisations": {
      |      "totalAccountPostedInterest": 12.34,
      |      "totalAccountAccruingInterest": 43.21
      |     }
      |   }
      |  }
      | }
      |}
      |""".stripMargin
  )

  val mockOKHttpResponseWithValidBody: HttpResponse = HttpResponse.apply(
    status = Status.OK, json = getFinancialDetailsAsJson, headers = Map.empty)

  val mockOKHttpResponseWithInvalidBody: HttpResponse =
    HttpResponse.apply(status = Status.OK, json = Json.parse(
      """
           {
            "documentDetails": [{
               "documentOutstandingAmount": "xyz"
              }]
            }
           """.stripMargin
    ), headers = Map.empty)

  val mockISEHttpResponse: HttpResponse = HttpResponse.apply(status = Status.INTERNAL_SERVER_ERROR, body = "Something went wrong.")
  val mockBadRequestHttpResponse: HttpResponse = HttpResponse.apply(status = Status.BAD_REQUEST, body = "Bad Request.")
  val mockForbiddenHttpResponse: HttpResponse = HttpResponse.apply(status = Status.FORBIDDEN, body = "Forbidden.")
  val mockNotFoundHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NOT_FOUND, body = "Not Found.")
  val mockConflictHttpResponse: HttpResponse = HttpResponse.apply(status = Status.CONFLICT, body = "Conflict.")
  val mockNoContentHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NO_CONTENT, body = "")
  val mockUnprocessableEnityHttpResponse: HttpResponse = HttpResponse.apply(status = Status.UNPROCESSABLE_ENTITY, body = "Unprocessable Entity.")
  val mockServiceUnavailableHttpResponse: HttpResponse = HttpResponse.apply(status = Status.SERVICE_UNAVAILABLE, body = "Service Unavailable.")

  val mockImATeapotHttpResponse: HttpResponse = HttpResponse.apply(status = Status.IM_A_TEAPOT, body = "I'm a teapot.")


  "GetFinancialDetailsReads" should {
    s"parse an OK (${Status.OK}) response" when {
      s"the body of the response is valid" in {
        val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockOKHttpResponseWithValidBody)
        result.isRight shouldBe true
        result.toOption.get.asInstanceOf[GetFinancialDetailsSuccessResponse].financialDetails shouldBe mockGetFinancialDetailsModelAPI1811.financialDetails
      }
    }

    s"the body is malformed - returning a $Left $GetFinancialDetailsMalformed" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockOKHttpResponseWithInvalidBody)
      result.isLeft shouldBe true
    }

    s"parse an BAD REQUEST (${Status.BAD_REQUEST}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockBadRequestHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.BAD_REQUEST
        }
      }
    }

    s"parse an FORBIDDEN (${Status.FORBIDDEN}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockForbiddenHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.FORBIDDEN
        }
      }
    }

    s"parse an NOT FOUND (${Status.NOT_FOUND}) response" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockNotFoundHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.INVALID_JSON_RECEIVED_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.NOT_FOUND
        }
      }
    }

    s"parse an CONFLICT (${Status.CONFLICT}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockConflictHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.CONFLICT
        }
      }
    }

    s"parse a NO_CONTENT (${Status.NO_CONTENT}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockNoContentHttpResponse)
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetFinancialDetailsNoContent
    }

    s"parse an UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockUnprocessableEnityHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.UNPROCESSABLE_ENTITY
        }
      }
    }

    s"parse an INTERNAL SERVER ERROR (${Status.INTERNAL_SERVER_ERROR}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockISEHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    s"parse an SERVICE UNAVAILABLE (${Status.SERVICE_UNAVAILABLE}) response - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockServiceUnavailableHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.SERVICE_UNAVAILABLE
        }
      }
    }

    s"parse an unknown error (e.g. IM A TEAPOT - ${Status.IM_A_TEAPOT}) - and log a PagerDuty" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockImATeapotHttpResponse)
          logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.IM_A_TEAPOT
        }
      }
    }
  }

}
