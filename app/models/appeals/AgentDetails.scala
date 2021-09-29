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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.emailaddress.EmailAddress
import utils.EmailAddressFormatter._

case class AgentDetails (
                     agentReferenceNo: String,
                     name: String,
                     addressLine1: String,
                     addressLine2: Option[String],
                     addressLine3: Option[String],
                     addressLine4: Option[String],
                     addressLine5: Option[String],
                     postCode: String,
                     agentEmailID: Option[EmailAddress]
                   )

object AgentDetails{
  implicit val oFmt: Format[Option[EmailAddress]] = Format(oReads, oWrites)
  implicit val fmt: Format[EmailAddress] = Format(reads, writes)
  implicit val format: Format[AgentDetails] = Json.format[AgentDetails]
}