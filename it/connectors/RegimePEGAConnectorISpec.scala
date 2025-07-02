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

import config.featureSwitches.{CallPEGA, FeatureSwitching}

import models.appeals.{AppealSubmission, CrimeAppealInformation}
import play.api.http.Status
import play.api.test.Helpers._
import utils.{RegimeAppealWiremock, IntegrationSpecCommonBase}
import models.{AgnosticEnrolmentKey, Regime, IdType, Id}
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class RegimePEGAConnectorISpec extends IntegrationSpecCommonBase with RegimeAppealWiremock with FeatureSwitching {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val connector: RegimePEGAConnector = injector.instanceOf[RegimePEGAConnector]
    val correlationId: String = "corId"
  }

  val (regime, idType, id) = (Regime("ITSA"), IdType("NINO"), Id("AB123456C"))

  val aKey = AgnosticEnrolmentKey(regime, idType, id) 

  "submitAppeal" should {
    "Jsonify the model and send the request and return the response with Headers- when PEGA feature switch enabled, call PEGA" in new Setup {
      enableFeatureSwitch(CallPEGA)
      mockResponseForAppealSubmissionPEGA(Status.OK, "1234567890")
      val modelToSend: AppealSubmission = AppealSubmission(
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
      val result = await(connector.submitAppeal(modelToSend, aKey, isLPP = false, penaltyNumber = "1234567890", correlationId))
      result.isRight shouldBe true
    }

    "Jsonify the model and send the request and return the response - when PEGA feature switch disabled, call stub" in new Setup {
      disableFeatureSwitch(CallPEGA)
      mockResponseForAppealSubmissionStub(Status.OK, aKey, penaltyNumber = "123456789")
      val modelToSend: AppealSubmission = AppealSubmission(
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
      val result = await(connector.submitAppeal(modelToSend, aKey, isLPP = false, penaltyNumber = "123456789", correlationId))
      result.isRight shouldBe true
    }

    "Jsonify the model and send the request and return the response - when PEGA feature switch disabled, call stub - for LPP" in new Setup {
      disableFeatureSwitch(CallPEGA)
      mockResponseForAppealSubmissionStub(Status.OK, aKey, isLPP = true, penaltyNumber = "123456789")
      val modelToSend: AppealSubmission = AppealSubmission  (
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = true,
        appealSubmittedBy = "client",
        agentDetails = None,
        appealInformation = CrimeAppealInformation(
          reasonableExcuse= "crime",
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
      val result = await(connector.submitAppeal(modelToSend, aKey, isLPP = true, penaltyNumber = "123456789", correlationId))
      result.isRight shouldBe true
    }
  }
}
