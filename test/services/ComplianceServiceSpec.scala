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

import base.SpecBase
import connectors.ComplianceConnector
import connectors.parsers.ComplianceParser._
import models.compliance.CompliancePayload
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ComplianceServiceSpec extends SpecBase {
  val mockComplianceConnector: ComplianceConnector = mock(classOf[ComplianceConnector])
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  class Setup {
    val service = new ComplianceService(mockComplianceConnector)(appConfig.config)
    reset(mockComplianceConnector)
  }

  "getComplianceData" should {
    "return Left and the status code" when {
      s"the failure model returned is $CompliancePayloadFailureResponse" in new Setup {
        when(mockComplianceConnector.getComplianceData(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadFailureResponse(BAD_REQUEST))))
        val result: Either[Int, CompliancePayload] = await(service.getComplianceData("123456789", "2020-01-31", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.getOrElse(IM_A_TEAPOT) shouldBe BAD_REQUEST
      }

      s"the failure model returned is $CompliancePayloadNoData" in new Setup {
        when(mockComplianceConnector.getComplianceData(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadNoData)))
        val result: Either[Int, CompliancePayload] = await(service.getComplianceData("123456789", "2020-01-31", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.getOrElse(IM_A_TEAPOT) shouldBe NOT_FOUND
      }

      s"the failure model returned is $CompliancePayloadMalformed" in new Setup {
        when(mockComplianceConnector.getComplianceData(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(Left(CompliancePayloadMalformed)))
        val result: Either[Int, CompliancePayload] = await(service.getComplianceData("123456789", "2020-01-31", "2020-12-31"))
        result.isLeft shouldBe true
        result.left.getOrElse(IM_A_TEAPOT) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
