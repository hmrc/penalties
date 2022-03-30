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

package models.v2

import base.SpecBase
import models.v2.latePaymentPenalty.{LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import models.v2.lateSubmissionPenalty.{LSPData, LSPDetails, LSPPenaltyCategoryEnum, LSPPenaltyStatusEnum, LSPSummary, LateSubmission}
import play.api.libs.json.Json

import java.time.LocalDate

class GetPenaltyDetailsSpec extends SpecBase {
  "be readable from JSON" when {
    "no LSP/LPP details exist - return empty JSON" in {
      val result = Json.fromJson(Json.parse("{}"))(GetPenaltyDetails.format)
      result.isSuccess shouldBe true
      result.get.latePaymentPenalty.isEmpty shouldBe true
      result.get.lateSubmissionPenalty.isEmpty shouldBe true
    }

    "only LSP data exists - populate only lateSubmissionPenalty" in {
      val result = Json.fromJson(Json.parse(
        """
          |{
          |  "lateSubmissionPenalty": {
          |   "summary": {
          |     "activePenaltyPoints": 1,
          |     "inactivePenaltyPoints": 2,
          |     "regimeThreshold": 3,
          |     "POCAchievementDate": "2022-01-01",
          |     "penaltyChargeAmount": 123.45
          |   },
          |   "details": [{
          |    	"penaltyNumber": "1234ABCD",
          |	    "penaltyOrder": "1",
          |	    "penaltyCategory": "P",
          |	    "penaltyStatus": "ACTIVE",
          |	    "penaltyCreationDate": "2022-01-01",
          |	    "penaltyExpiryDate": "2024-01-01",
          |	    "communicationsDate": "2022-01-01",
          |     "appealStatus": "1",
          |	    "appealLevel": "1",
          |	    "chargeReference": "foobar",
          |	    "chargeAmount": 123.45,
          |	    "chargeOutstandingAmount": 123.45,
          |	    "chargeDueDate": "2022-01-01",
          |     "lateSubmissions": [{
          |       "lateSubmissionID": "ID123",
          |       "taxPeriod": "1",
          |       "taxReturnStatus": "2",
          |       "taxPeriodStartDate": "2022-01-01",
          |       "taxPeriodEndDate": "2022-03-31",
          |       "taxPeriodDueDate": "2022-05-07",
          |       "returnReceiptDate": "2022-04-01"
          |     }]
          |   }]
          |  }
          |}
          |""".stripMargin))(GetPenaltyDetails.format)

      val lspModel: LSPData = LSPData(
        summary = LSPSummary(
          activePenaltyPoints = 1,
          inactivePenaltyPoints = 2,
          regimeThreshold = 3,
          POCAchievementDate = LocalDate.of(2022, 1, 1),
          penaltyChargeAmount = 123.45
        ),
        details = Seq(LSPDetails(
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyNumber = "1234ABCD",
          penaltyOrder = "1",
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          appealStatus = Some("1"),
          communicationsDate = LocalDate.of(2022, 1, 1),
          appealLevel = Some("1"),
          chargeReference = Some("foobar"),
          chargeAmount = Some(123.45),
          chargeOutstandingAmount = Some(123.45),
          chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
          lateSubmissions = Some(Seq(
            LateSubmission(
              lateSubmissionID = "ID123",
              taxPeriod = Some("1"),
              taxReturnStatus = "2",
              taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
              taxPeriodEndDate = Some(LocalDate.of(2022, 3, 31)),
              taxPeriodDueDate = Some(LocalDate.of(2022, 5, 7)),
              returnReceiptDate = Some(LocalDate.of(2022, 4, 1))
            )
          ))
        ))
      )
      result.isSuccess shouldBe true
      result.get.latePaymentPenalty.isEmpty shouldBe true
      result.get.lateSubmissionPenalty.get shouldBe lspModel
    }

    "only LPP data exists - populate only latePaymentPenalty" in {
      val result = Json.fromJson(Json.parse(
        """
          |{
          |  "latePaymentPenalty": {
          |   "details": [{
          |	    "penaltyNumber": "1234ABCD",
          |	    "penaltyCategory": "LPP1",
          |   	"penaltyStatus": "P",
          |	    "penaltyAmountAccruing": 123.45,
          |	    "penaltyAmountPosted": 123.45,
          |	    "penaltyChargeCreationDate": "2022-01-01",
          |	    "penaltyChargeDueDate": "2022-02-01",
          |	    "communicationsDate": "2022-01-01",
          |	    "appealLevel": "1",
          |	    "appealStatus": "1",
          |	    "penaltyChargeReference": "CHARGE123456",
          |     "principalChargeDueDate": "2022-03-01",
          |     "principalChargeReference": "CHARGING12345"
          |   }]
          |  }
          |}
          |""".stripMargin))(GetPenaltyDetails.format)

      val lppModel: LatePaymentPenalty = LatePaymentPenalty(
        penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
        penaltyNumber = "1234ABCD",
        penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
        penaltyChargeDueDate = LocalDate.of(2022, 2, 1),
        penaltyStatus = LPPPenaltyStatusEnum.Posted,
        appealStatus = Some("1"),
        communicationsDate = LocalDate.of(2022, 1, 1),
        principalChargeReference = "CHARGING12345",
        penaltyAmountAccruing = 123.45,
        penaltyAmountPosted = 123.45,
        penaltyChargeReference = "CHARGE123456",
        appealLevel = Some("1"),
        principalChargeDueDate = LocalDate.of(2022, 3, 1)
      )
      result.isSuccess shouldBe true
      result.get.latePaymentPenalty.get.head shouldBe lppModel
      result.get.lateSubmissionPenalty.isEmpty shouldBe true
    }

    "both sets of data exists - populate both fields" in {
      val result = Json.fromJson(Json.parse(
        """
          |{
          | "lateSubmissionPenalty": {
          |   "summary": {
          |     "activePenaltyPoints": 1,
          |     "inactivePenaltyPoints": 2,
          |     "regimeThreshold": 3,
          |     "POCAchievementDate": "2022-01-01",
          |     "penaltyChargeAmount": 123.45
          |   },
          |   "details": [{
          |    	"penaltyNumber": "1234ABCD",
          |	    "penaltyOrder": "1",
          |	    "penaltyCategory": "P",
          |	    "penaltyStatus": "ACTIVE",
          |	    "penaltyCreationDate": "2022-01-01",
          |	    "penaltyExpiryDate": "2024-01-01",
          |	    "communicationsDate": "2022-01-01",
          |     "appealStatus": "1",
          |	    "appealLevel": "1",
          |	    "chargeReference": "foobar",
          |	    "chargeAmount": 123.45,
          |	    "chargeOutstandingAmount": 123.45,
          |	    "chargeDueDate": "2022-01-01",
          |     "lateSubmissions": [{
          |       "lateSubmissionID": "ID123",
          |       "taxPeriod": "1",
          |       "taxReturnStatus": "2",
          |       "taxPeriodStartDate": "2022-01-01",
          |       "taxPeriodEndDate": "2022-03-31",
          |       "taxPeriodDueDate": "2022-05-07",
          |       "returnReceiptDate": "2022-04-01"
          |     }]
          |   }]
          |  },
          |  "latePaymentPenalty": {
          |   "details": [{
          |	    "penaltyNumber": "1234ABCD",
          |	    "penaltyCategory": "LPP1",
          |   	"penaltyStatus": "P",
          |	    "penaltyAmountAccruing": 123.45,
          |	    "penaltyAmountPosted": 123.45,
          |	    "penaltyChargeCreationDate": "2022-01-01",
          |	    "penaltyChargeDueDate": "2022-02-01",
          |	    "communicationsDate": "2022-01-01",
          |	    "appealLevel": "1",
          |	    "appealStatus": "1",
          |	    "penaltyChargeReference": "CHARGE123456",
          |     "principalChargeDueDate": "2022-03-01",
          |     "principalChargeReference": "CHARGING12345"
          |   }]
          |  }
          |}
          |""".stripMargin))(GetPenaltyDetails.format)
      val lppModel: LatePaymentPenalty = LatePaymentPenalty(
        penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
        penaltyNumber = "1234ABCD",
        penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
        penaltyChargeDueDate = LocalDate.of(2022, 2, 1),
        penaltyStatus = LPPPenaltyStatusEnum.Posted,
        appealStatus = Some("1"),
        communicationsDate = LocalDate.of(2022, 1, 1),
        principalChargeReference = "CHARGING12345",
        penaltyAmountAccruing = 123.45,
        penaltyAmountPosted = 123.45,
        penaltyChargeReference = "CHARGE123456",
        appealLevel = Some("1"),
        principalChargeDueDate = LocalDate.of(2022, 3, 1)
      )
      val lspModel: LSPData = LSPData(
        summary = LSPSummary(
          activePenaltyPoints = 1,
          inactivePenaltyPoints = 2,
          regimeThreshold = 3,
          POCAchievementDate = LocalDate.of(2022, 1, 1),
          penaltyChargeAmount = 123.45
        ),
        details = Seq(LSPDetails(
          penaltyCategory = LSPPenaltyCategoryEnum.Point,
          penaltyNumber = "1234ABCD",
          penaltyOrder = "1",
          penaltyCreationDate = LocalDate.of(2022, 1, 1),
          penaltyExpiryDate = LocalDate.of(2024, 1, 1),
          penaltyStatus = LSPPenaltyStatusEnum.Active,
          appealStatus = Some("1"),
          communicationsDate = LocalDate.of(2022, 1, 1),
          appealLevel = Some("1"),
          chargeReference = Some("foobar"),
          chargeAmount = Some(123.45),
          chargeOutstandingAmount = Some(123.45),
          chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
          lateSubmissions = Some(Seq(
            LateSubmission(
              lateSubmissionID = "ID123",
              taxPeriod = Some("1"),
              taxReturnStatus = "2",
              taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
              taxPeriodEndDate = Some(LocalDate.of(2022, 3, 31)),
              taxPeriodDueDate = Some(LocalDate.of(2022, 5, 7)),
              returnReceiptDate = Some(LocalDate.of(2022, 4, 1))
            )
          ))
        ))
      )
      result.isSuccess shouldBe true
      result.get.latePaymentPenalty.get.head shouldBe lppModel
      result.get.lateSubmissionPenalty.get shouldBe lspModel
    }

  }

  "be writable to JSON" when {
    "no LSP/LPP details exist - return empty JSON" in {
      val model: GetPenaltyDetails = GetPenaltyDetails(
        lateSubmissionPenalty = None,
        latePaymentPenalty = None
      )
      val result = Json.toJson(model)(GetPenaltyDetails.format)
      val expectedResult = Json.obj()
      result shouldBe expectedResult
    }

    "only LSP data exists - return lateSubmissionPenalty object" in {
      val expectedResult = Json.parse(
        """
          |{
          | "lateSubmissionPenalty": {
          |   "summary": {
          |     "activePenaltyPoints": 1,
          |     "inactivePenaltyPoints": 2,
          |     "regimeThreshold": 3,
          |     "POCAchievementDate": "2022-01-01",
          |     "penaltyChargeAmount": 123.45
          |   },
          |   "details": [{
          |    	"penaltyNumber": "1234ABCD",
          |	    "penaltyOrder": "1",
          |	    "penaltyCategory": "P",
          |	    "penaltyStatus": "ACTIVE",
          |	    "penaltyCreationDate": "2022-01-01",
          |	    "penaltyExpiryDate": "2024-01-01",
          |	    "communicationsDate": "2022-01-01",
          |     "appealStatus": "1",
          |	    "appealLevel": "1",
          |	    "chargeReference": "foobar",
          |	    "chargeAmount": 123.45,
          |	    "chargeOutstandingAmount": 123.45,
          |	    "chargeDueDate": "2022-01-01",
          |     "lateSubmissions": [{
          |       "lateSubmissionID": "ID123",
          |       "taxPeriod": "1",
          |       "taxReturnStatus": "2",
          |       "taxPeriodStartDate": "2022-01-01",
          |       "taxPeriodEndDate": "2022-03-31",
          |       "taxPeriodDueDate": "2022-05-07",
          |       "returnReceiptDate": "2022-04-01"
          |     }]
          |   }]
          |  }
          |}
          |""".stripMargin)
      val model: GetPenaltyDetails = GetPenaltyDetails(
        lateSubmissionPenalty = Some(
          LSPData(
            summary = LSPSummary(
              activePenaltyPoints = 1,
              inactivePenaltyPoints = 2,
              regimeThreshold = 3,
              POCAchievementDate = LocalDate.of(2022, 1, 1),
              penaltyChargeAmount = 123.45
            ),
            details = Seq(LSPDetails(
              penaltyCategory = LSPPenaltyCategoryEnum.Point,
              penaltyNumber = "1234ABCD",
              penaltyOrder = "1",
              penaltyCreationDate = LocalDate.of(2022, 1, 1),
              penaltyExpiryDate = LocalDate.of(2024, 1, 1),
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              appealStatus = Some("1"),
              communicationsDate = LocalDate.of(2022, 1, 1),
              appealLevel = Some("1"),
              chargeReference = Some("foobar"),
              chargeAmount = Some(123.45),
              chargeOutstandingAmount = Some(123.45),
              chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
              lateSubmissions = Some(Seq(
                LateSubmission(
                  lateSubmissionID = "ID123",
                  taxPeriod = Some("1"),
                  taxReturnStatus = "2",
                  taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                  taxPeriodEndDate = Some(LocalDate.of(2022, 3, 31)),
                  taxPeriodDueDate = Some(LocalDate.of(2022, 5, 7)),
                  returnReceiptDate = Some(LocalDate.of(2022, 4, 1))
                )
              ))
            ))
          )),
        latePaymentPenalty = None)
      val result = Json.toJson(model)(GetPenaltyDetails.format)
      result shouldBe expectedResult
    }

    "only LPP data exists - return latePaymentPenalty object" in {
      val expectedResult = Json.parse(
        """
          |{
          |  "latePaymentPenalty": {
          |   "details": [{
          |	    "penaltyNumber": "1234ABCD",
          |	    "penaltyCategory": "LPP1",
          |   	"penaltyStatus": "P",
          |	    "penaltyAmountAccruing": 123.45,
          |	    "penaltyAmountPosted": 123.45,
          |	    "penaltyChargeCreationDate": "2022-01-01",
          |	    "penaltyChargeDueDate": "2022-02-01",
          |	    "communicationsDate": "2022-01-01",
          |	    "appealLevel": "1",
          |	    "appealStatus": "1",
          |	    "penaltyChargeReference": "CHARGE123456",
          |     "principalChargeDueDate": "2022-03-01",
          |     "principalChargeReference": "CHARGING12345"
          |   }]
          |  }
          |}
          |""".stripMargin)
      val model: GetPenaltyDetails = GetPenaltyDetails(
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(Seq(
          LatePaymentPenalty(
            penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
            penaltyNumber = "1234ABCD",
            penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
            penaltyChargeDueDate = LocalDate.of(2022, 2, 1),
            penaltyStatus = LPPPenaltyStatusEnum.Posted,
            appealStatus = Some("1"),
            communicationsDate = LocalDate.of(2022, 1, 1),
            principalChargeReference = "CHARGING12345",
            penaltyAmountAccruing = 123.45,
            penaltyAmountPosted = 123.45,
            penaltyChargeReference = "CHARGE123456",
            appealLevel = Some("1"),
            principalChargeDueDate = LocalDate.of(2022, 3, 1)
          )
        ))
      )
      val result = Json.toJson(model)(GetPenaltyDetails.format)
      result shouldBe expectedResult
    }

    "both data exists" in {
      val expectedResult = Json.parse(
        """
          |{
          | "lateSubmissionPenalty": {
          |   "summary": {
          |     "activePenaltyPoints": 1,
          |     "inactivePenaltyPoints": 2,
          |     "regimeThreshold": 3,
          |     "POCAchievementDate": "2022-01-01",
          |     "penaltyChargeAmount": 123.45
          |   },
          |   "details": [{
          |    	"penaltyNumber": "1234ABCD",
          |	    "penaltyOrder": "1",
          |	    "penaltyCategory": "P",
          |	    "penaltyStatus": "ACTIVE",
          |	    "penaltyCreationDate": "2022-01-01",
          |	    "penaltyExpiryDate": "2024-01-01",
          |	    "communicationsDate": "2022-01-01",
          |     "appealStatus": "1",
          |	    "appealLevel": "1",
          |	    "chargeReference": "foobar",
          |	    "chargeAmount": 123.45,
          |	    "chargeOutstandingAmount": 123.45,
          |	    "chargeDueDate": "2022-01-01",
          |     "lateSubmissions": [{
          |       "lateSubmissionID": "ID123",
          |       "taxPeriod": "1",
          |       "taxReturnStatus": "2",
          |       "taxPeriodStartDate": "2022-01-01",
          |       "taxPeriodEndDate": "2022-03-31",
          |       "taxPeriodDueDate": "2022-05-07",
          |       "returnReceiptDate": "2022-04-01"
          |     }]
          |   }]
          |  },
          |  "latePaymentPenalty": {
          |   "details": [{
          |	    "penaltyNumber": "1234ABCD",
          |	    "penaltyCategory": "LPP1",
          |   	"penaltyStatus": "P",
          |	    "penaltyAmountAccruing": 123.45,
          |	    "penaltyAmountPosted": 123.45,
          |	    "penaltyChargeCreationDate": "2022-01-01",
          |	    "penaltyChargeDueDate": "2022-02-01",
          |	    "communicationsDate": "2022-01-01",
          |	    "appealLevel": "1",
          |	    "appealStatus": "1",
          |	    "penaltyChargeReference": "CHARGE123456",
          |     "principalChargeDueDate": "2022-03-01",
          |     "principalChargeReference": "CHARGING12345"
          |   }]
          |  }
          |}
          |""".stripMargin)
      val model: GetPenaltyDetails = GetPenaltyDetails(
        lateSubmissionPenalty = Some(LSPData(
          summary = LSPSummary(
            activePenaltyPoints = 1,
            inactivePenaltyPoints = 2,
            regimeThreshold = 3,
            POCAchievementDate = LocalDate.of(2022, 1, 1),
            penaltyChargeAmount = 123.45
          ),
          details = Seq(LSPDetails(
            penaltyCategory = LSPPenaltyCategoryEnum.Point,
            penaltyNumber = "1234ABCD",
            penaltyOrder = "1",
            penaltyCreationDate = LocalDate.of(2022, 1, 1),
            penaltyExpiryDate = LocalDate.of(2024, 1, 1),
            penaltyStatus = LSPPenaltyStatusEnum.Active,
            appealStatus = Some("1"),
            communicationsDate = LocalDate.of(2022, 1, 1),
            appealLevel = Some("1"),
            chargeReference = Some("foobar"),
            chargeAmount = Some(123.45),
            chargeOutstandingAmount = Some(123.45),
            chargeDueDate = Some(LocalDate.of(2022, 1, 1)),
            lateSubmissions = Some(Seq(
              LateSubmission(
                lateSubmissionID = "ID123",
                taxPeriod = Some("1"),
                taxReturnStatus = "2",
                taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                taxPeriodEndDate = Some(LocalDate.of(2022, 3, 31)),
                taxPeriodDueDate = Some(LocalDate.of(2022, 5, 7)),
                returnReceiptDate = Some(LocalDate.of(2022, 4, 1))
              )
            ))
          ))
        )),
        latePaymentPenalty = Some(Seq(
          LatePaymentPenalty(
            penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
            penaltyNumber = "1234ABCD",
            penaltyChargeCreationDate = LocalDate.of(2022, 1, 1),
            penaltyChargeDueDate = LocalDate.of(2022, 2, 1),
            penaltyStatus = LPPPenaltyStatusEnum.Posted,
            appealStatus = Some("1"),
            communicationsDate = LocalDate.of(2022, 1, 1),
            principalChargeReference = "CHARGING12345",
            penaltyAmountAccruing = 123.45,
            penaltyAmountPosted = 123.45,
            penaltyChargeReference = "CHARGE123456",
            appealLevel = Some("1"),
            principalChargeDueDate = LocalDate.of(2022, 3, 1)
          )
        )))
      val result = Json.toJson(model)(GetPenaltyDetails.format)
      result shouldBe expectedResult
    }
  }
}
