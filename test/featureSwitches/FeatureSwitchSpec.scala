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

package featureSwitches

import base.SpecBase
import config.AppConfig
import org.mockito.Mockito._
import org.mockito.Matchers

class FeatureSwitchSpec extends SpecBase {
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])

  class Setup {
    val featureSwitching: FeatureSwitching = new FeatureSwitching {
      override implicit val appConfig: AppConfig = mockAppConfig
    }
  }

  "FeatureSwitch listOfAllFeatureSwitches" should {
    "be all the featureswitches in the app" in {
      FeatureSwitch.listOfAllFeatureSwitches shouldBe List(CallETMP, CallPEGA, CallDES, CallAPI1812ETMP, CallAPI1811ETMP, UseAPI1812Model)
    }
  }

  "FeatureSwitching isEnabled" should {
    s"return true if ETMP feature switch is enabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallETMP.name)))
        .thenReturn(true)
      featureSwitching.isEnabled(CallETMP) shouldBe true
    }

    s"return.false if ETMP feature switch is disabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallETMP.name)))
        .thenReturn(false)
      featureSwitching.isEnabled(CallETMP) shouldBe false
    }

    s"return true if PEGA feature switch is enabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallPEGA.name)))
        .thenReturn(true)
      featureSwitching.isEnabled(CallPEGA) shouldBe true
    }

    s"return.false if PEGA feature switch is disabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallPEGA.name)))
        .thenReturn(false)
      featureSwitching.isEnabled(CallPEGA) shouldBe false
    }

    "return true if DES feature switch is enabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallDES.name)))
        .thenReturn(true)
      featureSwitching.isEnabled(CallDES) shouldBe true
    }

    "return false if DES feature switch is disabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallDES.name)))
        .thenReturn(false)
      featureSwitching.isEnabled(CallDES) shouldBe false
    }

    "return true if CallAPI1812ETMP feature switch is enabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallAPI1812ETMP.name)))
        .thenReturn(true)
      featureSwitching.isEnabled(CallAPI1812ETMP) shouldBe true
    }

    "return false if CallAPI1812ETMP feature switch is disabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallAPI1812ETMP.name)))
        .thenReturn(false)
      featureSwitching.isEnabled(CallAPI1812ETMP) shouldBe false
    }

    "return true is CallAPI1811ETMP feature switch is enabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallAPI1811ETMP.name)))
        .thenReturn(true)
      featureSwitching.isEnabled(CallAPI1811ETMP) shouldBe true
    }

    "return false if CallAPI1811ETMP feature switch is disabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(CallAPI1811ETMP.name)))
        .thenReturn(false)
      featureSwitching.isEnabled(CallAPI1811ETMP) shouldBe false
    }

    "return true is UseAPI1812Model feature switch is enabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(UseAPI1812Model.name)))
        .thenReturn(true)
      featureSwitching.isEnabled(UseAPI1812Model) shouldBe true
    }

    "return false if UseAPI1812Model feature switch is disabled" in new Setup {
      when(mockAppConfig.isFeatureSwitchEnabled(Matchers.eq(UseAPI1812Model.name)))
        .thenReturn(false)
      featureSwitching.isEnabled(UseAPI1812Model) shouldBe false
    }
  }
}
