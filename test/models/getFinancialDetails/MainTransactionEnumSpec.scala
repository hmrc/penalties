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

package models.getFinancialDetails

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class MainTransactionEnumSpec extends SpecBase {
  "be writable to JSON for 'VAT Return Charge' (4700)" in {
    val result = Json.toJson(MainTransactionEnum.VATReturnCharge)
    result shouldBe JsString("4700")
  }

  "be writable to JSON for 'VAT Return 1st LPP' (4703)" in {
    val result = Json.toJson(MainTransactionEnum.VATReturnFirstLPP)
    result shouldBe JsString("4703")
  }

  "be writable to JSON for 'VAT Return 2nd LPP' (4704)" in {
    val result = Json.toJson(MainTransactionEnum.VATReturnSecondLPP)
    result shouldBe JsString("4704")
  }

  "be writable to JSON for 'VAT Central Assessment' (4720)" in {
    val result = Json.toJson(MainTransactionEnum.CentralAssessment)
    result shouldBe JsString("4720")
  }

  "be writable to JSON for 'VAT Central Assessment 1st LPP' (4723)" in {
    val result = Json.toJson(MainTransactionEnum.CentralAssessmentFirstLPP)
    result shouldBe JsString("4723")
  }

  "be writable to JSON for 'VAT Central Assessment 2nd LPP' (4724)" in {
    val result = Json.toJson(MainTransactionEnum.CentralAssessmentSecondLPP)
    result shouldBe JsString("4724")
  }

  "be writable to JSON for 'VAT Officer's Assessment' (4730)" in {
    val result = Json.toJson(MainTransactionEnum.OfficersAssessment)
    result shouldBe JsString("4730")
  }

  "be writable to JSON for 'VAT Officer's Assessment 1st LPP' (4741)" in {
    val result = Json.toJson(MainTransactionEnum.OfficersAssessmentFirstLPP)
    result shouldBe JsString("4741")
  }

  "be writable to JSON for 'VAT Officer's Assessment 2nd LPP' (4742)" in {
    val result = Json.toJson(MainTransactionEnum.OfficersAssessmentSecondLPP)
    result shouldBe JsString("4742")
  }

  "be writable to JSON for 'VAT Error Correction' (4731)" in {
    val result = Json.toJson(MainTransactionEnum.ErrorCorrection)
    result shouldBe JsString("4731")
  }

  "be writable to JSON for 'VAT Error Correction 1st LPP' (4743)" in {
    val result = Json.toJson(MainTransactionEnum.ErrorCorrectionFirstLPP)
    result shouldBe JsString("4743")
  }

  "be writable to JSON for 'VAT Error Correction 2nd LPP' (4744)" in {
    val result = Json.toJson(MainTransactionEnum.ErrorCorrectionSecondLPP)
    result shouldBe JsString("4744")
  }

  "be writable to JSON for 'VAT Additional Assessment' (4732)" in {
    val result = Json.toJson(MainTransactionEnum.AdditionalAssessment)
    result shouldBe JsString("4732")
  }

  "be writable to JSON for 'VAT Additional Assessment 1st LPP' (4758)" in {
    val result = Json.toJson(MainTransactionEnum.AdditionalAssessmentFirstLPP)
    result shouldBe JsString("4758")
  }

  "be writable to JSON for 'VAT Additional Assessment 2nd LPP' (4759)" in {
    val result = Json.toJson(MainTransactionEnum.AdditionalAssessmentSecondLPP)
    result shouldBe JsString("4759")
  }

  "be writable to JSON for 'VAT Protective Assessment' (4733)" in {
    val result = Json.toJson(MainTransactionEnum.ProtectiveAssessment)
    result shouldBe JsString("4733")
  }

  "be writable to JSON for 'VAT Protective Assessment 1st LPP' (4761)" in {
    val result = Json.toJson(MainTransactionEnum.ProtectiveAssessmentFirstLPP)
    result shouldBe JsString("4761")
  }

  "be writable to JSON for 'VAT Protective Assessment 2nd LPP' (4762)" in {
    val result = Json.toJson(MainTransactionEnum.ProtectiveAssessmentSecondLPP)
    result shouldBe JsString("4762")
  }

  "be writable to JSON for 'VAT POA Return Charge' (4701)" in {
    val result = Json.toJson(MainTransactionEnum.POAReturnCharge)
    result shouldBe JsString("4701")
  }

  "be writable to JSON for 'VAT POA Return 1st LPP' (4716)" in {
    val result = Json.toJson(MainTransactionEnum.POAReturnChargeFirstLPP)
    result shouldBe JsString("4716")
  }

  "be writable to JSON for 'VAT POA Return 2nd LPP' (4717)" in {
    val result = Json.toJson(MainTransactionEnum.POAReturnChargeSecondLPP)
    result shouldBe JsString("4717")
  }

  "be writable to JSON for 'VAT AA Return Charge' (4702)" in {
    val result = Json.toJson(MainTransactionEnum.AAReturnCharge)
    result shouldBe JsString("4702")
  }

  "be writable to JSON for 'VAT AA Return Charge 1st LPP' (4718)" in {
    val result = Json.toJson(MainTransactionEnum.AAReturnChargeFirstLPP)
    result shouldBe JsString("4718")
  }

  "be writable to JSON for 'VAT AA Return Charge 2nd LPP' (4719)" in {
    val result = Json.toJson(MainTransactionEnum.AAReturnChargeSecondLPP)
    result shouldBe JsString("4719")
  }

  "be writable to JSON for 'VAT Manual LPP' (4787)" in {
    val result = Json.toJson(MainTransactionEnum.ManualLPP)
    result shouldBe JsString("4787")
  }

  "be writable to JSON for 'VAT Overpayment For Tax' (4764)" in {
    val result = Json.toJson(MainTransactionEnum.VATOverpaymentForTax)
    result shouldBe JsString("4764")
  }

  "be readable from JSON for 'VAT Return Charge' (4700)" in {
    val result = Json.fromJson(JsString("4700"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.VATReturnCharge
  }

  "be readable from JSON for 'VAT Return 1st LPP' (4703)" in {
    val result = Json.fromJson(JsString("4703"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.VATReturnFirstLPP
  }

  "be readable from JSON for 'VAT Return 2nd LPP' (4704)" in {
    val result = Json.fromJson(JsString("4704"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.VATReturnSecondLPP
  }

  "be readable from JSON for 'VAT Central Assessment' (4720)" in {
    val result = Json.fromJson(JsString("4720"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.CentralAssessment
  }

  "be readable from JSON for 'VAT Central Assessment 1st LPP' (4723)" in {
    val result = Json.fromJson(JsString("4723"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.CentralAssessmentFirstLPP
  }

  "be readable from JSON for 'VAT Central Assessment 2nd LPP' (4724)" in {
    val result = Json.fromJson(JsString("4724"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.CentralAssessmentSecondLPP
  }

  "be readable from JSON for 'VAT Officer's Assessment' (4730)" in {
    val result = Json.fromJson(JsString("4730"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.OfficersAssessment
  }

  "be readable from JSON for 'VAT Office's Assessment 1st LPP' (4741)" in {
    val result = Json.fromJson(JsString("4741"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.OfficersAssessmentFirstLPP
  }

  "be readable from JSON for 'VAT Officer's Assessment 2nd LPP' (4742)" in {
    val result = Json.fromJson(JsString("4742"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.OfficersAssessmentSecondLPP
  }

  "be readable from JSON for 'VAT Error Correction' (4731)" in {
    val result = Json.fromJson(JsString("4731"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.ErrorCorrection
  }

  "be readable from JSON for 'VAT Error Correction 1st LPP' (4743)" in {
    val result = Json.fromJson(JsString("4743"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.ErrorCorrectionFirstLPP
  }

  "be readable from JSON for 'VAT Error Correction 2nd LPP' (4744)" in {
    val result = Json.fromJson(JsString("4744"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.ErrorCorrectionSecondLPP
  }

  "be readable from JSON for 'VAT Additional Assessment' (4732)" in {
    val result = Json.fromJson(JsString("4732"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.AdditionalAssessment
  }

  "be readable from JSON for 'VAT Additional Assessment 1st LPP' (4758)" in {
    val result = Json.fromJson(JsString("4758"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.AdditionalAssessmentFirstLPP
  }

  "be readable from JSON for 'VAT Additional Assessment 2nd LPP' (4759)" in {
    val result = Json.fromJson(JsString("4759"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.AdditionalAssessmentSecondLPP
  }

  "be readable from JSON for 'VAT Protective Assessment' (4733)" in {
    val result = Json.fromJson(JsString("4733"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.ProtectiveAssessment
  }

  "be readable from JSON for 'VAT Protective Assessment 1st LPP' (4761)" in {
    val result = Json.fromJson(JsString("4761"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.ProtectiveAssessmentFirstLPP
  }

  "be readable from JSON for 'VAT Protective Assessment 2nd LPP' (4762)" in {
    val result = Json.fromJson(JsString("4762"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.ProtectiveAssessmentSecondLPP
  }

  "be readable from JSON for 'VAT POA Return Charge' (4701)" in {
    val result = Json.fromJson(JsString("4701"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.POAReturnCharge
  }

  "be readable from JSON for 'VAT POA Return 1st LPP' (4716)" in {
    val result = Json.fromJson(JsString("4716"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.POAReturnChargeFirstLPP
  }

  "be readable from JSON for 'VAT POA Return 2nd LPP' (4717)" in {
    val result = Json.fromJson(JsString("4717"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.POAReturnChargeSecondLPP
  }

  "be readable from JSON for 'VAT AA Return Charge' (4702)" in {
    val result = Json.fromJson(JsString("4702"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.AAReturnCharge
  }

  "be readable from JSON for 'VAT AA Return Charge 1st LPP' (4718)" in {
    val result = Json.fromJson(JsString("4718"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.AAReturnChargeFirstLPP
  }

  "be readable from JSON for 'VAT AA Return Charge 2nd LPP' (4719)" in {
    val result = Json.fromJson(JsString("4719"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.AAReturnChargeSecondLPP
  }

  "be readable from JSON for 'VAT Manual LPP' (4787)" in {
    val result = Json.fromJson(JsString("4787"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.ManualLPP
  }

  "be readable from JSON for 'VAT Overpayment For Tax' (4764)" in {
    val result = Json.fromJson(JsString("4764"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.VATOverpaymentForTax
  }

  "return Unknown when the enum is not recognised" in {
    val result = Json.fromJson(JsString("5032"))(MainTransactionEnum.format)
    result.get shouldBe MainTransactionEnum.Unknown
  }
}
