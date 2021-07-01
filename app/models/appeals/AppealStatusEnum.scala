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

package models.appeals

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

object AppealStatusEnum extends Enumeration {

  val Under_Review = Value
  val Accepted = Value
  val Rejected = Value

  implicit val format: Format[AppealStatusEnum.Value] = new Format[AppealStatusEnum.Value] {
    override def writes(o: AppealStatusEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[AppealStatusEnum.Value] = {
      json.as[String].toUpperCase match {
        case "UNDER_REVIEW" => JsSuccess(Under_Review)
        case "ACCEPTED" => JsSuccess(Accepted)
        case "REJECTED" => JsSuccess(Rejected)
        case e => JsError(s"$e not recognised")
      }
    }
  }

}