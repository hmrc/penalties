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

import com.codahale.metrics.SharedMetricRegistries
import config.AppConfig
import helpers.WiremockHelper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestSuite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.{Application, Configuration}
import uk.gov.hmrc.http.HeaderCarrier

trait IntegrationSpecCommonBase extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with
  BeforeAndAfterAll with BeforeAndAfterEach with TestSuite with WiremockHelper with DatastreamWiremock {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val injector: Injector = app.injector

  override def afterEach(): Unit = {
    resetAll()
    super.afterEach()
    SharedMetricRegistries.clear()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockAuditResponse()
    mockMergedAuditResponse()
    SharedMetricRegistries.clear()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    start()
    SharedMetricRegistries.clear()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    resetAll()
    stop()
    SharedMetricRegistries.clear()
  }

  val configForApp: Map[String, Any] = Map(
    "auditing.enabled" -> true,
    "auditing.traceRequests" -> false,
    "microservice.services.penalties-stub.host" -> stubHost,
    "microservice.services.penalties-stub.port" -> stubPort,
    "microservice.services.etmp.host" -> stubHost,
    "microservice.services.etmp.port" -> stubPort,
    "microservice.services.pega.host" -> stubHost,
    "microservice.services.pega.port" -> stubPort,
    "microservice.services.des.host" -> stubHost,
    "microservice.services.des.port" -> stubPort,
    "auditing.consumer.baseUri.host" -> stubHost,
    "auditing.consumer.baseUri.port" -> stubPort,
    "microservice.services.penalties-file-notification-orchestrator.host" -> stubHost,
    "microservice.services.penalties-file-notification-orchestrator.port" -> stubPort
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(configForApp)
    .build()

  implicit val appConfig: AppConfig = injector.instanceOf[AppConfig]

  implicit val config: Configuration = appConfig.config

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  def buildClientForRequestToApp(baseUrl: String = "/penalties", uri: String): WSRequest = {
    ws.url(s"http://localhost:$port$baseUrl$uri").withFollowRedirects(false)
  }
}
