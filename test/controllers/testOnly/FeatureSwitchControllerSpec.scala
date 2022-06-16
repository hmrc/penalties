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
import config.featureSwitches.{FeatureSwitch, UseAPI1812Model}
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.time.LocalDate

class FeatureSwitchControllerSpec extends SpecBase {
  val mockConfig: Configuration = mock(classOf[Configuration])
  class Setup {
    reset(mockConfig)
    val controller = new FeatureSwitchController(stubControllerComponents())(mockConfig)
    FeatureSwitch.listOfAllFeatureSwitches.foreach(sys.props -= _.name)
  }

  "enableOrDisableFeature" should {
    s"return NOT_FOUND (${Status.NOT_FOUND}) when the specified name does not exist" in new Setup {
      val result = controller.enableOrDisableFeature("invalid", false)(FakeRequest())
      status(result) shouldBe NOT_FOUND
    }

    s"return OK (${Status.OK}) when the name exists and the feature switch was disabled" in new Setup {
      val result = controller.enableOrDisableFeature("feature.switch.use-api-1812-model", false)(FakeRequest())
      status(result) shouldBe OK
      (sys.props get UseAPI1812Model.name get) shouldBe "false"
    }

    s"return OK (${Status.OK}) when the name exists and the feature switch was enabled" in new Setup {
      val result = controller.enableOrDisableFeature("feature.switch.use-api-1812-model", true)(FakeRequest())
      status(result) shouldBe OK
      (sys.props get UseAPI1812Model.name get) shouldBe "true"
    }
  }

  "setTimeMachineDate" should {
    s"return NOT_FOUND (${Status.NOT_FOUND}) when the date provided is invalid" in new Setup {
      val result = controller.setTimeMachineDate(Some("this-is-invalid"))(FakeRequest())
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "The date provided is in an invalid format"
    }

    s"return OK (${Status.OK}) and reset the date back to today's date if no date provided" in new Setup {
      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(None)
      val result = controller.setTimeMachineDate(None)(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result) shouldBe s"Time machine set to: ${LocalDate.now().toString}"
      controller.getTimeMachineDate shouldBe LocalDate.now()
    }

    s"return OK (${Status.OK}) and set the correct date provided" in new Setup {
      val result = controller.setTimeMachineDate(Some("2022-01-01"))(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result) shouldBe s"Time machine set to: ${LocalDate.of(2022, 1, 1).toString}"
      (sys.props get "TIME_MACHINE_NOW" get) shouldBe LocalDate.of(2022, 1, 1).toString
    }
  }
}
