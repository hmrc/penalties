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

import config.featureSwitches.{CallAPI1812ETMP, FeatureSwitching}
import connectors.parsers.getPenaltyDetails.PenaltyDetailsParser.{
  GetPenaltyDetailsFailureResponse,
  GetPenaltyDetailsMalformed,
  GetPenaltyDetailsNoContent,
  GetPenaltyDetailsResponse,
  GetPenaltyDetailsSuccessResponse
}
import models.getPenaltyDetails.GetPenaltyDetails
import models.getPenaltyDetails.appealInfo.{AppealInformationType, AppealStatusEnum}
import models.getPenaltyDetails.lateSubmission._
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.http.Status.IM_A_TEAPOT
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{IntegrationSpecCommonBase, RegimeETMPWiremock}

import java.time.LocalDate

class PenaltyDetailsConnectorISpec extends IntegrationSpecCommonBase with RegimeETMPWiremock with FeatureSwitching with TableDrivenPropertyChecks {

  class Setup {
    val connector: PenaltyDetailsConnector = injector.instanceOf[PenaltyDetailsConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()

  }

  Table(
    ("Regime", "IdType", "Id"),
    (Regime("VATC"), IdType("VRN"), Id("123456789")),
    (Regime("ITSA"), IdType("NINO"), Id("AB123456C")),
  ).forEvery { (regime, idType, id) =>

    val aKey = AgnosticEnrolmentKey(regime, idType, id) 
    s"getPenaltyDetails for $regime" should {
      "return a successful response when called" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id.value)
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey)(hc))
        result.isRight shouldBe true
      }

      "return a successful response with the penaltyCategory returning as a point when it is blank in the body" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        val bodyWithEmptyCategory: String =
          """
            |{
            |    "lateSubmissionPenalty": {
            |      "summary": {
            |        "activePenaltyPoints": 2,
            |        "inactivePenaltyPoints": 2,
            |        "PoCAchievementDate": "2021-04-23",
            |        "regimeThreshold": 2,
            |        "penaltyChargeAmount": 200.00
            |      },
            |      "details": [
            |        {
            |          "penaltyCategory": " ",
            |          "penaltyNumber": "123456793",
            |          "penaltyOrder": "1",
            |          "penaltyCreationDate": "2021-04-23",
            |          "penaltyExpiryDate": "2021-04-23",
            |          "penaltyStatus": "INACTIVE",
            |          "appealInformation": [
            |            {
            |              "appealStatus": "99"
            |            }
            |          ],
            |          "chargeAmount": 200.00,
            |          "chargeOutstandingAmount": 200.00,
            |          "communicationsDate": "2021-04-23",
            |          "triggeringProcess": "P123",
            |          "lateSubmissions": [
            |            {
            |              "lateSubmissionID": "001",
            |              "taxPeriod":  "23AA",
            |              "taxPeriodStartDate": "2021-04-23",
            |              "taxPeriodEndDate": "2021-04-23",
            |              "taxPeriodDueDate": "2021-04-23",
            |              "returnReceiptDate": "2021-04-23",
            |              "taxReturnStatus": "OPEN"
            |            }
            |          ]
            |        }
            |      ]
            |    }
            |}
            """.stripMargin

        val model: GetPenaltyDetails = GetPenaltyDetails(
          totalisations = None,
          lateSubmissionPenalty = Some(LateSubmissionPenalty(
            summary = LSPSummary(
              activePenaltyPoints = 2,
              inactivePenaltyPoints = 2,
              regimeThreshold = 2,
              penaltyChargeAmount = 200.00,
              PoCAchievementDate = Some(LocalDate.of(2021, 4, 23))
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
                FAPIndicator = None,
                lateSubmissions = Some(
                  Seq(
                    LateSubmission(
                      lateSubmissionID = "001",
                      taxPeriod = Some("23AA"),
                      taxPeriodStartDate = Some(LocalDate.of(2021, 4, 23)),
                      taxPeriodEndDate = Some(LocalDate.of(2021, 4, 23)),
                      taxPeriodDueDate = Some(LocalDate.of(2021, 4, 23)),
                      returnReceiptDate = Some(LocalDate.of(2021, 4, 23)),
                      taxReturnStatus = Some(TaxReturnStatusEnum.Open)
                    )
                  )),
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
          )),
          latePaymentPenalty = None,
          breathingSpace = None
        )
        mockResponseForGetPenaltyDetails(Status.OK, regime, aKey.idType, aKey.id.value, body = Some(bodyWithEmptyCategory))
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey)(hc))
        result.isRight shouldBe true

        result.getOrElse(GetPenaltyDetailsSuccessResponse(model.copy(lateSubmissionPenalty = None))) shouldBe GetPenaltyDetailsSuccessResponse(model)
      }

      s"return a $GetPenaltyDetailsMalformed response when called" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        val malformedBody =
          """
          {
           "lateSubmissionPenalty": {
             "summary": {}
             }
           }
          """
        mockResponseForGetPenaltyDetails(Status.OK, regime, aKey.idType, aKey.id.value, body = Some(malformedBody))
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetPenaltyDetailsMalformed
      }

      s"return a $GetPenaltyDetailsFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.INTERNAL_SERVER_ERROR, regime, aKey.idType, aKey.id.value)
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      s"return a $GetPenaltyDetailsFailureResponse when the response status is ISE (${Status.SERVICE_UNAVAILABLE})" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.SERVICE_UNAVAILABLE, regime, aKey.idType, aKey.id.value)
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.SERVICE_UNAVAILABLE
      }

      s"return a $GetPenaltyDetailsFailureResponse when the response status is NOT FOUND (${Status.NOT_FOUND})" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.NOT_FOUND, regime, aKey.idType, aKey.id.value)
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.NOT_FOUND
      }

      s"return a $GetPenaltyDetailsNoContent when the response status is NOT FOUND (${Status.NOT_FOUND}) but with NO_DATA_FOUND in JSON body" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        val noDataFoundBody: String =
          """
            |{
            | "failures": [
            |   {
            |     "code": "NO_DATA_FOUND",
            |     "reason": "Some reason"
            |   }
            | ]
            |}
            |""".stripMargin
        mockResponseForGetPenaltyDetails(Status.NOT_FOUND, regime, aKey.idType, aKey.id.value, body = Some(noDataFoundBody))
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetPenaltyDetailsNoContent
      }

      s"return a $GetPenaltyDetailsFailureResponse when the response status is NO CONTENT (${Status.NO_CONTENT})" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.NO_CONTENT, regime, aKey.idType, aKey.id.value)
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.NO_CONTENT
      }

      s"return a $GetPenaltyDetailsFailureResponse when the response status is CONFLICT (${Status.CONFLICT})" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.CONFLICT, regime, aKey.idType, aKey.id.value)
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.CONFLICT
      }

      s"return a $GetPenaltyDetailsFailureResponse when the response status is UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY})" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.UNPROCESSABLE_ENTITY, regime, aKey.idType, aKey.id.value)
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.UNPROCESSABLE_ENTITY
      }

      s"return a $GetPenaltyDetailsFailureResponse when the response status is ISE (${Status.BAD_REQUEST})" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.BAD_REQUEST, regime, aKey.idType, aKey.id.value)
        val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails(aKey))
        result.isLeft shouldBe true
        result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.BAD_REQUEST
      }
    }

    s"getPenaltyDetailsForAPI for $regime" should {
      "return a 200 response" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetails(Status.OK, regime, aKey.idType, s"${aKey.id.value}?dateLimit=09")
        val result: HttpResponse = await(connector.getPenaltyDetailsForAPI(aKey, dateLimit = Some("09")))
        result.status shouldBe Status.OK
      }

      "handle a UpstreamErrorResponse" when {
        "a 4xx error is returned" in new Setup {
          enableFeatureSwitch(CallAPI1812ETMP)
          mockResponseForGetPenaltyDetails(Status.FORBIDDEN, regime, aKey.idType, s"${aKey.id.value}?dateLimit=09")
          val result: HttpResponse = await(connector.getPenaltyDetailsForAPI(aKey, dateLimit = Some("09")))
          result.status shouldBe Status.FORBIDDEN
        }

        "a 5xx error is returned" in new Setup {
          enableFeatureSwitch(CallAPI1812ETMP)
          mockResponseForGetPenaltyDetails(Status.BAD_GATEWAY, regime, aKey.idType, s"${aKey.id.value}?dateLimit=09")
          val result: HttpResponse = await(connector.getPenaltyDetailsForAPI(aKey, dateLimit = Some("09")))
          result.status shouldBe Status.BAD_GATEWAY
        }
      }
    }
  }
}
