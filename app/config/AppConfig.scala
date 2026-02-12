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
import models.AgnosticEnrolmentKey
import models.getFinancialDetails.FinancialDetailsRequestModel
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.Base64
import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val config: Configuration, servicesConfig: ServicesConfig) extends FeatureSwitching {

  import servicesConfig._

  lazy val queryParametersForGetFinancialDetails: String =
    s"?includeClearedItems=${config.get[Boolean]("eis.includeCleared")}" +
      s"&includeStatisticalItems=${config.get[Boolean]("eis.includeStatistical")}" +
      s"&includePaymentOnAccount=${config.get[Boolean]("eis.includePOA")}" +
      s"&addRegimeTotalisation=${config.get[Boolean]("eis.addRegimeTotalisation")}" +
      s"&addLockInformation=${config.get[Boolean]("eis.includeLocks")}" +
      s"&addPenaltyDetails=${config.get[Boolean]("eis.includePenaltyDetails")}" +
      s"&addPostedInterestDetails=${config.get[Boolean]("eis.calculatePostedInterest")}" +
      s"&addAccruingInterestDetails=${config.get[Boolean]("eis.calculateAccruedInterest")}"

  lazy val baseFinancialDetailsRequestModel: FinancialDetailsRequestModel = FinancialDetailsRequestModel(
    searchType = None,
    searchItem = None,
    dateType = Some(config.get[String]("eis.dateType")),
    dateFrom = Some(getTimeMachineDateTime.toLocalDate.minusYears(2).toString),
    dateTo = Some(getTimeMachineDateTime.toLocalDate.toString),
    includeClearedItems = Some(config.get[Boolean]("eis.includeCleared")),
    includeStatisticalItems = Some(config.get[Boolean]("eis.includeStatistical")),
    includePaymentOnAccount = Some(config.get[Boolean]("eis.includePOA")),
    addRegimeTotalisation = Some(config.get[Boolean]("eis.addRegimeTotalisation")),
    addLockInformation = Some(config.get[Boolean]("eis.includeLocks")),
    addPenaltyDetails = Some(config.get[Boolean]("eis.includePenaltyDetails")),
    addPostedInterestDetails = Some(config.get[Boolean]("eis.calculatePostedInterest")),
    addAccruingInterestDetails = Some(config.get[Boolean]("eis.calculateAccruedInterest"))
  )

  lazy val queryParametersForGetFinancialDetailsWithoutClearedItems: String =
    s"?includeClearedItems=false" +
      s"&includeStatisticalItems=${config.get[Boolean]("eis.includeStatistical")}" +
      s"&includePaymentOnAccount=${config.get[Boolean]("eis.includePOA")}" +
      s"&addRegimeTotalisation=${config.get[Boolean]("eis.addRegimeTotalisation")}" +
      s"&addLockInformation=${config.get[Boolean]("eis.includeLocks")}" +
      s"&addPenaltyDetails=${config.get[Boolean]("eis.includePenaltyDetails")}" +
      s"&addPostedInterestDetails=${config.get[Boolean]("eis.calculatePostedInterest")}" +
      s"&addAccruingInterestDetails=${config.get[Boolean]("eis.calculateAccruedInterest")}"

  def addDateRangeQueryParameters(): String = s"&dateType=${config.get[String]("eis.dateType")}" +
    s"&dateFrom=${getTimeMachineDateTime.toLocalDate.minusYears(2)}" +
    s"&dateTo=${getTimeMachineDateTime.toLocalDate}"

  lazy val appName: String = config.get[String]("appName")

  lazy val eiOutboundBearerToken: String = config.get[String]("eis.outboundBearerToken")
  lazy val eisEnvironment: String        = config.get[String]("eis.environment")

  lazy val desBearerToken: String = config.get[String]("des.outboundBearerToken")

  private lazy val stubBase: String = servicesConfig.baseUrl("penalties-stub")

  private lazy val etmpBase: String = servicesConfig.baseUrl("etmp")

  private lazy val pegaBase: String = servicesConfig.baseUrl("pega")

  private lazy val desBase: String = servicesConfig.baseUrl("des")

  private lazy val hipBase: String = servicesConfig.baseUrl("hip")

  private lazy val fileNotificationOrchestrator: String = servicesConfig.baseUrl("penalties-file-notification-orchestrator")

  lazy val maximumFilenameLength: Int = config.get[Int]("sdes.maximumFilenameLength")

  lazy val checksumAlgorithmForFileNotifications: String = config.get[String]("sdes.checksumAlgorithm")

  def postFileNotificationUrl: String = s"$fileNotificationOrchestrator/penalties-file-notification-orchestrator/new-notifications"

  lazy val SDESNotificationInfoType: String      = config.get[String]("SDESNotification.informationType")
  lazy val SDESNotificationFileRecipient: String = config.get[String]("SDESNotification.file.recipient")

  def isReasonableExcuseEnabled(excuseName: String): Boolean =
    config.get[Boolean](s"reasonableExcuses.$excuseName.enabled")

  def getMimeType(mimeType: String): Option[String] = config.getOptional[String](s"files.extensions.$mimeType")

  def getAppealSubmissionIfUrl(penaltyNumber: String): String = {
    val baseUrl = if (isEnabled(CallPEGA)) pegaBase else s"$stubBase/penalties-stub"
    s"$baseUrl/penalty/first-stage-appeal/$penaltyNumber"
  }

  def getAppealSubmissionHipUrl: String = {
    val baseUrl = if (isEnabled(CallPEGA)) hipBase else stubBase
    s"$baseUrl/pegacms/v1/penalty/appeal"
  }

  def getFinancialDetailsIfUrl(enrolmentKey: AgnosticEnrolmentKey): String = {
    val taxRegime = enrolmentKey.regime.value
    val id        = enrolmentKey.idType.value
    val idValue   = enrolmentKey.id.value
    val baseUrl   = if (isEnabled(CallAPI1811ETMP)) etmpBase else s"$stubBase/penalties-stub"
    s"$baseUrl/penalty/financial-data/$id/$idValue/$taxRegime"
  }

  def getFinancialDetailsHipUrl: String = {
    val baseUrl = if (isEnabled(CallAPI1811Stub)) stubBase else hipBase
    s"$baseUrl/etmp/RESTAdapter/cross-regime/taxpayer/financial-data/query"
  }

  def getPenaltyDetailsUrl(agnosticEnrolmentKey: AgnosticEnrolmentKey): String = {
    val regime  = agnosticEnrolmentKey.regime.value
    val idType  = agnosticEnrolmentKey.idType.value
    val idValue = agnosticEnrolmentKey.id.value
    val baseUrl = if (isEnabled(CallAPI1812ETMP)) etmpBase else s"$stubBase/penalties-stub"
    s"$baseUrl/penalty/details/$regime/$idType/$idValue"
  }

  def getHIPPenaltyDetailsUrl(agnosticEnrolmentKey: AgnosticEnrolmentKey, dateLimit: Option[String] = None): String = {
    val regime                 = agnosticEnrolmentKey.regime.value
    val idType                 = agnosticEnrolmentKey.idType.value
    val idValue                = agnosticEnrolmentKey.id.value
    val dateLimitParam: String = dateLimit.map(dateLimit => s"&dateLimit=$dateLimit").getOrElse("")
    val penaltiesHipUrl = s"/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=$regime&idType=$idType&idNumber=$idValue$dateLimitParam"
    if (isEnabled(CallAPI1812HIP)) hipBase + penaltiesHipUrl
    else stubBase + penaltiesHipUrl
  }

  def getComplianceDataUrl(agnosticEnrolmentKey: AgnosticEnrolmentKey, fromDate: String, toDate: String): String = {
    val regime  = agnosticEnrolmentKey.regime.value
    val idType  = agnosticEnrolmentKey.idType.value
    val idValue = agnosticEnrolmentKey.id.value
    val baseUrl =
      if (isEnabled(CallDES)) desBase + s"/enterprise/obligation-data/" else stubBase + s"/penalties-stub/enterprise/obligation-data/"
    s"$baseUrl$idType/$idValue/$regime?from=$fromDate&to=$toDate"
  }

  private val clientIdV1: String    = getString("microservice.services.hip.client-id")
  private val secretV1: String      = getString("microservice.services.hip.client-secret")
  def hipAuthorisationToken: String = Base64.getEncoder.encodeToString(s"$clientIdV1:$secretV1".getBytes("UTF-8"))

  lazy val hipEnvironment: String = getString("microservice.services.hip.environment")

}
