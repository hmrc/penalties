/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json._

sealed abstract class AppealLevel(val value: String) {
  override val toString: String = value
}

object AppealLevel {
  case object FirstStageAppeal extends AppealLevel("01")

  case object SecondStageAppeal extends AppealLevel("02")

  val values: Seq[AppealLevel] = Seq(FirstStageAppeal, SecondStageAppeal)

  def fromString(str: String): Option[AppealLevel] =
    values.find(_.value == str)

  implicit val format: Format[AppealLevel] = new Format[AppealLevel] {
    def writes(o: AppealLevel): JsValue = JsString(o.value)

    def reads(json: JsValue): JsResult[AppealLevel] = json match {
      case JsString(str) =>
        fromString(str) match {
          case Some(enum) => JsSuccess(enum)
          case None       => JsError(s"AppealLevel '$str' not recognised")
        }
      case _ => JsError("String value expected for AppealLevel")
    }
  }

}
