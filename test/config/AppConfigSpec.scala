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

package config

import featureSwitches._
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.matchers.should.{Matchers => ShouldMatchers}
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfigSpec extends AnyWordSpec with ShouldMatchers {
  val mockConfiguration: Configuration = mock(classOf[Configuration])
  val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
  class Setup {
    reset(mockConfiguration)
    reset(mockServicesConfig)
    val config: AppConfig = new AppConfig(mockConfiguration, mockServicesConfig)
  }

  "getVATPenaltiesURL" should {
    "call ETMP when the feature switch is enabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallETMP.name))(any()))
        .thenReturn(true)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getVATPenaltiesURL
      //TODO: change this once we have the correct URL
      result shouldBe "localhost:0000/"
    }

    "call the stub when the feature switch is disabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallETMP.name))(any()))
        .thenReturn(false)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getVATPenaltiesURL
      result shouldBe "localhost:0000/penalties-stub/etmp/mtd-vat/"
    }
  }

  "getPenaltyDetailsUrl" should {
    "call ETMP when the feature switch is enabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallAPI1812ETMP.name))(any()))
        .thenReturn(true)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getPenaltyDetailsUrl
      //TODO: change this once we have the correct URL
      result shouldBe "localhost:0000/penalty/details/VATC/VRN/"
    }

    "call the stub when the feature switch is disabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallAPI1812ETMP.name))(any()))
        .thenReturn(false)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getPenaltyDetailsUrl
      result shouldBe "localhost:0000/penalties-stub/penalty/details/VATC/VRN/"
    }
  }

  "getFinancialDetailsUrl" should {
    "call ETMP when the feature switch is enabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallAPI1811ETMP.name))(any()))
        .thenReturn(true)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getFinancialDetailsUrl
      //TODO: change this once we have the correct URL
      result shouldBe "localhost:0000/penalty/financial-data"
    }

    "call the stub when the feature switch is disabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallAPI1811ETMP.name))(any()))
        .thenReturn(false)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getFinancialDetailsUrl
      result shouldBe "localhost:0000/penalties-stub/penalty/financial-data"
    }
  }

  "getAppealSubmissionURL" should {
    "call PEGA when the feature switch is enabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallPEGA.name))(any()))
        .thenReturn(true)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "0000001")
      result shouldBe "localhost:0000/penalty/first-stage-appeal/0000001"
    }

    "call the stub when the feature switch is disabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallPEGA.name))(any()))
        .thenReturn(false)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "0000001")
      result shouldBe "localhost:0000/penalties-stub/appeals/submit?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=0000001"
    }

    "call the stub when the feature switch is disabled - for LPP" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallPEGA.name))(any()))
        .thenReturn(false)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyNumber = "0000001")
      result shouldBe "localhost:0000/penalties-stub/appeals/submit?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true&penaltyNumber=0000001"
    }
  }

  "getComplianceData" should {
    "call the stub when the feature switch is disabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallDES.name))(any()))
        .thenReturn(false)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getComplianceData("123456789", "2020-01-01", "2020-12-31")
      result shouldBe "localhost:0000/penalties-stub/enterprise/obligation-data/vrn/123456789/VATC?from=2020-01-01&to=2020-12-31"
    }

    "call the stub when the feature switch is enabled" in new Setup {
      when(mockConfiguration.get[Boolean](Matchers.eq(CallDES.name))(any()))
        .thenReturn(true)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getComplianceData("123456789", "2020-01-01", "2020-12-31")
      result shouldBe "localhost:0000/enterprise/obligation-data/vrn/123456789/VATC?from=2020-01-01&to=2020-12-31"
    }
  }
}
