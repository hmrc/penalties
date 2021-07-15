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

package models.payment

import models.appeals.AppealStatusEnum
import models.communication.Communication
import models.point.{PenaltyTypeEnum, PointStatusEnum}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class LatePaymentPenalty(
                         `type`: PenaltyTypeEnum.Value,
                         id: String,
                         reason: String,
                         dateCreated: LocalDateTime,
                         status: PointStatusEnum.Value,
                         appealStatus: Option[AppealStatusEnum.Value] = None,
                         period: PaymentPeriod,
                         communications: Seq[Communication],
                         financial: PaymentFinancial
                       )

object LatePaymentPenalty {
  implicit val format: OFormat[LatePaymentPenalty] = Json.format[LatePaymentPenalty]
}