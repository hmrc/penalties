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

package models.failure

import play.api.libs.json._

object FailureCodeEnum extends Enumeration {
  val NoDataFound: FailureCodeEnum.Value = Value("NO_DATA_FOUND")

  implicit val format: Format[FailureCodeEnum.Value] = new Format[FailureCodeEnum.Value] {
    override def writes(o: FailureCodeEnum.Value): JsValue = {
      JsString(o.toString)
    }

    override def reads(json: JsValue): JsResult[FailureCodeEnum.Value] = {
      json.as[String].toUpperCase match {
        case "NO_DATA_FOUND" => JsSuccess(NoDataFound)
        case e => JsError(s"$e not recognised")
      }
    }
  }
}
