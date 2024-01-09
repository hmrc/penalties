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

package connectors.parsers

import base.LogCapturing
import connectors.parsers.ComplianceParser._
import models.compliance.{CompliancePayload, ComplianceStatusEnum, ObligationDetail, ObligationIdentification}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys

import java.time.LocalDate

class ComplianceParserSpec extends AnyWordSpec with Matchers with LogCapturing {
  val compliancePayloadAsJson: JsValue = Json.parse(
    """
      |{
      |   "obligations": [
      |     {
      |			"identification": {
      |				"referenceNumber": "123456789",
      |				"referenceType": "VRN"
      |			},
      |			"obligationDetails": [
      |				{
      |					"status": "O",
      |					"inboundCorrespondenceFromDate": "1920-02-29",
      |					"inboundCorrespondenceToDate": "1920-02-29",
      |					"inboundCorrespondenceDueDate": "1920-02-29",
      |					"periodKey": "#001"
      |				},
      |				{
      |					"status": "F",
      |					"inboundCorrespondenceFromDate": "1920-02-29",
      |					"inboundCorrespondenceToDate": "1920-02-29",
      |					"inboundCorrespondenceDateReceived": "1920-02-29",
      |					"inboundCorrespondenceDueDate": "1920-02-29",
      |					"periodKey": "#001"
      |				}
      |			]
      |		}
      |  ]
      |}
      |""".stripMargin)

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
        inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
        inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
        inboundCorrespondenceDateReceived = Some(LocalDate.of(1920, 2, 29)),
        inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
        periodKey = "#001"
      )
    )
  )

  "CompliancePayloadReads" should {
    s"return a $CompliancePayloadSuccessResponse when the http response is OK" in {
      val result = ComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(200, compliancePayloadAsJson, Map.empty[String, Seq[String]]))
      result.isRight shouldBe true
      result.toOption.get shouldBe CompliancePayloadSuccessResponse(compliancePayloadAsModel)
    }

    s"return a $CompliancePayloadFailureResponse" when {
      s"the status is $BAD_REQUEST" in {
        withCaptureOfLoggingFrom(logger) {
          logs => {
            val result = ComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(400, ""))
            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_4XX_FROM_1330_API.toString)) shouldBe true
            result.isLeft shouldBe true
            result.left.getOrElse(CompliancePayloadFailureResponse(IM_A_TEAPOT)) shouldBe CompliancePayloadFailureResponse(BAD_REQUEST)
          }
        }
      }

      s"the status is $INTERNAL_SERVER_ERROR" in {
        withCaptureOfLoggingFrom(logger) {
          logs => {
            val result = ComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(500, ""))
            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1330_API.toString)) shouldBe true
            result.isLeft shouldBe true
            result.left.getOrElse(CompliancePayloadFailureResponse(IM_A_TEAPOT)) shouldBe CompliancePayloadFailureResponse(INTERNAL_SERVER_ERROR)
          }
        }
      }

      s"the status is any other non-200 status - and log a PagerDuty" in {
        withCaptureOfLoggingFrom(logger) {
          logs => {
            val result = ComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(503, ""))
            logs.exists(_.getMessage.contains(PagerDutyKeys.RECEIVED_5XX_FROM_1330_API.toString)) shouldBe true
            result.isLeft shouldBe true
            result.left.getOrElse(CompliancePayloadFailureResponse(IM_A_TEAPOT)) shouldBe CompliancePayloadFailureResponse(SERVICE_UNAVAILABLE)
          }
        }
      }
    }

    s"return a $CompliancePayloadNoData when there is no data associated to the VRN" in {
      val result = ComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(404, ""))
      result.isLeft shouldBe true
      result.left.getOrElse(CompliancePayloadFailureResponse(IM_A_TEAPOT)) shouldBe CompliancePayloadNoData
    }

    s"return a $CompliancePayloadNoData when the body is malformed" in {
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = ComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(200, "{}"))
          logs.exists(_.getMessage.contains(PagerDutyKeys.INVALID_JSON_RECEIVED_FROM_1330_API.toString)) shouldBe true
          result.isLeft shouldBe true
          result.left.getOrElse(CompliancePayloadFailureResponse(IM_A_TEAPOT)) shouldBe CompliancePayloadMalformed
        }
      }
    }
  }
}
