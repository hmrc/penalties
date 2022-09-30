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

package models.getPenaltyDetails.lateSubmission

import base.SpecBase
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import play.api.libs.json.{JsResult, JsValue, Json}

import java.time.LocalDate

class LSPDetailsSpec extends SpecBase {
  val jsonRepresentingModel: JsValue = Json.parse(
    """
      |{
      |   "penaltyNumber": "12345678901234",
      |   "penaltyOrder": "01",
      |   "penaltyCategory": "P",
      |   "penaltyStatus": "ACTIVE",
      |   "FAPIndicator": "X",
      |   "penaltyCreationDate": "2022-10-30",
      |   "penaltyExpiryDate": "2022-10-30",
      |   "expiryReason": "FAP",
      |   "communicationsDate": "2022-10-30",
      |   "lateSubmissions": [
      |      {
      |        "taxPeriodStartDate": "2022-01-01",
      |        "taxPeriodEndDate": "2022-12-31",
      |        "taxPeriodDueDate": "2023-02-07",
      |        "returnReceiptDate": "2023-02-01",
      |        "taxReturnStatus": "Fulfilled"
      |      }
      |   ],
      |   "appealInformation": [
      |      {
      |        "appealStatus": "99",
      |        "appealLevel": "01"
      |      }
      |   ],
      |   "chargeDueDate": "2022-10-30",
      |   "chargeOutstandingAmount": 200,
      |   "chargeAmount": 200
      |}
      |""".stripMargin)

  val model: LSPDetails = LSPDetails(
    penaltyNumber = "12345678901234",
    penaltyOrder = "01",
    penaltyCategory = LSPPenaltyCategoryEnum.Point,
    penaltyStatus = LSPPenaltyStatusEnum.Active,
    penaltyCreationDate = LocalDate.of(2022, 10, 30),
    penaltyExpiryDate = LocalDate.of(2022, 10, 30),
    communicationsDate = LocalDate.of(2022, 10, 30),
    FAPIndicator = Some("X"),
    lateSubmissions = Some(
      Seq(
        LateSubmission(
          taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
          taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
          taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
          returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
          taxReturnStatus = TaxReturnStatusEnum.Fulfilled
        )
      )
    ),
    expiryReason = Some(ExpiryReasonEnum.Adjustment),
    appealInformation = Some(
      Seq(
        AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC))
      )
    ),
    chargeDueDate = Some(LocalDate.of(2022, 10, 30)),
    chargeOutstandingAmount = Some(200),
    chargeAmount = Some(200)
  )

  "be readable from JSON" in {
    val result: JsResult[LSPDetails] = Json.fromJson(jsonRepresentingModel)(LSPDetails.format)
    result.isSuccess shouldBe true
    result.get shouldBe model
  }

  "be writable to JSON" in {
    val result: JsValue = Json.toJson(model)(LSPDetails.format)
    result shouldBe jsonRepresentingModel
  }
}
