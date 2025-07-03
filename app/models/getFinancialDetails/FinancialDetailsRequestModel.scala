/*
 * Copyright 2025 HM Revenue & Customs
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

package models.getFinancialDetails

import models.getFinancialDetails.totalisation.FinancialDetailsTotalisation
import play.api.libs.json.{Format, Json}

case class FinancialDetailsRequestModel (searchType: Option[String],
                                         searchItem: Option[String],
                                         dateType: Option[String],
                                         dateFrom: Option[String],
                                         dateTo: Option[String],
                                         includeClearedItems: Option[Boolean],
                                         includeStatisticalItems: Option[Boolean],
                                         includePaymentOnAccount: Option[Boolean],
                                         addRegimeTotalisation: Option[Boolean],
                                         addLockInformation: Option[Boolean],
                                         addPenaltyDetails: Option[Boolean],
                                         addPostedInterestDetails: Option[Boolean],
                                         addAccruingInterestDetails: Option[Boolean])

object FinancialDetailsRequestModel {
  implicit val format: Format[FinancialDetailsRequestModel] = Json.format[FinancialDetailsRequestModel]
}
