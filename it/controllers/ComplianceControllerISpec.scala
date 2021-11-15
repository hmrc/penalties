/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import models.compliance.{CompliancePayload, MissingReturn, Return, ReturnStatusEnum}
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.{ComplianceWiremock, IntegrationSpecCommonBase}

import java.time.LocalDateTime

class ComplianceControllerISpec extends IntegrationSpecCommonBase with ComplianceWiremock {
  val enrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"

  "getComplianceDataForEnrolmentKey" should {
    "return 404" when {
      "the service returns None because the past returns call fails" in {
        mockResponseForStubPastReturnPayload(INTERNAL_SERVER_ERROR, "123456789", LocalDateTime.now().minusYears(2), LocalDateTime.now(), "mtd-vat", Some(""))
        mockResponseForStubComplianceSummaryPayload(OK, "123456789", "mtd-vat", Some("{}"))
        val result = await(buildClientForRequestToApp(uri = s"/compliance/compliance-data?enrolmentKey=$enrolmentKey").get())
        result.status shouldBe NOT_FOUND
        result.body shouldBe s"Could not find any compliance data for enrolment: $enrolmentKey"
      }

      "the service returns None because the compliance summary returns call fails" in {
        mockResponseForStubPastReturnPayload(OK, "123456789", LocalDateTime.now().minusYears(2), LocalDateTime.now(), "mtd-vat", Some("{}"))
        mockResponseForStubComplianceSummaryPayload(INTERNAL_SERVER_ERROR, "123456789", "mtd-vat", Some("{}"))
        val result = await(buildClientForRequestToApp(uri = s"/compliance/compliance-data?enrolmentKey=$enrolmentKey").get())
        result.status shouldBe NOT_FOUND
        result.body shouldBe s"Could not find any compliance data for enrolment: $enrolmentKey"
      }
    }

    "return 500" when {
      "the service fails to parse the body of the response" in {
        mockResponseForStubPastReturnPayload(OK, "123456789", LocalDateTime.now().minusYears(2), LocalDateTime.now(), "mtd-vat", Some(""))
        mockResponseForStubComplianceSummaryPayload(OK, "123456789", "mtd-vat", Some(""))
        val result = await(buildClientForRequestToApp(uri = s"/compliance/compliance-data?enrolmentKey=$enrolmentKey").get())
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return 200" when {
      "the service returns Some due to a valid response from ETMP/stub" in {
        val complianceModelRepresentingJSON = CompliancePayload(
          noOfMissingReturns = "2",
          noOfSubmissionsReqForCompliance = "4",
          expiryDateOfAllPenaltyPoints = LocalDateTime.of(2023, 3, 31, 0, 0, 0, 0),
          missingReturns = Seq(
            MissingReturn(
              startDate = LocalDateTime.of(2020, 10, 1, 0, 0, 0, 0),
              endDate = LocalDateTime.of(2020, 12, 31, 23, 59, 59, 0)
            )
          ),
          returns = Seq(
            Return(
              startDate = LocalDateTime.of(2020, 10, 1, 0, 0, 0, 0),
              endDate = LocalDateTime.of(2020, 12, 31, 23, 59, 59, 0),
              dueDate = LocalDateTime.of(2021, 5, 7, 23, 59, 59, 0),
              status = Some(ReturnStatusEnum.submitted)
            ),
            Return(
              startDate = LocalDateTime.of(2021, 4, 1, 0, 0, 0, 0),
              endDate = LocalDateTime.of(2021, 6, 30, 23, 59, 59, 0),
              dueDate = LocalDateTime.of(2021, 8, 7, 23, 59, 59, 0),
              status = None
            )
          )
        )
        mockResponseForStubPastReturnPayload(OK, "123456789", LocalDateTime.now().minusYears(2), LocalDateTime.now(), "mtd-vat", None)
        mockResponseForStubComplianceSummaryPayload(OK, "123456789", "mtd-vat", None)
        val result = await(buildClientForRequestToApp(uri = s"/compliance/compliance-data?enrolmentKey=$enrolmentKey").get())
        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.toJson(complianceModelRepresentingJSON)
      }
    }
  }

  "getComplianceDataFromDES" should {
    "return 200 with the associated model when the call succeeds" in {
      mockResponseForComplianceDataFromDES(OK, "123456789", "2020-01-31", "2020-12-31", hasBody = true)
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe OK
      Json.parse(result.body) shouldBe compliancePayloadAsJson
    }

    "return 400 when the downstream service returns 400" in {
      mockResponseForComplianceDataFromDES(BAD_REQUEST, "123456789", "2020-01-31", "2020-12-31")
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe BAD_REQUEST
    }

    "return 404 when the downstream service has no data for the VRN" in {
      mockResponseForComplianceDataFromDES(NOT_FOUND, "123456789", "2020-01-31", "2020-12-31")
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe NOT_FOUND
    }

    "return 500 when the downstream service has returns 500" in {
      mockResponseForComplianceDataFromDES(INTERNAL_SERVER_ERROR, "123456789", "2020-01-31", "2020-12-31")
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return 503 when the downstream service has returns 503" in {
      mockResponseForComplianceDataFromDES(SERVICE_UNAVAILABLE, "123456789", "2020-01-31", "2020-12-31")
      val result = await(buildClientForRequestToApp(uri = s"/compliance/des/compliance-data?vrn=123456789&fromDate=2020-01-31&toDate=2020-12-31").get())
      result.status shouldBe SERVICE_UNAVAILABLE
    }
  }
}
