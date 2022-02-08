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

package models.v2.financialDetails

import play.api.libs.json.{Format, Json}

//NOTE: Although taxPayerDetails, balanceDetails and codingDetails are provided we do not use them at this point in time.
case class GetFinancialDetails(
                              documentDetails: Seq[DocumentDetails],
                              financialDetails: Seq[FinancialDetails]
                              )

object GetFinancialDetails {
  implicit val format: Format[GetFinancialDetails] = Json.format[GetFinancialDetails]
}
