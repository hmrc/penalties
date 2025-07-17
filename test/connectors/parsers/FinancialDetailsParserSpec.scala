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

package connectors.parsers

import base.LogCapturing
import connectors.parsers.getFinancialDetails.FinancialDetailsParser
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, GetFinancialData, LineItemDetails}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDate

class FinancialDetailsParserSpec extends AnyWordSpec with Matchers with LogCapturing {

  val mockGetFinancialDetailsModelAPI1811: GetFinancialData = GetFinancialData(
    FinancialDetails(
      documentDetails = Some(
        Seq(DocumentDetails(
          chargeReferenceNumber = None,
          documentOutstandingAmount = Some(0.00),
          lineItemDetails = Some(Seq(LineItemDetails(None))),
          documentTotalAmount = Some(100.00),
          issueDate = Some(LocalDate.of(2023, 1, 1))
        ))),
      totalisation = Some(
        FinancialDetailsTotalisation(
          regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(1000.0))),
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
  val getHipFinancialDetailsAsJson: JsValue = Json.parse(
    """
      |{
      | "success": {
      |   "processingDate": "2023-11-28T10:15:10Z",
      |   "financialData": {
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

  def financialDetailsParserReads(httpResponse: HttpResponse): FinancialDetailsResponse =
    FinancialDetailsParser.FinancialDetailsReads.read("GET", "/", httpResponse)

  "FinancialDetailsReads" when {
    "parsing an OK response" should {
      "return the body in a FinancialDetailsHipSuccessResponse when HIP body is valid" in {
        val validHipResponse = HttpResponse(status = OK, json = getHipFinancialDetailsAsJson, headers = Map.empty)
        val result           = financialDetailsParserReads(validHipResponse)

        result.isRight shouldBe true
        result.toOption.get
          .asInstanceOf[FinancialDetailsHipSuccessResponse]
          .financialDetails shouldBe mockGetFinancialDetailsModelAPI1811.financialDetails
      }
      "return the body in a FinancialDetailsSuccessResponse when non-HIP body is valid" in {
        val validNonHipResponse = HttpResponse(status = OK, json = getFinancialDetailsAsJson, headers = Map.empty)
        val result              = financialDetailsParserReads(validNonHipResponse)

        result.isRight shouldBe true
        result.toOption.get
          .asInstanceOf[FinancialDetailsSuccessResponse]
          .financialDetails shouldBe mockGetFinancialDetailsModelAPI1811.financialDetails
      }
      "return a FinancialDetailsMalformed when body is invalid" in {
        val invalidBody         = Json.parse("""{"documentDetails": [{"documentOutstandingAmount": "xyz"}]}""")
        val invalidBodyResponse = HttpResponse(status = OK, json = invalidBody, headers = Map.empty)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result = financialDetailsParserReads(invalidBodyResponse)
          logs.exists(_.getMessage.contains("Unable to validate Json for HIP nor IF schemas")) shouldBe true
          result shouldBe Left(FinancialDetailsMalformed)
        }
      }
    }

    "parsing a CREATED response" should {
      "return the body in a FinancialDetailsHipSuccessResponse when HIP body is valid" in {
        val validHipResponse = HttpResponse(status = CREATED, json = getHipFinancialDetailsAsJson, headers = Map.empty)
        val result           = financialDetailsParserReads(validHipResponse)

        result.isRight shouldBe true
        result.toOption.get
          .asInstanceOf[FinancialDetailsHipSuccessResponse]
          .financialDetails shouldBe mockGetFinancialDetailsModelAPI1811.financialDetails
      }
      "return the body in a FinancialDetailsSuccessResponse when non-HIP body is valid" in {
        val validNonHipResponse = HttpResponse(status = CREATED, json = getFinancialDetailsAsJson, headers = Map.empty)
        val result              = financialDetailsParserReads(validNonHipResponse)

        result.isRight shouldBe true
        result.toOption.get
          .asInstanceOf[FinancialDetailsSuccessResponse]
          .financialDetails shouldBe mockGetFinancialDetailsModelAPI1811.financialDetails
      }
      "return a FinancialDetailsMalformed when body is invalid" in {
        val invalidBody         = Json.parse("""{"documentDetails": [{"documentOutstandingAmount": "xyz"}]}""")
        val invalidBodyResponse = HttpResponse(status = CREATED, json = invalidBody, headers = Map.empty)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result = financialDetailsParserReads(invalidBodyResponse)
          logs.exists(_.getMessage.contains("Unable to validate Json for HIP nor IF schemas")) shouldBe true
          result shouldBe Left(FinancialDetailsMalformed)
        }
      }
    }

    "parsing a NOT_FOUND response with body" should {
      def notFoundHttpResponseWithBody(responseBody: String): HttpResponse = HttpResponse.apply(status = NOT_FOUND, responseBody)
      "return a FinancialDetailsNoContent response" when {
        "able to validate a HIP BusinessError body with a '016' failure code and text" in {
          val noDataFailureResponseBody = """{"errors":{"processingDate":"2025-03-03", "code":"016", "text":"Invalid ID Number"}}"""
          val notFoundHttpResponse      = notFoundHttpResponseWithBody(noDataFailureResponseBody)

          val result = financialDetailsParserReads(notFoundHttpResponse)
          result shouldBe Left(FinancialDetailsNoContent)
        }
        "able to validate an IF FailureResponse body with a 'NO_DATA_FOUND' failure code" in {
          val noDataFailureResponseBody = """{"failures":[{"code": "NO_DATA_FOUND", "reason": "No data returned found for ID"}]}"""
          val notFoundHttpResponse      = notFoundHttpResponseWithBody(noDataFailureResponseBody)

          val result = financialDetailsParserReads(notFoundHttpResponse)
          result shouldBe Left(FinancialDetailsNoContent)
        }
      }
      "return a 404 FinancialDetailsFailureResponse" when {
        "HIP BusinessError body does not have correct '016' failure code" in {
          val bodyWithInvalidCode        = """{"errors":{"processingDate":"2025-03-03", "code":"16", "text":"Invalid ID Number"}}"""
          val notFoundNoBodyHttpResponse = notFoundHttpResponseWithBody(bodyWithInvalidCode)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result = financialDetailsParserReads(notFoundNoBodyHttpResponse)
            val expectedLog =
              """Error response body: {"errors":{"processingDate":"2025-03-03","code":"16","text":"Invalid ID Number"}}"""
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(FinancialDetailsFailureResponse(NOT_FOUND))
          }
        }
        "HIP BusinessError body does not have correct '016' failure text" in {
          val bodyWithInvalidText        = """{"errors":{"processingDate":"2025-03-03", "code":"016", "text":"Invalid id num."}}"""
          val notFoundNoBodyHttpResponse = notFoundHttpResponseWithBody(bodyWithInvalidText)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result = financialDetailsParserReads(notFoundNoBodyHttpResponse)
            val expectedLog =
              """Error response body: {"errors":{"processingDate":"2025-03-03","code":"016","text":"Invalid id num."}}"""
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(FinancialDetailsFailureResponse(NOT_FOUND))
          }
        }
        "IF FailureResponse body does not have correct 'NO_DATA_FOUND' failure code" in {
          val bodyWithInvalidCode        = """{"failures":[{"code": "NOT_FOUND", "reason": "No data returned found for ID"}]}"""
          val notFoundNoBodyHttpResponse = notFoundHttpResponseWithBody(bodyWithInvalidCode)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result = financialDetailsParserReads(notFoundNoBodyHttpResponse)
            val expectedLog =
              """Error response body: {"failures":[{"code":"NOT_FOUND","reason":"No data returned found for ID"}]}"""
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(FinancialDetailsFailureResponse(NOT_FOUND))
          }
        }
        "response body cannot be validated as BusinessError nor FailureResponse" in {
          val invalidBody          = "{Not good}"
          val notFoundHttpResponse = HttpResponse.apply(status = NOT_FOUND, body = invalidBody)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result = financialDetailsParserReads(notFoundHttpResponse)
            logs.exists(_.getMessage.contains(invalidBody)) shouldBe true
            result shouldBe Left(FinancialDetailsFailureResponse(NOT_FOUND))
          }
        }
      }
    }

    "parsing a NO_CONTENT response should return a FinancialDetailsNoContent" in {
      val mockNoContentHttpResponse = HttpResponse.apply(status = NO_CONTENT, body = "")

      withCaptureOfLoggingFrom(logger) { logs =>
        val result = financialDetailsParserReads(mockNoContentHttpResponse)
        logs.exists(_.getMessage.contains("Received no content from 1811 call")) shouldBe true
        result shouldBe Left(FinancialDetailsNoContent)
      }
    }

    "will return a FinancialDetailsFailureResponse" when {
      "parsing an error with a TechnicalError response body" in {
        val error                  = """{"error":{"code": "errorCode", "message": "errorMessage", "logId": "errorLogId"}}"""
        val technicalErrorResponse = HttpResponse(status = SERVICE_UNAVAILABLE, body = error)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result = financialDetailsParserReads(technicalErrorResponse)
          logs.exists(_.getMessage.contains("Technical error returned: errorCode - errorMessage")) shouldBe true
          result shouldBe Left(FinancialDetailsFailureResponse(SERVICE_UNAVAILABLE))
        }
      }
      "parsing an error with a BusinessError response body" in {
        val errors =
          """{"errors": [
            |   {"processingDate": "errorDate", "code": "errorCode", "text": "errorText"},
            |   {"processingDate": "errorDate2", "code": "errorCode2", "text": "errorText2"}
            |]}""".stripMargin
        val businessErrorResponse = HttpResponse(status = UNPROCESSABLE_ENTITY, body = errors)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result = financialDetailsParserReads(businessErrorResponse)
          logs.exists(_.getMessage.contains("Business errors returned:")) shouldBe true
          result shouldBe Left(FinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
        }
      }
      "parsing an error with a HipWrappedError response body" in {
        val hipWrappedError        = """{"response":{"type": "errorType", "reason": "errorReason"}}"""
        val technicalErrorResponse = HttpResponse(status = BAD_REQUEST, body = hipWrappedError)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result = financialDetailsParserReads(technicalErrorResponse)
          logs.exists(_.getMessage.contains("HIP wrapped error returned: errorType - errorReason")) shouldBe true
          result shouldBe Left(FinancialDetailsFailureResponse(BAD_REQUEST))
        }
      }
    }

    "parsing an unknown error (e.g. IM A TEAPOT - 418) - and log a PagerDuty" in {
      val imATeapotHttpResponse = HttpResponse.apply(status = IM_A_TEAPOT, body = "I'm a teapot.")

      withCaptureOfLoggingFrom(logger) { logs =>
        val result = financialDetailsParserReads(imATeapotHttpResponse)
        logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
        result shouldBe Left(FinancialDetailsFailureResponse(IM_A_TEAPOT))
      }
    }
  }

}
