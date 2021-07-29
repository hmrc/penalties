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

import base.SpecBase
import connectors.{AppealsConnector, ETMPConnector}
import connectors.parsers.ETMPPayloadParser._
import models.ETMPPayload
import models.appeals.{AppealSubmission, CrimeAppealInformation}
import models.communication.{Communication, CommunicationTypeEnum}
import models.payment.{LatePaymentPenalty, PaymentFinancial, PaymentPeriod, PaymentStatusEnum}
import models.penalty.PenaltyPeriod
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import models.submission.{Submission, SubmissionStatusEnum}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class ETMPServiceSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockEtmpConnector: ETMPConnector = mock(classOf[ETMPConnector])
  val mockAppealsConnector: AppealsConnector = mock(classOf[AppealsConnector])

  class Setup {
    val service = new ETMPService(
      mockEtmpConnector,
      mockAppealsConnector
    )

    reset(mockEtmpConnector)
    reset(mockAppealsConnector)
  }

  "getPenaltyDataFromETMPForEnrolment" should {
    s"call the connector and return a $Some when the request is successful" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel))))

      val result: (Option[ETMPPayload], ETMPPayloadResponse) = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe true
      result._1.get shouldBe mockETMPPayloadResponseAsModel
    }

    s"return $None when the connector returns No Content (${NO_CONTENT})" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadNoContent)))

      val result: (Option[ETMPPayload], ETMPPayloadResponse) = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadNoContent
    }

    s"return $None when the response body is malformed" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadMalformed)))

      val result: (Option[ETMPPayload], ETMPPayloadResponse) = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadMalformed
    }

    s"return $None when the connector receives an unmatched status code" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadFailureResponse(IM_A_TEAPOT))))

      val result: (Option[ETMPPayload], ETMPPayloadResponse) = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadFailureResponse(IM_A_TEAPOT)
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))

      val result: Exception = intercept[Exception](await(service.getPenaltyDataFromETMPForEnrolment("123456789")))
      result.getMessage shouldBe "Something has gone wrong."
    }
  }

  "submitAppeal" should {
    val modelToPassToServer: AppealSubmission = AppealSubmission(
      submittedBy = "client",
      penaltyId = "1234567890",
      reasonableExcuse = "ENUM_PEGA_LIST",
      honestyDeclaration = true,
      appealInformation = CrimeAppealInformation(
        `type` = "crime",
        dateOfEvent = "2021-04-23T18:25:43.511Z",
        reportedIssue = true,
        statement = None,
        lateAppeal = false,
        lateAppealReason = None,
        whoPlannedToSubmit = None,
        causeOfLateSubmissionAgent = None
      )
    )
    "return the response from the connector i.e. act as a pass-through function" in new Setup {
      when(mockAppealsConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val result = await(service.submitAppeal(modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", false))
      result.status shouldBe OK
    }

    "throw an exception when the connector throws an exception" in new Setup {
      when(mockAppealsConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("Something went wrong")))

      val result = intercept[Exception](await(service.submitAppeal(modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", false)))
      result.getMessage shouldBe "Something went wrong"
    }
  }

  "isMultiplePenaltiesInSamePeriod" should {
    val mockETMPPayloadResponseAsModelWithLPPSamePeriodAsLSP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 1,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(
        PenaltyPoint(
          `type` = PenaltyTypeEnum.Point,
          number = "1",
          id = "123456790",
          appealStatus = None,
          dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(2025, 5, 8, 0, 0, 0)),
          status = PointStatusEnum.Active,
          reason = Some("reason"),
          period = Some(PenaltyPeriod(
            startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              submittedDate = Some(LocalDateTime.of(2023, 5, 8, 0, 0, 0)),
              status = SubmissionStatusEnum.Submitted
            )
          )),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.secureMessage,
              dateSent = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
              documentId = "123456789"
            )
          ),
          financial = None
        )
      ),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = "",
            dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
            status = PointStatusEnum.Due,
            appealStatus = None,
            period = PaymentPeriod(
              startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
              endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              paymentStatus = PaymentStatusEnum.Paid
            ),
            communications = Seq(
              Communication(
                `type` = CommunicationTypeEnum.letter,
                dateSent = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
                documentId = "DOC1"
              )
            ),
            financial = PaymentFinancial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    val mockETMPPayloadResponseAsModelWith2LPPWithSamePeriod: ETMPPayload = ETMPPayload(
      pointsTotal = 0,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 0,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456801",
            reason = "",
            dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
            status = PointStatusEnum.Due,
            appealStatus = None,
            period = PaymentPeriod(
              startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
              endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              paymentStatus = PaymentStatusEnum.Paid
            ),
            communications = Seq(
              Communication(
                `type` = CommunicationTypeEnum.letter,
                dateSent = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
                documentId = "DOC1"
              )
            ),
            financial = PaymentFinancial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          ),
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = "",
            dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
            status = PointStatusEnum.Due,
            appealStatus = None,
            period = PaymentPeriod(
              startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
              endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              paymentStatus = PaymentStatusEnum.Paid
            ),
            communications = Seq(
              Communication(
                `type` = CommunicationTypeEnum.letter,
                dateSent = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
                documentId = "DOC1"
              )
            ),
            financial = PaymentFinancial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    val mockETMPPayloadResponseAsModelWithLPPDifferentPeriodAsLSP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 1,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(
        PenaltyPoint(
          `type` = PenaltyTypeEnum.Point,
          number = "1",
          id = "123456790",
          appealStatus = None,
          dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(2025, 5, 8, 0, 0, 0)),
          status = PointStatusEnum.Active,
          reason = Some("reason"),
          period = Some(PenaltyPeriod(
            startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              submittedDate = Some(LocalDateTime.of(2023, 5, 8, 0, 0, 0)),
              status = SubmissionStatusEnum.Submitted
            )
          )),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.secureMessage,
              dateSent = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
              documentId = "123456789"
            )
          ),
          financial = None
        )
      ),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = "",
            dateCreated = LocalDateTime.of(2023, 8, 8, 0, 0, 0),
            status = PointStatusEnum.Due,
            appealStatus = None,
            period = PaymentPeriod(
              startDate = LocalDateTime.of(2023, 4, 1, 0, 0, 0),
              endDate = LocalDateTime.of(2023, 6, 30, 0, 0, 0),
              dueDate = LocalDateTime.of(2023, 8, 7, 0, 0, 0),
              paymentStatus = PaymentStatusEnum.Paid
            ),
            communications = Seq(
              Communication(
                `type` = CommunicationTypeEnum.letter,
                dateSent = LocalDateTime.of(2023, 8, 8, 0, 0, 0),
                documentId = "DOC1"
              )
            ),
            financial = PaymentFinancial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 8, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    "return true" when {
      "there is a LPP in the same period as the LSP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPSamePeriodAsLSP))))

        val result = service.isMultiplePenaltiesInSamePeriod("123456790", "123456789", isLPP = false)
        await(result) shouldBe true
      }

      "there is a LSP in the same period as the LPP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPSamePeriodAsLSP))))

        val result = service.isMultiplePenaltiesInSamePeriod("123456800", "123456789", isLPP = true)
        await(result) shouldBe true
      }

      "there is another LPP in the same period as another LPP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWith2LPPWithSamePeriod))))

        val result = service.isMultiplePenaltiesInSamePeriod("123456800", "123456789", isLPP = true)
        await(result) shouldBe true
      }
    }

    "return false" when {
      "the penalty is a LPP not LSP - LSP penalty ID provided for LPP check" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPSamePeriodAsLSP))))

        val result = service.isMultiplePenaltiesInSamePeriod("123456790", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "the penalty is a LSP not LPP - LPP penalty ID provided for LSP check" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWith2LPPWithSamePeriod))))

        val result = service.isMultiplePenaltiesInSamePeriod("123456800", "123456789", isLPP = false)
        await(result) shouldBe false
      }

      "the penalty is not in the payload" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWith2LPPWithSamePeriod))))

        val result = service.isMultiplePenaltiesInSamePeriod("1234", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "there is no matching period in the payload - when called with LPP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPDifferentPeriodAsLSP))))

        val result = service.isMultiplePenaltiesInSamePeriod("123456800", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "there is no matching period in the payload - when called with LSP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPDifferentPeriodAsLSP))))

        val result = service.isMultiplePenaltiesInSamePeriod("123456790", "123456789", isLPP = false)
        await(result) shouldBe false
      }

      "the call to retrieve penalty data fails" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetETMPPayloadMalformed)))

        val result = service.isMultiplePenaltiesInSamePeriod("1234", "123456789", isLPP = true)
        await(result) shouldBe false
      }
    }
  }
}
