/*
 * Copyright 2024 HM Revenue & Customs
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

import models.EnrolmentKey
import play.api.libs.json.{JsString, JsValue, Json}
import utils.JsonUtils

import scala.util.Try

case class ThirdPartyAPI1811RetrievalRegimeAuditModel(enrolmentKey: EnrolmentKey,
                                                      responseCode: Int,
                                                      responseBody: String) extends JsonAuditModel with JsonUtils {

  override val auditType: String = "Penalties3rdPartyFinancialPenaltyDetailsDataRetrieval"
  override val transactionName: String = "penalty-financial-penalty-data-retrieval"
  val response: JsValue = Try(Json.parse(responseBody)).getOrElse(JsString(responseBody))

  override val detail: JsValue = jsonObjNoNulls(
    enrolmentKey.keyType.name.toLowerCase -> enrolmentKey.key,
    "responseCodeSentAPIService" -> responseCode,
    "etmp-response" -> response
  )
}
