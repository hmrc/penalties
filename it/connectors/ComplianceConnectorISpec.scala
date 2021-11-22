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

package connectors

import java.time.{LocalDate, LocalDateTime}
import connectors.parsers.ComplianceParser._
import connectors.parsers.DESComplianceParser._
import featureSwitches.{CallDES, CallETMP, FeatureSwitching}
import models.compliance.{CompliancePayloadObligationAPI, ComplianceStatusEnum, ObligationDetail, ObligationIdentification}
import play.api.http.Status
import play.api.test.Helpers._
import utils.{ComplianceWiremock, IntegrationSpecCommonBase}

import scala.concurrent.ExecutionContext

class ComplianceConnectorISpec extends IntegrationSpecCommonBase with ComplianceWiremock with FeatureSwitching {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val testStartDate: LocalDateTime = LocalDateTime.of(2021,1,1, 1,0,0)
  val testEndDate: LocalDateTime = LocalDateTime.of(2021,1,8,1,0,0)

  class Setup {
    val connector: ComplianceConnector = injector.instanceOf[ComplianceConnector]
    enableFeatureSwitch(CallDES)
  }

  "getPastReturnsForEnrolmentKey" should {
    "call ETMP when the feature switch is enabled and handle a successful response" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForPastReturnPayload(Status.OK, "123456789", testStartDate, testEndDate)
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789", testStartDate, testEndDate, "mtd-vat"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse].jsValue shouldBe pastReturnPayloadAsJson
    }

    "call the stub when the feature switch is disabled and handle a successful response" in new Setup {
      disableFeatureSwitch(CallETMP)
      mockResponseForStubPastReturnPayload(Status.OK, "123456789", testStartDate, testEndDate, "mtd-vat")
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789", testStartDate, testEndDate, "mtd-vat"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse].jsValue shouldBe pastReturnPayloadAsJson
    }

