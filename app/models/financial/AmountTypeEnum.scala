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

package models.financial

import play.api.libs.json._

object AmountTypeEnum extends Enumeration {
  val VAT: AmountTypeEnum.Value = Value
  val Central_Assessment: AmountTypeEnum.Value = Value
  val Officers_Assessment: AmountTypeEnum.Value = Value
  val ECN: AmountTypeEnum.Value = Value

  implicit val format: Format[AmountTypeEnum.Value] = new Format[AmountTypeEnum.Value] {
    override def writes(o: AmountTypeEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[AmountTypeEnum.Value] = {
      json.as[String] match {
        case "VAT" => JsSuccess(VAT)
        case "CENTRAL_ASSESSMENT" => JsSuccess(Central_Assessment)
        case "OFFICERS_ASSESSMENT" => JsSuccess(Officers_Assessment)
        case "ECN" => JsSuccess(ECN)
        case e => JsError(s"$e not recognised")
      }
    }
  }
}
