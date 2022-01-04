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

package services

import connectors.parsers.ComplianceParser._
import featureSwitches.{CallDES, FeatureSwitching}
import models.compliance.{CompliancePayload, ComplianceStatusEnum, ObligationDetail, ObligationIdentification}
import play.api.test.Helpers._
import utils.{ComplianceWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class ComplianceServiceISpec extends IntegrationSpecCommonBase with ComplianceWiremock with FeatureSwitching {
  val complianceService: ComplianceService = injector.instanceOf[ComplianceService]
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val startOfLogMsg: String = ""

  class Setup {
    enableFeatureSwitch(CallDES)
  }

  "getComplianceData" should {
    "return Left(ISE)" when {
      s"the connector returns $CompliancePayloadFailureResponse" in new Setup {
        mockResponseForComplianceDataFromDES(INTERNAL_SERVER_ERROR, "123456789", "2020-01-01", "2020-12-31")
        val result = await(complianceService.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.get shouldBe INTERNAL_SERVER_ERROR
      }

      s"the connector returns $CompliancePayloadMalformed" in new Setup {
        mockResponseForComplianceDataFromDES(OK, "123456789", "2020-01-01", "2020-12-31", invalidBody = true)
        val result = await(complianceService.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.get shouldBe INTERNAL_SERVER_ERROR
      }
    }

    s"return Left(NOT_FOUND) when the connector returns $CompliancePayloadNoData" in new Setup {
      mockResponseForComplianceDataFromDES(NOT_FOUND, "123456789", "2020-01-01", "2020-12-31")
      val result = await(complianceService.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isLeft shouldBe true
      result.left.get shouldBe NOT_FOUND
    }

    s"return Right(model) when the connector returns $CompliancePayloadSuccessResponse" in new Setup {
      val compliancePayloadAsModel: CompliancePayload = CompliancePayload(
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
      mockResponseForComplianceDataFromDES(OK, "123456789", "2020-01-01", "2020-12-31", hasBody = true)
      val result = await(complianceService.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isRight shouldBe true
      result.right.get shouldBe compliancePayloadAsModel
    }
  }
}
