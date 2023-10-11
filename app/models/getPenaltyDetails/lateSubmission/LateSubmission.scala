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

package models.getPenaltyDetails.lateSubmission

import play.api.libs.json.{JsValue, Json, Reads, Writes}

import java.time.LocalDate

case class LateSubmission(
                           lateSubmissionID: String,
                           taxPeriod: Option[String],
                           taxPeriodStartDate: Option[LocalDate],
                           taxPeriodEndDate: Option[LocalDate],
                           taxPeriodDueDate: Option[LocalDate],
                           returnReceiptDate: Option[LocalDate],
                           taxReturnStatus: Option[TaxReturnStatusEnum.Value]
                         )

object LateSubmission {
  implicit val reads: Reads[LateSubmission] = (json: JsValue) => for {
    lateSubmissionID <- (json \ "lateSubmissionID").validate[String]
    taxPeriod <- (json \ "taxPeriod").validateOpt[String]
    taxPeriodStartDate <- (json \ "taxPeriodStartDate").validateOpt[LocalDate]
    taxPeriodEndDate <- (json \ "taxPeriodEndDate").validateOpt[LocalDate]
    taxPeriodDueDate <- (json \ "taxPeriodDueDate").validateOpt[LocalDate]
    returnReceiptDate <- (json \ "returnReceiptDate").validateOpt[LocalDate]
    optTaxReturnStatus <- (json \ "taxReturnStatus").validateOpt[TaxReturnStatusEnum.Value]
  } yield {
    LateSubmission(lateSubmissionID, taxPeriod, taxPeriodStartDate, taxPeriodEndDate, taxPeriodDueDate, returnReceiptDate, optTaxReturnStatus)
  }

  implicit val writes: Writes[LateSubmission] = Json.writes[LateSubmission]
}
