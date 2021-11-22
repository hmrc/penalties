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

import play.api.libs.json.{JsResult, JsValue, Json, OFormat, OWrites, Reads}

//TODO: rename this model when we switch to new API
case class CompliancePayloadObligationAPI(
                                            identification: ObligationIdentification,
                                            obligationDetails: Seq[ObligationDetail]
                                         )

object CompliancePayloadObligationAPI {
  implicit val writes: OWrites[CompliancePayloadObligationAPI] = Json.writes[CompliancePayloadObligationAPI]
  implicit val reads: Reads[CompliancePayloadObligationAPI] = Json.reads[CompliancePayloadObligationAPI]

  val seqReads: Reads[Seq[CompliancePayloadObligationAPI]] = new Reads[Seq[CompliancePayloadObligationAPI]] {
    override def reads(json: JsValue): JsResult[Seq[CompliancePayloadObligationAPI]] = {
      (json \ "obligations").validate[Seq[CompliancePayloadObligationAPI]](Reads.seq[CompliancePayloadObligationAPI])
    }
  }
}