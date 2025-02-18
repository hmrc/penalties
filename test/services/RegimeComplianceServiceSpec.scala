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
import connectors.{RegimeComplianceConnector}
import connectors.parsers.ComplianceParser._
import models.compliance.CompliancePayload
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import models.{AgnosticEnrolmentKey, Regime, IdType, Id}
import scala.concurrent.{ExecutionContext, Future}

class RegimeComplianceServiceSpec extends SpecBase {
  val mockComplianceConnector: RegimeComplianceConnector = mock(classOf[RegimeComplianceConnector])
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  
  val regime = Regime("VATC") 
  val idType = IdType("VRN")
  val id = Id("123456789")
  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    regime, idType, id
  )

  class Setup {
    val service = new RegimeComplianceService(mockComplianceConnector)(appConfig.config)
    reset(mockComplianceConnector)
  }

  "getComplianceData" should {
    "return Left and the status code" when {
      s"the failure model returned is $CompliancePayloadFailureResponse" in new Setup {
        when(mockComplianceConnector.getComplianceData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadFailureResponse(BAD_REQUEST))))
        val result: Either[Int, CompliancePayload] = await(service.getComplianceData(vrn123456789, "2020-01-31", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.getOrElse(IM_A_TEAPOT) shouldBe BAD_REQUEST
      }

      s"the failure model returned is $CompliancePayloadNoData" in new Setup {
        when(mockComplianceConnector.getComplianceData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadNoData)))
        val result: Either[Int, CompliancePayload] = await(service.getComplianceData(vrn123456789, "2020-01-31", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.getOrElse(IM_A_TEAPOT) shouldBe NOT_FOUND
      }

      s"the failure model returned is $CompliancePayloadMalformed" in new Setup {
        when(mockComplianceConnector.getComplianceData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadMalformed)))
        val result: Either[Int, CompliancePayload] = await(service.getComplianceData(vrn123456789, "2020-01-31", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.getOrElse(IM_A_TEAPOT) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
