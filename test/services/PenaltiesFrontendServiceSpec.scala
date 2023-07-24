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
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import models.getFinancialDetails.MainTransactionEnum.{VATReturnFirstLPP, VATReturnSecondLPP}
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.getFinancialDetails.{DocumentDetails, FinancialDetails, LineItemDetails, MainTransactionEnum}
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import org.mockito.Matchers
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
  val mockAppealService: AppealService = mock(classOf[AppealService])
  val mockAuditService: AuditService = mock(classOf[AuditService])
  val mockGetFinancialDetailsService: GetFinancialDetailsService = mock(classOf[GetFinancialDetailsService])
  val dateHelper: DateHelper = injector.instanceOf[DateHelper]

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
    reset(mockAppealService)
    reset(mockAuditService)
    reset(mockGetFinancialDetailsService)
    val sampleFinancialDetails: FinancialDetails = FinancialDetails(
      documentDetails = Some(Seq(
        DocumentDetails(
          chargeReferenceNumber = Some("123456790"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnSecondLPP))))),
        DocumentDetails(
          chargeReferenceNumber = Some("123456789"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))))
      )),
      totalisation = None
    )
    val penaltiesFrontendService: PenaltiesFrontendService = new PenaltiesFrontendService(mockGetFinancialDetailsService, mockAppConfig, dateHelper, mockAuditService)
  }

  "combineAPIData" should {
    "update both first and second penalty separately (no TTP)" in new Setup {
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
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
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
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
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
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnSecondLPP))))),
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))))
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
                mainTransaction = Some(MainTransactionEnum.VATReturnCharge), outstandingAmount = None
              ),
              penaltyAmountAccruing = BigDecimal(99.9),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
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
                mainTransaction = Some(MainTransactionEnum.VATReturnCharge), outstandingAmount = None
              ),
              penaltyAmountAccruing = BigDecimal(99.9),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithFirstAndSecondPenalty, financialDetails, FinancialDetails(None, None))
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

    "update only the first penalty if no second penalty present" in new Setup {
      val penaltyDetailsWithFirstPenalty = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                penaltyAmountPosted = 1101.44,
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
                  timeToPay = Some(Seq(TimeToPay(
                    TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                    TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                  )))
                ),
                penaltyAmountAccruing = BigDecimal(0),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
              )
            )
          )
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("1234567890"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))))
        )),
        totalisation = None
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              penaltyAmountPosted = 1101.44,
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
                mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
                outstandingAmount = Some(123.45),
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithFirstPenalty, financialDetails, FinancialDetails(None, None))
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

    "update only the second penalty if no first penalty present" in new Setup {
      val penaltyDetailsWithSecondPenalty = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.SecondPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                penaltyAmountPosted = 1101.44,
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
                  timeToPay = Some(Seq(TimeToPay(
                    TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                    TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                  )))
                ),
                penaltyAmountAccruing = BigDecimal(0),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
              )
            )
          )
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("1234567890"),
          documentOutstandingAmount = Some(123.45),
          lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnSecondLPP)))))
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
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              penaltyAmountPosted = 1101.44,
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
                mainTransaction = Some(MainTransactionEnum.VATReturnSecondLPP),
                outstandingAmount = Some(123.45),
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithSecondPenalty, financialDetails, FinancialDetails(None, None))
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

    "update two different principal charge penalties" in new Setup {
      val penaltyDetailsWithDifferentPrincipals = GetPenaltyDetails(
        totalisations = None, lateSubmissionPenalty = None,
        latePaymentPenalty = Some(LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567891",
                penaltyChargeReference = Some("1234567891"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                penaltyAmountPosted = 1101.44,
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
                  timeToPay = Some(Seq(TimeToPay(
                    TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                    TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                  )))
                ),
                penaltyAmountAccruing = BigDecimal(0),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
              ),
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = Some(99.99),
                penaltyAmountPaid = Some(1001.45),
                penaltyAmountPosted = 1101.44,
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
                  timeToPay = Some(Seq(TimeToPay(
                    TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                    TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                  )))
                ),
                penaltyAmountAccruing = BigDecimal(0),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
              )
            )
          )
        )),
        breathingSpace = None
      )

      val financialDetails: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(
          DocumentDetails(
            chargeReferenceNumber = Some("1234567891"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.OfficersAssessmentFirstLPP))))),
          DocumentDetails(
            chargeReferenceNumber = Some("1234567890"),
            documentOutstandingAmount = Some(123.45),
            lineItemDetails = Some(Seq(LineItemDetails(Some(MainTransactionEnum.VATReturnFirstLPP)))))
        )),
        totalisation = None
      )

      val expectedResult = LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567891",
              penaltyChargeReference = Some("1234567891"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              penaltyAmountPosted = 1101.44,
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
                mainTransaction = Some(MainTransactionEnum.OfficersAssessmentFirstLPP),
                outstandingAmount = Some(123.45),
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            ),
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyStatus = LPPPenaltyStatusEnum.Posted,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = Some(LocalDate.of(2022, 10, 30)),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              penaltyAmountPosted = 1101.44,
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
                mainTransaction = Some(MainTransactionEnum.VATReturnFirstLPP),
                outstandingAmount = Some(123.45),
                timeToPay = Some(Seq(TimeToPay(
                  TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                  TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                )))
              ),
              penaltyAmountAccruing = BigDecimal(0),
              principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
            )
          )
        )
      )

      val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithDifferentPrincipals, financialDetails, FinancialDetails(None, None))
      result.latePaymentPenalty.isDefined shouldBe true
      result.latePaymentPenalty.get shouldBe expectedResult
    }

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
          regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(123.45))),
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
          regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(123.45))),
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

    def successfulAppealOrChargeReversedTest(appealStatusEnum: AppealStatusEnum.Value, appealStatusFriendlyName: String) = {
      s"not combine the financial data when the LPP has been appealed successfully (for $appealStatusFriendlyName)" in new Setup {
        val penaltyDetailsWithSecondPenalty = GetPenaltyDetails(
          totalisations = None, lateSubmissionPenalty = None,
          latePaymentPenalty = Some(LatePaymentPenalty(
            details = Some(
              Seq(
                LPPDetails(
                  penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                  principalChargeReference = "1234567890",
                  penaltyChargeReference = Some("1234567890"),
                  penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                  penaltyStatus = LPPPenaltyStatusEnum.Posted,
                  appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(appealStatusEnum), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                  principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                  principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                  principalChargeDueDate = LocalDate.of(2022, 10, 30),
                  communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                  penaltyAmountOutstanding = None,
                  penaltyAmountPaid = None,
                  penaltyAmountPosted = 1101.44,
                  LPP1LRDays = Some("15"),
                  LPP1HRDays = Some("31"),
                  LPP2Days = Some("31"),
                  LPP1HRCalculationAmount = Some(99.99),
                  LPP1LRCalculationAmount = Some(99.99),
                  LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                  LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                  LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                  penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                  principalChargeLatestClearing = Some(LocalDate.of(2022, 10, 30)),
                  metadata = LPPDetailsMetadata(
                    timeToPay = None
                  ),
                  penaltyAmountAccruing = BigDecimal(0),
                  principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
                )
              )
            )
          )),
          breathingSpace = None
        )

        val financialDetails: FinancialDetails = FinancialDetails(
          documentDetails = None,
          totalisation = None
        )

        val expectedResult = LatePaymentPenalty(
          details = Some(
            Seq(
              LPPDetails(
                penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
                principalChargeReference = "1234567890",
                penaltyChargeReference = Some("1234567890"),
                penaltyChargeCreationDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyStatus = LPPPenaltyStatusEnum.Posted,
                appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(appealStatusEnum), appealLevel = Some(AppealLevelEnum.HMRC), appealDescription = Some("Some value")))),
                principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
                principalChargeBillingTo = LocalDate.of(2022, 10, 30),
                principalChargeDueDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                penaltyAmountOutstanding = None,
                penaltyAmountPaid = None,
                penaltyAmountPosted = 1101.44,
                LPP1LRDays = Some("15"),
                LPP1HRDays = Some("31"),
                LPP2Days = Some("31"),
                LPP1HRCalculationAmount = Some(99.99),
                LPP1LRCalculationAmount = Some(99.99),
                LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
                LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = Some(LocalDate.of(2022, 10, 30)),
                metadata = LPPDetailsMetadata(
                  mainTransaction = Some(MainTransactionEnum.VATReturnCharge),
                  outstandingAmount = None,
                  timeToPay = None
                ),
                penaltyAmountAccruing = BigDecimal(0),
                principalChargeMainTransaction = MainTransactionEnum.VATReturnCharge
              )
            )
          )
        )

        val result = penaltiesFrontendService.combineAPIData(penaltyDetailsWithSecondPenalty, financialDetails, FinancialDetails(None, None))
        result.latePaymentPenalty.isDefined shouldBe true
        result.latePaymentPenalty.get shouldBe expectedResult
      }
    }

    successfulAppealOrChargeReversedTest(AppealStatusEnum.Upheld, "Upheld")
    successfulAppealOrChargeReversedTest(AppealStatusEnum.AppealUpheldChargeAlreadyReversed, "AppealUpheldChargeAlreadyReversed")
    successfulAppealOrChargeReversedTest(AppealStatusEnum.AppealRejectedChargeAlreadyReversed, "AppealRejectedChargeAlreadyReversed")
  }

  "handleAndCombineGetFinancialDetailsData" should {

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the first API 1811 call fails" in new Setup {
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR))))
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", None)(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the first API 1811 call response body is malformed" in new Setup {
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Left(GetFinancialDetailsMalformed)))
        .thenReturn(Future.successful(Left(GetFinancialDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", None)(fakeRequest, implicitly, implicitly))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true
        }
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the first API 1811 call returns no data" in new Setup {
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(Status.NOT_FOUND))))
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return NO_CONTENT (${Status.NO_CONTENT}) when the first API 1811 call returns no data (DATA_NOT_FOUND response)" in new Setup {
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.NO_CONTENT
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the second API 1811 call fails" in new Setup {
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(
        Future.successful(Right(GetFinancialDetailsSuccessResponse(sampleFinancialDetails))),
        Future.successful(Left(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR)))
      )
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", None)(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the second API 1811 call response body is malformed" in new Setup {
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(
        Future.successful(Right(GetFinancialDetailsSuccessResponse(sampleFinancialDetails))),
        Future.successful(Left(GetFinancialDetailsMalformed))
      )
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result = await(penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", None)(fakeRequest, implicitly, implicitly))
          result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(PagerDutyKeys.MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true
        }
      }
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the second API 1811 call returns no data" in new Setup {
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(
        Future.successful(Right(GetFinancialDetailsSuccessResponse(sampleFinancialDetails))),
        Future.successful(Left(GetFinancialDetailsFailureResponse(Status.NOT_FOUND)))
      )
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return OK (${Status.OK}) when the second API 1811 call returns no data (DATA_NOT_FOUND response) - default totalisations field" in new Setup {
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(
          Future.successful(Right(GetFinancialDetailsSuccessResponse(sampleFinancialDetails))),
          Future.successful(Left(GetFinancialDetailsNoContent))
        )
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.OK
    }

    s"return OK (${Status.OK}) when the first 1811 call returns no data (if penalty data contains no LPPs)" in new Setup {
      val penaltyDetails: GetPenaltyDetails = getPenaltyDetails.copy(latePaymentPenalty = None)
      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(penaltyDetails, "123456789", Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(penaltyDetails)
    }

    "combine the 1812 and 1811 data and return a new GetPenaltyDetails model" in new Setup {
      val financialDetailsWithoutTotalisations: FinancialDetails = FinancialDetails(
        documentDetails = Some(Seq(DocumentDetails(
          chargeReferenceNumber = Some("123456790"),
          documentOutstandingAmount = Some(0.00),
          lineItemDetails = Some(Seq(LineItemDetails(Some(VATReturnSecondLPP))))),
          DocumentDetails(
            chargeReferenceNumber = Some("123456789"),
            documentOutstandingAmount = Some(0.00),
            lineItemDetails = Some(Seq(LineItemDetails(Some(VATReturnFirstLPP)))))
        )),
        totalisation = None
      )

      val financialDetailsWithTotalisations: FinancialDetails = FinancialDetails(
        documentDetails = None,
        totalisation = Some(FinancialDetailsTotalisation(
          regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
          interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
        ))
      )

      when(mockGetFinancialDetailsService.getFinancialDetails(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(
          Future.successful(Right(GetFinancialDetailsSuccessResponse(financialDetailsWithoutTotalisations))),
          Future.successful(Right(GetFinancialDetailsSuccessResponse(financialDetailsWithTotalisations)))
        )

      val penaltyDetails = getPenaltyDetails.copy(
        totalisations = Some(
          Totalisations(
            LSPTotalValue = None,
            penalisedPrincipalTotal = None,
            LPPPostedTotal = None,
            LPPEstimatedTotal = None,
            totalAccountOverdue = Some(1000),
            totalAccountPostedInterest = Some(123.45),
            totalAccountAccruingInterest = Some(23.45)
          )
        ),
        lateSubmissionPenalty = None,
        latePaymentPenalty = Some(
          LatePaymentPenalty(
            Some(
              Seq(
                lpp2.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted,
                  penaltyChargeReference = Some("123456790"),
                  metadata = LPPDetailsMetadata(
                    mainTransaction = Some(VATReturnSecondLPP),
                    outstandingAmount = Some(0),
                    timeToPay = Some(Seq(TimeToPay(
                      TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                      TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                    )))
                  )
                ),
                lpp1PrincipalChargeDueToday.copy(penaltyStatus = LPPPenaltyStatusEnum.Posted,
                  penaltyChargeReference = Some("123456789"),
                  metadata = LPPDetailsMetadata(
                    mainTransaction = Some(VATReturnFirstLPP),
                    outstandingAmount = Some(0),
                    timeToPay = Some(Seq(TimeToPay(
                      TTPStartDate = Some(LocalDate.of(2022, 1, 1)),
                      TTPEndDate = Some(LocalDate.of(2022, 12, 31))
                    )))
                  ))
              )
            )
          )
        )
      )

      val result = penaltiesFrontendService.handleAndCombineGetFinancialDetailsData(getPenaltyDetails, "123456789", Some(""))(fakeRequest, implicitly, implicitly)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(penaltyDetails)
    }
  }

  "handleErrorResponseFromGetFinancialDetails" should {
    s"return NOT_FOUND (${Status.NOT_FOUND})" when {
      "the status returned from the call is NOT_FOUND" in new Setup {
        val response: GetFinancialDetailsFailureResponse = GetFinancialDetailsFailureResponse(NOT_FOUND)
        val result: Result = penaltiesFrontendService.handleErrorResponseFromGetFinancialDetails(response, "123456789")(Ok(""))
        result.header.status shouldBe NOT_FOUND
      }
    }

    s"return INTERNAL_SERVER_ERROR (${Status.INTERNAL_SERVER_ERROR})" when {
      "the status returned from the call is not matched" in new Setup {
        val response: GetFinancialDetailsFailureResponse = GetFinancialDetailsFailureResponse(IM_A_TEAPOT)
        val result: Result = penaltiesFrontendService.handleErrorResponseFromGetFinancialDetails(response, "123456789")(Ok(""))
        result.header.status shouldBe INTERNAL_SERVER_ERROR
      }

      "the response body is malformed - logging a PagerDuty" in new Setup {
        val response: GetFinancialDetailsFailure = GetFinancialDetailsMalformed
        withCaptureOfLoggingFrom(logger) { logs =>
          val result: Result = penaltiesFrontendService.handleErrorResponseFromGetFinancialDetails(response, "123456789")(Ok(""))
          result.header.status shouldBe INTERNAL_SERVER_ERROR
          logs.exists(_.getMessage.contains(MALFORMED_RESPONSE_FROM_1811_API.toString)) shouldBe true
        }
      }
    }

    "return a custom response" when {
      s"$GetFinancialDetailsNoContent is returned from the call" in new Setup {
        val response: GetFinancialDetailsFailure = GetFinancialDetailsNoContent
        val result: Result = penaltiesFrontendService.handleErrorResponseFromGetFinancialDetails(response, "123456789")(Gone(""))
        result.header.status shouldBe GONE
      }
    }
  }
}
