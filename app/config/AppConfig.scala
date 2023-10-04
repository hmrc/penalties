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

package config

import config.featureSwitches._
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(val config: Configuration, servicesConfig: ServicesConfig) extends FeatureSwitching {
  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  lazy val queryParametersForGetFinancialDetails: String = {
    s"?includeClearedItems=${config.get[Boolean]("eis.includeCleared")}" +
      s"&includeStatisticalItems=${config.get[Boolean]("eis.includeStatistical")}" +
      s"&includePaymentOnAccount=${config.get[Boolean]("eis.includePOA")}" +
      s"&addRegimeTotalisation=${config.get[Boolean]("eis.addRegimeTotalisation")}" +
      s"&addLockInformation=${config.get[Boolean]("eis.includeLocks")}" +
      s"&addPenaltyDetails=${config.get[Boolean]("eis.includePenaltyDetails")}" +
      s"&addPostedInterestDetails=${config.get[Boolean]("eis.calculatePostedInterest")}" +
      s"&addAccruingInterestDetails=${config.get[Boolean]("eis.calculateAccruedInterest")}"
  }

  lazy val queryParametersForGetFinancialDetailsWithoutClearedItems: String = {
    s"?includeClearedItems=false" +
      s"&includeStatisticalItems=${config.get[Boolean]("eis.includeStatistical")}" +
      s"&includePaymentOnAccount=${config.get[Boolean]("eis.includePOA")}" +
      s"&addRegimeTotalisation=${config.get[Boolean]("eis.addRegimeTotalisation")}" +
      s"&addLockInformation=${config.get[Boolean]("eis.includeLocks")}" +
      s"&addPenaltyDetails=${config.get[Boolean]("eis.includePenaltyDetails")}" +
      s"&addPostedInterestDetails=${config.get[Boolean]("eis.calculatePostedInterest")}" +
      s"&addAccruingInterestDetails=${config.get[Boolean]("eis.calculateAccruedInterest")}"
  }

  def addDateRangeQueryParameters(): String = s"&dateType=${config.get[String]("eis.dateType")}" +
    s"&dateFrom=${getTimeMachineDateTime.toLocalDate.minusYears(2)}" +
    s"&dateTo=${getTimeMachineDateTime.toLocalDate}"

  lazy val appName: String = config.get[String]("appName")

  lazy val eiOutboundBearerToken: String = config.get[String]("eis.outboundBearerToken")
  lazy val eisEnvironment: String = config.get[String]("eis.environment")

  lazy val desBearerToken: String = config.get[String]("des.outboundBearerToken")

  lazy val stubBase: String = servicesConfig.baseUrl("penalties-stub")

  lazy val etmpBase: String = servicesConfig.baseUrl("etmp")

  lazy val pegaBase: String = servicesConfig.baseUrl("pega")

  lazy val desBase: String = servicesConfig.baseUrl("des")

  lazy val fileNotificationOrchestrator: String = servicesConfig.baseUrl("penalties-file-notification-orchestrator")

  lazy val maximumFilenameLength: Int = config.get[Int]("sdes.maximumFilenameLength")

  lazy val checksumAlgorithmForFileNotifications: String = config.get[String]("sdes.checksumAlgorithm")

  def postFileNotificationUrl: String = s"$fileNotificationOrchestrator/penalties-file-notification-orchestrator/new-notifications"

  lazy val SDESNotificationInfoType: String = config.get[String]("SDESNotification.informationType")
  lazy val SDESNotificationFileRecipient: String = config.get[String]("SDESNotification.file.recipient")

  def getPenaltyDetailsUrl: String = {
    if (!isEnabled(CallAPI1812ETMP)) stubBase + "/penalties-stub/penalty/details/VATC/VRN/"
    else etmpBase + "/penalty/details/VATC/VRN/"
  }

  def getFinancialDetailsUrl(vrn: String): String = {
    if (!isEnabled(CallAPI1811ETMP)) stubBase + s"/penalties-stub/penalty/financial-data/VRN/$vrn/VATC"
    else etmpBase + s"/penalty/financial-data/VRN/$vrn/VATC"
  }

  def getAppealSubmissionURL(enrolmentKey: String, isLPP: Boolean, penaltyNumber: String): String = {
    if (!isEnabled(CallPEGA)) stubBase + s"/penalties-stub/appeals/submit?enrolmentKey=$enrolmentKey&isLPP=$isLPP&penaltyNumber=$penaltyNumber"
    else pegaBase + s"/penalty/first-stage-appeal/$penaltyNumber"
  }

  def isReasonableExcuseEnabled(excuseName: String): Boolean = {
    config.get[Boolean](s"reasonableExcuses.$excuseName.enabled")
  }

  def getComplianceData(vrn: String, fromDate: String, toDate: String): String = {
    if (isEnabled(CallDES)) {
      desBase + s"/enterprise/obligation-data/vrn/$vrn/VATC?from=$fromDate&to=$toDate"
    } else {
      stubBase + s"/penalties-stub/enterprise/obligation-data/vrn/$vrn/VATC?from=$fromDate&to=$toDate"
    }
  }

  def getMimeType(mimeType: String): Option[String] = config.getOptional[String](s"files.extensions.$mimeType")
}
