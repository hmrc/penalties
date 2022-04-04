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

package services

import base.SpecBase
import connectors.parsers.AppealsParser
import connectors.parsers.AppealsParser.UnexpectedFailure
import connectors.parsers.ETMPPayloadParser._
import connectors.{PEGAConnector, ETMPConnector}
import models.ETMPPayload
import models.appeals.{AppealResponseModel, AppealSubmission, CrimeAppealInformation}
import models.communication.{Communication, CommunicationTypeEnum}
import models.financial.Financial
import models.payment.{LatePaymentPenalty, PaymentPeriod, PaymentStatusEnum}
import models.penalty.PenaltyPeriod
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import models.reason.PaymentPenaltyReasonEnum
import models.submission.{Submission, SubmissionStatusEnum}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class ETMPServiceSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq("CorrelationId" -> "id"))
  val mockEtmpConnector: ETMPConnector = mock(classOf[ETMPConnector])
  val mockAppealsConnector: PEGAConnector = mock(classOf[PEGAConnector])
  val correlationId: String = "correlationId"

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
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(Matchers.eq("123456789"))(Matchers.any()))
        .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel))))

      val result: (Option[ETMPPayload], ETMPPayloadResponse) = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe true
      result._1.get shouldBe mockETMPPayloadResponseAsModel
    }

    s"return $None when the connector returns No Content ($NO_CONTENT)" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(Matchers.eq("123456789"))(Matchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadNoContent)))

      val result: (Option[ETMPPayload], ETMPPayloadResponse) = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadNoContent
    }

    s"return $None when the response body is malformed" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(Matchers.eq("123456789"))(Matchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadMalformed)))

      val result: (Option[ETMPPayload], ETMPPayloadResponse) = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadMalformed
    }

    s"return $None when the connector receives an unmatched status code" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(Matchers.eq("123456789"))(Matchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadFailureResponse(IM_A_TEAPOT))))

      val result: (Option[ETMPPayload], ETMPPayloadResponse) = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadFailureResponse(IM_A_TEAPOT)
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(Matchers.eq("123456789"))(Matchers.any()))
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
      agentReferenceNo = None,
      appealInformation = CrimeAppealInformation(
        startDateOfEvent = "2021-04-23T18:25:43.511Z",
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
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(appealResponseModel)))

      val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] = await(
        service.submitAppeal(modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId))
      result shouldBe Right(appealResponseModel)
    }

    "return the response from the connector on error i.e. act as a pass-through function" in new Setup {
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.successful(
        Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))))

      val result: Either[AppealsParser.ErrorResponse, AppealResponseModel] = await(service.submitAppeal(
        modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId))
      result shouldBe Left(UnexpectedFailure(BAD_GATEWAY, s"Unexpected response, status $BAD_GATEWAY returned"))
    }

    "throw an exception when the connector throws an exception" in new Setup {
      when(mockAppealsConnector.submitAppeal(Matchers.any(), Matchers.any(), Matchers.any(),
        Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("Something went wrong")))

      val result: Exception = intercept[Exception](await(service.submitAppeal(
        modelToPassToServer, "HMRC-MTD-VAT~VRN~123456789", isLPP = false, penaltyNumber = "123456789", correlationId = correlationId)))
      result.getMessage shouldBe "Something went wrong"
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
          period = Some(Seq(PenaltyPeriod(
            startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
              submittedDate = None,
              status = SubmissionStatusEnum.Overdue
            )
          ))),
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
              paymentStatus = PaymentStatusEnum.Paid,
              paymentReceivedDate = Some(LocalDateTime.of(2023, 6, 7, 0, 0, 0))
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
          period = Some(Seq(PenaltyPeriod(
            startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
              submittedDate = None,
              status = SubmissionStatusEnum.Overdue
            )
          ))),
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
              paymentStatus = PaymentStatusEnum.Paid,
              paymentReceivedDate = Some(LocalDateTime.of(2023, 6, 7, 0, 0, 0))
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
          period = Some(Seq(PenaltyPeriod(
            startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              submittedDate = Some(LocalDateTime.of(2023, 5, 8, 0, 0, 0)),
              status = SubmissionStatusEnum.Submitted
            )
          ))),
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
              paymentStatus = PaymentStatusEnum.Paid,
              paymentReceivedDate = Some(LocalDateTime.of(2023, 6, 7, 0, 0, 0))
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
              paymentStatus = PaymentStatusEnum.Paid,
              paymentReceivedDate = Some(LocalDateTime.of(2023, 6, 7, 0, 0, 0))
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
          period = Some(Seq(PenaltyPeriod(
            startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              submittedDate = Some(LocalDateTime.of(2023, 5, 8, 0, 0, 0)),
              status = SubmissionStatusEnum.Submitted
            )
          ))),
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
          period = Some(Seq(PenaltyPeriod(
            startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2023, 3, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(2023, 5, 7, 0, 0, 0),
              submittedDate = Some(LocalDateTime.of(2023, 5, 8, 0, 0, 0)),
              status = SubmissionStatusEnum.Submitted
            )
          ))),
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
              paymentStatus = PaymentStatusEnum.Paid,
              paymentReceivedDate = Some(LocalDateTime.of(2023, 6, 7, 0, 0, 0))
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
      val result: BigDecimal = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelWithEstimateLSPAndLPP)
      result shouldBe BigDecimal(122)
    }

    "return the outstanding amount of LSPs (if no LPPs with status ESTIMATE exist and only for LSPs with ESTIMATE status)" in new Setup {
      val result: BigDecimal = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelWithEstimateLSPAndDueLPPs)
      result shouldBe BigDecimal(100)
    }

    "return the outstanding amount of LPPs (if no LSPs with status ESTIMATE exist and only for LPPs with ESTIMATE status)" in new Setup {
      val result: BigDecimal = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelWithDueLSPAndEstimateLPPs)
      result shouldBe BigDecimal(22)
    }

    "return 0 if no LSPs or LPPs exist" in new Setup {
      val result: BigDecimal = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelNoPenalties)
      result shouldBe BigDecimal(0)
    }

    "return 0 if no LSPs or LPPs exist with ESTIMATE status" in new Setup {
      val result: BigDecimal = service.findEstimatedPenaltiesAmount(mockETMPPayloadResponseAsModelNoEstimateLSPAndLPP)
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
          period = Some(Seq(PenaltyPeriod(
            startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
              submittedDate = None,
              status = SubmissionStatusEnum.Overdue
            )
          ))),
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
              paymentStatus = PaymentStatusEnum.Paid,
              paymentReceivedDate = Some(LocalDateTime.of(2023, 6, 7, 0, 0, 0))
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
          period = Some(Seq(PenaltyPeriod(
            startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
            submission = Submission(
              dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
              submittedDate = None,
              status = SubmissionStatusEnum.Overdue
            )
          ))),
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
              paymentStatus = PaymentStatusEnum.Paid,
              paymentReceivedDate = Some(LocalDateTime.of(2023, 6, 7, 0, 0, 0))
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

      "etmp Payload has NO LPP but has LSP" in new Setup {
        service.checkIfHasAnyPenaltyData(etmpPayloadWithLSP) shouldBe true
      }

      "etmp Payload has LPP but has NO LSP" in new Setup {
        service.checkIfHasAnyPenaltyData(etmpPayloadWithLPP) shouldBe true
      }
    }
    "return false" when {
      "etmp Payload has NO LPP and NO LSP" in new Setup {
        service.checkIfHasAnyPenaltyData(etmpPayloadWithNOLSPandLPP) shouldBe false
      }
    }
  }

  "getNumberOfCrystallizedPenalties" should {

    "return the correct amount of due penalties in a payload " in new Setup {
      val result = service.getNumberOfCrystalizedPenalties(mockETMPPayloadResponseAsModelForLPPWithAdditionalPenalties)
      result shouldBe 2
    }

    "return 0 when a payload has no due penalties" in new Setup {
      val result: Int = service.getNumberOfCrystalizedPenalties(mockETMPPayloadResponseAsModel)
      result shouldBe 0
    }

  }

  "getCrystallisedPenaltyTotal" should {

    "return the correct total of due penalties in a payload" in new Setup {
      val result: BigDecimal = service.getCrystalisedPenaltyTotal(mockETMPPayloadResponseAsModelForLPPWithAdditionalPenalties)
      result shouldBe BigDecimal(288)
    }

    "return 0 when the payload has no due penalties" in new Setup {
      val result: BigDecimal = service.getCrystalisedPenaltyTotal(mockETMPPayloadResponseAsModel)
      result shouldBe BigDecimal(0)
    }
  }
}
