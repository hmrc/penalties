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

package connectors

import config.featureSwitches.{CallDES, FeatureSwitching}
import connectors.parsers.ComplianceParser._
import models.compliance.{CompliancePayload, ComplianceStatusEnum, ObligationDetail, ObligationIdentification}
import play.api.http.Status
import play.api.test.Helpers._
import utils.{ComplianceWiremock, IntegrationSpecCommonBase}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext

class ComplianceConnectorISpec extends IntegrationSpecCommonBase with ComplianceWiremock with FeatureSwitching {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val testStartDate: LocalDateTime = LocalDateTime.of(2021,1,1, 1,0,0)
  val testEndDate: LocalDateTime = LocalDateTime.of(2021,1,8,1,0,0)

  class Setup {
    val connector: ComplianceConnector = injector.instanceOf[ComplianceConnector]
    enableFeatureSwitch(CallDES)
  }

  "getComplianceData" should {
    "call DES and handle a successful response" in new Setup {
      enableFeatureSwitch(CallDES)
      val compliancePayloadAsModel: CompliancePayload = CompliancePayload(
        identification = Some(ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        )),
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
            inboundCorrespondenceFromDate = LocalDate.of(1920, 3, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 3, 29),
            inboundCorrespondenceDateReceived = Some(LocalDate.of(1920, 3, 29)),
            inboundCorrespondenceDueDate = LocalDate.of(1920, 3, 29),
            periodKey = "#001"
          )
        )
      )
      mockResponseForComplianceDataFromDES(Status.OK, "123456789", "2020-01-01", "2020-12-31", hasBody = true)
      val result: CompliancePayloadResponse = await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isRight shouldBe true
      result.toOption.get.asInstanceOf[CompliancePayloadSuccessResponse].model shouldBe compliancePayloadAsModel
    }

    "call stub and handle a successful response" in new Setup {
      disableFeatureSwitch(CallDES)
      val compliancePayloadAsModel: CompliancePayload = CompliancePayload(
        identification = Some(ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        )),
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
            inboundCorrespondenceFromDate = LocalDate.of(1920, 3, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 3, 29),
            inboundCorrespondenceDateReceived = Some(LocalDate.of(1920, 3, 29)),
            inboundCorrespondenceDueDate = LocalDate.of(1920, 3, 29),
            periodKey = "#001"
          )
        )
      )
      mockResponseForComplianceDataFromStub(Status.OK, "123456789", "2020-01-01", "2020-12-31")
      val result: CompliancePayloadResponse = await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isRight shouldBe true
      result.toOption.get.asInstanceOf[CompliancePayloadSuccessResponse].model shouldBe compliancePayloadAsModel
    }

    s"return a $CompliancePayloadNoData when the response status is Not Found (${Status.NOT_FOUND})" in new Setup {
      mockResponseForComplianceDataFromDES(Status.NOT_FOUND, "123456789", "2020-01-01", "2020-12-31")
      val result: CompliancePayloadResponse = await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isLeft shouldBe true
      result.left.getOrElse(CompliancePayloadFailureResponse(IM_A_TEAPOT)) shouldBe CompliancePayloadNoData
    }

    s"return a $CompliancePayloadFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      mockResponseForComplianceDataFromDES(Status.INTERNAL_SERVER_ERROR, "123456789", "2020-01-01", "2020-12-31")
      val result: CompliancePayloadResponse = await(connector.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isLeft shouldBe true
      result.left.getOrElse(CompliancePayloadFailureResponse(IM_A_TEAPOT)) shouldBe CompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)
    }

    s"return a $CompliancePayloadFailureResponse when the response status is unmatched i.e. Gateway Timeout (${Status.SERVICE_UNAVAILABLE})" in new Setup {
      mockResponseForComplianceDataFromDES(Status.SERVICE_UNAVAILABLE,"123456789", "2020-01-01", "2020-12-31")
      val result: CompliancePayloadResponse = await(connector.getComplianceData("123456789", "2020-01-01","2020-12-31"))
      result.isLeft shouldBe true
      result.left.getOrElse(CompliancePayloadFailureResponse(IM_A_TEAPOT)) shouldBe CompliancePayloadFailureResponse(Status.SERVICE_UNAVAILABLE)
    }
  }
}
