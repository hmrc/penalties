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
import connectors.parsers.getFinancialDetails.HIPFinancialDetailsParser
import connectors.parsers.getFinancialDetails.HIPFinancialDetailsParser._
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

class HIPFinancialDetailsParserSpec extends AnyWordSpec with Matchers with LogCapturing {

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

  def financialDetailsParserReads(httpResponse: HttpResponse): HIPFinancialDetailsResponse =
    HIPFinancialDetailsParser.HIPFinancialDetailsReads.read("GET", "/", httpResponse)

  "FinancialDetailsReads" when {
    "parsing a CREATED response" should {
      "return the body in a HIPFinancialDetailsSuccessResponse when HIP body is valid" in {
        val validHipResponse = HttpResponse(status = CREATED, json = getHipFinancialDetailsAsJson, headers = Map.empty)
        val result           = financialDetailsParserReads(validHipResponse)

        result.isRight shouldBe true
        result.toOption.get
          .asInstanceOf[HIPFinancialDetailsSuccessResponse]
          .financialDetails shouldBe mockGetFinancialDetailsModelAPI1811.financialDetails
      }
      "return a HIPFinancialDetailsMalformed when body is invalid" in {
        val invalidBody         = Json.parse("""{"documentDetails": [{"documentOutstandingAmount": "xyz"}]}""")
        val invalidBodyResponse = HttpResponse(status = CREATED, json = invalidBody, headers = Map.empty)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result = financialDetailsParserReads(invalidBodyResponse)
          logs.exists(_.getMessage.contains("Json validation of 201 body failed with errors")) shouldBe true
          result shouldBe Left(HIPFinancialDetailsMalformed)
        }
      }
    }

    "parsing an UNPROCESSABLE_ENTITY response" should {
      def errorResponse(responseBody: String): HttpResponse = HttpResponse.apply(status = UNPROCESSABLE_ENTITY, responseBody)
      "return a HIPFinancialDetailsNoContent response" when {
        "able to validate a HIP BusinessError body with a '016' failure code and text" in {
          val noDataFailureResponseBody = """{"errors":{"processingDate":"2025-03-03", "code":"016", "text":"Invalid ID Number"}}"""
          val notFoundHttpResponse      = errorResponse(noDataFailureResponseBody)

          val result = financialDetailsParserReads(notFoundHttpResponse)
          result shouldBe Left(HIPFinancialDetailsNoContent)
        }
        "able to validate a HIP BusinessError body with a '018' failure code and text" in {
          val noDataFailureResponseBody = """{"errors":{"processingDate":"2025-03-03", "code":"018", "text":"No Data Identified"}}"""
          val notFoundHttpResponse      = errorResponse(noDataFailureResponseBody)

          val result = financialDetailsParserReads(notFoundHttpResponse)
          result shouldBe Left(HIPFinancialDetailsNoContent)
        }
      }
      "return a 422 HIPFinancialDetailsFailureResponse" when {
        "HIP BusinessError body does not have correct '016' failure code" in {
          val bodyWithInvalidCode        = """{"errors":{"processingDate":"2025-03-03", "code":"16", "text":"Invalid ID Number"}}"""
          val notFoundNoBodyHttpResponse = errorResponse(bodyWithInvalidCode)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result      = financialDetailsParserReads(notFoundNoBodyHttpResponse)
            val expectedLog = "422 Error with code: 16 - Invalid ID Number"
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(HIPFinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
          }
        }
        "HIP BusinessError body does not have correct '016' failure text" in {
          val bodyWithInvalidText        = """{"errors":{"processingDate":"2025-03-03", "code":"016", "text":"Invalid id num."}}"""
          val notFoundNoBodyHttpResponse = errorResponse(bodyWithInvalidText)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result      = financialDetailsParserReads(notFoundNoBodyHttpResponse)
            val expectedLog = "422 Error with code: 016 - Invalid id num"
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(HIPFinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
          }
        }
        "response body cannot be validated as a BusinessError" in {
          val invalidBody          = """{"notGood":"isWrong"}"""
          val notFoundHttpResponse = HttpResponse.apply(status = UNPROCESSABLE_ENTITY, body = invalidBody)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result      = financialDetailsParserReads(notFoundHttpResponse)
            val expectedLog = s"Unable to parse 422 error body to expected format. Error: $invalidBody"
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(HIPFinancialDetailsFailureResponse(UNPROCESSABLE_ENTITY))
          }
        }
      }
    }

    "will return a HIPFinancialDetailsFailureResponse" when {
      "parsing an error with a TechnicalError response body" in {
        val technicalError         = """{"response":{"error":{"code":"errorCode","message":"errorMessage","logId":"errorLogId"}}}"""
        val technicalErrorResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, body = technicalError)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result      = financialDetailsParserReads(technicalErrorResponse)
          val expectedLog = s"$INTERNAL_SERVER_ERROR error: errorCode - errorMessage"
          logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
          result shouldBe Left(HIPFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))
        }
      }
      "parsing an error with an array of HipWrappedError response body" in {
        val hipWrappedError =
          """{"response":{"failures":[
            |{"type": "errorType", "reason": "errorReason"},
            |{"type": "errorType2", "reason": "errorReason2"}
            |]}}""".stripMargin
        val technicalErrorResponse = HttpResponse(status = BAD_REQUEST, body = hipWrappedError)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result      = financialDetailsParserReads(technicalErrorResponse)
          val expectedLog = "400 error: errorType - errorReason,\nerrorType2 - errorReason"
          logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
          result shouldBe Left(HIPFinancialDetailsFailureResponse(BAD_REQUEST))
        }
      }
      "response body cannot be parsed as expected error format" in {
        val invalidBody          = """{"notGood":"isWrong"}"""
        val notFoundHttpResponse = HttpResponse.apply(status = CONFLICT, body = invalidBody)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result      = financialDetailsParserReads(notFoundHttpResponse)
          val expectedLog = s"Received unexpected response from FinancialDetails, status code: $CONFLICT and body: $invalidBody"
          logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
          result shouldBe Left(HIPFinancialDetailsFailureResponse(CONFLICT))
        }
      }
    }

    "parsing an unknown error (e.g. IM A TEAPOT - 418) - and log a PagerDuty" in {
      val imATeapotHttpResponse = HttpResponse.apply(status = IM_A_TEAPOT, body = "I'm a teapot.")

      withCaptureOfLoggingFrom(logger) { logs =>
        val result = financialDetailsParserReads(imATeapotHttpResponse)
        logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1811_API.toString)) shouldBe true
        result shouldBe Left(HIPFinancialDetailsFailureResponse(IM_A_TEAPOT))
      }
    }
  }

}
