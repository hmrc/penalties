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

package models.compliance

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CompliancePayloadSpec extends AnyWordSpec with Matchers{


  val sampleDateTime: LocalDateTime = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(998, ChronoUnit.MILLIS)

  val regime = "VAT"
  val vrn = "123456789"

  val return1: Return = Return(sampleDateTime, sampleDateTime, sampleDateTime, Some(ReturnStatusEnum.submitted))
  val return2: Return = Return(sampleDateTime, sampleDateTime, sampleDateTime, None)

  val missingReturn1 : MissingReturn = MissingReturn(sampleDateTime, sampleDateTime)

  val emptyMissingReturns:Seq[MissingReturn] = Seq.empty[MissingReturn]

  val someMissingReturns: Seq[MissingReturn] = Seq[MissingReturn](missingReturn1, missingReturn1)

  val returns: Seq[Return] = Seq[Return](return1, return2)

  val emptyReturnsModel:CompliancePayload = CompliancePayload(regime, vrn, 0, 2, sampleDateTime, emptyMissingReturns, returns)

  val someReturnsModel:CompliancePayload = CompliancePayload(regime, vrn, 0, 2, sampleDateTime, someMissingReturns, returns)

  def getComplianceDataJson(missingReturns: Int):JsObject = Json.obj(
    "regime" -> regime,
    "VRN" -> vrn,
    "NoOfMissingReturns" -> missingReturns,
    "noOfSubmissionsReqForCompliance"-> 2,
    "expiryDateOfAllPenaltyPoints" -> sampleDateTime,
    "missingReturns" -> (missingReturns match {
      case 0 => emptyMissingReturns
      case _=> someMissingReturns
    }),
    "returns" -> returns
  )

  "CompliancePayload" should {
    "be writeable to JSON when no missing returns" in {
      val result = Json.toJson(emptyReturnsModel)
      result shouldBe getComplianceDataJson(0)
    }

    "be writeable to JSON when there are missing returns" in {
      val result = Json.toJson(someReturnsModel)
      result shouldBe getComplianceDataJson(2)
    }

    s"be readable from JSON when no missing returns" in {
      val result = Json.fromJson(getComplianceDataJson(0))(CompliancePayload.format)
      result.isSuccess shouldBe true
      result.get shouldBe emptyReturnsModel
    }

    s"be readable from JSON when there are missing returns" in {
      val result = Json.fromJson(getComplianceDataJson(2))(CompliancePayload.format)
      result.isSuccess shouldBe true
      result.get shouldBe someReturnsModel
    }
  }
}
