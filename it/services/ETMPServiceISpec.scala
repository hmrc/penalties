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

package services

import connectors.parsers.ETMPPayloadParser.{GetETMPPayloadFailureResponse, GetETMPPayloadMalformed, GetETMPPayloadNoContent}
import models.ETMPPayload
import models.communication.{Communication, CommunicationTypeEnum}
import models.financial.Financial
import models.penalty.PenaltyPeriod
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import models.submission.{Submission, SubmissionStatusEnum}
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ETMPServiceISpec extends IntegrationSpecCommonBase with ETMPWiremock {
  val service: ETMPService = injector.instanceOf[ETMPService]

  "getPenaltyDataFromETMPForEnrolment" should {
    s"call the connector and return a tuple - first is the $Some result and second is the parser result - successful result" in {
      mockResponseForStubETMPPayload(Status.OK, "123456789")
      val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe true
      result._2.isRight shouldBe true
      result._1.get shouldBe etmpPayloadModel
    }

    s"call the connector and return a $None" when {
      s"the response is No Content (${Status.NO_CONTENT}) - second tuple value: $GetETMPPayloadNoContent" in {
        mockResponseForStubETMPPayload(Status.NO_CONTENT, "123456789")
        val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
        result._1.isDefined shouldBe false
        result._2.isLeft shouldBe true
        result._2.left.get shouldBe GetETMPPayloadNoContent
      }

      s"the response body is not well formed - second tuple value: $GetETMPPayloadMalformed" in {
        mockResponseForStubETMPPayload(Status.OK, "123456789", body = Some("""{ "this": "is amazing json" }"""))
        val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
        result._1.isDefined shouldBe false
        result._2.isLeft shouldBe true
        result._2.left.get shouldBe GetETMPPayloadMalformed
      }

      s"an unknown response is returned from the connector - second tuple value: $GetETMPPayloadFailureResponse" in {
        mockResponseForStubETMPPayload(Status.IM_A_TEAPOT, "123456789")
        val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
        result._1.isDefined shouldBe false
        result._2.isLeft shouldBe true
        result._2.left.get shouldBe GetETMPPayloadFailureResponse(Status.IM_A_TEAPOT)
      }
    }
  }
}
