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

import base.SpecBase
import models.penalty.PenaltyPeriod
import models.submission.{Submission, SubmissionStatusEnum}
import models.v3.getPenaltyDetails.lateSubmission.LateSubmission

import java.time.{LocalDate, LocalDateTime}

class PenaltyPeriodHelperSpec extends SpecBase {

  val penaltyPeriods = Seq(PenaltyPeriod(
    startDate = LocalDateTime.of(2023, 1, 16, 0, 0, 0),
    endDate = LocalDateTime.of(2023, 1, 31, 0, 0, 0),
    submission = Submission(
      dueDate = LocalDateTime.of(2023, 5, 23, 0, 0, 0),
      submittedDate = Some(LocalDateTime.of(2023, 5, 25, 0, 0, 0)),
      status = SubmissionStatusEnum.Submitted
    )),
    PenaltyPeriod(
      startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
      endDate = LocalDateTime.of(2023, 1, 15, 0, 0, 0),
      submission = Submission(
        dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
        submittedDate = Some(LocalDateTime.of(2023, 5, 12, 0, 0, 0)),
        status = SubmissionStatusEnum.Under_Review
      ))
  )

  "sortedPenaltyPeriod" should {
    "return sorted Penalty Period with oldest startDate " in {
      val sortedPenaltyPeriod = PenaltyPeriodHelper.sortedPenaltyPeriod(penaltyPeriods)
      sortedPenaltyPeriod.head.startDate.toLocalDate shouldBe LocalDate.of(2023, 1, 1)
      sortedPenaltyPeriod.head.endDate.toLocalDate shouldBe LocalDate.of(2023, 1, 15)
    }
  }

  "sortByPenaltyStartDate" should {
    "return -1 when the first period is earlier than the second period" in {
      val earlierSubmission = LateSubmission(
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None
      )
      val laterSubmission = LateSubmission(
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 2)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None
      )
      val result = PenaltyPeriodHelper.sortByPenaltyStartDate(earlierSubmission, laterSubmission)
      result shouldBe -1
    }

    "return 0 when the first period is equal to the second period" in {
      val submission = LateSubmission(
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None
      )
      val result = PenaltyPeriodHelper.sortByPenaltyStartDate(submission, submission)
      result shouldBe 0
    }

    "return 1 when the first period is later than the second period" in {
      val earlierSubmission = LateSubmission(
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None
      )
      val laterSubmission = LateSubmission(
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 2)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None
      )
      val result = PenaltyPeriodHelper.sortByPenaltyStartDate(laterSubmission, earlierSubmission)
      result shouldBe 1
    }
  }
}
