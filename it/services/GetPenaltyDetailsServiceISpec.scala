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

package services

import connectors.parsers.getPenaltyDetails.GetPenaltyDetailsParser._
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealLevelEnum, AppealStatusEnum}
import models.getPenaltyDetails.latePayment.{LPPDetails, LPPDetailsMetadata, LPPPenaltyCategoryEnum, LPPPenaltyStatusEnum, LatePaymentPenalty}
import models.getPenaltyDetails.lateSubmission._
import models.getPenaltyDetails.{GetPenaltyDetails, Totalisations}
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDate

class GetPenaltyDetailsServiceISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val service: GetPenaltyDetailsService = injector.instanceOf[GetPenaltyDetailsService]

  "getDataFromPenaltyServiceForVATCVRN" when {
    val getPenaltyDetailsModel: GetPenaltyDetails = GetPenaltyDetails(
      totalisations = Some(
        Totalisations(
          LSPTotalValue = Some(200),
          penalisedPrincipalTotal = Some(2000),
          LPPPostedTotal = Some(165.25),
          LPPEstimatedTotal = Some(15.26),
          LPIPostedTotal = Some(1968.2),
          LPIEstimatedTotal = Some(7)
        )
      ),
      lateSubmissionPenalty = Some(
        LateSubmissionPenalty(
          summary = LSPSummary(
            activePenaltyPoints = 10,
            inactivePenaltyPoints = 12,
            regimeThreshold = 10,
            penaltyChargeAmount = 684.25
          ),
          details = Seq(
            LSPDetails(
              penaltyNumber = "12345678901234",
              penaltyOrder = "01",
              penaltyCategory = LSPPenaltyCategoryEnum.Point,
              penaltyStatus = LSPPenaltyStatusEnum.Active,
              penaltyCreationDate = LocalDate.of(2022, 10, 30),
              penaltyExpiryDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              FAPIndicator = Some("X"),
              lateSubmissions = Some(
                Seq(
                  LateSubmission(
                    taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
                    taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
                    taxPeriodDueDate = Some(LocalDate.of(2023, 2, 7)),
                    returnReceiptDate = Some(LocalDate.of(2023, 2, 1)),
                    taxReturnStatus = TaxReturnStatusEnum.Fulfilled
                  )
                )
              ),
              expiryReason = Some("FAP"),
              appealInformation = Some(
                Seq(
                  AppealInformationType(
                    appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)
                  )
                )
              ),
              chargeDueDate = Some(LocalDate.of(2022, 10, 30)),
              chargeOutstandingAmount = Some(200),
              chargeAmount = Some(200)
            )
          )
        )
      ),
      latePaymentPenalty = Some(LatePaymentPenalty(
        details = Some(
          Seq(
            LPPDetails(
              penaltyCategory = LPPPenaltyCategoryEnum.FirstPenalty,
              principalChargeReference = "1234567890",
              penaltyChargeReference = Some("1234567890"),
              penaltyChargeCreationDate = LocalDate.of(2022, 10, 30),
              penaltyStatus = LPPPenaltyStatusEnum.Accruing,
              appealInformation = Some(Seq(AppealInformationType(appealStatus = Some(AppealStatusEnum.Unappealable), appealLevel = Some(AppealLevelEnum.HMRC)))),
              principalChargeBillingFrom = LocalDate.of(2022, 10, 30),
              principalChargeBillingTo = LocalDate.of(2022, 10, 30),
              principalChargeDueDate = LocalDate.of(2022, 10, 30),
              communicationsDate = LocalDate.of(2022, 10, 30),
              penaltyAmountOutstanding = Some(99.99),
              penaltyAmountPaid = Some(1001.45),
              LPP1LRDays = Some("15"),
              LPP1HRDays = Some("31"),
              LPP2Days = Some("31"),
              LPP1HRCalculationAmount = Some(99.99),
              LPP1LRCalculationAmount = Some(99.99),
              LPP2Percentage = Some(BigDecimal(4.00).setScale(2)),
              LPP1LRPercentage = Some(BigDecimal(2.00).setScale(2)),
              LPP1HRPercentage = Some(BigDecimal(2.00).setScale(2)),
              penaltyChargeDueDate = LocalDate.of(2022, 10, 30),
              principalChargeLatestClearing = None,
              metadata = LPPDetailsMetadata()
            )
          )
        )
      ))
    )

    s"call the connector and return a successful result" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789")
      val result = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isRight shouldBe true
      result.right.get shouldBe GetPenaltyDetailsSuccessResponse(getPenaltyDetailsModel)
    }

    s"the response body is not well formed: $GetPenaltyDetailsMalformed" in {
      mockStubResponseForGetPenaltyDetails(Status.OK, "123456789", body = Some("""
          {
           "lateSubmissionPenalty": {
             "summary": {}
             }
           }
          """))
      val result = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetPenaltyDetailsMalformed
    }

    s"the response body contains NO_DATA_FOUND for 404 response - returning $GetPenaltyDetailsNoContent" in {
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
      mockStubResponseForGetPenaltyDetails(Status.NOT_FOUND, "123456789", body = Some(noDataFoundBody))
      val result = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetPenaltyDetailsNoContent
    }

    s"an unknown response is returned from the connector - $GetPenaltyDetailsFailureResponse" in {
      mockStubResponseForGetPenaltyDetails(Status.IM_A_TEAPOT, "123456789")
      val result = await(service.getDataFromPenaltyServiceForVATCVRN("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetPenaltyDetailsFailureResponse(Status.IM_A_TEAPOT)
    }
  }
}

