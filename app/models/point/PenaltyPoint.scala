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

package models.point

import models.communication.Communication
import models.financial.Financial
import models.penalty.PenaltyPeriod
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class PenaltyPoint(
                         `type`: PenaltyTypeEnum.Value,
                         number: String,
                         dateCreated: LocalDateTime,
                         dateExpired: Option[LocalDateTime] = None,
                         status: PointStatusEnum.Value,
                         reason: Option[String],
                         period: Option[PenaltyPeriod] = None,
                         communications: Seq[Communication],
                         financial: Option[Financial] = None
                       )

object PenaltyPoint {
  implicit val format: OFormat[PenaltyPoint] = Json.format[PenaltyPoint]
}
