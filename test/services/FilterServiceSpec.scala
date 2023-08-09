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

package services

import base.{LPPDetailsBase, LSPDetailsBase, SpecBase}
import config.AppConfig
import config.featureSwitches.FeatureSwitching
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.AppealStatusEnum
import models.getPenaltyDetails.latePayment.LatePaymentPenalty
import models.getPenaltyDetails.lateSubmission.{LSPSummary, LateSubmissionPenalty}
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, when}
import play.api.Configuration
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class FilterServiceSpec extends SpecBase with LSPDetailsBase with LPPDetailsBase {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockConfig = mock(classOf[Configuration])
  val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
  val featureSwitching = new FeatureSwitching {
    override implicit val config: Configuration = mockConfig
  }

  sys.props -= featureSwitching.TIME_MACHINE_NOW
  sys.props -= featureSwitching.ESTIMATED_LPP1_FILTER_END_DATE

  val mockAppConfig: AppConfig = new AppConfig(mockConfig, mockServicesConfig)
  val filterService = new FilterService

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
      val result = filterService.tryJsonParseOrJsString(sampleJsonResponse.toString())
      result shouldBe expectedResult
    }

    "return a JsString when body cannot be parsed" in {
      val result = filterService.tryJsonParseOrJsString("error")
      val expectedResult: JsString = JsString("error")
      result shouldBe expectedResult
    }
  }

  "filterEstimatedLPP1DuringPeriodOfFamiliarisation" should {
    "filter LPP1s with a principal charge due within the filtering window" in {
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


      featureSwitching.setEstimatedLPP1FilterEndDate(Some(LocalDate.now()))

      val result = filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails, "foo", "bar", "123456789")
      result shouldBe expectedResult
    }

    "remove all LPP1s with a principal charge due within the filtering window" in {
      val penaltiesDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
          lpp1PrincipalChargeDueToday,
          lpp1PrincipalChargeDueYesterday
        )))),
        breathingSpace = None
      )

      val expectedResult = GetPenaltyDetails(None, None, None, None)

      when(mockConfig.getOptional[String](any())(any()))
        .thenReturn(Some(LocalDate.now().toString))

      featureSwitching.setEstimatedLPP1FilterEndDate(Some(LocalDate.now()))

      val result = filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails, "foo", "bar", "123456789")
      result shouldBe expectedResult
    }

    "remove no LPP1s when their principal charges are NOT due in the filtering window" in {
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

      val result = filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltiesDetails, "foo", "bar", "123456789")
      result shouldBe expectedResult
    }
  }

  "filterPenaltiesWith9xAppealStatus" should {
    "filter LSPs with an appeal status of 91,92, 93 or 94" in {
      val lspSummary = LSPSummary(
        activePenaltyPoints = 2,
        inactivePenaltyPoints = 2,
        regimeThreshold = 2,
        penaltyChargeAmount = 200,
        PoCAchievementDate = Some(LocalDate.now().plusYears(2))
      )

      val penaltiesDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = Some(LateSubmissionPenalty(summary = lspSummary, details = Seq(
          lspThresholdDetails,
          lspThresholdDetailsWithAppealStatus(AppealStatusEnum.AppealRejectedPointAlreadyRemoved.toString),
          lspPointDetails,
          lspPointDetailsWithAppealStatus(AppealStatusEnum.AppealUpheldPointAlreadyRemoved.toString)
        ))),
        latePaymentPenalty = None,
        breathingSpace = None
      )

      val expectedResult = GetPenaltyDetails(totalisations = None,
        lateSubmissionPenalty = Some(LateSubmissionPenalty(summary = lspSummary, details = Seq(
          lspThresholdDetails,
          lspPointDetails))),
        latePaymentPenalty = None,
        breathingSpace = None
      )

      val result = filterService.filterPenaltiesWith9xAppealStatus(penaltiesDetails)("foo", "bar", "123456789")
      result.lateSubmissionPenalty.get.details shouldBe
        expectedResult.lateSubmissionPenalty.get.details
    }

    "filter LPPs with an appeal status of 91,92, 93 or 94" in {
      val penaltiesDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
          lpp2,
          lpp2WithAppealStatus(AppealStatusEnum.AppealUpheldChargeAlreadyReversed.toString),
          lpp1PrincipalChargeDueToday,
          lpp1PrincipalChargeDueTodayAppealStatus(AppealStatusEnum.AppealRejectedChargeAlreadyReversed.toString))))),
        breathingSpace = None
      )

      val expectedResult = GetPenaltyDetails(totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
          lpp2,
          lpp1PrincipalChargeDueToday)))),
        breathingSpace = None
      )

      val result = filterService.filterPenaltiesWith9xAppealStatus(penaltiesDetails)("foo", "bar", "123456789")
      result.latePaymentPenalty.get.details shouldBe expectedResult.latePaymentPenalty.get.details
    }
  }

  "filter LSPs and LPPs with an appeal status of 91,92, 93 or 94" in {
    val lspSummary = LSPSummary(
      activePenaltyPoints = 2,
      inactivePenaltyPoints = 2,
      regimeThreshold = 2,
      penaltyChargeAmount = 200,
      PoCAchievementDate = Some(LocalDate.now().plusYears(2))
    )

    val penaltiesDetails = GetPenaltyDetails(
      totalisations = None,
      lateSubmissionPenalty = Some(LateSubmissionPenalty(summary = lspSummary, details = Seq(
        lspThresholdDetails,
        lspThresholdDetailsWithAppealStatus(AppealStatusEnum.AppealRejectedPointAlreadyRemoved.toString),
        lspPointDetails,
        lspPointDetailsWithAppealStatus(AppealStatusEnum.AppealUpheldPointAlreadyRemoved.toString)))),
      latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
        lpp2,
        lpp2WithAppealStatus(AppealStatusEnum.AppealUpheldChargeAlreadyReversed.toString),
        lpp1PrincipalChargeDueToday,
        lpp1PrincipalChargeDueTodayAppealStatus(AppealStatusEnum.AppealRejectedChargeAlreadyReversed.toString))))),
      breathingSpace = None
    )

    val expectedResult = GetPenaltyDetails(totalisations = None,
      lateSubmissionPenalty = Some(LateSubmissionPenalty(summary = lspSummary, details = Seq(
        lspThresholdDetails,
        lspPointDetails))),
      latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
        lpp2,
        lpp1PrincipalChargeDueToday)))),
      breathingSpace = None
    )

    val result = filterService.filterPenaltiesWith9xAppealStatus(penaltiesDetails)("foo", "bar", "123456789")
    result.lateSubmissionPenalty.get.details shouldBe expectedResult.lateSubmissionPenalty.get.details
    result.latePaymentPenalty.get.details shouldBe expectedResult.latePaymentPenalty.get.details
  }

  "filter all LSPs and LPPs with an appeal status of 91,92, 93 or 94" in {
    val lspSummary = LSPSummary(
      activePenaltyPoints = 2,
      inactivePenaltyPoints = 2,
      regimeThreshold = 2,
      penaltyChargeAmount = 200,
      PoCAchievementDate = Some(LocalDate.now().plusYears(2))
    )

    val penaltiesDetails = GetPenaltyDetails(
      totalisations = None,
      lateSubmissionPenalty = Some(LateSubmissionPenalty(summary = lspSummary, details = Seq(
        lspThresholdDetailsWithAppealStatus(AppealStatusEnum.AppealRejectedPointAlreadyRemoved.toString),
        lspPointDetailsWithAppealStatus(AppealStatusEnum.AppealUpheldPointAlreadyRemoved.toString)))),
      latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
        lpp2WithAppealStatus(AppealStatusEnum.AppealUpheldChargeAlreadyReversed.toString),
        lpp1PrincipalChargeDueTodayAppealStatus(AppealStatusEnum.AppealRejectedChargeAlreadyReversed.toString))))),
      breathingSpace = None
    )

    val expectedResult = GetPenaltyDetails(totalisations = None,
      lateSubmissionPenalty = None,
      latePaymentPenalty = None,
      breathingSpace = None
    )

    val result = filterService.filterPenaltiesWith9xAppealStatus(penaltiesDetails)("foo", "bar", "123456789")
    result.lateSubmissionPenalty shouldBe expectedResult.lateSubmissionPenalty
    result.latePaymentPenalty shouldBe expectedResult.latePaymentPenalty
  }

  "Not filter LSPs and LPPs when there no penalties with appeal status 91, 92, 93 or 94" in {
    val lspSummary = LSPSummary(
      activePenaltyPoints = 2,
      inactivePenaltyPoints = 2,
      regimeThreshold = 2,
      penaltyChargeAmount = 200,
      PoCAchievementDate = Some(LocalDate.now().plusYears(2))
    )

    val penaltiesDetails = GetPenaltyDetails(
      totalisations = None,
      lateSubmissionPenalty = Some(LateSubmissionPenalty(summary = lspSummary, details = Seq(
        lspThresholdDetails,
        lspPointDetails))),
      latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
        lpp2,
        lpp1PrincipalChargeDueToday)))),
      breathingSpace = None
    )

    val expectedResult = GetPenaltyDetails(totalisations = None,
      lateSubmissionPenalty = Some(LateSubmissionPenalty(summary = lspSummary, details = Seq(
        lspThresholdDetails,
        lspPointDetails))),
      latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(
        lpp2,
        lpp1PrincipalChargeDueToday)))),
      breathingSpace = None
    )

    val result = filterService.filterPenaltiesWith9xAppealStatus(penaltiesDetails)("foo", "bar", "123456789")
    result.lateSubmissionPenalty.get.details shouldBe expectedResult.lateSubmissionPenalty.get.details
    result.latePaymentPenalty.get.details shouldBe expectedResult.latePaymentPenalty.get.details
  }
}
