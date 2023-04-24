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

package utils

import java.time.LocalDate

import base.{LPPDetailsBase, SpecBase}
import config.AppConfig
import config.featureSwitches.FeatureSwitching
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.latePayment.LatePaymentPenalty
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, when}
import play.api.Configuration
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

class EstimatedLPP1FilterSpec extends SpecBase with LPPDetailsBase {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockConfig = mock(classOf[Configuration])
  val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
  val featureSwitching = new FeatureSwitching {
    override implicit val config: Configuration = mockConfig
  }

  sys.props -= featureSwitching.TIME_MACHINE_NOW
  sys.props -= featureSwitching.ESTIMATED_LPP1_FILTER_END_DATE

  val mockAppConfig: AppConfig = new AppConfig(mockConfig, mockServicesConfig)
  val filter = new EstimatedLPP1Filter

  "tryJsonParseOrJsString" should {
    "return a JsValue when the body can be parsed" in {
      val sampleJsonResponse = Json.obj(
        "foo" -> "bar"
      )
      val expectedResult = Json.parse(
        """
          |{
          | "foo": "bar"
          |}
          |""".stripMargin)
      val result = filter.tryJsonParseOrJsSting(sampleJsonResponse.toString())
      result shouldBe expectedResult
    }

    "return a JsString when body cannot be parsed" in {
      val result = filter.tryJsonParseOrJsSting("error")
      val expectedResult: JsString = JsString("error")
      result shouldBe expectedResult
    }
  }

  "returnFilteredLPPs" should {
    "filter LPP1s with a principle charge due within the filtering window" in {
      val penaltiesDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
          lpp2,
          lpp1PrincipalChargeDueToday,
          lpp1PrincipalChargeDueTomorrow,
          lpp1PrincipalChargeDueYesterday,
          lpp1PrincipalChargeDueYesterdayPosted
        )))),
        breathingSpace = None
      )

      val expectedResult = GetPenaltyDetails(totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(lpp2, lpp1PrincipalChargeDueTomorrow, lpp1PrincipalChargeDueYesterdayPosted)))),
        breathingSpace = None
      )

      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(Some(LocalDate.now().toString))

      featureSwitching.setEstimatedLPP1FilterEndDate(Some(LocalDate.now()))

      val result = filter.returnFilteredLPPs(penaltiesDetails, "foo", "bar", "123456789")
      result shouldBe expectedResult
    }

    "remove all LPP1s with a principle charge due within the filtering window" in {
      val penaltiesDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
          lpp1PrincipalChargeDueToday,
          lpp1PrincipalChargeDueYesterday
        )))),
        breathingSpace = None
      )

      val expectedResult = GetPenaltyDetails(None, None, Some(LatePaymentPenalty(Some(List()))), None)

      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(Some(LocalDate.now().toString))

      featureSwitching.setEstimatedLPP1FilterEndDate(Some(LocalDate.now()))

      val result = filter.returnFilteredLPPs(penaltiesDetails, "foo", "bar", "123456789")
      result shouldBe expectedResult
    }

    "remove no LPP1s when their principle charges are NOT due in the filtering window" in {
      val penaltiesDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
          lpp2,
          lpp1PrincipalChargeDueTomorrow,
          lpp1PrincipalChargeDueYesterdayPosted
        )))),
        breathingSpace = None
      )

      val expectedResult = GetPenaltyDetails(None, None, Some(LatePaymentPenalty(Some(List(
        lpp2, lpp1PrincipalChargeDueTomorrow, lpp1PrincipalChargeDueYesterdayPosted
      )))), None)

      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(Some(LocalDate.now().toString))

      featureSwitching.setEstimatedLPP1FilterEndDate(Some(LocalDate.now()))

      val result = filter.returnFilteredLPPs(penaltiesDetails, "foo", "bar", "123456789")
      result shouldBe expectedResult
    }
    }
}
