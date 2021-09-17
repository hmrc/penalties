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

import play.api.libs.json._

object ReturnStatusEnum extends Enumeration {
  val submitted: ReturnStatusEnum.Value = Value

  implicit val format: Format[ReturnStatusEnum.Value] = new Format[ReturnStatusEnum.Value] {
    override def writes(o: ReturnStatusEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[ReturnStatusEnum.Value] = {
      json.as[String].toUpperCase match {
        case "SUBMITTED" => JsSuccess(submitted)
        case e => JsError(s"$e not recognised")
      }
    }
  }
}
