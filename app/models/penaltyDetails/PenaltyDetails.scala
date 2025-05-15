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

package models.penaltyDetails

import models.penaltyDetails.breathingSpace.BreathingSpace
import models.penaltyDetails.latePayment.LatePaymentPenalty
import models.penaltyDetails.lateSubmission.LateSubmissionPenalty
import play.api.libs.json.{Format, Json}
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.time.Instant
import scala.util.{Try, Success, Failure}

case class PenaltyDetails(
    processingDate: Instant,
    totalisations: Option[Totalisations],
    lateSubmissionPenalty: Option[LateSubmissionPenalty],
    latePaymentPenalty: Option[LatePaymentPenalty],
    breathingSpace: Option[Seq[BreathingSpace]]
)

object PenaltyDetails {
  implicit val instantReads: Reads[Instant] = Reads[Instant] {
    case JsString(s) =>
      Try(Instant.parse(s)) match {
        case Success(instant) => JsSuccess(instant)
        case Failure(_)       => JsError(s"Invalid Instant format: $s")
      }
    case _ => JsError("Expected a string for Instant")
  }
  implicit val getPenaltyDetailsReads: Reads[PenaltyDetails] = (
    (JsPath \ "success" \ "processingDate").read[Instant](instantReads) and
      (JsPath \ "success" \ "penaltyData" \ "totalisations")
        .readNullable[Totalisations] and
      (JsPath \ "success" \ "penaltyData" \ "lsp")
        .readNullable[LateSubmissionPenalty] and
      (JsPath \ "success" \ "penaltyData" \ "lpp")
        .readNullable[LatePaymentPenalty] and
      (JsPath \ "success" \ "penaltyData" \ "breathingSpace")
        .readNullable[Seq[BreathingSpace]]
  )(PenaltyDetails.apply _)

  implicit val writes: Writes[PenaltyDetails] = Writes { pd =>
    Json.obj(
      "success" -> Json.obj(
        "processingDate" -> pd.processingDate,
        "penaltyData" -> Json.obj(
          "totalisations" -> pd.totalisations,
          "lsp" -> pd.lateSubmissionPenalty,
          "lpp" -> pd.latePaymentPenalty,
          "breathingSpace" -> pd.breathingSpace
        )
      )
    )
  }

  implicit val format: Format[PenaltyDetails] =
    Format(getPenaltyDetailsReads, writes)

}
