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

package utils

import play.api.libs.json._
import uk.gov.hmrc.emailaddress.EmailAddress

object EmailAddressFormatter {

  val oReads: Reads[Option[EmailAddress]] = JsPath.readNullable[String].map(_.map(EmailAddress(_)))
  val oWrites: Writes[Option[EmailAddress]] = Writes {
    case Some(x) => JsString(x.value)
    case None => JsNull
  }

  val reads: Reads[EmailAddress] = JsPath.read[String].map(EmailAddress(_))
  val writes: Writes[EmailAddress] = Writes { model => JsString(model.value) }
}
