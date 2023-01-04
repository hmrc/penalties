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

package services.auditing

import config.AppConfig
import models.auditing.JsonAuditModel
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AuditService @Inject()(auditConnector: AuditConnector,
                             appConfig: AppConfig) {

  def audit(dataSource: JsonAuditModel)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Unit =
    auditConnector.sendExtendedEvent(toExtendedDataEvent(dataSource, request.path))

  def toExtendedDataEvent(auditModel: JsonAuditModel, path: String)(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    val event = ExtendedDataEvent(
      auditSource = appConfig.appName,
      auditType = auditModel.auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(auditModel.transactionName, path),
      detail = auditModel.detail
    )
    logger.debug(s"[AuditService][toExtendedDataEvent] Audit Event: $event")
    event
  }
}
