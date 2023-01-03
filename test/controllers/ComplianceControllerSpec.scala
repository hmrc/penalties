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

package controllers

import base.SpecBase
import models.compliance._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import services.ComplianceService

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ComplianceControllerSpec extends SpecBase {
  val mockService: ComplianceService = mock(classOf[ComplianceService])
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val controller: ComplianceController = new ComplianceController(mockService, stubControllerComponents())

    reset(mockService)
  }

  "getComplianceData" should {
    "return the status which was returned by the service" in new Setup {
      when(mockService.getComplianceData(Matchers.any(), Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Left(INTERNAL_SERVER_ERROR)))
      val result: Future[Result] = controller.getComplianceData("123456789", "2020-01-01", "2020-12-31")(fakeRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 200 if the service returns a model" in new Setup {
      val compliancePayloadAsModel: CompliancePayload = CompliancePayload(
        identification = Some(ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        )),
        obligationDetails = Seq(
          ObligationDetail(
            status = ComplianceStatusEnum.open,
            inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceDateReceived = None,
            inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
            periodKey = "#001"
          ),
          ObligationDetail(
            status = ComplianceStatusEnum.fulfilled,
            inboundCorrespondenceFromDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceToDate = LocalDate.of(1920, 2, 29),
            inboundCorrespondenceDateReceived = Some(LocalDate.of(1920, 2, 29)),
            inboundCorrespondenceDueDate = LocalDate.of(1920, 2, 29),
            periodKey = "#001"
          )
        )
      )
      when(mockService.getComplianceData(Matchers.any(), Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Right(compliancePayloadAsModel)))
      val result: Future[Result] = controller.getComplianceData("123456789", "2020-01-01", "2020-12-31")(fakeRequest)
      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(compliancePayloadAsModel)
    }
  }
}
