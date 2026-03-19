/*
 * Copyright 2026 HM Revenue & Customs
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
import org.scalatest.BeforeAndAfterEach
import play.api.Configuration

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime}

class DateHelperSpec extends SpecBase with BeforeAndAfterEach {

  private val TIME_MACHINE_NOW = "TIME_MACHINE_NOW"

  private val emptyConfig = Configuration.empty

  override def afterEach(): Unit =
    sys.props -= TIME_MACHINE_NOW

  "dateNow" should {
    "return today's date when no time machine is set" in {
      val dateHelper = new DateHelper(emptyConfig)

      val expected = LocalDate.now()

      dateHelper.dateNow() shouldBe expected
    }

    "return the time machine date when system property is set" in {
      val fixedDateTime = LocalDateTime.of(2025, 1, 1, 12, 30, 45)

      sys.props += TIME_MACHINE_NOW -> fixedDateTime.toString

      val dateHelper = new DateHelper(emptyConfig)

      dateHelper.dateNow() shouldBe LocalDate.of(2025, 1, 1)
    }
  }

  "formattedHipReceiptTimestamp" should {
    "return the formatted DateTime - truncated to seconds - when no time machine is set" in {
      val dateHelper = new DateHelper(emptyConfig)

      val expectedInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString

      dateHelper.formattedHipReceiptTimestamp() shouldBe expectedInstant
    }
    "return the formatted time machine DateTime - truncated to seconds - when system property is set" in {
      val fixedDateTime = LocalDateTime.of(2025, 1, 1, 12, 30, 45, 999999999)

      sys.props += TIME_MACHINE_NOW -> fixedDateTime.toString

      val dateHelper = new DateHelper(emptyConfig)

      val expectedInstant = "2025-01-01T12:30:45Z"

      dateHelper.formattedHipReceiptTimestamp() shouldBe expectedInstant
    }
  }

  "isDateBeforeOrEqual" should {
    "return true" when {
      "this date is before that date" in {
        val thisDate = LocalDate.of(2022, 1, 1)
        val thatDate = LocalDate.of(2022, 2, 1)
        DateHelper.isDateBeforeOrEqual(thisDate, thatDate) shouldBe true
      }

      "this date is equal to that date" in {
        val thisDate = LocalDate.of(2022, 1, 1)
        val thatDate = LocalDate.of(2022, 1, 1)
        DateHelper.isDateBeforeOrEqual(thisDate, thatDate) shouldBe true
      }
    }

    "return false" when {
      "this date is after that date" in {
        val thisDate = LocalDate.of(2022, 2, 1)
        val thatDate = LocalDate.of(2022, 1, 1)
        DateHelper.isDateBeforeOrEqual(thisDate, thatDate) shouldBe false
      }
    }
  }

  "isDateAfterOrEqual" should {
    "return true" when {
      "this date is after that date" in {
        val thisDate = LocalDate.of(2022, 2, 1)
        val thatDate = LocalDate.of(2022, 1, 1)
        DateHelper.isDateAfterOrEqual(thisDate, thatDate) shouldBe true
      }

      "this date is equal to that date" in {
        val thisDate = LocalDate.of(2022, 1, 1)
        val thatDate = LocalDate.of(2022, 1, 1)
        DateHelper.isDateAfterOrEqual(thisDate, thatDate) shouldBe true
      }
    }

    "return false" when {
      "this date is before that date" in {
        val thisDate = LocalDate.of(2022, 1, 1)
        val thatDate = LocalDate.of(2022, 2, 1)
        DateHelper.isDateAfterOrEqual(thisDate, thatDate) shouldBe false
      }
    }
  }

}
