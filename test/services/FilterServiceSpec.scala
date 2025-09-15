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

package services

import base.SpecBase
import config.AppConfig
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsString, Json}
import services.RegimeFilterService.tryJsonParseOrJsString

import java.time.LocalDate

class FilterServiceSpec extends SpecBase with MockitoSugar {

  val mockAppConfig: AppConfig = mock[AppConfig]
  implicit val filterService: FilterService = new FilterService()(mockAppConfig)
  implicit val loggingContext: LoggingContext = LoggingContext("TestService", "testMethod", "VATC~VRN~123456789")

  val pastDate: LocalDate = LocalDate.of(2020, 1, 1)
  val futureDate: LocalDate = LocalDate.of(2030, 1, 1)

  "RegimeFilterService" should {

    "filterEstimatedLPP1DuringPeriodOfFamiliarisation" should {
      "filter out LPP1 penalties that are accruing and within filter window" in {
        when(mockAppConfig.withinLPP1FilterWindow(any[LocalDate])).thenReturn(true)

                 val lppToFilter = LPPDetails(
           penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
           penaltyChargeReference = Some("REF123"),
           principalChargeReference = "123456789",
           penaltyChargeCreationDate = Some(pastDate),
           penaltyStatus = LPPPenaltyStatusEnum.Accruing,
           penaltyAmountAccruing = BigDecimal(100),
           penaltyAmountPosted = BigDecimal(0),
           penaltyAmountOutstanding = None,
           penaltyAmountPaid = None,
           principalChargeMainTransaction = models.getFinancialDetails.MainTransactionEnum.VATReturnCharge,
           principalChargeBillingFrom = pastDate,
           principalChargeBillingTo = pastDate,
           principalChargeDueDate = pastDate,
           LPP1LRDays = None,
           LPP1HRDays = None,
           LPP2Days = None,
           LPP1HRCalculationAmount = None,
           LPP1LRCalculationAmount = None,
           LPP2Percentage = None,
           LPP1LRPercentage = None,
           LPP1HRPercentage = None,
           communicationsDate = Some(pastDate),
           penaltyChargeDueDate = Some(futureDate),
           appealInformation = None,
           principalChargeLatestClearing = None,
           vatOutstandingAmount = None,
           metadata = LPPDetailsMetadata()
         )

        val lppToKeep = lppToFilter.copy(
          penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty
        )

        val penaltyDetails = GetPenaltyDetails(
          totalisations = None,
          lateSubmissionPenalty = None,
          latePaymentPenalty = Some(LatePaymentPenalty(
            details = Some(Seq(lppToFilter, lppToKeep)),
            ManualLPPIndicator = Some(false)
          )),
          breathingSpace = None
        )

        val result = filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltyDetails)

        result.latePaymentPenalty.get.details.get should contain only lppToKeep
      }

      "not filter LPP1 penalties that are not within filter window" in {
        when(mockAppConfig.withinLPP1FilterWindow(any[LocalDate])).thenReturn(false)

          val lppToKeep = LPPDetails(
             penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
             penaltyChargeReference = Some("REF123"),
             principalChargeReference = "123456789",
             penaltyChargeCreationDate = Some(pastDate),
             penaltyStatus = LPPPenaltyStatusEnum.Accruing,
             penaltyAmountAccruing = BigDecimal(100),
             penaltyAmountPosted = BigDecimal(0),
             penaltyAmountOutstanding = None,
             penaltyAmountPaid = None,
             principalChargeMainTransaction = models.getFinancialDetails.MainTransactionEnum.VATReturnCharge,
             principalChargeBillingFrom = pastDate,
             principalChargeBillingTo = pastDate,
             principalChargeDueDate = pastDate,
             LPP1LRDays = None,
             LPP1HRDays = None,
             LPP2Days = None,
             LPP1HRCalculationAmount = None,
             LPP1LRCalculationAmount = None,
             LPP2Percentage = None,
             LPP1LRPercentage = None,
             LPP1HRPercentage = None,
             communicationsDate = Some(pastDate),
             penaltyChargeDueDate = Some(futureDate),
             appealInformation = None,
             principalChargeLatestClearing = None,
             vatOutstandingAmount = None,
             metadata = LPPDetailsMetadata()
           )

          val penaltyDetails = GetPenaltyDetails(
            totalisations = None,
            lateSubmissionPenalty = None,
            latePaymentPenalty = Some(LatePaymentPenalty(
              details = Some(Seq(lppToKeep)),
              ManualLPPIndicator = Some(false)
            )),
            breathingSpace = None
          )

          val result = filterService.filterEstimatedLPP1DuringPeriodOfFamiliarisation(penaltyDetails)

          result.latePaymentPenalty.get.details.get should contain(lppToKeep)
      }
    }

