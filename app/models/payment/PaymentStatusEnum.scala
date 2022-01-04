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

package models.payment

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

object PaymentStatusEnum extends Enumeration {

  val Paid: PaymentStatusEnum.Value = Value
  val Due: PaymentStatusEnum.Value = Value

  implicit val format: Format[PaymentStatusEnum.Value] = new Format[PaymentStatusEnum.Value] {
    override def writes(o: PaymentStatusEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[PaymentStatusEnum.Value] = {
      json.as[String].toUpperCase match {
        case "PAID" => JsSuccess(Paid)
        case "DUE" => JsSuccess(Due)
        case e => JsError(s"$e not recognised")
      }
    }
  }
}
