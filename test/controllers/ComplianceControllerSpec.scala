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
import models.compliance._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.ComplianceService

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

class ComplianceControllerSpec extends SpecBase {
  val mockService: ComplianceService = mock(classOf[ComplianceService])
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  class Setup {
    val controller: ComplianceController = new ComplianceController(mockService, stubControllerComponents())

    reset(mockService)
  }

  "getComplianceDataForEnrolmentKey" should {
    "return 404" when {
      "the service returns None" in new Setup {
        val enrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockService.getComplianceDataForEnrolmentKey(ArgumentMatchers.eq(enrolmentKey))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.getComplianceDataForEnrolmentKey(enrolmentKey)(fakeRequest)
        await(result).header.status shouldBe NOT_FOUND
        contentAsString(result) shouldBe s"Could not find any compliance data for enrolment: $enrolmentKey"
      }
    }

    "return 200" when {
      val sampleDateTime: LocalDateTime = LocalDateTime.of(
        2019, 1, 31, 23, 59, 59).plus(998, ChronoUnit.MILLIS)
      val return1: Return = Return(sampleDateTime, sampleDateTime, sampleDateTime, Some(ReturnStatusEnum.submitted))
      val return2: Return = Return(sampleDateTime, sampleDateTime, sampleDateTime, None)
      val missingReturn1 : MissingReturn = MissingReturn(sampleDateTime, sampleDateTime)
      val someMissingReturns: Seq[MissingReturn] = Seq[MissingReturn](missingReturn1, missingReturn1)
      val returns: Seq[Return] = Seq[Return](return1, return2)
      val returnModel: CompliancePayload = CompliancePayload("2", "2", sampleDateTime, someMissingReturns, returns)
      s"the service returns $Some $CompliancePayload" in new Setup {
        val enrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockService.getComplianceDataForEnrolmentKey(ArgumentMatchers.eq(enrolmentKey))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(returnModel)))

        val result: Future[Result] = controller.getComplianceDataForEnrolmentKey(enrolmentKey)(fakeRequest)
        await(result).header.status shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(returnModel)
      }
    }

    "return 500" when {
      "the service fails to get the compliance data" in new Setup {
        val enrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"
        when(mockService.getComplianceDataForEnrolmentKey(ArgumentMatchers.eq(enrolmentKey))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Exception("failure in calling service")))

        val result: Future[Result] = controller.getComplianceDataForEnrolmentKey(enrolmentKey)(fakeRequest)
        await(result).header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "getComplianceDataFromDES" should {
    "return the status which was returned by the service" in new Setup {
      when(mockService.getComplianceDataFromDES(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(INTERNAL_SERVER_ERROR)))
      val result: Future[Result] = controller.getComplianceDataFromDES("123456789", "2020-01-01", "2020-12-31")(fakeRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 200 if the service returns a model" in new Setup {
      val compliancePayloadAsModel: CompliancePayloadObligationAPI = CompliancePayloadObligationAPI(
        identification = ObligationIdentification(
          incomeSourceType = None,
          referenceNumber = "123456789",
          referenceType = "VRN"
        ),
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
      when(mockService.getComplianceDataFromDES(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(compliancePayloadAsModel)))
      val result: Future[Result] = controller.getComplianceDataFromDES("123456789", "2020-01-01", "2020-12-31")(fakeRequest)
      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(compliancePayloadAsModel)
    }
  }
}
