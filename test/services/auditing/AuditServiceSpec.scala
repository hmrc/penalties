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

import base.SpecBase
import config.AppConfig
import models.auditing.JsonAuditModel
import org.mockito.Matchers
import org.mockito.Mockito.{mock, reset, verify, when}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, RequestId, SessionId}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {
  val mockConfig: AppConfig = mock(classOf[AppConfig])
  val mockAuditConnector: AuditConnector = mock(classOf[AuditConnector])

  class Setup {
    reset(mockConfig)
    reset(mockAuditConnector)
    val service = new AuditService(mockAuditConnector, mockConfig)
  }

  "toExtendedDataEvent" should {
    "turn a JsonAuditModel into a ExtendedDataEvent" in new Setup {
      when(mockConfig.appName).thenReturn("penalties")
      when(mockAuditConnector.sendExtendedEvent(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val jsonAuditModel: JsonAuditModel = new JsonAuditModel {
        override val auditType: String = "AuditType"
        override val transactionName: String = "TransactionName"
        override val detail: JsValue = Json.parse("{}")
      }
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session1")), requestId = Some(RequestId("request1")))
      val result: ExtendedDataEvent = service.toExtendedDataEvent(jsonAuditModel, "/path")
      result.auditType shouldBe "AuditType"
      result.tags("X-Session-ID") shouldBe "session1"
      result.tags("X-Request-ID") shouldBe "request1"
    }
  }

  "audit" should {
    "send the audit event to Datastream" in new Setup {
      when(mockConfig.appName).thenReturn("penalties")
      when(mockAuditConnector.sendExtendedEvent(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val jsonAuditModel: JsonAuditModel = new JsonAuditModel {
        override val auditType: String = "AuditType"
        override val transactionName: String = "TransactionName"
        override val detail: JsValue = Json.parse("{}")
      }
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session1")), requestId = Some(RequestId("request1")))
      service.audit(jsonAuditModel)(implicitly, implicitly, fakeRequest)
      verify(mockAuditConnector)
        .sendExtendedEvent(Matchers.any())(Matchers.any(), Matchers.any())
    }
  }
}
