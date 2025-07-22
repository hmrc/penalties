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

package connectors.getPenaltyDetails

import java.time.LocalDate
import config.featureSwitches.{CallAPI1812HIP, FeatureSwitching}
import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser.{
  HIPPenaltyDetailsFailureResponse,
  HIPPenaltyDetailsMalformed,
  HIPPenaltyDetailsNoContent,
  HIPPenaltyDetailsResponse,
  HIPPenaltyDetailsSuccessResponse
}
import models.{AgnosticEnrolmentKey, Regime, IdType, Id}
import models.hipPenaltyDetails.PenaltyDetails
import models.hipPenaltyDetails.appealInfo.{
  AppealStatusEnum,
}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.http.Status.IM_A_TEAPOT
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier}
import utils.{IntegrationSpecCommonBase}
import utils.HIPPenaltiesWiremock
import models.hipPenaltyDetails.lateSubmission.{LateSubmissionPenalty, LSPSummary, LateSubmission, LSPDetails}
import models.hipPenaltyDetails.lateSubmission.{LSPPenaltyCategoryEnum, LSPPenaltyStatusEnum, TaxReturnStatusEnum}
import models.hipPenaltyDetails.appealInfo.AppealInformationType
import java.time.Instant

class HIPPenaltyDetailsConnectorISpec
    extends IntegrationSpecCommonBase
    with HIPPenaltiesWiremock
    with FeatureSwitching
    with TableDrivenPropertyChecks {
  val processingDate = Instant.parse("2025-04-24T12:00:00Z")

  class Setup {
    val connector: HIPPenaltyDetailsConnector =
      injector.instanceOf[HIPPenaltyDetailsConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()

  }

  Table(
    ("Regime", "IdType", "Id"),
    (Regime("VATC"), IdType("VRN"), Id("123456789")),
    (Regime("ITSA"), IdType("NINO"), Id("AB123456C"))
  ).forEvery { (regime, idType, id) =>
    val aKey = AgnosticEnrolmentKey(regime, idType, id)
    s"getPenaltyDetails for $regime" should {
      "return a successful response when called" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        mockResponseForHIPPenaltyDetails(Status.OK, regime, idType, id)
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey)(hc))
        result.isRight shouldBe true
      }

      "return a successful response with the penaltyCategory returning as a point when it is blank in the body" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val bodyWithEmptyCategory: String = """
        {
          "success": {
            "processingDate": "2025-04-24T12:00:00Z",
            "penaltyData": {
          "lsp": {
          "lspSummary": {
            "activePenaltyPoints": 2,
            "inactivePenaltyPoints": 2,
            "pocAchievementDate": "2021-04-23",
            "regimeThreshold": 2,
            "penaltyChargeAmount": 200.00
          },
          "lspDetails": [
            {
              "penaltyCategory": " ",
              "penaltyNumber": "123456793",
              "penaltyOrder": "1",
              "penaltyCreationDate": "2021-04-23",
              "penaltyExpiryDate": "2021-04-23",
              "penaltyStatus": "INACTIVE",
              "appealInformation": [
                {
                  "appealStatus": "99"
                }
              ],
              "chargeAmount": 200.00,
              "chargeOutstandingAmount": 200.00,
              "communicationsDate": "2021-04-23",
              "triggeringProcess": "P123",
              "lateSubmissions": [
                {
                  "lateSubmissionID": "001",
                  "taxPeriod": "23AA",
                  "taxPeriodStartDate": "2021-04-23",
                  "taxPeriodEndDate": "2021-04-23",
                  "taxPeriodDueDate": "2021-04-23",
                  "returnReceiptDate": "2021-04-23",
                  "taxReturnStatus": "OPEN"
                }
              ]
            }
          ]
        }
            }
          }
        }
        """.stripMargin

        val model: PenaltyDetails = PenaltyDetails(
          processingDate,
          totalisations = None,
          lateSubmissionPenalty = Some(
            LateSubmissionPenalty(
              summary = LSPSummary(
                activePenaltyPoints = 2,
                inactivePenaltyPoints = 2,
                regimeThreshold = 2,
                penaltyChargeAmount = 200.00,
                pocAchievementDate = Some(LocalDate.of(2021, 4, 23))
              ),
              details = Seq(
                LSPDetails(
                  penaltyNumber = "123456793",
                  penaltyOrder = Some("1"),
                  penaltyCategory = Some(LSPPenaltyCategoryEnum.Point),
                  penaltyStatus = LSPPenaltyStatusEnum.Inactive,
                  penaltyCreationDate = LocalDate.of(2021, 4, 23),
                  penaltyExpiryDate = LocalDate.of(2021, 4, 23),
                  communicationsDate = Some(LocalDate.of(2021, 4, 23)),
                  fapIndicator = None,
                  lateSubmissions = Some(
                    Seq(
                      LateSubmission(
                        lateSubmissionID = "001",
                        incomeSource = None,
                        taxPeriod = Some("23AA"),
                        taxPeriodStartDate = Some(LocalDate.of(2021, 4, 23)),
                        taxPeriodEndDate = Some(LocalDate.of(2021, 4, 23)),
                        taxPeriodDueDate = Some(LocalDate.of(2021, 4, 23)),
                        returnReceiptDate = Some(LocalDate.of(2021, 4, 23)),
                        taxReturnStatus = Some(TaxReturnStatusEnum.Open)
                      )
                    )
                  ),
                  expiryReason = None,
                  appealInformation = Some(
                    Seq(
                      AppealInformationType(
                        appealStatus = Some(AppealStatusEnum.Unappealable),
                        appealLevel = None,
                        appealDescription = None
                      )
                    )
                  ),
                  chargeDueDate = None,
                  chargeOutstandingAmount = Some(200.00),
                  chargeAmount = Some(200.00),
                  triggeringProcess = Some("P123"),
                  chargeReference = None
                )
              )
            )
          ),
          latePaymentPenalty = None,
          breathingSpace = None
        )
        mockResponseForHIPPenaltyDetails(
          Status.OK,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(bodyWithEmptyCategory)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey)(hc))
        result.isRight shouldBe true

        result.getOrElse(
          HIPPenaltyDetailsSuccessResponse(
            model.copy(lateSubmissionPenalty = None)
          )
        ) shouldBe HIPPenaltyDetailsSuccessResponse(model)
      }

      s"return a $HIPPenaltyDetailsMalformed response when called" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val malformedBody =
          """
          {
           "lateSubmissionPenalty": {
             "summary": {}
             }
           }
          """
        mockResponseForHIPPenaltyDetails(
          Status.OK,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(malformedBody)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(
          HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT)
        ) shouldBe HIPPenaltyDetailsMalformed
      }

      s"return a $HIPPenaltyDetailsFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val errorBody = """{"error": {"code": "ISE", "message": "Internal Server Error", "logId": "123"}}"""
        mockResponseForHIPPenaltyDetails(
          Status.INTERNAL_SERVER_ERROR,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(errorBody)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left
          .getOrElse(HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT))
          .asInstanceOf[HIPPenaltyDetailsFailureResponse]
          .status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      s"return a $HIPPenaltyDetailsFailureResponse when the response status is ISE (${Status.SERVICE_UNAVAILABLE})" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val errorBody = """{"error": {"code": "SERVICE_UNAVAILABLE", "message": "Service Unavailable", "logId": "123"}}"""
        mockResponseForHIPPenaltyDetails(
          Status.SERVICE_UNAVAILABLE,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(errorBody)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left
          .getOrElse(HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT))
          .asInstanceOf[HIPPenaltyDetailsFailureResponse]
          .status shouldBe Status.SERVICE_UNAVAILABLE
      }

      s"return a $HIPPenaltyDetailsFailureResponse when the response status is NOT FOUND (${Status.NOT_FOUND})" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val errorBody = """{"error": {"code": "NOT_FOUND", "message": "Not Found", "logId": "123"}}"""
        mockResponseForHIPPenaltyDetails(
          Status.NOT_FOUND,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(errorBody)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left
          .getOrElse(HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT))
          .asInstanceOf[HIPPenaltyDetailsFailureResponse]
          .status shouldBe Status.NOT_FOUND
      }

      s"return a $HIPPenaltyDetailsNoContent when the response status is UNPROCESSABLE_ENTITY FOUND (${Status.UNPROCESSABLE_ENTITY}) but with NO_DATA_FOUND in JSON body" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val noDataFoundBody: String = """{"errors":{"processingDate":"2025-03-03", "code":"016", "text":"Invalid ID Number"}}"""
        mockResponseForHIPPenaltyDetails(
          Status.UNPROCESSABLE_ENTITY,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(noDataFoundBody)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(
          HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT)
        ) shouldBe HIPPenaltyDetailsNoContent
      }

      s"return a $HIPPenaltyDetailsFailureResponse when the response status is NO CONTENT (${Status.NO_CONTENT})" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        mockResponseForHIPPenaltyDetails(
          Status.NO_CONTENT,
          regime,
          aKey.idType,
          aKey.id
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(
          HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT))
            .asInstanceOf[HIPPenaltyDetailsFailureResponse]
            .status shouldBe Status.NO_CONTENT
      }

      s"return a $HIPPenaltyDetailsFailureResponse when the response status is CONFLICT (${Status.CONFLICT})" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val errorBody = """{"error": {"code": "CONFLICT", "message": "Conflict", "logId": "123"}}"""
        mockResponseForHIPPenaltyDetails(
          Status.CONFLICT,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(errorBody)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left
          .getOrElse(HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT))
          .asInstanceOf[HIPPenaltyDetailsFailureResponse]
          .status shouldBe Status.CONFLICT
      }

      s"return a $HIPPenaltyDetailsFailureResponse when the response status is UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY})" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val errorBody = """{"error": {"code": "UNPROCESSABLE_ENTITY", "message": "Unprocessable Entity", "logId": "123"}}"""
        mockResponseForHIPPenaltyDetails(
          Status.UNPROCESSABLE_ENTITY,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(errorBody)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left
          .getOrElse(HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT))
          .asInstanceOf[HIPPenaltyDetailsFailureResponse]
          .status shouldBe Status.UNPROCESSABLE_ENTITY
      }

      s"return a $HIPPenaltyDetailsFailureResponse when the response status is ISE (${Status.BAD_REQUEST})" in new Setup {
        enableFeatureSwitch(CallAPI1812HIP)
        val errorBody = """{"error": {"code": "BAD_REQUEST", "message": "Bad Request", "logId": "123"}}"""
        mockResponseForHIPPenaltyDetails(
          Status.BAD_REQUEST,
          regime,
          aKey.idType,
          aKey.id,
          body = Some(errorBody)
        )
        val result: HIPPenaltyDetailsResponse =
          await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left
          .getOrElse(HIPPenaltyDetailsFailureResponse(IM_A_TEAPOT))
          .asInstanceOf[HIPPenaltyDetailsFailureResponse]
          .status shouldBe Status.BAD_REQUEST
      }
    }
  }
}