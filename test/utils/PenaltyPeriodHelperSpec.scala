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

  "sortByPenaltyStartDate" should {
    "return -1 when the first period is earlier than the second period" in {
      val earlierSubmission = LateSubmission(
        lateSubmissionID = "001",
        taxPeriod = Some("23AA"),
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None, taxReturnStatus = Some(TaxReturnStatusEnum.Open)
      )
      val laterSubmission = LateSubmission(
        lateSubmissionID = "001",
        taxPeriod = Some("23AA"),
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 2)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None, taxReturnStatus = Some(TaxReturnStatusEnum.Open)
      )
      val result = PenaltyPeriodHelper.sortByPenaltyStartDate(earlierSubmission, laterSubmission)
      result shouldBe -1
    }

    "return 0 when the first period is equal to the second period" in {
      val submission = LateSubmission(
        lateSubmissionID = "001",
        taxPeriod = Some("23AA"),
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None, taxReturnStatus = Some(TaxReturnStatusEnum.Open)
      )
      val result = PenaltyPeriodHelper.sortByPenaltyStartDate(submission, submission)
      result shouldBe 0
    }

    "return 1 when the first period is later than the second period" in {
      val earlierSubmission = LateSubmission(
        lateSubmissionID = "001",
        taxPeriod = Some("23AA"),
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None, taxReturnStatus = Some(TaxReturnStatusEnum.Open)
      )
      val laterSubmission = LateSubmission(
        lateSubmissionID = "001",
        taxPeriod = Some("23AA"),
        taxPeriodStartDate = Some(LocalDate.of(2022, 1, 2)), taxPeriodEndDate = None, taxPeriodDueDate = None, returnReceiptDate = None, taxReturnStatus = Some(TaxReturnStatusEnum.Open)
      )
      val result = PenaltyPeriodHelper.sortByPenaltyStartDate(laterSubmission, earlierSubmission)
      result shouldBe 1
    }
  }
}
