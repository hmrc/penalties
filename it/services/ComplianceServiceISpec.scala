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

package services

import config.featureSwitches.{CallDES, FeatureSwitching}
import connectors.parsers.ComplianceParser._
import models.compliance.{CompliancePayload, ComplianceStatusEnum, ObligationDetail, ObligationIdentification}
import play.api.libs.json.{JsValue, Json}
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
        result.left.getOrElse(IM_A_TEAPOT) shouldBe INTERNAL_SERVER_ERROR
      }

      s"the connector returns $CompliancePayloadMalformed" in new Setup {
        mockResponseForComplianceDataFromDES(OK, "123456789", "2020-01-01", "2020-12-31", invalidBody = true)
        val result = await(complianceService.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.getOrElse(IM_A_TEAPOT) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    s"return Left(NOT_FOUND) when the connector returns $CompliancePayloadNoData" in new Setup {
      mockResponseForComplianceDataFromDES(NOT_FOUND, "123456789", "2020-01-01", "2020-12-31")
      val result = await(complianceService.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isLeft shouldBe true
      result.left.getOrElse(IM_A_TEAPOT) shouldBe NOT_FOUND
    }

    s"return Right(model) when the connector returns $CompliancePayloadSuccessResponse" in new Setup {
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
      mockResponseForComplianceDataFromDES(OK, "123456789", "2020-01-01", "2020-12-31", hasBody = true)
      val result = await(complianceService.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isRight shouldBe true
      result.toOption.get shouldBe compliancePayloadAsModel
    }

    s"return Right(model) when the connector returns $CompliancePayloadSuccessResponse (sorting the obligations by due date)" in new Setup {
      val expectedOrderedModel: CompliancePayload = CompliancePayload(
        identification = Some(ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        )),
        obligationDetails = Seq(
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31"), None, LocalDate.parse("2023-03-07"), "23AA"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28"), None, LocalDate.parse("2023-04-07"), "23AB"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-03-01"), LocalDate.parse("2023-03-31"), None, LocalDate.parse("2023-05-07"), "23AC"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-04-01"), LocalDate.parse("2023-04-30"), None, LocalDate.parse("2023-06-07"), "23AD"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-05-01"), LocalDate.parse("2023-05-31"), None, LocalDate.parse("2023-07-07"), "23AE"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-06-01"), LocalDate.parse("2023-06-30"), None, LocalDate.parse("2023-08-07"), "23AF"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-07-01"), LocalDate.parse("2023-07-31"), None, LocalDate.parse("2023-09-07"), "23AG"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-08-01"), LocalDate.parse("2023-08-31"), None, LocalDate.parse("2023-10-07"), "23AH"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-09-01"), LocalDate.parse("2023-09-30"), None, LocalDate.parse("2023-11-07"), "23AI"),
          ObligationDetail(ComplianceStatusEnum.open, LocalDate.parse("2023-10-01"), LocalDate.parse("2023-10-31"), None, LocalDate.parse("2023-12-07"), "23AJ"))
      )
      val compliancePayloadAsJson: JsValue = Json.parse(
        """
          |{
          |        "obligations" : [
          |            {
          |                "identification" : {
          |                    "referenceNumber" : "123456789",
          |                    "referenceType" : "VRN"
          |                },
          |                "obligationDetails" : [
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-10-01",
          |                        "inboundCorrespondenceToDate" : "2023-10-31",
          |                        "inboundCorrespondenceDueDate" : "2023-12-07",
          |                        "periodKey" : "23AJ"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-09-01",
          |                        "inboundCorrespondenceToDate" : "2023-09-30",
          |                        "inboundCorrespondenceDueDate" : "2023-11-07",
          |                        "periodKey" : "23AI"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-08-01",
          |                        "inboundCorrespondenceToDate" : "2023-08-31",
          |                        "inboundCorrespondenceDueDate" : "2023-10-07",
          |                        "periodKey" : "23AH"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-07-01",
          |                        "inboundCorrespondenceToDate" : "2023-07-31",
          |                        "inboundCorrespondenceDueDate" : "2023-09-07",
          |                        "periodKey" : "23AG"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-06-01",
          |                        "inboundCorrespondenceToDate" : "2023-06-30",
          |                        "inboundCorrespondenceDueDate" : "2023-08-07",
          |                        "periodKey" : "23AF"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-05-01",
          |                        "inboundCorrespondenceToDate" : "2023-05-31",
          |                        "inboundCorrespondenceDueDate" : "2023-07-07",
          |                        "periodKey" : "23AE"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-04-01",
          |                        "inboundCorrespondenceToDate" : "2023-04-30",
          |                        "inboundCorrespondenceDueDate" : "2023-06-07",
          |                        "periodKey" : "23AD"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-03-01",
          |                        "inboundCorrespondenceToDate" : "2023-03-31",
          |                        "inboundCorrespondenceDueDate" : "2023-05-07",
          |                        "periodKey" : "23AC"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-02-01",
          |                        "inboundCorrespondenceToDate" : "2023-02-28",
          |                        "inboundCorrespondenceDueDate" : "2023-04-07",
          |                        "periodKey" : "23AB"
          |                    },
          |                    {
          |                        "status" : "O",
          |                        "inboundCorrespondenceFromDate" : "2023-01-01",
          |                        "inboundCorrespondenceToDate" : "2023-01-31",
          |                        "inboundCorrespondenceDueDate" : "2023-03-07",
          |                        "periodKey" : "23AA"
          |                    }
          |                ]
          |            }
          |        ]
          |    }
          |""".stripMargin)
      mockResponseForComplianceDataFromDES(OK, "123456789", "2020-01-01", "2020-12-31", hasBody = true, optBody = Some(compliancePayloadAsJson.toString()))
      val result = await(complianceService.getComplianceData("123456789", "2020-01-01", "2020-12-31"))
      result.isRight shouldBe true
      result.toOption.get shouldBe expectedOrderedModel
    }
  }
}
