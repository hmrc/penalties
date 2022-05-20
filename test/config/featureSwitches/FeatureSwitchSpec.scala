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

package config.featureSwitches

import base.SpecBase
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration

class FeatureSwitchSpec extends SpecBase {

  val mockConfig: Configuration = mock(classOf[Configuration])

  class Setup {
    reset(mockConfig)
    val featureSwitching: FeatureSwitching = new FeatureSwitching {
      override implicit val config: Configuration = mockConfig
    }
    FeatureSwitch.listOfAllFeatureSwitches.map(sys.props -= _.name)
  }

  "FeatureSwitch listOfAllFeatureSwitches" should {
    "be all the featureswitches in the app" in {
      FeatureSwitch.listOfAllFeatureSwitches shouldBe List(CallETMP, CallPEGA, CallDES, CallAPI1812ETMP, CallAPI1811ETMP, UseAPI1812Model)
    }
  }
  "FeatureSwitching constants" should {
    "be true and false" in new Setup {
      featureSwitching.FEATURE_SWITCH_ON shouldBe "true"
      featureSwitching.FEATURE_SWITCH_OFF shouldBe "false"
    }
  }

  "FeatureSwitching isEnabled" should {
    s"return true if ETMP feature switch is enabled" in new Setup {
      featureSwitching.enableFeatureSwitch(CallETMP)
      featureSwitching.isEnabled(CallETMP) shouldBe true
    }

    s"return.false if ETMP feature switch is disabled" in new Setup {
      featureSwitching.disableFeatureSwitch(CallETMP)
      featureSwitching.isEnabled(CallETMP) shouldBe false
    }

    "return false if ETMP feature switch does not exist" in new Setup {
      featureSwitching.isEnabled(CallETMP) shouldBe false
    }

    s"return true if PEGA feature switch is enabled" in new Setup {
      featureSwitching.enableFeatureSwitch(CallPEGA)
      featureSwitching.isEnabled(CallPEGA) shouldBe true
    }

    s"return.false if PEGA feature switch is disabled" in new Setup {
      featureSwitching.disableFeatureSwitch(CallETMP)
      featureSwitching.isEnabled(CallPEGA) shouldBe false
    }

    "return false if PEGA feature switch does not exist" in new Setup {
      featureSwitching.isEnabled(CallPEGA) shouldBe false
    }

    "return true if DES feature switch is enabled" in new Setup {
      featureSwitching.enableFeatureSwitch(CallDES)
      featureSwitching.isEnabled(CallDES) shouldBe true
    }

    "return false if DES feature switch is disabled" in new Setup {
      featureSwitching.disableFeatureSwitch(CallDES)
      featureSwitching.isEnabled(CallDES) shouldBe false
    }

    "return false if DES feature switch does not exist" in new Setup {
      featureSwitching.isEnabled(CallDES) shouldBe false
    }

    "return true if CallAPI1812ETMP feature switch is enabled" in new Setup {
      featureSwitching.enableFeatureSwitch(CallAPI1812ETMP)
      featureSwitching.isEnabled(CallAPI1812ETMP) shouldBe true
    }

    "return false if CallAPI1812ETMP feature switch is disabled" in new Setup {
      featureSwitching.disableFeatureSwitch(CallAPI1812ETMP)
      featureSwitching.isEnabled(CallAPI1812ETMP) shouldBe false
    }

    "return false if CallAPI1812ETMP feature switch does not exist" in new Setup {
      featureSwitching.isEnabled(CallAPI1812ETMP) shouldBe false
    }

    "return true is CallAPI1811ETMP feature switch is enabled" in new Setup {
      featureSwitching.enableFeatureSwitch(CallAPI1811ETMP)
      featureSwitching.isEnabled(CallAPI1811ETMP) shouldBe true
    }

    "return false if CallAPI1811ETMP feature switch is disabled" in new Setup {
      featureSwitching.disableFeatureSwitch(CallAPI1811ETMP)
      featureSwitching.isEnabled(CallAPI1811ETMP) shouldBe false
    }

    "return false if CallAPI1811ETMP feature switch does not exist" in new Setup {
      featureSwitching.isEnabled(CallAPI1811ETMP) shouldBe false
    }

    "return true is UseAPI1812Model feature switch is enabled" in new Setup {
      featureSwitching.enableFeatureSwitch(UseAPI1812Model)
      featureSwitching.isEnabled(UseAPI1812Model) shouldBe true
    }

    "return false if UseAPI1812Model feature switch is disabled" in new Setup {
      featureSwitching.disableFeatureSwitch(UseAPI1812Model)
      featureSwitching.isEnabled(UseAPI1812Model) shouldBe false
    }

    "return false if UseAPI1812Model feature switch does not exist" in new Setup {
      featureSwitching.isEnabled(UseAPI1812Model) shouldBe false
    }

    "return true if a feature switch is not in the system props but is in config" in new Setup {
      when(mockConfig.get[Boolean](any())(any()))
        .thenReturn(true)
      featureSwitching.isEnabled(UseAPI1812Model) shouldBe true
    }
  }

