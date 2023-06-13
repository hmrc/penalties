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

package models.upload

import play.api.libs.json._

object FailureReasonEnum extends Enumeration {
  val QUARANTINE: FailureReasonEnum.Value = Value
  val REJECTED: FailureReasonEnum.Value = Value
  val UNKNOWN: FailureReasonEnum.Value = Value
  val DUPLICATE: FailureReasonEnum.Value = Value

  def getEnumFromString(s: String): Option[Value] = values.find(_.toString == s)

  implicit val format: Format[FailureReasonEnum.Value] = new Format[FailureReasonEnum.Value] {
    override def writes(o: FailureReasonEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[FailureReasonEnum.Value] = {
      getEnumFromString(json.as[String].toUpperCase) match {
        case Some(v) => JsSuccess(v)
        case e => JsError(s"$e Failure reason not recognised")
      }
    }
  }
}
