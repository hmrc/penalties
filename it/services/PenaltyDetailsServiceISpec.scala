/*
 * Copyright 2025 HM Revenue & Customs
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

import config.featureSwitches.FeatureSwitching
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser._
import models.getFinancialDetails.MainTransactionEnum
import models.penaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.penaltyDetails.breathingSpace.BreathingSpace
import models.penaltyDetails.latePayment._
import models.penaltyDetails.lateSubmission._
import models.penaltyDetails.{PenaltyDetails, Totalisations}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.{RegimeETMPWiremock, IntegrationSpecCommonBase}
import models.{AgnosticEnrolmentKey, Regime, IdType, Id}
import java.time.{LocalDate}

class PenaltyDetailsServiceISpec extends IntegrationSpecCommonBase with RegimeETMPWiremock with FeatureSwitching with TableDrivenPropertyChecks {
  setEnabledFeatureSwitches()
  val service: PenaltyDetailsService = injector.instanceOf[PenaltyDetailsService]
   Table(
    ("Regime", "IdType", "Id"),
    (Regime("VATC"), IdType("VRN"), Id("123456789")),
    (Regime("ITSA"), IdType("NINO"), Id("AB123456C")),
  ).forEvery { (regime, idType, id) =>

    val enrolmentKey = AgnosticEnrolmentKey(regime, idType, id) 

    s"getDataFromPenaltyService for $regime" when {
      val getPenaltyDetailsModel: PenaltyDetails = PenaltyDetails(
        mockInstant,
        totalisations = Some(
          Totalisations(
            lspTotalValue = Some(200),
            penalisedPrincipalTotal = Some(2000),
            lppPostedTotal = Some(165.25),
            lppEstimatedTotal = Some(15.26),
            totalAccountOverdue = None,
            totalAccountPostedInterest = None,
            totalAccountAccruingInterest = None
          )
        ),
        lateSubmissionPenalty = Some(
          LateSubmissionPenalty(
            summary = LSPSummary(
              activePenaltyPoints = 10,
              inactivePenaltyPoints = 12,
              regimeThreshold = 10,
              penaltyChargeAmount = 684.25,
              pocAchievementDate = Some(LocalDate.of(2022, 1, 1))
            ),
            details = Seq(
              LSPDetails(
                penaltyNumber = "12345678901234",
                penaltyOrder = Some("01"),
                penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
                penaltyStatus = LSPPenaltyStatusEnum.Active,
                penaltyCreationDate = LocalDate.of(2022, 10, 30),
                penaltyExpiryDate = LocalDate.of(2022, 10, 30),
                communicationsDate = Some(LocalDate.of(2022, 10, 30)),
                fapIndicator = None,
                lateSubmissions = Some(
                  Seq(
                    LateSubmission(
                      lateSubmissionID = "001",
                      incomeSource = None,
                      taxPeriod = Some("23AA"),
                      taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                      taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
                      taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
                      returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
                      taxReturnStatus = Some(TaxReturnStatusEnum.Fulfilled)
                    )
                  )
                ),
                expiryReason = None,
                appealInformation = Some(
                  Seq(
                    AppealInformationType(
                      appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = None, appealDescription = Some("Some value")
                    )
                  )
                ),
                chargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                chargeOutstandingAmount = Some(200),
                chargeAmount = Some(200),
                triggeringProcess = Some("P123"),
                chargeReference = Some("CHARGEREF1")
              )
            )
          )
        ),
        latePaymentPenalty = Some(LatePaymentPenalty(
          manualLPPIndicator = false,
          lppDetails = Some(
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
                lpp1LRDays = Some("15"),
                lpp1HRDays = Some("31"),
                lpp2Days = Some("31"),
                lpp1HRCalculationAmt = Some(99.99),
                lpp1LRCalculationAmt = Some(99.99),
                lpp2Percentage = Some(BigDecimal(4.00).setScale(2)),
                lpp1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
                lpp1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
                penaltyChargeDueDate = Some(LocalDate.of(2022, 10, 30)),
                principalChargeLatestClearing = None,
                timeToPay = Some(Seq(TimeToPay(
                  ttpStartDate = Some(LocalDate.of(2022, 1, 1)),
                  ttpEndDate = Some(LocalDate.of(2022, 12, 31))
                ))),
                principalChargeDocNumber = Some("DOC1"),
                principalChargeSubTr = Some("SUB1"),
                penaltyAmountAccruing = BigDecimal(99.99),
                principalChargeMainTr = MainTransactionEnum.VATReturnCharge,
                vatOutstandingAmount = None
              )
            )
          )
        )),
        breathingSpace = Some(Seq(BreathingSpace(bsStartDate = LocalDate.of(2023, 1, 1), bsEndDate = LocalDate.of(2023, 12, 31))))
      )

      s"call the connector and return a successful result" in {
        mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id)
        val result = await(service.getDataFromPenaltyService(enrolmentKey))
        result.isRight shouldBe true
        result.toOption.get shouldBe PenaltyDetailsSuccessResponse(getPenaltyDetailsModel)
      }

      s"the response body is not well formed: $PenaltyDetailsMalformed" in {
        mockStubResponseForPenaltyDetails(Status.OK, regime, idType, id, body = Some(
          """
          {
           "success": {
             "penaltyData": {
               "lsp": {
                 "lspSummary": {}
               }
             }
           }
          }
          """))
        val result = await(service.getDataFromPenaltyService(enrolmentKey))
        result.isLeft shouldBe true
        result.left.getOrElse(PenaltyDetailsFailureResponse(IM_A_TEAPOT)) shouldBe PenaltyDetailsMalformed
      }

      s"the response body contains NO_DATA_FOUND for 404 response - returning $PenaltyDetailsNoContent" in {
        val noDataFoundBody =
          """
            |{
            | "failures": [
            |   {
            |     "code": "NO_DATA_FOUND",
            |     "reason": "This is a reason"
            |   }
            | ]
            |}
            |""".stripMargin
        mockStubResponseForPenaltyDetails(Status.NOT_FOUND, regime, idType, id, body = Some(noDataFoundBody))
        val result = await(service.getDataFromPenaltyService(enrolmentKey))
        result.isLeft shouldBe true
        result.left.getOrElse(PenaltyDetailsFailureResponse(IM_A_TEAPOT)) shouldBe PenaltyDetailsNoContent
      }

      s"an unknown response is returned from the connector - $PenaltyDetailsFailureResponse" in {
        mockStubResponseForPenaltyDetails(Status.IM_A_TEAPOT, regime, idType, id)
        val result = await(service.getDataFromPenaltyService(enrolmentKey))
        result.isLeft shouldBe true
        result.left.getOrElse(PenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe PenaltyDetailsFailureResponse(Status.IM_A_TEAPOT)
      }
    }
  }
}

