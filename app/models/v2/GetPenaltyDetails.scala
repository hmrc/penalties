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

package models.v2

import models.v2.latePaymentPenalty.LatePaymentPenalty
import models.v2.lateSubmissionPenalty.LSPData
import play.api.libs.json.{Format, JsResult, JsValue, Json}
import utils.JsonUtils

case class GetPenaltyDetails(
                              lateSubmissionPenalty: Option[LSPData],
                              latePaymentPenalty: Option[Seq[LatePaymentPenalty]]
                            )

object GetPenaltyDetails extends JsonUtils {
  implicit val format: Format[GetPenaltyDetails] = new Format[GetPenaltyDetails] {
    override def reads(json: JsValue): JsResult[GetPenaltyDetails] = {
      for {
        latePaymentPenalty <- (json \ "latePaymentPenalty" \ "details").validateOpt[Seq[LatePaymentPenalty]]
        lateSubmissionPenalty <- (json \ "lateSubmissionPenalty").validateOpt[LSPData]
      } yield {
        GetPenaltyDetails(lateSubmissionPenalty, latePaymentPenalty)
      }
    }

    override def writes(details: GetPenaltyDetails): JsValue = {
      jsonObjNoNulls(
        "lateSubmissionPenalty" -> details.lateSubmissionPenalty,
        if(details.latePaymentPenalty.isDefined) "latePaymentPenalty" -> Json.obj("details" -> details.latePaymentPenalty) else "latePaymentPenalty" -> Json.obj()
      )
    }
  }
}
