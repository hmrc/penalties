/*
 * Copyright 2021 HM Revenue & Customs
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

import featureSwitches.{CallETMP, FeatureSwitching}
import models.appeals.{AppealSubmission, CrimeAppealInformation}
import play.api.http.Status
import play.api.test.Helpers._
import utils.{AppealWiremock, IntegrationSpecCommonBase}

import scala.concurrent.ExecutionContext

class AppealsConnectorISpec extends IntegrationSpecCommonBase with AppealWiremock with FeatureSwitching {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val connector: AppealsConnector = injector.instanceOf[AppealsConnector]
  }

  "submitAppeal" should {
    "Jsonify the model and send the request and return the response - when ETMP feature switch enabled, call ETMP" in new Setup {
      enableFeatureSwitch(CallETMP)
      mockResponseForAppealSubmissionETMP(Status.OK)
      val modelToSend: AppealSubmission = AppealSubmission(
        submittedBy = "client",
        penaltyId = "1234567890",
        reasonableExcuse = "ENUM_PEGA_LIST",
        honestyDeclaration = true,
        appealInformation = CrimeAppealInformation(
          `type` = "crime",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssue = true,
          statement = None
        )
      )
      val result = await(connector.submitAppeal(modelToSend))
      result.status shouldBe OK
    }

    "Jsonify the model and send the request and return the response - when ETMP feature switch disabled, call stub" in new Setup {
      disableFeatureSwitch(CallETMP)
      mockResponseForAppealSubmissionStub(Status.OK)
      val modelToSend: AppealSubmission = AppealSubmission(
        submittedBy = "client",
        penaltyId = "1234567890",
        reasonableExcuse = "ENUM_PEGA_LIST",
        honestyDeclaration = true,
        appealInformation = CrimeAppealInformation(
          `type` = "crime",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssue = true,
          statement = None
        )
      )
      val result = await(connector.submitAppeal(modelToSend))
      result.status shouldBe OK
    }
  }
}
