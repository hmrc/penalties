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
import models.appeals.AppealData
import models.appeals.AppealTypeEnum.Late_Submission
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import services.ETMPService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AppealsControllerSpec extends SpecBase {
  val mockETMPService: ETMPService = mock[ETMPService]
  val mockAppConfig: AppConfig = mock[AppConfig]

  class Setup(withRealAppConfig: Boolean = true) {
    reset(mockAppConfig)
    reset(mockETMPService)
    val controller = new AppealsController(if(withRealAppConfig) appConfig else mockAppConfig, mockETMPService, stubControllerComponents())
  }
  "getAppealsDataForLateSubmissionPenalty" should {
    s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP can not find the data for the given enrolment key" in new Setup {
      val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.eq(sampleEnrolmentKey))(ArgumentMatchers.any()))
        .thenReturn(Future.successful((None, Left(GetETMPPayloadNoContent))))

      val result = controller.getAppealsDataForLateSubmissionPenalty("1", sampleEnrolmentKey)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
      contentAsString(result) shouldBe s"Could not retrieve ETMP penalty data for $sampleEnrolmentKey"
    }

    s"return NOT_FOUND (${Status.NOT_FOUND}) when ETMP returns data but the given penaltyId is wrong" in new Setup {
      val samplePenaltyId: String = "1234"
      val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.eq(sampleEnrolmentKey))(ArgumentMatchers.any()))
        .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))

      val result = controller.getAppealsDataForLateSubmissionPenalty(samplePenaltyId, sampleEnrolmentKey)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
      contentAsString(result) shouldBe "Penalty ID was not found in users penalties."
    }

    s"return ISE (${Status.INTERNAL_SERVER_ERROR}) when the call to ETMP fails for some reason" in new Setup {
      val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.eq(sampleEnrolmentKey))(ArgumentMatchers.any()))
        .thenReturn(Future.successful((None, Left(GetETMPPayloadMalformed))))

      val result = controller.getAppealsDataForLateSubmissionPenalty("1", sampleEnrolmentKey)(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return OK (${Status.OK}) when the call to ETMP succeeds and the penalty ID matches" in new Setup {
      val samplePenaltyId: String = "123456789"
      val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.eq(sampleEnrolmentKey))(ArgumentMatchers.any()))
        .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModel), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel)))))

      val result = controller.getAppealsDataForLateSubmissionPenalty(samplePenaltyId, sampleEnrolmentKey)(fakeRequest)
      status(result) shouldBe Status.OK
      val appealDataToReturn: AppealData = AppealData(
        Late_Submission,
        mockETMPPayloadResponseAsModel.penaltyPoints.head.period.get.startDate,
        mockETMPPayloadResponseAsModel.penaltyPoints.head.period.get.endDate
      )
      contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
    }

    s"return OK (${Status.OK}) when the call to ETMP succeeds and the correct model for the specified penalty ID" in new Setup {
      val samplePenaltyId: String = "123456789"
      val sampleEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
      when(mockETMPService.getPenaltyDataFromETMPForEnrolment(ArgumentMatchers.eq(sampleEnrolmentKey))(ArgumentMatchers.any()))
        .thenReturn(Future.successful((Some(mockETMPPayloadResponseAsModelMultiplePoints), Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelMultiplePoints)))))

      val result = controller.getAppealsDataForLateSubmissionPenalty(samplePenaltyId, sampleEnrolmentKey)(fakeRequest)
      status(result) shouldBe Status.OK
      val appealDataToReturn: AppealData = AppealData(
        Late_Submission,
        mockETMPPayloadResponseAsModelMultiplePoints.penaltyPoints.last.period.get.startDate,
        mockETMPPayloadResponseAsModelMultiplePoints.penaltyPoints.last.period.get.endDate
      )
      contentAsString(result) shouldBe Json.toJson(appealDataToReturn).toString()
    }
  }

  "getReasonableExcuses" should {
    "return all the excuses that are stored in the ReasonableExcuse model" in new Setup {
      val jsonExpectedToReturn: JsValue = Json.parse(
        """
          |{
          |  "excuses": [
          |    {
          |      "type": "bereavement",
          |      "descriptionKey": "reasonableExcuses.bereavementReason"
          |    },
          |    {
          |      "type": "crime",
          |      "descriptionKey": "reasonableExcuses.crimeReason"
          |    },
          |    {
          |      "type": "fireOrFlood",
          |      "descriptionKey": "reasonableExcuses.fireOrFloodReason"
          |    },
          |    {
          |      "type": "health",
          |      "descriptionKey": "reasonableExcuses.healthReason"
          |    },
          |    {
          |      "type": "lossOfStaff",
          |      "descriptionKey": "reasonableExcuses.lossOfStaffReason"
          |    },
          |    {
          |      "type": "technicalIssues",
          |      "descriptionKey": "reasonableExcuses.technicalIssuesReason"
          |    },
          |    {
          |      "type": "other",
          |      "descriptionKey": "reasonableExcuses.otherReason"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val result = controller.getReasonableExcuses()(fakeRequest)
      status(result) shouldBe OK
      contentAsJson(result) shouldBe jsonExpectedToReturn
    }

    "return only those reasonable excuses that are active based on config" in new Setup(withRealAppConfig = false) {
      val jsonExpectedToReturn: JsValue = Json.parse(
        """
          |{
          |  "excuses": [
          |    {
          |      "type": "bereavement",
          |      "descriptionKey": "reasonableExcuses.bereavementReason"
          |    },
          |    {
          |      "type": "crime",
          |      "descriptionKey": "reasonableExcuses.crimeReason"
          |    },
          |    {
          |      "type": "fireOrFlood",
          |      "descriptionKey": "reasonableExcuses.fireOrFloodReason"
          |    },
          |    {
          |      "type": "health",
          |      "descriptionKey": "reasonableExcuses.healthReason"
          |    },
          |    {
          |      "type": "lossOfStaff",
          |      "descriptionKey": "reasonableExcuses.lossOfStaffReason"
          |    },
          |    {
          |      "type": "technicalIssues",
          |      "descriptionKey": "reasonableExcuses.technicalIssuesReason"
          |    }
          |  ]
          |}
          |""".stripMargin
      )
      when(mockAppConfig.isReasonableExcuseEnabled(ArgumentMatchers.any()))
        .thenReturn(true)
      when(mockAppConfig.isReasonableExcuseEnabled(ArgumentMatchers.eq("other")))
        .thenReturn(false)
      val result = controller.getReasonableExcuses()(fakeRequest)
      status(result) shouldBe OK
      contentAsJson(result) shouldBe jsonExpectedToReturn
    }
  }
}
