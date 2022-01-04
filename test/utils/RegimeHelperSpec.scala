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

package utils

import base.SpecBase
import org.mockito.Mockito.{mock, when}

import java.time.LocalDateTime

class RegimeHelperSpec extends SpecBase {
  val mockDateTimeHelper = mock(classOf[DateHelper])

  "getDateTimeBasedOnRegimeFromEnrolmentKey" should {
    lazy val localDateTimeNow = LocalDateTime.now()
    lazy val localDateTimeNowMinus2Years = localDateTimeNow.minusYears(2)
    "return now - 2 years for MTD-VAT" in {
      when(mockDateTimeHelper.dateTimeNow()).thenReturn(localDateTimeNow)
      val result = RegimeHelper.getDateTimeBasedOnRegimeFromEnrolmentKey("HMRC-MTD-VAT~VRN~123456789", mockDateTimeHelper)
      result shouldBe localDateTimeNowMinus2Years
    }

    "return now for any other regime" in {
      when(mockDateTimeHelper.dateTimeNow()).thenReturn(localDateTimeNow)
      val result = RegimeHelper.getDateTimeBasedOnRegimeFromEnrolmentKey("IR-SA~UTR~123456789", mockDateTimeHelper)
      result shouldBe localDateTimeNow
    }
  }

  "getRegimeFromEnrolmentKey" should {
    "return MTD-VAT for MTD VAT Regime" in {
      val result = RegimeHelper.getRegimeFromEnrolmentKey("HMRC-MTD-VAT~VRN~123456789")
      result shouldBe "mtd-vat"
    }

    "return empty string for any other regime" in {
      val result = RegimeHelper.getRegimeFromEnrolmentKey("IR-SA~UTR~123456789")
      result shouldBe ""
    }
  }

  "getIdentifierTypeFromEnrolmentKey" should {
    "return VRN for MTD-VAT" in {
      val result = RegimeHelper.getIdentifierTypeFromEnrolmentKey("HMRC-MTD-VAT~VRN~123456789")
      result shouldBe "VRN"
    }

    "return an empty string for any other regime" in {
      val result = RegimeHelper.getIdentifierTypeFromEnrolmentKey("IR-SA~UTR~123456789")
      result shouldBe ""
    }
  }

  "getIdentifierFromEnrolmentKey" should {
    "return the identifier value for MTD VAT" in {
      val result = RegimeHelper.getIdentifierFromEnrolmentKey("HMRC-MTD-VAT~VRN~123456789")
      result shouldBe "123456789"
    }

    "return an empty string for any other regime" in {
      val result = RegimeHelper.getIdentifierFromEnrolmentKey("IR-SA~UTR~123456789")
      result shouldBe ""
    }
  }

  "constructMTDVATEnrolmentKey" should {
    "append the VRN to MTD VAT" in {
      val result = RegimeHelper.constructMTDVATEnrolmentKey("123456789")
      result shouldBe "HMRC-MTD-VAT~VRN~123456789"
    }
  }
}
