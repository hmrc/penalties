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

package controllers

import base.SpecBase
import controllers.auth.AuthAction
import models.compliance._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import services.RegimeComplianceService
import utils.AuthActionMock
import models.{Regime, IdType, Id}
import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class RegimeComplianceControllerSpec extends SpecBase {
  val mockService: RegimeComplianceService = mock(classOf[RegimeComplianceService])
  val mockAuthAction: AuthAction = injector.instanceOf(classOf[AuthActionMock])
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val controller: RegimeComplianceController = new RegimeComplianceController(mockService, stubControllerComponents(), mockAuthAction)

    reset(mockService)
  }

  "getComplianceData" should {
    "return the status which was returned by the service" in new Setup {
      when(mockService.getComplianceData(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(INTERNAL_SERVER_ERROR)))
      val result: Future[Result] = controller.getComplianceData(Regime("ITSA"), IdType("NINO"), Id("AB123456C"), "2020-01-01", "2020-12-31")(fakeRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 200 if the service returns a model" in new Setup {
      val compliancePayloadAsModel: CompliancePayload = CompliancePayload(
        identification = Some(ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "AB123456C",
          referenceType = "NINO"
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
      when(mockService.getComplianceData(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(compliancePayloadAsModel)))
      val result: Future[Result] = controller.getComplianceData(Regime("ITSA"), IdType("NINO"), Id("AB123456C"), "2020-01-01", "2020-12-31")(fakeRequest)
      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(compliancePayloadAsModel)
    }
  }
}