    s"return a $GetCompliancePayloadNoContent when the response status is No Content (${Status.NO_CONTENT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForPastReturnPayload(Status.NO_CONTENT, "123456789", testStartDate, testEndDate, Some("{}"))
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789", testStartDate, testEndDate, "mtd-vat"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadNoContent
    }

    s"return a $GetCompliancePayloadFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForPastReturnPayload(Status.INTERNAL_SERVER_ERROR, "123456789", testStartDate, testEndDate)
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789", testStartDate, testEndDate, "mtd-vat"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)
    }

    s"return a $GetCompliancePayloadFailureResponse when the response status is unmatched i.e. Gateway Timeout (${Status.GATEWAY_TIMEOUT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForPastReturnPayload(Status.GATEWAY_TIMEOUT, "123456789", testStartDate, testEndDate)
      val result: CompliancePayloadResponse = await(connector.getPastReturnsForEnrolmentKey("123456789", testStartDate, testEndDate, "mtd-vat"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadFailureResponse(Status.GATEWAY_TIMEOUT)
    }
  }

  "getComplianceSummaryForEnrolmentKey" should {
    "call ETMP when the feature switch is enabled and handle a successful response" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForComplianceSummaryPayload(Status.OK, "123456789", "mtd-vat")
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789", "mtd-vat"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse].jsValue shouldBe complianceSummaryPayloadAsJson
    }

    "call the stub when the feature switch is disabled and handle a successful response" in new Setup {
      disableFeatureSwitch(CallETMP)
      mockResponseForStubComplianceSummaryPayload(Status.OK, "123456789", "mtd-vat")
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789", "mtd-vat"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[GetCompliancePayloadSuccessResponse].jsValue shouldBe complianceSummaryPayloadAsJson
    }

    s"return a $GetCompliancePayloadNoContent when the response status is No Content (${Status.NO_CONTENT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForComplianceSummaryPayload(Status.NO_CONTENT, "123456789","mtd-vat", Some("{}"))
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789", "mtd-vat"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadNoContent
    }

    s"return a $GetCompliancePayloadFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForComplianceSummaryPayload(Status.INTERNAL_SERVER_ERROR, "123456789", "mtd-vat")
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789", "mtd-vat"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)
    }

    s"return a $GetCompliancePayloadFailureResponse when the response status is unmatched i.e. Gateway Timeout (${Status.GATEWAY_TIMEOUT})" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForComplianceSummaryPayload(Status.GATEWAY_TIMEOUT, "123456789", "mtd-vat")
      val result: CompliancePayloadResponse = await(connector.getComplianceSummaryForEnrolmentKey("123456789", "mtd-vat"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetCompliancePayloadFailureResponse(Status.GATEWAY_TIMEOUT)
    }
  }

  "getComplianceDataFromDES" should {
    "call DES and handle a successful response" in new Setup {
      enableFeatureSwitch(CallDES)
      val compliancePayloadAsModel: CompliancePayloadObligationAPI = CompliancePayloadObligationAPI(
        identification = ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        ),
        obligationDetails = Seq(
          ObligationDetail(
            status = ComplianceStatusEnum.open,
            inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceDateReceived = None,
            inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
            periodKey = "#001"
          ),
          ObligationDetail(
            status = ComplianceStatusEnum.fulfilled,
            inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceDateReceived = Some(LocalDate.of(1920, 2, 29)),
            inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
            periodKey = "#001"
          )
        )
      )
      mockResponseForComplianceDataFromDES(Status.OK, "123456789", "2020-01-01", "2020-12-31", hasBody = true)
      val result: DESCompliancePayloadResponse = await(connector.getComplianceDataFromDES("123456789", "2020-01-01", "2020-12-31"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[DESCompliancePayloadSuccessResponse].model shouldBe compliancePayloadAsModel
    }

    "call stub and handle a successful response" in new Setup {
      disableFeatureSwitch(CallDES)
      val compliancePayloadAsModel: CompliancePayloadObligationAPI = CompliancePayloadObligationAPI(
        identification = ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        ),
        obligationDetails = Seq(
          ObligationDetail(
            status = ComplianceStatusEnum.open,
            inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceDateReceived = None,
            inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
            periodKey = "#001"
          ),
          ObligationDetail(
            status = ComplianceStatusEnum.fulfilled,
            inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceDateReceived = Some(LocalDate.of(1920, 2, 29)),
            inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
            periodKey = "#001"
          )
        )
      )
      mockResponseForComplianceDataFromStub(Status.OK, "123456789", "2020-01-01", "2020-12-31")
      val result: DESCompliancePayloadResponse = await(connector.getComplianceDataFromDES("123456789", "2020-01-01", "2020-12-31"))
      result.isRight shouldBe true
      result.right.get.asInstanceOf[DESCompliancePayloadSuccessResponse].model shouldBe compliancePayloadAsModel
    }

    s"return a $DESCompliancePayloadNoData when the response status is Not Found (${Status.NOT_FOUND})" in new Setup {
      mockResponseForComplianceDataFromDES(Status.NOT_FOUND, "123456789", "2020-01-01", "2020-12-31")
      val result: DESCompliancePayloadResponse = await(connector.getComplianceDataFromDES("123456789", "2020-01-01", "2020-12-31"))
      result.isLeft shouldBe true
      result.left.get shouldBe DESCompliancePayloadNoData
    }

    s"return a $DESCompliancePayloadFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      mockResponseForComplianceDataFromDES(Status.INTERNAL_SERVER_ERROR, "123456789", "2020-01-01", "2020-12-31")
      val result: DESCompliancePayloadResponse = await(connector.getComplianceDataFromDES("123456789", "2020-01-01", "2020-12-31"))
      result.isLeft shouldBe true
      result.left.get shouldBe DESCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)
    }

    s"return a $DESCompliancePayloadFailureResponse when the response status is unmatched i.e. Gateway Timeout (${Status.SERVICE_UNAVAILABLE})" in new Setup {
      mockResponseForComplianceDataFromDES(Status.SERVICE_UNAVAILABLE,"123456789", "2020-01-01", "2020-12-31")
      val result: DESCompliancePayloadResponse = await(connector.getComplianceDataFromDES("123456789", "2020-01-01","2020-12-31"))
      result.isLeft shouldBe true
      result.left.get shouldBe DESCompliancePayloadFailureResponse(Status.SERVICE_UNAVAILABLE)
    }
  }
}
