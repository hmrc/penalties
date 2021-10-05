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
import models.financial.Financial
import models.payment.{LatePaymentPenalty, PaymentPeriod, PaymentStatusEnum}
import models.penalty.PenaltyPeriod
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import models.reason.PaymentPenaltyReasonEnum
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

    s"return $None when the connector returns No Content ($NO_CONTENT)" in new Setup {
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
      taxRegime = "VAT",
      customerReferenceNo = "123456789",
      dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
      isLPP = false,
      appealSubmittedBy = "client",
      agentDetails = None,
      appealInformation = CrimeAppealInformation(
        dateOfEvent = "2021-04-23T18:25:43.511Z",
        reportedIssueToPolice = true,
        reasonableExcuse = "crime",
        honestyDeclaration = true,
        statement = None,
        lateAppeal = false,
        lateAppealReason = None,
        isClientResponsibleForSubmission = None,
        isClientResponsibleForLateSubmission = None
      )
    )
    "return the response from the connector i.e. act as a pass-through function" in new Setup {
      when(mockAppealsConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val result: HttpResponse = await(service.submitAppeal(modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyId = "123456789"))
      result.status shouldBe OK
    }

    "throw an exception when the connector throws an exception" in new Setup {
      when(mockAppealsConnector.submitAppeal(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("Something went wrong")))

      val result: Exception = intercept[Exception](await(service.submitAppeal(modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyId = "123456789")))
      result.getMessage shouldBe "Something went wrong"
    }
  }

  "isMultiplePenaltiesInSamePeriod" should {
    val mockETMPPayloadResponseAsModelWithLPPSamePeriodAsLSP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0,
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
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
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
            financial = Financial(
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
      lateSubmissions = 0,
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
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
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
            financial = Financial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          ),
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
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
            financial = Financial(
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
      lateSubmissions = 0,
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
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
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
            financial = Financial(
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

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("123456790", "123456789", isLPP = false)
        await(result) shouldBe true
      }

      "there is a LSP in the same period as the LPP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPSamePeriodAsLSP))))

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("123456800", "123456789", isLPP = true)
        await(result) shouldBe true
      }

      "there is another LPP in the same period as another LPP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWith2LPPWithSamePeriod))))

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("123456800", "123456789", isLPP = true)
        await(result) shouldBe true
      }
    }

    "return false" when {
      "the penalty is a LPP not LSP - LSP penalty ID provided for LPP check" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPSamePeriodAsLSP))))

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("123456790", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "the penalty is a LSP not LPP - LPP penalty ID provided for LSP check" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWith2LPPWithSamePeriod))))

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("123456800", "123456789", isLPP = false)
        await(result) shouldBe false
      }

      "the penalty is not in the payload" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWith2LPPWithSamePeriod))))

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("1234", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "there is no matching period in the payload - when called with LPP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPDifferentPeriodAsLSP))))

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("123456800", "123456789", isLPP = true)
        await(result) shouldBe false
      }

      "there is no matching period in the payload - when called with LSP" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModelWithLPPDifferentPeriodAsLSP))))

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("123456790", "123456789", isLPP = false)
        await(result) shouldBe false
      }

      "the call to retrieve penalty data fails" in new Setup {
        when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetETMPPayloadMalformed)))

        val result: Future[Boolean] = service.isMultiplePenaltiesInSamePeriod("1234", "123456789", isLPP = true)
        await(result) shouldBe false
      }
    }
  }

  "getNumberOfEstimatedPenalties" should {
    val etmpPayloadWithEstimatedLSPandLPP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 1,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(
        PenaltyPoint(
          `type` = PenaltyTypeEnum.Financial,
          number = "1",
          id = "123456789",
          appealStatus = None,
          dateCreated = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
          status = PointStatusEnum.Estimated,
          reason = None,
          period = Some(PenaltyPeriod(
            startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
              submittedDate = None,
              status = SubmissionStatusEnum.Overdue
            )
          )),
          communications = Seq.empty,
          financial = None
          )
      ),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
            dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
            status = PointStatusEnum.Estimated,
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
            financial = Financial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    val etmpPayloadWithNOEstimatedLSPandLPP: ETMPPayload = ETMPPayload(
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
          id = "123456789",
          appealStatus = None,
          dateCreated = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
          status = PointStatusEnum.Due,
          reason = None,
          period = Some(PenaltyPeriod(
            startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
              submittedDate = None,
              status = SubmissionStatusEnum.Overdue
            )
          )),
          communications = Seq.empty,
          financial = None
        )
      ),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
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
            financial = Financial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )
    "return total number of estimated penalties" when {
      "etmpPayload has LSP and LPP with status Estimated" in new Setup {
        val noOfEstimatedPenalties = 2
        service.getNumberOfEstimatedPenalties(etmpPayloadWithEstimatedLSPandLPP) shouldBe noOfEstimatedPenalties
      }
    }

    "return zero estimated penalties" when {
      "etmpPayload has no LSP and LPP with status Estimated" in new Setup {
        val noOfEstimatedPenalties = 0
        service.getNumberOfEstimatedPenalties(etmpPayloadWithNOEstimatedLSPandLPP) shouldBe noOfEstimatedPenalties
      }
    }
  }

  "findEstimatedPenaltiesAmount" should {
    val mockETMPPayloadResponseAsModelWithEstimateLSPAndLPP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 1,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(
        PenaltyPoint(
          `type` = PenaltyTypeEnum.Financial,
          number = "1",
          id = "123456790",
          appealStatus = None,
          dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(2025, 5, 8, 0, 0, 0)),
          status = PointStatusEnum.Estimated,
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
          financial = Some(
            Financial(
              amountDue = 200,
              outstandingAmountDue = 100,
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              outstandingAmountDay15 = None,
              outstandingAmountDay31 = None,
              percentageOfOutstandingAmtCharged = None,
              estimatedInterest = None,
              crystalizedInterest = None
            )
          )
        )
      ),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_AFTER_30_DAYS,
            dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
            status = PointStatusEnum.Estimated,
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
            financial = Financial(
              amountDue = 23,
              outstandingAmountDue = 22,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    val mockETMPPayloadResponseAsModelWithEstimateLSPAndDueLPPs = mockETMPPayloadResponseAsModelWithEstimateLSPAndLPP.copy(
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
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
            financial = Financial(
              amountDue = 144,
              outstandingAmountDue = 122,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    val mockETMPPayloadResponseAsModelWithDueLSPAndEstimateLPPs = mockETMPPayloadResponseAsModelWithEstimateLSPAndLPP.copy(
      penaltyPoints = Seq(
        PenaltyPoint(
          `type` = PenaltyTypeEnum.Financial,
          number = "1",
          id = "1234",
          dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(2025, 5, 8, 0, 0, 0)),
          status = PointStatusEnum.Due,
          period = Some(PenaltyPeriod(
            startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              submittedDate = Some(LocalDateTime.of(2023, 5, 8, 0, 0, 0)),
              status = SubmissionStatusEnum.Submitted
            )
          )),
          communications = Seq.empty,
          financial = Some(Financial(
            amountDue = 200,
            outstandingAmountDue = 122,
            dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
          )),
          reason = None
        )
      )
    )

    val mockETMPPayloadResponseAsModelNoPenalties = mockETMPPayloadResponseAsModel.copy(
      pointsTotal = 0,
      penaltyAmountsTotal = 0,
      penaltyPoints = Seq()
    )

    val mockETMPPayloadResponseAsModelNoEstimateLSPAndLPP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 1,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(
        PenaltyPoint(
          `type` = PenaltyTypeEnum.Financial,
          number = "1",
          id = "123456790",
          appealStatus = None,
          dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(2025, 5, 8, 0, 0, 0)),
          status = PointStatusEnum.Due,
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
          financial = Some(
            Financial(
              amountDue = 200,
              outstandingAmountDue = 100,
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              outstandingAmountDay15 = None,
              outstandingAmountDay31 = None,
              percentageOfOutstandingAmtCharged = None,
              estimatedInterest = None,
              crystalizedInterest = None
            )
          )
        )
      ),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_AFTER_30_DAYS,
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
            financial = Financial(
              amountDue = 23,
              outstandingAmountDue = 22,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    "return the outstanding amount of LSPs + LPPs (only for penalties with ESTIMATE status)" in new Setup {
      val result = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelWithEstimateLSPAndLPP)
      result shouldBe BigDecimal(122)
    }

    "return the outstanding amount of LSPs (if no LPPs with status ESTIMATE exist and only for LSPs with ESTIMATE status)" in new Setup {
      val result = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelWithEstimateLSPAndDueLPPs)
      result shouldBe BigDecimal(100)
    }

    "return the outstanding amount of LPPs (if no LSPs with status ESTIMATE exist and only for LPPs with ESTIMATE status)" in new Setup {
      val result = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelWithDueLSPAndEstimateLPPs)
      result shouldBe BigDecimal(22)
    }

    "return 0 if no LSPs or LPPs exist" in new Setup {
      val result = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelNoPenalties)
      result shouldBe BigDecimal(0)
    }

    "return 0 if no LSPs or LPPs exist with ESTIMATE status" in new Setup {
      val result = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelNoEstimateLSPAndLPP)
      result shouldBe BigDecimal(0)
    }
  }

  "checkIfHasAnyPenaltyData" should {
    val etmpPayloadWithLSPandLPP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 1,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(
        PenaltyPoint(
          `type` = PenaltyTypeEnum.Financial,
          number = "1",
          id = "123456789",
          appealStatus = None,
          dateCreated = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
          status = PointStatusEnum.Estimated,
          reason = None,
          period = Some(PenaltyPeriod(
            startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
              submittedDate = None,
              status = SubmissionStatusEnum.Overdue
            )
          )),
          communications = Seq.empty,
          financial = None
        )
      ),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
            dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
            status = PointStatusEnum.Estimated,
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
            financial = Financial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    val etmpPayloadWithLSP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 1,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(
        PenaltyPoint(
          `type` = PenaltyTypeEnum.Financial,
          number = "1",
          id = "123456789",
          appealStatus = None,
          dateCreated = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
          dateExpired = Some(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
          status = PointStatusEnum.Estimated,
          reason = None,
          period = Some(PenaltyPeriod(
            startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
              submittedDate = None,
              status = SubmissionStatusEnum.Overdue
            )
          )),
          communications = Seq.empty,
          financial = None
        )
      )
    )

    val etmpPayloadWithLPP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 1,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq(),
      latePaymentPenalties = Some(
        Seq(
          LatePaymentPenalty(
            `type` = PenaltyTypeEnum.Financial,
            id = "123456800",
            reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
            dateCreated = LocalDateTime.of(2023, 5, 8, 0, 0, 0),
            status = PointStatusEnum.Estimated,
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
            financial = Financial(
              amountDue = 144,
              outstandingAmountDue = 0,
              dueDate = LocalDateTime.of(2023, 5, 8, 0, 0, 0)
            )
          )
        )
      )
    )

    val etmpPayloadWithNOLSPandLPP: ETMPPayload = ETMPPayload(
      pointsTotal = 1,
      lateSubmissions = 0 ,
      adjustmentPointsTotal = 0,
      fixedPenaltyAmount = 0,
      penaltyAmountsTotal = 0,
      penaltyPointsThreshold = 3,
      penaltyPoints = Seq()
    )

    "return true" when{
      "etmp Payload has LPP and LSP" in new Setup{
        service.checkIfHasAnyPenaltyData(etmpPayloadWithLSPandLPP) shouldBe true
      }
    }
    "return false" when{
      "etmp Payload has NO LPP and LSP" in new Setup{
        service.checkIfHasAnyPenaltyData(etmpPayloadWithNOLSPandLPP) shouldBe false
      }
    }
    "return false" when{
      "etmp Payload has LPP and NO LSP" in new Setup{
        service.checkIfHasAnyPenaltyData(etmpPayloadWithLPP) shouldBe false
      }
    }
    "return false" when{
      "etmp Payload has NO LPP but has LSP" in new Setup{
        service.checkIfHasAnyPenaltyData(etmpPayloadWithLSP) shouldBe false
      }
    }
  }

  "getCrystallizedPenaltyAmount" should {

    "return the correct amount of due penalties in a payload " in new Setup {
      val result = service.getCrystallizedPenaltyAmount(mockETMPPayloadResponseAsModelForLPPWithAdditionalPenalties)
      result shouldBe 2
    }

    "return 0 when a payload has no due penalties" in new Setup {
      val result = service.getCrystallizedPenaltyAmount(mockETMPPayloadResponseAsModel)
      result shouldBe 0
    }

  }
}
