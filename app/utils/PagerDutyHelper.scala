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

import utils.Logger.logger

object PagerDutyHelper {

  object PagerDutyKeys extends Enumeration {
    final val RECEIVED_4XX_FROM_1812_API = Value
    final val RECEIVED_5XX_FROM_1812_API = Value
    final val RECEIVED_4XX_FROM_1811_API = Value
    final val RECEIVED_5XX_FROM_1811_API = Value
    final val RECEIVED_4XX_FROM_1808_API = Value
    final val RECEIVED_5XX_FROM_1808_API = Value
    final val RECEIVED_4XX_FROM_1330_API = Value
    final val RECEIVED_5XX_FROM_1330_API = Value
    final val RECEIVED_4XX_FROM_FILE_NOTIFICATION_ORCHESTRATOR = Value
    final val RECEIVED_5XX_FROM_FILE_NOTIFICATION_ORCHESTRATOR = Value
    final val UNKNOWN_EXCEPTION_CALLING_1812_API = Value
    final val UNKNOWN_EXCEPTION_CALLING_1811_API = Value
    final val UNKNOWN_EXCEPTION_CALLING_1330_API = Value
    final val UNKNOWN_EXCEPTION_CALLING_1808_API = Value
    final val MALFORMED_RESPONSE_FROM_1812_API = Value
    final val MALFORMED_RESPONSE_FROM_1811_API = Value
    final val INVALID_JSON_RECEIVED_FROM_1811_API = Value
    final val INVALID_JSON_RECEIVED_FROM_1812_API = Value
    final val INVALID_JSON_RECEIVED_FROM_1808_API = Value
    final val INVALID_JSON_RECEIVED_FROM_1330_API = Value
  }

  def log(methodName: String, pagerDutyKey: PagerDutyKeys.Value): Unit = {
    logger.warn(s"$pagerDutyKey - $methodName")
  }

  def logStatusCode(methodName: String, code: Int)(keyOn4xx: PagerDutyKeys.Value, keyOn5xx: PagerDutyKeys.Value): Unit = {
    code match {
      case code if code >= 400 && code <= 499 => log(methodName, keyOn4xx)
      case code if code >= 500 => log(methodName, keyOn5xx)
      case _ =>
    }
  }
}
