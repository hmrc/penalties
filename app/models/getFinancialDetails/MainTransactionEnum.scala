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

import play.api.libs.json._

object MainTransactionEnum extends Enumeration {
  val VATReturnCharge: MainTransactionEnum.Value = Value("4700")
  val VATReturnFirstLPP: MainTransactionEnum.Value = Value("4703")
  val VATReturnSecondLPP: MainTransactionEnum.Value = Value("4704")
  val CentralAssessment: MainTransactionEnum.Value = Value("4720")
  val CentralAssessmentFirstLPP: MainTransactionEnum.Value = Value("4723")
  val CentralAssessmentSecondLPP: MainTransactionEnum.Value = Value("4724")
  val OfficersAssessment: MainTransactionEnum.Value = Value("4730")
  val OfficersAssessmentFirstLPP: MainTransactionEnum.Value = Value("4741")
  val OfficersAssessmentSecondLPP: MainTransactionEnum.Value = Value("4742")
  val ErrorCorrection: MainTransactionEnum.Value = Value("4731")
  val ErrorCorrectionFirstLPP: MainTransactionEnum.Value = Value("4743")
  val ErrorCorrectionSecondLPP: MainTransactionEnum.Value = Value("4744")
  val AdditionalAssessment: MainTransactionEnum.Value = Value("4732")
  val AdditionalAssessmentFirstLPP: MainTransactionEnum.Value = Value("4758")
  val AdditionalAssessmentSecondLPP: MainTransactionEnum.Value = Value("4759")
  val ProtectiveAssessment: MainTransactionEnum.Value = Value("4733")
  val ProtectiveAssessmentFirstLPP: MainTransactionEnum.Value = Value("4761")
  val ProtectiveAssessmentSecondLPP: MainTransactionEnum.Value = Value("4762")
  val POAReturnCharge: MainTransactionEnum.Value = Value("4701")
  val POAReturnChargeFirstLPP: MainTransactionEnum.Value = Value("4716")
  val POAReturnChargeSecondLPP: MainTransactionEnum.Value = Value("4717")
  val AAReturnCharge: MainTransactionEnum.Value = Value("4702")
  val AAReturnChargeFirstLPP: MainTransactionEnum.Value = Value("4718")
  val AAReturnChargeSecondLPP: MainTransactionEnum.Value = Value("4719")
  val ManualLPP: MainTransactionEnum.Value = Value("4787")
  val Unknown: MainTransactionEnum.Value = Value("4700")
  val VATOverpaymentForTax: MainTransactionEnum.Value = Value("4764")

  implicit val format: Format[MainTransactionEnum.Value] = new Format[MainTransactionEnum.Value] {
    override def writes(o: MainTransactionEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[MainTransactionEnum.Value] = {
      json.as[String].toUpperCase match {
        case "4700" => JsSuccess(VATReturnCharge)
        case "4703" => JsSuccess(VATReturnFirstLPP)
        case "4704" => JsSuccess(VATReturnSecondLPP)
        case "4720" => JsSuccess(CentralAssessment)
        case "4723" => JsSuccess(CentralAssessmentFirstLPP)
        case "4724" => JsSuccess(CentralAssessmentSecondLPP)
        case "4730" => JsSuccess(OfficersAssessment)
        case "4741" => JsSuccess(OfficersAssessmentFirstLPP)
        case "4742" => JsSuccess(OfficersAssessmentSecondLPP)
        case "4731" => JsSuccess(ErrorCorrection)
        case "4743" => JsSuccess(ErrorCorrectionFirstLPP)
        case "4744" => JsSuccess(ErrorCorrectionSecondLPP)
        case "4732" => JsSuccess(AdditionalAssessment)
        case "4758" => JsSuccess(AdditionalAssessmentFirstLPP)
        case "4759" => JsSuccess(AdditionalAssessmentSecondLPP)
        case "4733" => JsSuccess(ProtectiveAssessment)
        case "4761" => JsSuccess(ProtectiveAssessmentFirstLPP)
        case "4762" => JsSuccess(ProtectiveAssessmentSecondLPP)
        case "4701" => JsSuccess(POAReturnCharge)
        case "4716" => JsSuccess(POAReturnChargeFirstLPP)
        case "4717" => JsSuccess(POAReturnChargeSecondLPP)
        case "4702" => JsSuccess(AAReturnCharge)
        case "4718" => JsSuccess(AAReturnChargeFirstLPP)
        case "4719" => JsSuccess(AAReturnChargeSecondLPP)
        case "4787" => JsSuccess(ManualLPP)
        case "4764" => JsSuccess(VATOverpaymentForTax)
        case e => JsSuccess(Unknown)
      }
    }
  }
}
