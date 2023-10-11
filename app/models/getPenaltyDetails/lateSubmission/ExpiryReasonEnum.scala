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

package models.getPenaltyDetails.lateSubmission

import play.api.libs.json._

object ExpiryReasonEnum extends Enumeration {
  val Appeal: ExpiryReasonEnum.Value = Value("APP")
  val Adjustment: ExpiryReasonEnum.Value = Value("FAP")
  val Reversal: ExpiryReasonEnum.Value = Value("ICR")
  val Manual: ExpiryReasonEnum.Value = Value("MAN")
  val NaturalExpiration: ExpiryReasonEnum.Value = Value("NAT")
  val SubmissionOnTime: ExpiryReasonEnum.Value = Value("NLT")
  val Compliance: ExpiryReasonEnum.Value = Value("POC")
  val Reset: ExpiryReasonEnum.Value = Value("RES")

  implicit val format: Format[ExpiryReasonEnum.Value] = new Format[ExpiryReasonEnum.Value] {

    override def writes(o: ExpiryReasonEnum.Value): JsValue = JsString(o.toString.toUpperCase)

    override def reads(json: JsValue): JsResult[ExpiryReasonEnum.Value] = json.as[String].toUpperCase match {
      case "APP" => JsSuccess(Appeal)
      case "FAP" => JsSuccess(Adjustment)
      case "ICR" => JsSuccess(Reversal)
      case "MAN" => JsSuccess(Manual)
      case "NAT" => JsSuccess(NaturalExpiration)
      case "NLT" => JsSuccess(SubmissionOnTime)
      case "POC" => JsSuccess(Compliance)
      case "RES" => JsSuccess(Reset)
      case e => JsError(s"$e not recognised")
    }
  }
}
