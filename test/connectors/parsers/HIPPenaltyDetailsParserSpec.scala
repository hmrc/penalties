/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser
import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser._
import models.getPenaltyDetails.latePayment.PrincipalChargeMainTr.VATReturnCharge
import models.hipPenaltyDetails.PenaltyDetails
import models.hipPenaltyDetails.latePayment.{LPPDetails, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, IM_A_TEAPOT, INTERNAL_SERVER_ERROR, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.{Instant, LocalDate}

class HIPPenaltyDetailsParserSpec extends AnyWordSpec with Matchers with LogCapturing {

  def httpResponse(details: PenaltyDetails): HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(details), headers = Map.empty)

  val mockHIPPenaltyDetailsModelv3: PenaltyDetails = PenaltyDetails(
    processingDate = Instant.now(),
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = None,
    breathingSpace = None
  )

  val hipLpp1Details: LPPDetails = LPPDetails(
    principalChargeReference = "1000001",
    penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
    penaltyStatus = Some(LPPPenaltyStatusEnum.Posted),
    penaltyAmountAccruing = BigDecimal(0),
    penaltyAmountPosted = BigDecimal(144.21),
    penaltyAmountPaid = Some(BigDecimal(0)),
    penaltyAmountOutstanding = Some(BigDecimal(144.21)),
    lpp1LRCalculationAmt = None,
    lpp1LRDays = None,
    lpp1LRPercentage = None,
    lpp1HRCalculationAmt = None,
    lpp1HRDays = None,
    lpp1HRPercentage = None,
    lpp2Days = None,
    lpp2Percentage = None,
    penaltyChargeCreationDate = Some(LocalDate.now()),
    communicationsDate = Some(LocalDate.now()),
    penaltyChargeReference = Some("123456789"),
    penaltyChargeDueDate = None,
    appealInformation = None,
    principalChargeDocNumber = Some("DOC1"),
    principalChargeMainTr = VATReturnCharge,
    principalChargeSubTr = Some("SUB1"),
    principalChargeBillingFrom = LocalDate.now(),
    principalChargeBillingTo = LocalDate.now(),
    principalChargeDueDate = LocalDate.now(),
    principalChargeLatestClearing = None,
    timeToPay = None,
    supplement = false
  )

  val hipLpp2Details: LPPDetails = hipLpp1Details.copy(
    penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
    penaltyChargeReference = Some("123456790"),
    principalChargeLatestClearing = Some(LocalDate.now())
  )

  val mockHIPPenaltyDetailsModelWithMissingClearingDateForOnePostedLPP1: PenaltyDetails = PenaltyDetails(
    processingDate = Instant.now(),
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(hipLpp1Details, hipLpp2Details)), manualLPPIndicator = false)),
    breathingSpace = None
  )

  val mockHIPPenaltyDetailsModelWithMissingClearingDateForOnePostedLPP1NoLPP2: PenaltyDetails = PenaltyDetails(
    processingDate = Instant.now(),
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(hipLpp1Details)), manualLPPIndicator = false)),
    breathingSpace = None
  )

  val mockHIPPenaltyDetailsModelWithMissingClearingDateForMultiplePostedLPP1s: PenaltyDetails = PenaltyDetails(
    processingDate = Instant.now(),
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        Some(Seq(
          hipLpp1Details,
          hipLpp2Details,
          hipLpp1Details.copy(penaltyChargeReference = Some("123456791"), principalChargeReference = "1000002"),
          hipLpp2Details.copy(
            penaltyChargeReference = Some("123456792"),
            principalChargeReference = "1000002",
            principalChargeLatestClearing = Some(LocalDate.now().plusDays(1)))
        )),
        manualLPPIndicator = false
      )),
    breathingSpace = None
  )

  val mockHIPPenaltyDetailsModelWithAccruingLPP1: PenaltyDetails = PenaltyDetails(
    processingDate = Instant.now(),
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        Some(
          Seq(
            hipLpp1Details.copy(penaltyStatus = Some(LPPPenaltyStatusEnum.Accruing)),
            hipLpp2Details
          )),
        manualLPPIndicator = false
      )),
    breathingSpace = None
  )

  val mockOKHttpResponseWithInvalidBody: HttpResponse =
    HttpResponse.apply(
      status = Status.OK,
      json = Json.parse(
        """
        |{
        | "success": {
        |   "processingDate": "2023-11-28T10:15:10Z",
        |   "penaltyData": {
        |     "lsp": {
        |       "lspSummary": {
        |         "activePenaltyPoints": "invalid"
        |       }
        |     }
        |   }
        | }
        |}
        |""".stripMargin
      ),
      headers = Map.empty
    )

  val mockNotFoundHttpResponseJsonBody: HttpResponse = HttpResponse.apply(
    status = Status.NOT_FOUND,
    json = Json.parse(
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
    ),
    headers = Map.empty
  )

  val mockISEHttpResponse: HttpResponse = HttpResponse.apply(
    status = Status.INTERNAL_SERVER_ERROR,
    json = Json.parse("""{"error": {"code": "ISE", "message": "Something went wrong", "logId": "123"}}"""),
    headers = Map.empty
  )
  val mockBadRequestHttpResponse: HttpResponse = HttpResponse.apply(
    status = Status.BAD_REQUEST,
    json = Json.parse("""{"error": {"code": "BAD_REQUEST", "message": "Bad Request", "logId": "123"}}"""),
    headers = Map.empty)
  val mockNotFoundHttpResponse: HttpResponse       = HttpResponse.apply(status = Status.NOT_FOUND, body = "Not Found.")
  val mockNotFoundHttpResponseNoBody: HttpResponse = HttpResponse.apply(status = Status.NOT_FOUND, body = "")
  val mockNoContentHttpResponse: HttpResponse      = HttpResponse.apply(status = Status.NO_CONTENT, body = "")
  val mockConflictHttpResponse: HttpResponse = HttpResponse.apply(
    status = Status.CONFLICT,
    json = Json.parse("""{"error": {"code": "CONFLICT", "message": "Conflict", "logId": "123"}}"""),
    headers = Map.empty)
  val mockUnprocessableEntityHttpResponse: HttpResponse = HttpResponse.apply(
    status = Status.UNPROCESSABLE_ENTITY,
    json = Json.parse("""{"error": {"code": "UNPROCESSABLE", "message": "Unprocessable Entity", "logId": "123"}}"""),
    headers = Map.empty
  )
  val mockServiceUnavailableHttpResponse: HttpResponse = HttpResponse.apply(
    status = Status.SERVICE_UNAVAILABLE,
    json = Json.parse("""{"error": {"code": "SERVICE_UNAVAILABLE", "message": "Service Unavailable", "logId": "123"}}"""),
    headers = Map.empty
  )
  val mockImATeapotHttpResponse: HttpResponse = HttpResponse.apply(status = Status.IM_A_TEAPOT, body = "I'm a teapot.")

  val mockBusinessErrorsHttpResponse: HttpResponse = HttpResponse.apply(
    status = Status.BAD_REQUEST,
    json = Json.parse(
      """
      |{
      | "errors": [
      |   {
      |     "processingDate": "2023-11-28T10:15:10Z",
      |     "code": "BUSINESS_ERROR",
      |     "text": "Business validation failed"
      |   }
      | ]
      |}
      |""".stripMargin
    ),
    headers = Map.empty
  )

  val mockHIPErrorsHttpResponse: HttpResponse = HttpResponse.apply(
    status = Status.INTERNAL_SERVER_ERROR,
    json = Json.parse(
      """
      |{
      | "response": [
      |   {
      |     "type": "System Error",
      |     "reason": "HIP system temporarily unavailable"
      |   },
      |   {
      |     "type": "Validation Error",
      |     "reason": "Invalid request format"
      |   }
      | ]
      |}
      |""".stripMargin
    ),
    headers = Map.empty
  )

  val mockNotFoundWithHIPBusinessErrorHttpResponse: HttpResponse = HttpResponse.apply(
    status = Status.NOT_FOUND,
    json = Json.parse(
      """
      |{
      | "errors": {
      |   "processingDate": "2023-11-28T10:15:10Z",
      |   "code": "016",
      |   "text": "Invalid ID Number"
      | }
      |}
      |""".stripMargin
    ),
    headers = Map.empty
  )

  def penaltyDetailsParserReads(httpResponse: HttpResponse): HIPPenaltyDetailsResponse =
    HIPPenaltyDetailsParser.HIPPenaltyDetailsReads.read("GET", "/", httpResponse)

  "HIPPenaltyDetailsReads" should {
    s"parse an OK (${Status.OK}) response" when {
      s"the body of the response is valid" in {
        val result = penaltyDetailsParserReads(httpResponse(mockHIPPenaltyDetailsModelv3))
        result.isRight shouldBe true
        result.toOption.get.asInstanceOf[HIPPenaltyDetailsSuccessResponse].penaltyDetails shouldBe mockHIPPenaltyDetailsModelv3
      }

      s"the body is malformed - returning a $Left $HIPPenaltyDetailsMalformed" in {
        val result = penaltyDetailsParserReads(mockOKHttpResponseWithInvalidBody)
        result.isLeft shouldBe true
        result.left.getOrElse(HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT)) shouldBe HIPPenaltyDetailsMalformed
      }

      "there is a posted LPP1 with a missing clearing date with associated LPP2" in {
        val result = penaltyDetailsParserReads(httpResponse(mockHIPPenaltyDetailsModelWithMissingClearingDateForOnePostedLPP1))

        result.isRight shouldBe true
        val penaltyDetails = result.toOption.get.asInstanceOf[HIPPenaltyDetailsSuccessResponse].penaltyDetails
        val lpp1           = penaltyDetails.latePaymentPenalty.get.lppDetails.get.filter(_.penaltyChargeReference.contains("123456789")).head
        lpp1.principalChargeLatestClearing.get shouldBe hipLpp2Details.principalChargeLatestClearing.get
      }

      "there is a posted LPP1 with a missing clearing date without an associated LPP2 to retrieve the date from" in {
        val result = penaltyDetailsParserReads(httpResponse(mockHIPPenaltyDetailsModelWithMissingClearingDateForOnePostedLPP1NoLPP2))

        result.isRight shouldBe true
        val penaltyDetails = result.toOption.get.asInstanceOf[HIPPenaltyDetailsSuccessResponse].penaltyDetails
        val lpp1           = penaltyDetails.latePaymentPenalty.get.lppDetails.get.filter(_.penaltyChargeReference.contains("123456789")).head
        lpp1.principalChargeLatestClearing shouldBe None
      }

      "there are multiple posted LPP1s with missing clearing dates" in {
        val result = penaltyDetailsParserReads(httpResponse(mockHIPPenaltyDetailsModelWithMissingClearingDateForMultiplePostedLPP1s))

        result.isRight shouldBe true
        val penaltyDetails = result.toOption.get.asInstanceOf[HIPPenaltyDetailsSuccessResponse].penaltyDetails
        val lpp1First      = penaltyDetails.latePaymentPenalty.get.lppDetails.get.filter(_.penaltyChargeReference.contains("123456789")).head
        val lpp1Second     = penaltyDetails.latePaymentPenalty.get.lppDetails.get.filter(_.penaltyChargeReference.contains("123456791")).head

        lpp1First.principalChargeLatestClearing.get shouldBe hipLpp2Details.principalChargeLatestClearing.get
        lpp1Second.principalChargeLatestClearing.get shouldBe hipLpp2Details.principalChargeLatestClearing.get.plusDays(1)
      }

      "there is an LPP1 with Accruing status and missing clearing date (should NOT be updated)" in {
        val result = penaltyDetailsParserReads(httpResponse(mockHIPPenaltyDetailsModelWithAccruingLPP1))

        result.isRight shouldBe true
        val penaltyDetails = result.toOption.get.asInstanceOf[HIPPenaltyDetailsSuccessResponse].penaltyDetails
        val lpp1           = penaltyDetails.latePaymentPenalty.get.lppDetails.get.filter(_.penaltyChargeReference.contains("123456789")).head
        // Should remain None because status is Accruing, not Posted
        lpp1.principalChargeLatestClearing shouldBe None
      }
    }

    "parsing an UNPROCESSABLE_ENTITY 422 response" should {
      def errorResponse(responseBody: String): HttpResponse = HttpResponse.apply(status = UNPROCESSABLE_ENTITY, responseBody)
      "return a HIPPenaltyDetailsNoContent response" when {
        "able to validate a HIP BusinessError body with a '016' failure code and text" in {
          val noDataFailureResponseBody = """{"errors":{"processingDate":"2025-03-03", "code":"016", "text":"Invalid ID Number"}}"""
          val notFoundHttpResponse      = errorResponse(noDataFailureResponseBody)

          val result = penaltyDetailsParserReads(notFoundHttpResponse)
          result shouldBe Left(HIPPenaltyDetailsNoContent)
        }
      }
      "return a 422 HIPPenaltyDetailsFailureResponse" when {
        "HIP BusinessError body does not have correct '016' failure code" in {
          val bodyWithInvalidCode        = """{"errors":{"processingDate":"2025-03-03", "code":"16", "text":"Invalid ID Number"}}"""
          val notFoundNoBodyHttpResponse = errorResponse(bodyWithInvalidCode)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result      = penaltyDetailsParserReads(notFoundNoBodyHttpResponse)
            val expectedLog = "422 Error with code: 16 - Invalid ID Number"
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(HIPPenaltyDetailsFailureResponse(UNPROCESSABLE_ENTITY))
          }
        }
        "HIP BusinessError body does not have correct '016' failure text" in {
          val bodyWithInvalidText        = """{"errors":{"processingDate":"2025-03-03", "code":"016", "text":"Invalid id num."}}"""
          val notFoundNoBodyHttpResponse = errorResponse(bodyWithInvalidText)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result      = penaltyDetailsParserReads(notFoundNoBodyHttpResponse)
            val expectedLog = "422 Error with code: 016 - Invalid id num"
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(HIPPenaltyDetailsFailureResponse(UNPROCESSABLE_ENTITY))
          }
        }
        "response body cannot be validated as a BusinessError" in {
          val invalidBody          = """{"notGood":"isWrong"}"""
          val notFoundHttpResponse = HttpResponse.apply(status = UNPROCESSABLE_ENTITY, body = invalidBody)

          withCaptureOfLoggingFrom(logger) { logs =>
            val result      = penaltyDetailsParserReads(notFoundHttpResponse)
            val expectedLog = s"Unable to parse 422 error body to expected format. Error: $invalidBody"
            logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
            result shouldBe Left(HIPPenaltyDetailsFailureResponse(UNPROCESSABLE_ENTITY))
          }
        }
      }
    }

    "will return a HIPPenaltyDetailsFailureResponse" when {
      "parsing an error with a TechnicalError response body" in {
        val technicalError         = """{"response":{"error":{"code": "errorCode", "message": "errorMessage", "logId": "errorLogId"}}}"""
        val technicalErrorResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, body = technicalError)

        withCaptureOfLoggingFrom(logger) { logs =>
          val result      = penaltyDetailsParserReads(technicalErrorResponse)
          val expectedLog = s"$INTERNAL_SERVER_ERROR errors: errorCode - errorMessage"
          logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
          result shouldBe Left(HIPPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))
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
          val result      = penaltyDetailsParserReads(technicalErrorResponse)
          val expectedLog = "400 errors: errorType - errorReason,\nerrorType2 - errorReason2"
          logs.exists(_.getMessage.contains(expectedLog)) shouldBe true
          result shouldBe Left(HIPPenaltyDetailsFailureResponse(BAD_REQUEST))
        }
      }
    }

    "parsing an unknown error (e.g. IM A TEAPOT - 418) - and log a PagerDuty" in {
      val imATeapotHttpResponse = HttpResponse.apply(status = IM_A_TEAPOT, body = "I'm a teapot.")

      withCaptureOfLoggingFrom(logger) { logs =>
        val result = penaltyDetailsParserReads(imATeapotHttpResponse)
        logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1812_API.toString)) shouldBe true
        result shouldBe Left(HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT))
      }
    }
  }

}
