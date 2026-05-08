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

package utils

import base.SpecBase
import models.getPenaltyDetails.lateSubmission.{LateSubmission, TaxReturnStatusEnum}

import java.time.LocalDate

class PenaltyPeriodHelperSpec extends SpecBase {

  private def submission(id: String, startDate: Option[LocalDate]): LateSubmission =
    LateSubmission(
      lateSubmissionID = id,
      incomeSource = Some("IT"),
      taxPeriod = Some("23AA"),
      taxPeriodStartDate = startDate,
      taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
      taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
      returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
      taxReturnStatus = Some(TaxReturnStatusEnum.Fulfilled)
    )

  "earliestSubmissionByPenaltyStartDate" should {
    "return the submission with the earliest start date" in {
      val earliest = submission("001", Some(LocalDate.of(2022, 1, 1)))
      val latest   = submission("002", Some(LocalDate.of(2023, 1, 1)))

      PenaltyPeriodHelper.earliestSubmissionByPenaltyStartDate(Seq(latest, earliest)) shouldBe Some(earliest)
    }

    "ignore submissions with missing start dates" in {
      val withStart    = submission("001", Some(LocalDate.of(2022, 1, 1)))
      val withoutStart = submission("002", None)

      PenaltyPeriodHelper.earliestSubmissionByPenaltyStartDate(Seq(withoutStart, withStart)) shouldBe Some(withStart)
    }

    "return None when no submissions have a start date" in {
      PenaltyPeriodHelper.earliestSubmissionByPenaltyStartDate(Seq(submission("001", None))) shouldBe None
    }

    "return None when no submissions are supplied" in {
      PenaltyPeriodHelper.earliestSubmissionByPenaltyStartDate(Seq.empty) shouldBe None
    }
  }
}