    "filterPenaltiesWith9xAppealStatus" should {
      "filter out penalties with 9x appeal status" in {
        val appealInfoToFilter = Seq(
          AppealInformationType(
            appealStatus = Some(AppealStatusEnum.AppealRejectedChargeAlreadyReversed),
            appealLevel = Some(AppealLevelEnum.HMRC),
            appealDescription = Some("Test appeal 91 status")
          )
        )

        val lppToFilter = LPPDetails(
          penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
          penaltyChargeReference = Some("REF123"),
          principalChargeReference = "123456789",
          penaltyChargeCreationDate = Some(pastDate),
          penaltyStatus = LPPPenaltyStatusEnum.Posted,
          penaltyAmountAccruing = BigDecimal(0),
          penaltyAmountPosted = BigDecimal(100),
          penaltyAmountOutstanding = Some(BigDecimal(100)),
          penaltyAmountPaid = None,
          principalChargeMainTransaction = models.getFinancialDetails.MainTransactionEnum.VATReturnCharge,
          principalChargeBillingFrom = pastDate,
          principalChargeBillingTo = pastDate,
          principalChargeDueDate = pastDate,
          LPP1LRDays = None,
          LPP1HRDays = None,
          LPP2Days = None,
          LPP1HRCalculationAmount = None,
          LPP1LRCalculationAmount = None,
          LPP2Percentage = None,
          LPP1LRPercentage = None,
          LPP1HRPercentage = None,
          communicationsDate = Some(pastDate),
          penaltyChargeDueDate = Some(futureDate),
          appealInformation = Some(appealInfoToFilter),
          principalChargeLatestClearing = None,
          vatOutstandingAmount = None,
          metadata = LPPDetailsMetadata()
        )

        val lppToKeep = lppToFilter.copy(appealInformation = None)

        val penaltyDetails = GetPenaltyDetails(
          totalisations = None,
          lateSubmissionPenalty = None,
          latePaymentPenalty = Some(LatePaymentPenalty(
            details = Some(Seq(lppToFilter, lppToKeep)),
            ManualLPPIndicator = Some(false)
          )),
          breathingSpace = None
        )

        val result = filterService.filterPenaltiesWith9xAppealStatus(penaltyDetails)

        result.latePaymentPenalty.get.details.get should contain only lppToKeep
      }

      "validate the ignored status against appealInfoType values" in {
        val appealInfoToFilter = Seq(
          AppealInformationType(
            appealStatus = Some(AppealStatusEnum.AppealRejectedChargeAlreadyReversed),
            appealLevel = Some(AppealLevelEnum.HMRC),
            appealDescription = Some("Test appeal 91 status")
          ),
          AppealInformationType(
            appealStatus = Some(AppealStatusEnum.AppealUpheldPointAlreadyRemoved),
            appealLevel = Some(AppealLevelEnum.HMRC),
            appealDescription = Some("Test appeal 92 status")
          ),
          AppealInformationType(
            appealStatus = Some(AppealStatusEnum.AppealUpheldChargeAlreadyReversed),
            appealLevel = Some(AppealLevelEnum.HMRC),
            appealDescription = Some("Test appeal 93 status")
          ),
          AppealInformationType(
            appealStatus = Some(AppealStatusEnum.AppealRejectedPointAlreadyRemoved),
            appealLevel = Some(AppealLevelEnum.HMRC),
            appealDescription = Some("Test appeal 94 status")
          ),
          AppealInformationType(
            appealStatus = Some(AppealStatusEnum.Upheld),
            appealLevel = Some(AppealLevelEnum.HMRC),
            appealDescription = Some("Test appeal B status")
          ),
        )
        val expectedIgnoreStatusCount: Int = 4
        val actualIgnoreStatusCount: Int = appealInfoToFilter.count(appealInfoToFilter =>
                                          AppealStatusEnum.ignoredStatuses.contains(appealInfoToFilter.appealStatus.get))
        assert(expectedIgnoreStatusCount == actualIgnoreStatusCount)
      }
    }

    "tryJsonParseOrJsString" should {
      "parse valid JSON" in {
        val validJson = """{"test": "value"}"""
        val result = tryJsonParseOrJsString(validJson)
        result shouldBe Json.parse(validJson)
      }

      "return JsString for invalid JSON" in {
        val invalidJson = "invalid json"
        val result = tryJsonParseOrJsString(invalidJson)
        result shouldBe JsString(invalidJson)
      }
    }
  }
} 