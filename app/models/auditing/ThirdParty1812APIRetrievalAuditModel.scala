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

package models.auditing

import play.api.libs.json.JsValue
import utils.JsonUtils

case class ThirdParty1812APIRetrievalAuditModel(
                                            vrn: String,
                                            responseCode: Int,
                                            responseBody: JsValue
                                          ) extends JsonAuditModel with JsonUtils {
  override val auditType: String = "Penalties3rdPartyPenaltyDetailsDataRetrieval"
  override val transactionName: String = "penalty-penalty-data-retrieval"

  override val detail: JsValue = jsonObjNoNulls(
    "vrn" -> vrn,
    "responseCodeSentAPIService" -> responseCode,
    "etmp-response" -> responseBody
  )
}
