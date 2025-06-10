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

package connectors

import config.featureSwitches.FeatureSwitching
import connectors.parsers.AppealsParser.{BadRequest, DuplicateAppeal, UnexpectedFailure}
import models.appeals.{AppealSubmission, CrimeAppealInformation}
import play.api.test.Helpers._
import utils.{HIPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class HIPConnectorISpec extends IntegrationSpecCommonBase with HIPWiremock with FeatureSwitching {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val connector: HIPConnector = injector.instanceOf[HIPConnector]
    val correlationId: String = "corId"
  }

  val penaltyNumber: String = "1234567890"

  val submission:AppealSubmission = AppealSubmission(
    taxRegime = "VAT",
    customerReferenceNo = "123456789",
    dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
    isLPP = false,
    appealSubmittedBy = "client",
    agentDetails = None,
    appealInformation = CrimeAppealInformation(
      reasonableExcuse = "crime",
      honestyDeclaration = true,
      startDateOfEvent = "2021-04-23T00:00",
      reportedIssueToPolice = "yes",
      statement = None,
      lateAppeal = false,
      lateAppealReason = None,
      isClientResponsibleForSubmission = None,
      isClientResponsibleForLateSubmission = None
    )
  )

  "submitAppeal" should {
    "Jsonify the model and send the request and return the response with Headers" in new Setup {
      mockSuccessfulResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result.isRight shouldBe true
    }

    "return duplicated error response" in new Setup {
      mockDuplicateSubmissionResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(DuplicateAppeal)
    }

    "return Bad request if a 400 bad request error is received" in new Setup {
      mockInvalidPayloadResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(BadRequest)
    }

    "return Bad request if a 401 Unauthorized error is received" in new Setup {
      mockUnauthorisedResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(BadRequest)
    }

    "return Bad request if a 404 Not Found error is received" in new Setup {
      mockNotFoundResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(BadRequest)
    }

    "return Bad request if a 415 Unsupported media-type error is received" in new Setup {
      mockUnsupportedMediaTypeResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(BadRequest)
    }

    "return Bad request if a 422 Unprocessable Content error is received" in new Setup {
      mockEMTPErrorResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(BadRequest)
    }


    "return Unexpected Failure if a 500 Internal Server Error is received" in new Setup {
      mockInternalServerErrorResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(UnexpectedFailure(500, "Unexpected response, status 500 returned on submission to HIP with reason:{}"))
    }

    "return Unexpected Failure if a 502 Bad Gateway error is received" in new Setup {
      mockBadGatewayResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(UnexpectedFailure(502, "Unexpected response, status 502 returned on submission to HIP with reason:{\"failures\":[{\"dependentSystemHTTPCode\":\"500\",\"originatedFrom\":\"etmp\",\"code\":\"\",\"reason\":\"\"}]}"))
    }

    "return Unexpected Failure if a 503 Service Unavailable error is received" in new Setup {
      mockServiceUnavailableResponse()
      val result = await(connector.submitAppeal(submission, penaltyNumber, correlationId, "01"))
      result shouldBe Left(UnexpectedFailure(503, "Unexpected response, status 503 returned on submission to HIP with reason:{}"))
    }
  }
}
