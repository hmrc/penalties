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

package models.point

import play.api.libs.json._

object PenaltyTypeEnum extends Enumeration {

  val Financial = Value
  val Point = Value

  implicit val format: Format[PenaltyTypeEnum.Value] = new Format[PenaltyTypeEnum.Value] {
    override def writes(o: PenaltyTypeEnum.Value): JsValue = {
      JsString(o.toString.toLowerCase)
    }

    override def reads(json: JsValue): JsResult[PenaltyTypeEnum.Value] = {
      json.as[String] match {
        case "point" => JsSuccess(Point)
        case "financial" => JsSuccess(Financial)
        case e => JsError(s"$e not recognised")
      }
    }
  }
}
