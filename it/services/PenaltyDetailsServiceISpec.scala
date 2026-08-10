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
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.{HipFinancialWiremock, HipPenaltiesWiremock, IntegrationSpecCommonBase}

class PenaltyDetailsServiceISpec extends IntegrationSpecCommonBase with HipFinancialWiremock with HipPenaltiesWiremock with FeatureSwitching with TableDrivenPropertyChecks {
  setEnabledFeatureSwitches()
  val service: PenaltyDetailsService = injector.instanceOf[PenaltyDetailsService]

    Table(
      ("Regime", "IdType", "Id"),
      (Regime("VATC"), IdType("VRN"), Id("123456789")),
      (Regime("ITSA"), IdType("NINO"), Id("AB123456C")),
    ).forEvery { (regime, idType, id) =>

      val enrolmentKey = AgnosticEnrolmentKey(regime, idType, id)

      s"getPenaltyDetails for $regime" when {

        s"call the HIP connector and return a successful result converted to GetPenaltyDetails structure" in {
            mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id)
            val result = await(service.getPenaltyDetails(enrolmentKey))
            result.isRight shouldBe true
            result.toOption.get.isInstanceOf[GetPenaltyDetailsSuccessResponse] shouldBe true
        }

        s"return $GetPenaltyDetailsMalformed when the HIP response body is not well formed" in {
            mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(
              """
              {
               "lateSubmissionPenalty": {
                 "summary": {}
                 }
               }
              """))
            val result = await(service.getPenaltyDetails(enrolmentKey))
            result.isLeft shouldBe true
            result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetPenaltyDetailsMalformed
        }

        s"return $GetPenaltyDetailsNoContent when the HIP response body contains 'Invalid ID Number' for 422 response" in {
            val noDataFoundBody = """{"errors":{"processingDate":"2025-03-03", "code":"016", "text":"Invalid ID Number"}}"""
            mockResponseForGetPenaltyDetails(Status.UNPROCESSABLE_ENTITY, regime, idType, id, responseBody = Some(noDataFoundBody))
            val result = await(service.getPenaltyDetails(enrolmentKey))
            result.isLeft shouldBe true
            result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetPenaltyDetailsNoContent
        }

        s"return $GetPenaltyDetailsFailureResponse when an unknown response is returned from the HIP connector" in {
            mockResponseForGetPenaltyDetails(Status.IM_A_TEAPOT, regime, idType, id)
            val result = await(service.getPenaltyDetails(enrolmentKey))
            result.isLeft shouldBe true
            result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetPenaltyDetailsFailureResponse(Status.IM_A_TEAPOT)
        }

        s"successfully handle HIP response with incomeSource as null" in {
            mockResponseForGetPenaltyDetails(Status.OK, regime, idType, id, responseBody = Some(getHIPPenaltyDetailsWithIncomeSourceNoneAsJson.toString))
            val result = await(service.getPenaltyDetails(enrolmentKey))
            result.isRight shouldBe true
            result.toOption.get.isInstanceOf[GetPenaltyDetailsSuccessResponse] shouldBe true
            
            // Verify that the converted data has incomeSource as None
            val successResponse = result.toOption.get.asInstanceOf[GetPenaltyDetailsSuccessResponse]
            val penaltyDetails = successResponse.penaltyDetails
            penaltyDetails.lateSubmissionPenalty.isDefined shouldBe true
            
            val lspDetails = penaltyDetails.lateSubmissionPenalty.get.details
            lspDetails.nonEmpty shouldBe true
            
            val lateSubmissions = lspDetails.head.lateSubmissions
            lateSubmissions.isDefined shouldBe true
            lateSubmissions.get.nonEmpty shouldBe true
            
            // Check that incomeSource is None
            lateSubmissions.get.head.incomeSource shouldBe None
        }
      }
    }

}
