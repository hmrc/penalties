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

import featureSwitches.{CallETMP, FeatureSwitching}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfigSpec extends AnyWordSpec with Matchers with FeatureSwitching {
  val mockConfiguration: Configuration = mock(classOf[Configuration])
  val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
  class Setup {
    reset(mockConfiguration)
    reset(mockServicesConfig)
    val config: AppConfig = new AppConfig(mockConfiguration, mockServicesConfig)
  }

  "getVATPenaltiesURL" should {
    "call ETMP when the feature switch is enabled" in new Setup {
      enableFeatureSwitch(CallETMP)
      when(mockServicesConfig.baseUrl(ArgumentMatchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getVATPenaltiesURL
      //TODO: change this once we have the correct URL
      result shouldBe "localhost:0000/"
    }

    "call the stub when the feature switch is disabled" in new Setup {
      disableFeatureSwitch(CallETMP)
      when(mockServicesConfig.baseUrl(ArgumentMatchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getVATPenaltiesURL
      result shouldBe "localhost:0000/penalties-stub/etmp/mtd-vat/"
    }
  }

  "getAppealSubmissionURL" should {
    "call ETMP when the feature switch is enabled" in new Setup {
      enableFeatureSwitch(CallETMP)
      when(mockServicesConfig.baseUrl(ArgumentMatchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = false)
      //TODO: change this once we have the correct URL
      result shouldBe "localhost:0000/"
    }

    "call the stub when the feature switch is disabled" in new Setup {
      disableFeatureSwitch(CallETMP)
      when(mockServicesConfig.baseUrl(ArgumentMatchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = false)
      result shouldBe "localhost:0000/penalties-stub/appeals/submit?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=false"
    }

    "call the stub when the feature switch is disabled - for LPP" in new Setup {
      disableFeatureSwitch(CallETMP)
      when(mockServicesConfig.baseUrl(ArgumentMatchers.any()))
        .thenReturn("localhost:0000")
      val result: String = config.getAppealSubmissionURL("HMRC-MTD-VAT~VRN~123456789", isLPP = true)
      result shouldBe "localhost:0000/penalties-stub/appeals/submit?enrolmentKey=HMRC-MTD-VAT~VRN~123456789&isLPP=true"
    }
  }
}
