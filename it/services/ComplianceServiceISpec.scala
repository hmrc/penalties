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

package services

import models.compliance.{CompliancePayload, MissingReturn, Return, ReturnStatusEnum}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import utils.{ComplianceWiremock, IntegrationSpecCommonBase}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class ComplianceServiceISpec extends IntegrationSpecCommonBase with ComplianceWiremock {
  val complianceService: ComplianceService = injector.instanceOf[ComplianceService]
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val startOfLogMsg: String = ""

  "getComplianceSummary" should {
    "return None" when {
      "the connector returns an unknown response" in {
        mockResponseForStubComplianceSummaryPayload(INTERNAL_SERVER_ERROR, "123456789", "mtd-vat", Some("{}"))
        val result = await(complianceService.getComplianceSummary("123456789", "mtd-vat"))
        result.isDefined shouldBe false
      }

//      "the connector returns a success response but the JSON is invalid" in {
//        mockResponseForStubComplianceSummaryPayload(INTERNAL_SERVER_ERROR, "123456789", "mtd-vat", Some("{}"))
//        val result = await(complianceService.getComplianceSummary("123456789", "mtd-vat"))
//        result.isDefined shouldBe false
//      }
    }

    "return Some" when {
      "the connector returns a success response and the data is valid" in {
        mockResponseForStubComplianceSummaryPayload(OK, "123456789", "mtd-vat", None)
        val result = await(complianceService.getComplianceSummary("123456789", "mtd-vat"))
        result.isDefined shouldBe true
        result.get shouldBe Json.parse(
          """
            |{
            |   "regime":"VAT",
            |   "VRN":"10045678976543",
            |   "expiryDateOfAllPenaltyPoints":"2023-03-31T00:00:00.000Z",
            |   "noOfSubmissionsReqForCompliance":"4",
            |   "returns":[
            |      {
            |         "startDate":"2020-10-01T00:00:00.000Z",
            |         "endDate":"2020-12-31T23:59:59.000Z",
            |         "dueDate":"2021-05-07T23:59:59.000Z",
            |         "status":"Submitted"
            |      },
            |      {
            |         "startDate":"2021-04-01T00:00:00.000Z",
            |         "endDate":"2021-06-30T23:59:59.000Z",
            |         "dueDate":"2021-08-07T23:59:59.000Z"
            |      }
            |   ]
            |}
            |""".stripMargin
        )
      }
    }
  }

  "getComplianceHistory" should {
    "return None" when {
      "the connector returns an unknown response" in {
        mockResponseForStubPastReturnPayload(INTERNAL_SERVER_ERROR, "123456789", LocalDateTime.now(), LocalDateTime.now(), "mtd-vat", Some("{}"))
        val result = await(complianceService.getComplianceHistory("123456789", LocalDateTime.now().minusYears(2), LocalDateTime.now(), "mtd-vat"))
        result.isDefined shouldBe false
      }

      //      "the connector returns a success response but the JSON is invalid" in {
      //        mockResponseForStubComplianceSummaryPayload(INTERNAL_SERVER_ERROR, "123456789", "mtd-vat", Some("{}"))
      //        val result = await(complianceService.getComplianceSummary("123456789", "mtd-vat"))
      //        result.isDefined shouldBe false
      //      }
    }

    "return Some" when {
      "the connector returns a success response and the data is valid" in {
        mockResponseForStubPastReturnPayload(OK, "123456789", LocalDateTime.now(), LocalDateTime.now(), "mtd-vat", None)
        val result = await(complianceService.getComplianceHistory("123456789", LocalDateTime.now().minusYears(2), LocalDateTime.now(), "mtd-vat"))
        result.isDefined shouldBe true
        result.get shouldBe Json.parse(
          """
            |{
            |   "regime":"VAT",
            |   "VRN":"10045678976543",
            |   "noOfMissingReturns":"2",
            |   "missingReturns":[
            |      {
            |         "startDate":"2020-10-01T00:00:00.000Z",
            |         "endDate":"2020-12-31T23:59:59.000Z"
            |      }
            |   ]
            |}
            |""".stripMargin
        )
      }
    }
  }

  "getComplianceDataForEnrolmentKey" should {
    "return None" when {
      "the call to get previous compliance data fails but the compliance summary call succeeds" in {
        mockResponseForStubPastReturnPayload(INTERNAL_SERVER_ERROR, "123456789", LocalDateTime.now(), LocalDateTime.now(), "mtd-vat", Some("{}"))
        mockResponseForStubComplianceSummaryPayload(OK, "123456789", "mtd-vat", None)
        val result = await(complianceService.getComplianceDataForEnrolmentKey("123456789"))
        result.isDefined shouldBe false
      }

      "the call to get compliance summary data fails but the previous compliance call succeeds" in {
        mockResponseForStubPastReturnPayload(OK, "123456789", LocalDateTime.now(), LocalDateTime.now(), "mtd-vat", None)
        mockResponseForStubComplianceSummaryPayload(INTERNAL_SERVER_ERROR, "123456789", "mtd-vat", Some("{}"))
        val result = await(complianceService.getComplianceDataForEnrolmentKey("123456789"))
        result.isDefined shouldBe false
      }

      "both the calls succeeds but the body returned is invalid" in {
        mockResponseForStubPastReturnPayload(OK, "123456789", LocalDateTime.now(), LocalDateTime.now(), "mtd-vat", Some("{}"))
        mockResponseForStubComplianceSummaryPayload(OK, "123456789", "mtd-vat", Some("{}"))
        val result = await(complianceService.getComplianceDataForEnrolmentKey("123456789"))
        result.isDefined shouldBe false
      }
    }

    "return Some" when {
      "both calls succeed and return valid data" in {
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
        mockResponseForStubPastReturnPayload(OK, "123456789", LocalDateTime.now(), LocalDateTime.now(), "mtd-vat", None)
        mockResponseForStubComplianceSummaryPayload(OK, "123456789", "mtd-vat", None)
        val result = await(complianceService.getComplianceDataForEnrolmentKey("HMRC-MTD-VAT~VRN~123456789"))
        result.isDefined shouldBe true
        result.get shouldBe complianceModelRepresentingJSON
      }
    }
  }
}
