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

import base.SpecBase
import connectors.PEGAConnector
import connectors.parsers.AppealsParser
import connectors.parsers.AppealsParser.UnexpectedFailure
import models.appeals.{AppealResponseModel, AppealSubmission, CrimeAppealInformation}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class ETMPServiceSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq("CorrelationId" -> "id"))
  val mockAppealsConnector: PEGAConnector = mock(classOf[PEGAConnector])
  val correlationId: String = "correlationId"

  class Setup {
    val service = new ETMPService(
      mockAppealsConnector
    )
    reset(mockAppealsConnector)
  }

  "submitAppeal" should {
    val modelToPassToServer: AppealSubmission = AppealSubmission(
      taxRegime = "VAT",
      customerReferenceNo = "123456789",
      dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
      isLPP = false,
      appealSubmittedBy = "client",
      agentDetails = None,
      appealInformation = CrimeAppealInformation(
        startDateOfEvent = "2021-04-23T00:00",
        reportedIssueToPolice = true,
        reasonableExcuse = "crime",
        honestyDeclaration = true,
        statement = None,
        lateAppeal = false,
        lateAppealReason = None,
        isClientResponsibleForSubmission = None,
        isClientResponsibleForLateSubmission = None
      )
    )

    "return the response from the connector i.e. act as a pass-through function" in new Setup {
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(appealResponseModel)))

      val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] = await(
        service.submitAppeal(modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId))
      result shouldBe Right(appealResponseModel)
    }

    "return the response from the connector on error i.e. act as a pass-through function" in new Setup {
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.successful(
        Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))))

      val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] = await(service.submitAppeal(
        modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId))
      result shouldBe Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))
    }

    "throw an exception when the connector throws an exception" in new Setup {
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("Something went wrong")))

      val result: Exception = intercept[Exception](await(service.submitAppeal(
        modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)))
      result.getMessage shouldBe "Something went wrong"
    }
  }
}
