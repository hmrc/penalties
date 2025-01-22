/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import models.{EnrolmentKey, TaxRegime}
import org.mockito.Mockito.{mock, verifyNoInteractions}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Results.BadRequest
import play.api.mvc.{Result, Results}

import scala.concurrent.Await.result
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.duration.Duration.Inf
import scala.util.{Success, Try}

class PackageSpec extends AnyWordSpec with Matchers {

  "tryPlus.andThen" should {
    val someKey = EnrolmentKey(TaxRegime.ITSA, EnrolmentKey.NINO, "AB123456C")
    val good: Future[Result] = successful(Results.Ok("doubleplusgood"))
    val fail: Future[Result] = failed(new Exception("ungood"))

    "convert parse exception to a bad request result, without calling logic body" in {
      val mockBody: EnrolmentKey => Future[Result] = mock(classOf[EnrolmentKey => Future[Result]])
      val futureResult = Try(throw new Exception("doubleplusungood")).andThen ( mockBody )
      val response = result(futureResult, Inf)
      response shouldBe BadRequest("doubleplusungood")
      verifyNoInteractions (mockBody)
    }
    "return result of logic nobody normally" in {
      Try(someKey).andThen ( _ => good ) shouldBe good
    }
    "passthrough exceptions in logic body" in {
      Try(someKey).andThen ( _ => fail ) shouldBe fail
    }
  }

  "testComposeEnrolmentKey with idType" should {
    import utils.TestUtils.TryPlus
    "Fail for an unsupported regime" in {
      controllers.composeEnrolmentKey("FOO","BAR","FOOBAR") shouldFailWith "Unsupported regime: FOO"
    }
    "Fail for an unsupported key type" in {
      controllers.composeEnrolmentKey("ITSA","BAR","FOOBAR") shouldFailWith "Unsupported id type: BAR"
    }
    "Fail for an invalid VAT VRN" in {
      controllers.composeEnrolmentKey("VAT","VRN","FOOBAR") shouldFailWith "Invalid VATC VRN: FOOBAR"
    }
    "Fail for an invalid ITSA NINO" in {
      controllers.composeEnrolmentKey("ITSA","NINO","FOOBAR") shouldFailWith "Invalid ITSA NINO: FOOBAR"
    }
    "Support a valid VAT VRN" in {
      controllers.composeEnrolmentKey("VAT","VRN","123456789") shouldBe Success(EnrolmentKey(TaxRegime.VAT, EnrolmentKey.VRN, "123456789"))
    }
    "Support a valid ITSA NINO" in {
      controllers.composeEnrolmentKey("ITSA","NINO","AB123456C") shouldBe Success(EnrolmentKey(TaxRegime.ITSA, EnrolmentKey.NINO, "AB123456C"))
    }
  }

  "testComposeEnrolmentKey without idType" should {
    "Support a valid VAT VRN implicitly" in {
      controllers.composeEnrolmentKey("VAT","123456789") shouldBe Success(EnrolmentKey(TaxRegime.VAT, EnrolmentKey.VRN, "123456789"))
    }
    "Support a valid ITSA NINO implicitly" in {
      controllers.composeEnrolmentKey("ITSA","AB123456C") shouldBe Success(EnrolmentKey(TaxRegime.ITSA, EnrolmentKey.NINO, "AB123456C"))
    }
  }

}
