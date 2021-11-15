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

package connectors.parsers

import connectors.parsers.DESComplianceParser._
import models.compliance.{CompliancePayloadObligationAPI, ComplianceStatusEnum, ObligationDetail, ObligationIdentification}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate

class DESComplianceParserSpec extends AnyWordSpec with Matchers {
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

  "DESComplianceCompliancePayloadReads" should {
    s"return a $DESCompliancePayloadSuccessResponse when the http response is OK" in {
      val result = DESComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(200, compliancePayloadAsJson, Map.empty[String, Seq[String]]))
      result.isRight shouldBe true
      result.right.get shouldBe DESCompliancePayloadSuccessResponse(compliancePayloadAsModel)
    }

    s"return a $DESCompliancePayloadFailureResponse" when {
      s"the status is $BAD_REQUEST" in {
        val result = DESComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(400, ""))
        result.isLeft shouldBe true
        result.left.get shouldBe DESCompliancePayloadFailureResponse(BAD_REQUEST)
      }

      s"the status is $INTERNAL_SERVER_ERROR" in {
        val result = DESComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(500, ""))
        result.isLeft shouldBe true
        result.left.get shouldBe DESCompliancePayloadFailureResponse(INTERNAL_SERVER_ERROR)
      }

      s"the status is any other non-200 status" in {
        val result = DESComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(503, ""))
        result.isLeft shouldBe true
        result.left.get shouldBe DESCompliancePayloadFailureResponse(SERVICE_UNAVAILABLE)
      }
    }

    s"return a $DESCompliancePayloadNoData when there is no data associated to the VRN" in {
      val result = DESComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(404, ""))
      result.isLeft shouldBe true
      result.left.get shouldBe DESCompliancePayloadNoData
    }

    s"return a $DESCompliancePayloadNoData when the body is malformed" in {
      val result = DESComplianceCompliancePayloadReads.read("GET", "/", HttpResponse(200, "{}"))
      result.isLeft shouldBe true
      result.left.get shouldBe DESCompliancePayloadMalformed
    }
  }
}
