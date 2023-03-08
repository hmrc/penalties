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

package config.featureSwitches

import base.SpecBase
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration
import utils.DateHelper

import java.time.{LocalDate, LocalDateTime}
import scala.language.postfixOps

class FeatureSwitchSpec extends SpecBase {

  val mockConfig: Configuration = mock(classOf[Configuration])

  class Setup {
    reset(mockConfig)
    val featureSwitching: FeatureSwitching = new FeatureSwitching {
      override implicit val config: Configuration = mockConfig
    }
    FeatureSwitch.listOfAllFeatureSwitches.foreach(sys.props -= _.name)
    sys.props -= featureSwitching.TIME_MACHINE_NOW
  }

  "FeatureSwitch listOfAllFeatureSwitches" should {
    "list all the feature switches in the app" in {
      FeatureSwitch.listOfAllFeatureSwitches shouldBe List(CallPEGA, CallDES, CallAPI1812ETMP, CallAPI1811ETMP, UseInternalAuth, SanitiseFileName)
    }
  }
  "FeatureSwitching constants" should {
    "be true and false" in new Setup {
      featureSwitching.FEATURE_SWITCH_ON shouldBe "true"
      featureSwitching.FEATURE_SWITCH_OFF shouldBe "false"
    }
  }

  "FeatureSwitching isEnabled" should {
    FeatureSwitch.listOfAllFeatureSwitches.foreach(
      featureSwitch => {
        s"return true if ${featureSwitch.name} is enabled" in new Setup {
          featureSwitching.enableFeatureSwitch(featureSwitch)
          featureSwitching.isEnabled(featureSwitch) shouldBe true
        }

        s"return false if ${featureSwitch.name} is disabled" in new Setup {
          featureSwitching.disableFeatureSwitch(featureSwitch)
          featureSwitching.isEnabled(featureSwitch) shouldBe false
        }

        s"return false if ${featureSwitch.name} does not exist" in new Setup {
          featureSwitching.isEnabled(featureSwitch) shouldBe false
        }
      }
    )
  }

  "FeatureSwitching enableFeatureSwitch" should {
    FeatureSwitch.listOfAllFeatureSwitches.foreach(
      featureSwitch => {
        s"set ${featureSwitch.name} property to true" in new Setup {
          featureSwitching.enableFeatureSwitch(featureSwitch)
          (sys.props get featureSwitch.name get) shouldBe "true"
        }
      }
    )
  }

  "FeatureSwitching disableFeatureSwitch" should {
    FeatureSwitch.listOfAllFeatureSwitches.foreach(
      featureSwitch => {
        s"set ${featureSwitch.name} property to false" in new Setup {
          featureSwitching.disableFeatureSwitch(featureSwitch)
          (sys.props get featureSwitch.name get) shouldBe "false"
        }
      }
    )
  }

  "FeatureSwitching setTimeMachineDate" should {
    lazy val dateTimeNow: LocalDateTime = LocalDateTime.now()
    s"set the date when the parameter is $Some" in new Setup {
      val dateMinus1Day: LocalDateTime = dateTimeNow.minusDays(1)
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe None
      featureSwitching.setTimeMachineDate(Some(dateMinus1Day))
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe Some(dateMinus1Day.toString)
    }

    s"overwrite an existing date when the parameter is $Some" in new Setup {
      val dateMinus1Day: LocalDateTime = dateTimeNow.minusDays(1)
      val dateMinus2Days: LocalDateTime = dateTimeNow.minusDays(2)
      featureSwitching.setTimeMachineDate(Some(dateMinus1Day))
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe Some(dateMinus1Day.toString)
      featureSwitching.setTimeMachineDate(Some(dateMinus2Days))
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe Some(dateMinus2Days.toString)
    }

    s"remove an existing date when the parameter is $None" in new Setup {
      featureSwitching.setTimeMachineDate(Some(dateTimeNow))
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe Some(dateTimeNow.toString)
      featureSwitching.setTimeMachineDate(None)
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe None
    }
  }

  "FeatureSwitching getTimeMachineDate" should {
    lazy val dateTimeNowMinus1Day: LocalDateTime = LocalDateTime.parse(LocalDateTime.now().format(DateHelper.dateTimeFormatter), DateHelper.dateTimeFormatter).minusDays(1)
    s"get the date when it exists in system properties" in new Setup {
      featureSwitching.setTimeMachineDate(Some(dateTimeNowMinus1Day))
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe Some(dateTimeNowMinus1Day.toString)
      featureSwitching.getTimeMachineDateTime shouldBe dateTimeNowMinus1Day
    }

    s"get the date from config when the key value exists and is non-empty" in new Setup {
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe None
      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(Some(dateTimeNowMinus1Day.toString))
      featureSwitching.getTimeMachineDateTime shouldBe dateTimeNowMinus1Day
    }

    s"get the date from the system when it does not exist in properties nor in config (value empty)" in new Setup {
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe None
      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(Some(""))
      featureSwitching.getTimeMachineDateTime.toLocalDate shouldBe LocalDate.now() //Set to LocalDate to stop flaky tests
    }

    s"get the date from the system when it does not exist in properties nor in config (kv not present)" in new Setup {
      (sys.props get featureSwitching.TIME_MACHINE_NOW) shouldBe None
      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(None)
      featureSwitching.getTimeMachineDateTime.toLocalDate shouldBe LocalDate.now() //Set to LocalDate to stop flaky tests
    }
  }
}
