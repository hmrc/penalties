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

import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser
import models.getFinancialDetails._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate

class GetFinancialDetailsParserSpec extends AnyWordSpec with Matchers {

  val mockGetFinancialDetailsModelAPI1811: GetFinancialDetails = GetFinancialDetails(
    documentDetails = Seq(DocumentDetails(
      documentId = "1234",
      accruingInterestAmount = None,
      interestOutstandingAmount = None,
      metadata = DocumentDetailsMetadata(
        taxYear = "2022",
        documentDate = LocalDate.now,
        documentText = "asdf",
        documentDueDate = LocalDate.now,
        documentDescription = None,
        formBundleNumber = None,
        totalAmount = 0.00,
        documentOutstandingAmount = 0.00,
        lastClearingDate = None,
        lastClearingReason = None,
        lastClearedAmount = None,
        statisticalFlag = true,
        informationCode = None,
        paymentLot = None,
        paymentLotItem = None,
        interestRate = None,
        interestFromDate = None,
        interestEndDate = None,
        latePaymentInterestID = None,
        latePaymentInterestAmount = None,
        lpiWithDunningBlock = None,
        accruingPenaltyLPP1 = None
      ))
    ),
    financialDetails = Seq(FinancialDetails(
      documentId = "asdf",
      taxPeriodFrom = None,
      taxPeriodTo = None,
      items = Seq(FinancialItem(
        dueDate = None, clearingDate = None, metadata = FinancialItemMetadata(
          subItem = None,
          amount = None,
          clearingReason = None,
          outgoingPaymentMethod = None,
          paymentLock = None,
          clearingLock = None,
          interestLock = None,
          dunningLock = None,
          returnFlag = None,
          paymentReference = None,
          paymentAmount = None,
          paymentMethod = None,
          paymentLot = None,
          paymentLotItem = None,
          clearingSAPDocument = None,
          codingInitiationDate = None,
          statisticalDocument = None,
          DDCollectionInProgress = None,
          returnReason = None,
          promisetoPay = None
        )
      )),
      originalAmount = None,
      outstandingAmount = None,
      chargeReference = None,
      mainTransaction = None,
      metadata = FinancialDetailsMetadata(
        taxYear = "2022",
        chargeType = None,
        mainType = None,
        periodKey = None,
        periodKeyDescription = None,
        businessPartner = None,
        contractAccountCategory = None,
        contractAccount = None,
        contractObjectType = None,
        contractObject = None,
        sapDocumentNumber = None,
        sapDocumentNumberItem = None,
        subTransaction = None,
        clearedAmount = None,
        accruedInterest = None
      )
    ))
  )

  val mockOKHttpResponseWithValidBody: HttpResponse = HttpResponse.apply(
    status = Status.OK, json = Json.toJson(mockGetFinancialDetailsModelAPI1811), headers = Map.empty)

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
  val mockNoContentHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NO_CONTENT, body = "")
  val mockUnprocessableEnityHttpResponse: HttpResponse = HttpResponse.apply(status = Status.UNPROCESSABLE_ENTITY, body = "Unprocessable Entity.")
  val mockServiceUnavailableHttpResponse: HttpResponse = HttpResponse.apply(status = Status.SERVICE_UNAVAILABLE, body = "Service Unavailable.")

  val mockImATeapotHttpResponse: HttpResponse = HttpResponse.apply(status = Status.IM_A_TEAPOT, body = "I'm a teapot.")


  "GetFinancialDetailsReads" should {
    s"parse an OK (${Status.OK}) response" when {
      s"the body of the response is valid" in {
        val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockOKHttpResponseWithValidBody)
        result.isRight shouldBe true
        result.right.get.asInstanceOf[GetFinancialDetailsSuccessResponse].financialDetails shouldBe mockGetFinancialDetailsModelAPI1811
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

    s"parse an CONFLICT (${Status.CONFLICT}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockConflictHttpResponse)
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.CONFLICT
    }

    s"parse a NO_CONTENT (${Status.NO_CONTENT}) response" in {
      val result = GetFinancialDetailsParser.GetFinancialDetailsReads.read("GET", "/", mockNoContentHttpResponse)
      result.isLeft shouldBe true
      result.left.get shouldBe GetFinancialDetailsNoContent
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
