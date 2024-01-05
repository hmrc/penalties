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
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.matchers.should.{Matchers => ShouldMatchers}
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{LocalDateTime, LocalDate}

class AppConfigSpec extends AnyWordSpec with ShouldMatchers with FeatureSwitching {
  val mockConfiguration: Configuration = mock(classOf[Configuration])
  val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
  implicit val config: Configuration = mockConfiguration

  class Setup {
    reset(mockConfiguration)
    reset(mockServicesConfig)
    val config: AppConfig = new AppConfig(mockConfiguration, mockServicesConfig)
  }

  "addDateRangeQueryParameters" should {
    "set the correct dateTo and dateFrom" in new Setup {
      when(mockConfiguration.getOptional[String](any())(any())).thenReturn(Some(LocalDateTime.now().toString))
      when(mockConfiguration.get[String](any())(any())).thenReturn("POSTING")
      val expectedResult: String = s"&dateType=POSTING" +
        s"&dateFrom=${LocalDate.now().minusYears(2)}" +
        s"&dateTo=${LocalDate.now()}"

      val result: String = this.config.addDateRangeQueryParameters()
      result shouldBe expectedResult
    }
  }

  "getPenaltyDetailsUrl" should {
    "call API1812 when the feature switch is enabled" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getPenaltyDetailsUrl
      result shouldBe "localhost:0000/penalty/details/VATC/VRN/"
    }

    "call API1812 stub when the feature switch is disabled" in new Setup {
      disableFeatureSwitch(CallAPI1812ETMP)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getPenaltyDetailsUrl
      result shouldBe "localhost:0000/penalties-stub/penalty/details/VATC/VRN/"
    }
  }

  "getFinancialDetailsUrl" should {
    "call API1811 when the feature switch is enabled" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getFinancialDetailsUrl("123456789")
      result shouldBe "localhost:0000/penalty/financial-data/VRN/123456789/VATC"
    }

    "call API1811 stub when the feature switch is disabled" in new Setup {
      disableFeatureSwitch(CallAPI1811ETMP)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getFinancialDetailsUrl("123456789")
      result shouldBe "localhost:0000/penalties-stub/penalty/financial-data/VRN/123456789/VATC"
    }
  }

  "getAppealSubmissionURL" should {
    "call PEGA when the feature switch is enabled" in new Setup {
      enableFeatureSwitch(CallPEGA)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "0000001")
      result shouldBe "localhost:0000/penalty/first-stage-appeal/0000001"
    }

    "call the stub when the feature switch is disabled" in new Setup {
      disableFeatureSwitch(CallPEGA)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "0000001")
      result shouldBe "localhost:0000/penalties-stub/appeals/submit?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false&penaltyNumber=0000001"
    }

    "call the stub when the feature switch is disabled - for LPP" in new Setup {
      disableFeatureSwitch(CallPEGA)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyNumber = "0000001")
      result shouldBe "localhost:0000/penalties-stub/appeals/submit?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true&penaltyNumber=0000001"
    }
  }

  "getComplianceData" should {
    "call the stub when the feature switch is disabled" in new Setup {
      disableFeatureSwitch(CallDES)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getComplianceData("123456789", "2020-01-01", "2020-12-31")
      result shouldBe "localhost:0000/penalties-stub/enterprise/obligation-data/vrn/123456789/VATC?from=2020-01-01&to=2020-12-31"
    }

    "call the stub when the feature switch is enabled" in new Setup {
      enableFeatureSwitch(CallDES)
      when(mockServicesConfig.baseUrl(Matchers.any()))
        .thenReturn("localhost:0000")
      val result: String = this.config.getComplianceData("123456789", "2020-01-01", "2020-12-31")
      result shouldBe "localhost:0000/enterprise/obligation-data/vrn/123456789/VATC?from=2020-01-01&to=2020-12-31"
    }
  }

  "getMimeType" should {
    "return Some" when {
      "the config entry exists" in new Setup {
        when(mockConfiguration.getOptional[String](Matchers.eq("files.extensions.text.plain"))(any())).thenReturn(Some(".txt"))
        val result: Option[String] = this.config.getMimeType("text.plain")
        result.isDefined shouldBe true
        result.get shouldBe ".txt"
      }
    }

    "return None" when {
      "the config entry does not exist" in new Setup {
        when(mockConfiguration.getOptional[String](Matchers.eq("files.extensions.text.plain"))(any())).thenReturn(None)
        val result: Option[String] = this.config.getMimeType("text.plain")
        result.isEmpty shouldBe true
      }
    }
  }
}
