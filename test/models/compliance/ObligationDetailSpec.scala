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

package models.compliance

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class ObligationDetailSpec extends AnyWordSpec with Matchers {
  val obligationDetailNoDateReceivedAsJson: JsValue = Json.parse(
    """
      |{
      | "status": "O",
      | "inboundCorrespondenceFromDate": "2020-01-01",
      | "inboundCorrespondenceToDate": "2020-01-31",
      | "inboundCorrespondenceDueDate": "2020-03-07",
      | "periodKey": "period1"
      |}
      |""".stripMargin)

  val obligationDetailReceivedAsJson: JsValue = Json.parse(
    """
      |{
      | "status": "F",
      | "inboundCorrespondenceFromDate": "2020-01-01",
      | "inboundCorrespondenceToDate": "2020-01-31",
      | "inboundCorrespondenceDateReceived": "2020-03-05",
      | "inboundCorrespondenceDueDate": "2020-03-07",
      | "periodKey": "period1"
      |}
      |""".stripMargin)

  val obligationDetailNoDateReceivedAsModel: ObligationDetail = ObligationDetail(
    status = ComplianceStatusEnum.open,
    inboundCorrespondenceFromDate = LocalDate.of(2020, 1, 1),
    inboundCorrespondenceToDate = LocalDate.of(2020, 1, 31),
    inboundCorrespondenceDateReceived = None,
    inboundCorrespondenceDueDate = LocalDate.of(2020, 3, 7),
    periodKey = "period1"
  )

  val obligationDetailReceivedAsModel: ObligationDetail = ObligationDetail(
    status = ComplianceStatusEnum.fulfilled,
    inboundCorrespondenceFromDate = LocalDate.of(2020, 1, 1),
    inboundCorrespondenceToDate = LocalDate.of(2020, 1, 31),
    inboundCorrespondenceDateReceived = Some(LocalDate.of(2020, 3, 5)),
    inboundCorrespondenceDueDate = LocalDate.of(2020, 3, 7),
    periodKey = "period1"
  )

  "ObligationDetail" should {
    "correctly parse the model from JSON for open obligations" in {
      val result = Json.fromJson(obligationDetailNoDateReceivedAsJson)(ObligationDetail.format)
      result.isSuccess shouldBe true
      result.get shouldBe obligationDetailNoDateReceivedAsModel
    }

    "correctly parse the model from JSON for fulfilled obligations" in {
      val result = Json.fromJson(obligationDetailReceivedAsJson)(ObligationDetail.format)
      result.isSuccess shouldBe true
      result.get shouldBe obligationDetailReceivedAsModel
    }

    "correctly parse the model to JSON for open obligations" in {
      val result = Json.toJson(obligationDetailNoDateReceivedAsModel)(ObligationDetail.format)
      result shouldBe obligationDetailNoDateReceivedAsJson
    }

    "correctly parse the model to JSON for fulfilled obligations" in {
      val result = Json.toJson(obligationDetailReceivedAsModel)(ObligationDetail.format)
      result shouldBe obligationDetailReceivedAsJson
    }
  }
}
