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

class FeatureSwitchSpec extends SpecBase {

  class Setup {
    val featureSwitching: FeatureSwitching = new FeatureSwitching {}
    sys.props -= CallETMP.name
    sys.props -= CallPEGA.name
  }

  "FeatureSwitch listOfAllFeatureSwitches" should {
    "be all the featureswitches in the app" in {
      FeatureSwitch.listOfAllFeatureSwitches shouldBe List(CallETMP, CallPEGA, CallAPI1811ETMP)
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
    s"set ${CallAPI1811ETMP.name} property to true" in new Setup {
      featureSwitching.enableFeatureSwitch(CallAPI1811ETMP)
      (sys.props get CallAPI1811ETMP.name get) shouldBe "true"
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
    s"set ${CallAPI1811ETMP.name} property to false" in new Setup {
      featureSwitching.disableFeatureSwitch(CallAPI1811ETMP)
      (sys.props get CallAPI1811ETMP.name get) shouldBe "false"
    }
  }
}
