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

import base.{LPPDetailsBase, LSPDetailsBase, LogCapturing, SpecBase}
import config.AppConfig
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.getFinancialDetails.MainTransactionEnum.ManualLPP
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, LineItemDetails, MainTransactionEnum}
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum}
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import models.hipPenaltyDetails.appealInfo.AppealStatusEnum
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{mock, reset, when}
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{Gone, Ok}
import play.api.test.Helpers._
import services.auditing.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys
import utils.PagerDutyHelper.PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class PenaltiesFrontendServiceSpec extends SpecBase with LogCapturing with LPPDetailsBase with LSPDetailsBase {
  implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  val mockAuditService: AuditService = mock(classOf[AuditService])
  val mockFinancialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])
  val dateHelper: DateHelper = injector.instanceOf[DateHelper]

  val manualLPP: LPPDetails = LPPDetails(
    penaltyCategory = LPPPenaltyCategoryEnum.ManualLPP,
    penaltyChargeReference = None,
    principalChargeReference = "penalty123456",
    penaltyChargeCreationDate = Some(LocalDate.of(2023, 4, 1)),
    penaltyStatus = LPPPenaltyStatusEnum.Posted,
    penaltyAmountAccruing = 0,
    penaltyAmountPosted = BigDecimal(100),
    penaltyAmountOutstanding = Some(BigDecimal(45)),
    penaltyAmountPaid = Some(BigDecimal(55)),
    principalChargeMainTransaction = ManualLPP,
    principalChargeBillingFrom = LocalDate.of(2023, 4, 1),
    principalChargeBillingTo = LocalDate.of(2023, 4, 1),
    principalChargeDueDate = LocalDate.of(2023, 4, 1),
    None, None, None, None, None, None, None, None, None, None, None, None, None, LPPDetailsMetadata(mainTransaction = Some(ManualLPP))
  )

  val getPenaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = Some(
      LatePaymentPenalty(
        Some(
          Seq(
            lpp2.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted, penaltyChargeReference = Some("123456790")),
            lpp1PrincipalChargeDueToday.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted, penaltyChargeReference = Some("123456789"))
          )
        )
      )
    ),
    breathingSpace = None
  )

  class Setup {
    reset(mockAppConfig)
    reset(mockAuditService)
    reset(mockFinancialDetailsService)
    val sampleFinancialDetails: FinancialDetails = FinancialDetails(
      documentDetails = Some(Seq(
        DocumentDetails(
          chargeReferenceNumber = Some("1234567890"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnSecondLPP)))),
          documentTotalAmount = Some(100.00),
          issueDate = Some(LocalDate.now())),
        DocumentDetails(
          chargeReferenceNumber = Some("123456789"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))),
          documentTotalAmount = Some(100.00),
          issueDate = Some(LocalDate.now()))
      )),
      totalisation = None
    )
    val penaltiesFrontendService: PenaltiesFrontendService = new PenaltiesFrontendService(mockFinancialDetailsService, mockAppConfig, dateHelper, mockAuditService)
  }

  private val enrolmentKey = AgnosticEnrolmentKey(Regime("VATC"), IdType("VRN"), Id("123456789"))

  "combineAPIData" should {
    "combine the financial details totalisations - if totalisations already present" in new Setup {
      val penaltyDetails = GetPenaltyDetails(
        totalisations = Some(
          Totalisations(
            LSPTotalValue = Some(123.45),
            penalisedPrincipalTotal = Some(321.45),
            LPPPostedTotal = None,
            LPPEstimatedTotal = None,
            totalAccountOverdue = None,
            totalAccountPostedInterest = None,
            totalAccountAccruingInterest = None
          )
        ),
        lateSubmissionPenalty = None,
        latePaymentPenalty = None,
        breathingSpace = None
      )

      val financialDetailsWithClearedItems: FinancialDetails = FinancialDetails(
        documentDetails = None,
        totalisation = None
      )

      val financialDetailsWithoutClearedItems: FinancialDetails = FinancialDetails(
        totalisation = Some(FinancialDetailsTotalisation(
          regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(123.45))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = Some(43.21)))
        )),
        documentDetails = None
      )

      val expectedResult: Totalisations = Totalisations(
        LSPTotalValue = Some(123.45),
        penalisedPrincipalTotal = Some(321.45),
        LPPPostedTotal = None,
        LPPEstimatedTotal = None,
        totalAccountOverdue = Some(123.45),
        totalAccountPostedInterest = Some(12.34),
        totalAccountAccruingInterest = Some(43.21)
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetails, financialDetailsWithClearedItems, financialDetailsWithoutClearedItems)
      result.totalisations.isDefined shouldBe true
      result.totalisations.get shouldBe expectedResult
    }

    "combine the financial details totalisations - if totalisations already present (updating LPPPostedAmount with manual LPP amount)" in new Setup {
      val penaltyDetails = GetPenaltyDetails(
        totalisations = Some(
          Totalisations(
            LSPTotalValue = Some(123.45),
            penalisedPrincipalTotal = Some(321.45),
            LPPPostedTotal = Some(100.45),
            LPPEstimatedTotal = None,
            totalAccountOverdue = None,
            totalAccountPostedInterest = None,
            totalAccountAccruingInterest = None
          )
        ),
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(manualLPP)))),
        breathingSpace = None
      )

      val financialDetailsWithClearedItems: FinancialDetails = FinancialDetails(
        documentDetails = None,
        totalisation = None
      )

      val financialDetailsWithoutClearedItems: FinancialDetails = FinancialDetails(
        totalisation = Some(FinancialDetailsTotalisation(
          regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(123.45))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = Some(43.21)))
        )),
        documentDetails = None
      )

      val expectedResult: Totalisations = Totalisations(
        LSPTotalValue = Some(123.45),
        penalisedPrincipalTotal = Some(321.45),
        LPPPostedTotal = Some(145.45),
        LPPEstimatedTotal = None,
        totalAccountOverdue = Some(123.45),
        totalAccountPostedInterest = Some(12.34),
        totalAccountAccruingInterest = Some(43.21)
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetails, financialDetailsWithClearedItems, financialDetailsWithoutClearedItems)
      result.totalisations.isDefined shouldBe true
      result.totalisations.get shouldBe expectedResult
    }


    "combine the financial details totalisations - if totalisations NOT already present" in new Setup {
      val penaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = None,
        breathingSpace = None
      )

      val financialDetailsWithClearedItems: FinancialDetails = FinancialDetails(
        documentDetails = None,
        totalisation = None
      )

      val financialDetailsWithoutClearedItems: FinancialDetails = FinancialDetails(
        totalisation = Some(FinancialDetailsTotalisation(
          regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(123.45))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = None))
        )),
        documentDetails = None
      )

      val expectedResult: Totalisations = Totalisations(
        LSPTotalValue = None,
        penalisedPrincipalTotal = None,
        LPPPostedTotal = None,
        LPPEstimatedTotal = None,
        totalAccountOverdue = Some(123.45),
        totalAccountPostedInterest = Some(12.34),
        totalAccountAccruingInterest = None
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetails, financialDetailsWithClearedItems, financialDetailsWithoutClearedItems)
      result.totalisations.isDefined shouldBe true
      result.totalisations.get shouldBe expectedResult
    }

    "combine the financial details totalisations - if totalisations NOT already present (inserting manual LPP amount)" in new Setup {
      val penaltyDetails = GetPenaltyDetails(
        totalisations = None,
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(Some(Seq(manualLPP)))),
        breathingSpace = None
      )

      val financialDetailsWithClearedItems: FinancialDetails = FinancialDetails(
        documentDetails = None,
        totalisation = None
      )

      val financialDetailsWithoutClearedItems: FinancialDetails = FinancialDetails(
        totalisation = Some(FinancialDetailsTotalisation(
          regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(123.45))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(12.34), totalAccountAccruingInterest = None))
        )),
        documentDetails = None
      )

      val expectedResult: Totalisations = Totalisations(
        LSPTotalValue = None,
        penalisedPrincipalTotal = None,
        LPPPostedTotal = Some(45.00),
        LPPEstimatedTotal = None,
        totalAccountOverdue = Some(123.45),
        totalAccountPostedInterest = Some(12.34),
        totalAccountAccruingInterest = None
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetails, financialDetailsWithClearedItems, financialDetailsWithoutClearedItems)
      result.totalisations.isDefined shouldBe true
      result.totalisations.get shouldBe expectedResult
    }

    "construct a manual LPP from the 1811 data and insert a generated LPP entry into the API 1812 data (appending the new data)" in new Setup {
        val penaltyDetailsWithFirstAndSecondPenalty = GetPenaltyDetails(
          totalisations = None, lateSubmissionPenalty = None,
          latePaymentPenalty = Some(LatePaymentPenalty(
            details = Some(
              Seq(
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                  principalChargeReference = "1234567890",
                  penaltyChargeReference = Some("1234567890"),
                  penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                  penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                  appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                  principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                  principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                  principalChargeDueDate = LocalDate.of(2022, 10, 30),
                  communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                  penaltyAmountOutstanding = None,
                  penaltyAmountPaid = None,
                  penaltyAmountPosted = 0,
                  LPP1LRDays = Some("15"),
                  LPP1HRDays = Some("31"),
                  LPP2Days = Some("31"),
                  LPP1HRCalculationAmount = Some(99.99),
                  LPP1LRCalculationAmount = Some(99.99),
                  LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                  LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                  LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                  penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                  principalChargeLatestClearing = None,
                  metadata = LPPDetailsMetadata(),
                  penaltyAmountAccruing = BigDecimal(99.9),
                  principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                  vatOutstandingAmount = Some(BigDecimal(123.45))
                ),
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                  principalChargeReference = "1234567890",
                  penaltyChargeReference = Some("1234567890"),
                  penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                  penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                  appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                  principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                  principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                  principalChargeDueDate = LocalDate.of(2022, 10, 30),
                  communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                  penaltyAmountOutstanding = None,
                  penaltyAmountPaid = None,
                  penaltyAmountPosted = 0,
                  LPP1LRDays = Some("15"),
                  LPP1HRDays = Some("31"),
                  LPP2Days = Some("31"),
                  LPP1HRCalculationAmount = Some(99.99),
                  LPP1LRCalculationAmount = Some(99.99),
                  LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                  LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                  LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                  penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                  principalChargeLatestClearing = None,
                  metadata = LPPDetailsMetadata(),
                  penaltyAmountAccruing = BigDecimal(99.9),
                  principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                  vatOutstandingAmount = Some(BigDecimal(123.45))
                )
              )
            )
          )),
          breathingSpace = None
        )

        val financialDetails: FinancialDetails = FinancialDetails(
          documentDetails = Some(Seq(
            DocumentDetails(
              chargeReferenceNumber = Some("1234567890"),
              documentOutstandingAmount = Some(123.45),
              lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnSecondLPP)))),
              documentTotalAmount = Some(100),
              issueDate = Some(LocalDate.of(2022, 1, 1))),
            DocumentDetails(
              chargeReferenceNumber = Some("1234567890"),
              documentOutstandingAmount = Some(123.45),
              lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))),
              documentTotalAmount = Some(100),
              issueDate = Some(LocalDate.of(2022, 1, 1))),
            DocumentDetails(
              chargeReferenceNumber = Some("penalty123456"),
              documentOutstandingAmount = Some(BigDecimal(45)),
              lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.ManualLPP)))),
              documentTotalAmount = Some(BigDecimal(100)),
              issueDate = Some(LocalDate.of(2023, 4, 1)))
          )),
          totalisation = None
        )

        val expectedResult = LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 0,
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(
                  mainTransaction = Some(MainTransactionEnum.VATReturnCharge),
                ),
                penaltyAmountAccruing = BigDecimal(99.9),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 0,
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(
                  mainTransaction = Some(MainTransactionEnum.VATReturnCharge),
                ),
                penaltyAmountAccruing = BigDecimal(99.9),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              ),
              manualLPP
            )
          )
        )

        val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithFirstAndSecondPenalty, financialDetails, FinancialDetails(None, None))
        result.latePaymentPenalty.isDefined shouldBe true
        result.latePaymentPenalty.get shouldBe expectedResult
    }

    "construct a manual LPP from the 1811 data and insert a generated LPP entry into the API 1812 data (appending the new data - defaulting the penaltyAmountOutstanding to documentTotalAmount when not present for penaltyAmountPaid)" in new Setup {
      val penaltyDetailsWithFirstAndSecondPenalty = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 0,
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(99.9),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 0,
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(99.9),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = Some(BigDecimal(123.45))
              )
            )
          )
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnSecondLPP)))),
            documentTotalAmount = Some(100),
            issueDate = Some(LocalDate.of(2022, 1, 1))),
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))),
            documentTotalAmount = Some(100),
            issueDate = Some(LocalDate.of(2022, 1, 1))),
          DocumentDetails(
            chargeReferenceNumber = Some("penalty123456"),
            documentOutstandingAmount = Some(BigDecimal(100)),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.ManualLPP)))),
            documentTotalAmount = Some(BigDecimal(100)),
            issueDate = Some(LocalDate.of(2023, 4, 1)))
        )),
        totalisation = None
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = None,
              penaltyAmountPosted = 0,
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnCharge),
              ),
              penaltyAmountAccruing = BigDecimal(99.9),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
              vatOutstandingAmount = Some(BigDecimal(123.45))
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = None,
              penaltyAmountPosted = 0,
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnCharge),
              ),
              penaltyAmountAccruing = BigDecimal(99.9),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
              vatOutstandingAmount = Some(BigDecimal(123.45))
            ),
            manualLPP.copy(penaltyAmountPaid = Some(0), penaltyAmountOutstanding = Some(BigDecimal(100)))
          )
        )
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithFirstAndSecondPenalty, financialDetails, FinancialDetails(None, None))
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

    "combined the financial details when there is no outstanding amount (there is no vatOutstandingAmount)" in new Setup {
      val penaltyDetailsWithFirstPenalty: GetPenaltyDetails = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Accruing,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 0,
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
                metadata = LPPDetailsMetadata(),
                penaltyAmountAccruing = BigDecimal(99.9),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = None
              )
            )
          )
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = None,
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))),
            documentTotalAmount = Some(100.00),
            issueDate = Some(LocalDate.now()))
        )),
        totalisation = None
      )

      val expectedResult: LatePaymentPenalty = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = None,
              penaltyAmountPaid = None,
              penaltyAmountPosted = 0,
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata(
                mainTransaction = Some(MainTransactionEnum.VATReturnCharge),
              ),
              penaltyAmountAccruing = BigDecimal(99.9),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge,
              vatOutstandingAmount = None
            )
          )
        )
      )

      val result: GetPenaltyDetails = penaltiesFrontendService.combineAPIData(penaltyDetailsWithFirstPenalty, financialDetails, FinancialDetails(None, None))
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }
  }

  "handleAndCombineGetFinancialDetailsData" should {

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the first API 1811 call fails" in new Setup {
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Left(FinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))))
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, enrolmentKey, None)(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the first API 1811 call response body is malformed" in new Setup {
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Left(FinancialDetailsMalformed)))
        .thenReturn(Future.successful(Left(FinancialDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, enrolmentKey, None)(fakeRequest, implicitly, implicitly))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true
        }
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the first API 1811 call returns no data" in new Setup {
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(FinancialDetailsFailureResponse(Status.NOT_FOUND))))
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, enrolmentKey, Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return NO_CONTENT (${Status.NO_CONTENT}) when the first API 1811 call returns no data (DATA_NOT_FOUND response)" in new Setup {
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(FinancialDetailsNoContent)))
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, enrolmentKey, Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.NO_CONTENT
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the second API 1811 call fails" in new Setup {
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(
        Future.successful(Right(FinancialDetailsSuccessResponse(sampleFinancialDetails))),
        Future.successful(Left(FinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR)))
      )
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, enrolmentKey, None)(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the second API 1811 call response body is malformed" in new Setup {
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(
        Future.successful(Right(FinancialDetailsSuccessResponse(sampleFinancialDetails))),
        Future.successful(Left(FinancialDetailsMalformed))
      )
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, enrolmentKey, None)(fakeRequest, implicitly, implicitly))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true
        }
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the second API 1811 call returns no data" in new Setup {
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(
        Future.successful(Right(FinancialDetailsSuccessResponse(sampleFinancialDetails))),
        Future.successful(Left(FinancialDetailsFailureResponse(Status.NOT_FOUND)))
      )
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, enrolmentKey, Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return OK (${Status.OK}) when the second API 1811 call returns no data (DATA_NOT_FOUND response) - default totalisations field" in new Setup {
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(
          Future.successful(Right(FinancialDetailsSuccessResponse(sampleFinancialDetails))),
          Future.successful(Left(FinancialDetailsNoContent))
        )
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, enrolmentKey, Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.OK
    }

    s"return OK (${Status.OK}) when the first 1811 call returns no data (if penalty data contains no LPPs)" in new Setup {
      val penaltyDetails: GetPenaltyDetails = getPenaltyDetails.copy(latePaymentPenalty = None)
      when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(FinancialDetailsNoContent)))
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(penaltyDetails, enrolmentKey, Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(penaltyDetails)
    }
  }

  "handleErrorResponseFromFinancialDetails" should {
    s"return NOT_FOUND (${Status.NOT_FOUND})" when {
      "the status returned from the call is NOT_FOUND" in new Setup {
        val response: FinancialDetailsFailureResponse = FinancialDetailsFailureResponse(NOT_FOUND)
        val result: Result = penaltiesFrontendService.handleErrorResponseFromGetFinancialDetails(response, enrolmentKey)(Ok(""))
        result.header.status shouldBe NOT_FOUND
      }
    }

    s"return INTERNAL_SERVER_ERROR (${Status.INTERNAL_SERVER_ERROR})" when {
      "the status returned from the call is not matched" in new Setup {
        val response: FinancialDetailsFailureResponse = FinancialDetailsFailureResponse(IM_A_TEAPOT)
        val result: Result = penaltiesFrontendService.handleErrorResponseFromGetFinancialDetails(response, enrolmentKey)(Ok(""))
        result.header.status shouldBe INTERNAL_SERVER_ERROR
      }

      "the response body is malformed - logging a PagerDuty" in new Setup {
        val response: FinancialDetailsFailure = FinancialDetailsMalformed
        withCaptureOfLoggingFrom(logger) { logs =>
          val result: Result = penaltiesFrontendService.handleErrorResponseFromGetFinancialDetails(response, enrolmentKey)(Ok(""))
          result.header.status shouldBe INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true
        }
      }
    }

    "return a custom response" when {
      s"$FinancialDetailsNoContent is returned from the call" in new Setup {
        val response: FinancialDetailsFailure = FinancialDetailsNoContent
        val result: Result = penaltiesFrontendService.handleErrorResponseFromGetFinancialDetails(response, enrolmentKey)(Gone(""))
        result.header.status shouldBe GONE
      }
    }
  }
}
