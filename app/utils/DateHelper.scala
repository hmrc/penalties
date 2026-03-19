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

import config.featureSwitches.FeatureSwitching
import play.api.Configuration
import utils.Logger.logger

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import javax.inject.Inject

class DateHelper @Inject() (val config: Configuration) extends FeatureSwitching {

  def dateNow(): LocalDate = getTimeMachineDateTime.toLocalDate

  def formattedHipReceiptTimestamp(): String = {
    val instant =
      getTimeMachineDateTime
        .atZone(ZoneId.systemDefault())
        .toInstant
        .truncatedTo(ChronoUnit.SECONDS)

    val localDateNow  = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    val formattedDate = DateTimeFormatter.ISO_INSTANT.format(instant)
    logger.info(s"[DateHelper][formattedHipReceiptTimestamp] LocalDate.now = $localDateNow, Formatted TimeMachine date = $formattedDate")

    formattedDate
  }

}

object DateHelper {
  def isDateBeforeOrEqual(thisDate: LocalDate, thatDate: LocalDate): Boolean =
    thisDate.isBefore(thatDate) || thisDate.isEqual(thatDate)

  def isDateAfterOrEqual(thisDate: LocalDate, thatDate: LocalDate): Boolean =
    thisDate.isAfter(thatDate) || thisDate.isEqual(thatDate)

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
}
