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
import config.featureSwitches.UseInternalAuth
import models.compliance._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ComplianceService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Retrieval}
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ComplianceControllerSpec extends SpecBase {
  implicit val cc: ControllerComponents = stubControllerComponents()
  val mockConfig: Configuration = mock(classOf[Configuration])
  lazy val mockAuth: StubBehaviour = mock(classOf[StubBehaviour])
  lazy val authComponent: BackendAuthComponents = BackendAuthComponentsStub(mockAuth)
  val mockService: ComplianceService = mock(classOf[ComplianceService])

  class Setup {
    sys.props -= UseInternalAuth.name
    val controller: ComplianceController = new ComplianceController(mockService, cc)(implicitly, mockConfig, authComponent)
    reset(mockService)
    reset(mockConfig)
    reset(mockAuth)
    when(mockAuth.stubAuth(Matchers.any(), Matchers.any[Retrieval[Unit]])).thenReturn(Future.unit)
    when(mockConfig.get[Boolean](Matchers.eq(UseInternalAuth.name))(Matchers.any())).thenReturn(true)
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

    "return UNAUTHORIZED (401)" when {
      "the caller does not provide an auth token" in new Setup {
        val result: Future[Result] = controller.getComplianceData("123456789", "2020-01-01", "2020-12-31")(FakeRequest("GET", "/"))
        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "return FORBIDDEN (403)" when {
      "the caller does not have the sufficient permissions" in new Setup {
        when(mockAuth.stubAuth(Matchers.any(), Matchers.any[Retrieval[Unit]])).thenReturn(Future.failed(UpstreamErrorResponse("FORBIDDEN", Status.FORBIDDEN)))
        val result: Future[Result] = controller.getComplianceData("123456789", "2020-01-01", "2020-12-31")(fakeRequest)
        status(result) shouldBe Status.FORBIDDEN
      }
    }
  }
}
