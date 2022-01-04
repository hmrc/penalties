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

package models.submission

import play.api.libs.json._

object SubmissionStatusEnum extends Enumeration {
  val Submitted: SubmissionStatusEnum.Value = Value
  val Overdue: SubmissionStatusEnum.Value = Value
  val Under_Review: SubmissionStatusEnum.Value = Value
  val Tax_Tribunal: SubmissionStatusEnum.Value = Value

  implicit val format: Format[SubmissionStatusEnum.Value] = new Format[SubmissionStatusEnum.Value] {
    override def writes(o: SubmissionStatusEnum.Value): JsValue = {
      JsString(o.toString.toUpperCase)
    }

    override def reads(json: JsValue): JsResult[SubmissionStatusEnum.Value] = {
      json.as[String] match {
        case "SUBMITTED" => JsSuccess(Submitted)
        case "OVERDUE" => JsSuccess(Overdue)
        case "UNDER_REVIEW" => JsSuccess(Under_Review)
        case "TAX_TRIBUNAL" => JsSuccess(Tax_Tribunal)
        case e => JsError(s"$e not recognised")
      }
    }
  }
}
