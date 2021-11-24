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

package config

import featureSwitches.{CallDES, CallETMP, CallPEGA, FeatureSwitching}

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) extends FeatureSwitching {
  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String     = config.get[String]("microservice.metrics.graphite.host")

  lazy val stubBase: String = servicesConfig.baseUrl("penalties-stub")

  lazy val appName: String = config.get[String]("appName")

  lazy val etmpBase: String = servicesConfig.baseUrl("etmp")

  lazy val pegaBase: String = servicesConfig.baseUrl("pega")

  lazy val desBase: String = servicesConfig.baseUrl("des")

  lazy val desEnvironment: String = servicesConfig.getConfString("des.environment", "live")

  lazy val desBearerToken: String = s"Bearer ${servicesConfig.getConfString("des.bearerToken", "")}"

  def getVATPenaltiesURL: String = {
    if(!isEnabled(CallETMP)) stubBase + "/penalties-stub/etmp/mtd-vat/"
    //TODO: change to relevant URL when implemented
    else etmpBase + "/"
  }

  def getAppealSubmissionURL(enrolmentKey: String, isLPP: Boolean, penaltyId: String): String = {
    if(!isEnabled(CallPEGA)) stubBase + s"/penalties-stub/appeals/submit?enrolmentKey=$enrolmentKey&isLPP=$isLPP&penaltyId=$penaltyId"
    //TODO: change to relevant URL when implemented
    else pegaBase + s"/penalty/first-stage-appeal/$penaltyId"
  }

  def isReasonableExcuseEnabled(excuseName: String): Boolean = {
    config.get[Boolean](s"reasonableExcuses.$excuseName.enabled")
  }

  def getComplianceData(vrn: String, fromDate: String, toDate: String): String = {
    if(isEnabled(CallDES)) {
      desBase + s"/enterprise/obligation-data/vrn/$vrn/VATC?from=$fromDate&to=$toDate"
    } else {
      desBase + s"/penalties-stub/enterprise/obligation-data/vrn/$vrn/VATC?from=$fromDate&to=$toDate"
    }
  }
}
