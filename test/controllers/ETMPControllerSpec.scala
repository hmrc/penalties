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
import config.AppConfig
import connectors.parsers.ETMPPayloadParser.{GetETMPPayloadMalformed, GetETMPPayloadNoContent, GetETMPPayloadSuccessResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.ETMPService

import scala.concurrent.Future

class ETMPControllerSpec extends SpecBase {
  val mockAppConfig: AppConfig = mock[AppConfig]
  val mockETMPService: ETMPService = mock[ETMPService]

  class Setup {
    reset(mockAppConfig, mockETMPService)
    val controller: ETMPController = new ETMPController(
      mockAppConfig,
      mockETMPService,
      stubControllerComponents()
    )
  }

  "getPenaltiesData" should {
    "call the service to retrieve data from ETMP and return OK with the body if successful" in new Setup {
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.eq(sampleMTDVATEnrolmentKey))(ArgumentMatchers.any()))
        .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))

      val result = controller.getPenaltiesData(sampleMTDVATEnrolmentKey)(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(mockETMPPayloadResponseAsModel)
    }

    "call the service and return an ISE if there is a failure to do with processing" in new Setup {
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.eq(sampleMTDVATEnrolmentKey))(ArgumentMatchers.any()))
        .thenReturn(Future.successful((None, Left(GetETMPPayloadMalformed))))

      val result = controller.getPenaltiesData(sampleMTDVATEnrolmentKey)(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe s"Something went wrong."
    }

    "call the service and return a NotFound if there is a NoContent is returned from the connector" in new Setup {
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.eq(sampleMTDVATEnrolmentKey))(ArgumentMatchers.any()))
        .thenReturn(Future.successful((None, Left(GetETMPPayloadNoContent))))

      val result = controller.getPenaltiesData(sampleMTDVATEnrolmentKey)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
      contentAsString(result) shouldBe s"Could not retrieve ETMP penalty data for $sampleMTDVATEnrolmentKey"
    }
  }
}