  "FeatureSwitching enableFeatureSwitch" should {
    s"set ${CallETMP.name} property to true" in new Setup {
      featureSwitching.enableFeatureSwitch(CallETMP)
      (sys.props get CallETMP.name get) shouldBe "true"
    }

    s"set ${CallPEGA.name} property to true" in new Setup {
      featureSwitching.enableFeatureSwitch(CallPEGA)
      (sys.props get CallPEGA.name get) shouldBe "true"
    }

    s"set ${CallDES.name} property to true" in new Setup {
      featureSwitching.enableFeatureSwitch(CallDES)
      (sys.props get CallDES.name get) shouldBe "true"
    }

    s"set ${CallAPI1812ETMP.name} property to true" in new Setup {
      featureSwitching.enableFeatureSwitch(CallAPI1812ETMP)
      (sys.props get CallAPI1812ETMP.name get) shouldBe "true"
    }

    s"set ${CallAPI1811ETMP.name} property to true" in new Setup {
      featureSwitching.enableFeatureSwitch(CallAPI1811ETMP)
      (sys.props get CallAPI1811ETMP.name get) shouldBe "true"
    }

    s"set ${UseAPI1812Model.name} property to true" in new Setup {
      featureSwitching.enableFeatureSwitch(UseAPI1812Model)
      (sys.props get UseAPI1812Model.name get) shouldBe "true"
    }

  }

  "FeatureSwitching disableFeatureSwitch" should {
    s"set ${CallETMP.name} property to false" in new Setup {
      featureSwitching.disableFeatureSwitch(CallETMP)
      (sys.props get CallETMP.name get) shouldBe "false"
    }

    s"set ${CallPEGA.name} property to false" in new Setup {
      featureSwitching.disableFeatureSwitch(CallPEGA)
      (sys.props get CallPEGA.name get) shouldBe "false"
    }

    s"set ${CallDES.name} property to false" in new Setup {
      featureSwitching.disableFeatureSwitch(CallDES)
      (sys.props get CallDES.name get) shouldBe "false"
    }

    s"set ${CallAPI1812ETMP.name} property to false" in new Setup {
      featureSwitching.disableFeatureSwitch(CallAPI1812ETMP)
      (sys.props get CallAPI1812ETMP.name get) shouldBe "false"
    }

    s"set ${CallAPI1811ETMP.name} property to false" in new Setup {
      featureSwitching.disableFeatureSwitch(CallAPI1811ETMP)
      (sys.props get CallAPI1811ETMP.name get) shouldBe "false"
    }

    s"set ${UseAPI1812Model.name} property to false" in new Setup {
      featureSwitching.disableFeatureSwitch(UseAPI1812Model)
      (sys.props get UseAPI1812Model.name get) shouldBe "false"
    }

  }
}
