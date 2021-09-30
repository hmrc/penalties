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

package controllers

import base.SpecBase
import connectors.parsers.ETMPPayloadParser.{GetETMPPayloadFailureResponse, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{mock, reset, when}
import play.api.http.Status
import play.api.test.Helpers._
import services.ETMPService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIControllerSpec extends SpecBase {
  val mockETMPService: ETMPService = mock(classOf[ETMPService])

  class Setup() {
    reset(mockETMPService)
    val controller = new APIController(mockETMPService, stubControllerComponents())
  }

  "getSummaryDataForVRN" should {
    s"return NOT_FOUND (${Status.NOT_FOUND}) when the call to ETMP fails" in new Setup {
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful((None, Left(GetETMPPayloadFailureResponse(Status.INTERNAL_SERVER_ERROR)))))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when the call returns no data" in new Setup {
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful((None, Left(GetETMPPayloadNoContent))))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    s"return OK (${Status.OK}) when the call returns some data and can be parsed to the correct response" in new Setup {
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))
      val result = controller.getSummaryDataForVRN("123456789")(fakeRequest)
      status(result) shouldBe Status.OK
      //TODO: change data based on implementation
      contentAsString(result) shouldBe ""
    }
  }
}
