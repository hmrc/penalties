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

package connectors

import featureSwitches.{CallPEGA, FeatureSwitching}
import models.appeals.{AppealSubmission, CrimeAppealInformation}
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import utils.{AppealWiremock, IntegrationSpecCommonBase}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class AppealsConnectorISpec extends IntegrationSpecCommonBase with AppealWiremock with FeatureSwitching {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val connector: AppealsConnector = injector.instanceOf[AppealsConnector]
  }

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
        agentReferenceNo = None,
        appealInformation = CrimeAppealInformation(
          reasonableExcuse = "crime",
          honestyDeclaration = true,
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssueToPolice = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None
        )
      )
      val result = await(connector.submitAppeal(modelToSend, "HMRC-MTD-VAT~VRN~1234567890", isLPP = false, penaltyId = "1234567890"))
      result.isRight shouldBe true
    }

    "Jsonify the model and send the request and return the response - when PEGA feature switch disabled, call stub" in new Setup {
      disableFeatureSwitch(CallPEGA)
      mockResponseForAppealSubmissionStub(Status.OK, "HMRC-MTD-VAT~VRN~123456789", penaltyId = "123456789")
      val modelToSend: AppealSubmission = AppealSubmission(
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = false,
        appealSubmittedBy = "client",
        agentReferenceNo = None,
        appealInformation = CrimeAppealInformation(
          reasonableExcuse = "crime",
          honestyDeclaration = true,
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssueToPolice = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None
        )
      )
      val result = await(connector.submitAppeal(modelToSend, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyId = "123456789"))
      result.isRight shouldBe true
    }

    "Jsonify the model and send the request and return the response - when PEGA feature switch disabled, call stub - for LPP" in new Setup {
      disableFeatureSwitch(CallPEGA)
      mockResponseForAppealSubmissionStub(Status.OK, "HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyId = "123456789")
      val modelToSend: AppealSubmission = AppealSubmission  (
        taxRegime = "VAT",
        customerReferenceNo = "123456789",
        dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        isLPP = true,
        appealSubmittedBy = "client",
        agentReferenceNo = None,
        appealInformation = CrimeAppealInformation(
          reasonableExcuse= "crime",
          honestyDeclaration = true,
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssueToPolice = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None
        )
      )
      val result = await(connector.submitAppeal(modelToSend, "HMRC-MTD-VAT~VRN~123456789", isLPP = true, penaltyId = "123456789"))
      result.isRight shouldBe true
    }
  }
}
