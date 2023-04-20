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

package controllers.testOnly

import base.SpecBase
import config.featureSwitches.FeatureSwitch
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.time.{LocalDate, LocalDateTime}
import scala.language.postfixOps

class FeatureSwitchControllerSpec extends SpecBase {
  val mockConfig: Configuration = mock(classOf[Configuration])
  class Setup {
    reset(mockConfig)
    val controller = new FeatureSwitchController(stubControllerComponents())(mockConfig)
    FeatureSwitch.listOfAllFeatureSwitches.foreach(sys.props -= _.name)
    sys.props -= controller.TIME_MACHINE_NOW
  }

  "enableOrDisableFeature" should {
    s"return NOT_FOUND (${Status.NOT_FOUND}) when the specified name does not exist" in new Setup {
      val result = controller.enableOrDisableFeature("invalid", false)(FakeRequest())
      status(result) shouldBe NOT_FOUND
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
      controller.getTimeMachineDateTime.toLocalDate shouldBe LocalDate.now() //Set to LocalDate to stop flaky tests
    }

    s"return OK (${Status.OK}) and set the correct date provided" in new Setup {
      val result = controller.setTimeMachineDate(Some("2022-01-01T12:00:01"))(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result) shouldBe s"Time machine set to: ${LocalDateTime.of(2022, 1, 1, 12, 0, 1).toString}"
      (sys.props get "TIME_MACHINE_NOW" get) shouldBe LocalDateTime.of(2022, 1, 1, 12, 0, 1).toString
    }
  }

  "setEstimatedLPP1FilterEndDate" should {
    s"return BAD_REQUEST (${Status.BAD_REQUEST}) when the date provided is invalid" in new Setup {
      val result = controller.setEstimatedLPP1FilterEndDate(Some("this-is-invalid"))(FakeRequest())
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "The date provided is in an invalid format"
    }

    s"return OK (${Status.OK}) and return NONE if no date provided" in new Setup {
      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(None)
      val result = controller.setEstimatedLPP1FilterEndDate(None)(FakeRequest())
      status(result) shouldBe OK
      controller.getEstimatedLPP1FilterEndDate shouldBe None
    }

    s"return OK (${Status.OK}) and set the correct date provided" in new Setup {
      val result = controller.setEstimatedLPP1FilterEndDate(Some("2022-01-01"))(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result) shouldBe s"Estimated LPP1 filter end date set to: ${LocalDate.of(2022, 1, 1).toString}"
      (sys.props get "ESTIMATED_LPP1_FILTER_END_DATE" get) shouldBe LocalDate.of(2022, 1, 1).toString
    }
  }
}
