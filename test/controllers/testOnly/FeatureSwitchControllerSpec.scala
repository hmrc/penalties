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

package controllers.testOnly

import base.SpecBase
import featureSwitches.{CallETMP, FeatureSwitching}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class FeatureSwitchControllerSpec extends SpecBase with FeatureSwitching {
  val controller: FeatureSwitchController = injector.instanceOf[FeatureSwitchController]

  "FeatureSwitchController" should {
    "return NOT FOUND when the feature switch is not defined" in {
      val result = controller.enableOrDisableFeature("fake", true)(FakeRequest())
      status(result) shouldBe NOT_FOUND
    }

    "return OK and enable the feature switch when specified" in {
      val result = controller.enableOrDisableFeature("feature.switch.call-etmp", true)(FakeRequest())
      status(result) shouldBe OK
      isEnabled(CallETMP) shouldBe true
    }

    "return OK and disable the feature switch when specified" in {
      val result = controller.enableOrDisableFeature("feature.switch.call-etmp", false)(FakeRequest())
      status(result) shouldBe OK
      isEnabled(CallETMP) shouldBe false
    }
  }
}
