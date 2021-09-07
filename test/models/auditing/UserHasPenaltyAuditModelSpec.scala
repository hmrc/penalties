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

package models.auditing

import base.{LogCapturing, SpecBase}
import models.ETMPPayload
import models.appeals.AppealStatusEnum
import models.communication.{Communication, CommunicationTypeEnum}
import models.financial.{AmountTypeEnum, Financial, OverviewElement}
import models.payment.{LatePaymentPenalty, PaymentPeriod, PaymentStatusEnum}
import models.penalty.PenaltyPeriod
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import models.reason.PaymentPenaltyReasonEnum
import models.submission.{Submission, SubmissionStatusEnum}
import utils.Logger

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class UserHasPenaltyAuditModelSpec extends SpecBase with LogCapturing {
  val sampleDateTime1: LocalDateTime = LocalDateTime.of(2019, 1, 31, 23, 59, 59).plus(998, ChronoUnit.MILLIS)

  val basicModel: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadResponseAsModel, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> "penalties-frontend"))
  val basicModelWithUserAgent = (userAgent: String) => UserHasPenaltyAuditModel(mockETMPPayloadResponseAsModel, "1234", "VRN", None)(fakeRequest.withHeaders("User-Agent" -> userAgent))
  val basicAgentModel: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadResponseAsModel, "1234", "VRN", Some("ARN123"))(fakeRequest)
  val mockETMPPayloadWithOutstandingVAT: ETMPPayload = ETMPPayload(
    pointsTotal = 0, lateSubmissions = 0, adjustmentPointsTotal = 0, fixedPenaltyAmount = 0, penaltyAmountsTotal = 0, otherPenalties = None, penaltyPointsThreshold = 4, penaltyPoints = Seq.empty, latePaymentPenalties = None,
    vatOverview = Some(
      Seq(
        OverviewElement(
          `type` = AmountTypeEnum.VAT, amount = 123.45, estimatedInterest = None, crystalizedInterest = None
        ),
        OverviewElement(
          `type` = AmountTypeEnum.Central_Assessment, amount = 123.45, estimatedInterest = None, crystalizedInterest = None
        ),
        OverviewElement(
          `type` = AmountTypeEnum.ECN, amount = 123.10, estimatedInterest = None, crystalizedInterest = None
        )
      )
    )
  )

  val mockETMPPayloadWithInterest: ETMPPayload = ETMPPayload(
    pointsTotal = 1, lateSubmissions = 1, adjustmentPointsTotal = 0, fixedPenaltyAmount = 0, penaltyAmountsTotal = 1, otherPenalties = None, penaltyPointsThreshold = 4,
    penaltyPoints = Seq(
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Financial,
        number = "4",
        id = "id",
        appealStatus = None,
        dateCreated = sampleDateTime1,
        dateExpired = Some(sampleDateTime1),
        status = PointStatusEnum.Due,
        reason = None,
        period = None,
        communications = Seq.empty,
        financial = Some(
          Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        )
      )
    ),
    latePaymentPenalties = Some(
      Seq(
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "1234",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = sampleDateTime1,
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = sampleDateTime1, endDate = sampleDateTime1, dueDate = sampleDateTime1, paymentStatus = PaymentStatusEnum.Due
          ),
          communications = Seq.empty,
          financial = Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        )
      )
    ),
    vatOverview = Some(
      Seq(
        OverviewElement(
          `type` = AmountTypeEnum.VAT, amount = 123.45, estimatedInterest = Some(10), crystalizedInterest = Some(10)
        ),
        OverviewElement(
          `type` = AmountTypeEnum.Central_Assessment, amount = 123.45, estimatedInterest = Some(10), crystalizedInterest = Some(10)
        ),
        OverviewElement(
          `type` = AmountTypeEnum.ECN, amount = 123.10, estimatedInterest = Some(10), crystalizedInterest = Some(10)
        )
      )
    )
  )

  val mockETMPPayloadWithAppeals = (appealStatus: AppealStatusEnum.Value) =>  ETMPPayload(
    pointsTotal = 0, lateSubmissions = 0, adjustmentPointsTotal = 0, fixedPenaltyAmount = 0, penaltyAmountsTotal = 0, otherPenalties = None, penaltyPointsThreshold = 4,
    penaltyPoints = Seq(
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Point,
        number = "4",
        id = "id",
        appealStatus = Some(appealStatus),
        dateCreated = sampleDateTime1,
        dateExpired = Some(sampleDateTime1),
        status = PointStatusEnum.Due,
        reason = None,
        period = None,
        communications = Seq.empty,
        financial = Some(
          Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        )
      )
    ),
    latePaymentPenalties = Some(
      Seq(
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "1234",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = sampleDateTime1,
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = sampleDateTime1, endDate = sampleDateTime1, dueDate = sampleDateTime1, paymentStatus = PaymentStatusEnum.Due
          ),
          communications = Seq.empty,
          financial = Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        )
      )
    ),
    vatOverview = Some(
      Seq(
        OverviewElement(
          `type` = AmountTypeEnum.VAT, amount = 123.45, estimatedInterest = Some(10), crystalizedInterest = Some(10)
        ),
        OverviewElement(
          `type` = AmountTypeEnum.Central_Assessment, amount = 123.45, estimatedInterest = Some(10), crystalizedInterest = Some(10)
        ),
        OverviewElement(
          `type` = AmountTypeEnum.ECN, amount = 123.10, estimatedInterest = Some(10), crystalizedInterest = Some(10)
        )
      )
    )
  )

  val mockETMPPayloadWithLPPs = (paymentStatus: PointStatusEnum.Value) => ETMPPayload(
    pointsTotal = 0, lateSubmissions = 0, adjustmentPointsTotal = 0, fixedPenaltyAmount = 0, penaltyAmountsTotal = 0, otherPenalties = None, penaltyPointsThreshold = 4,
    penaltyPoints = Seq(),
    latePaymentPenalties = Some(
      Seq(
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "1234",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = sampleDateTime1,
          status = paymentStatus,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = sampleDateTime1, endDate = sampleDateTime1, dueDate = sampleDateTime1, paymentStatus = PaymentStatusEnum.Due
          ),
          communications = Seq.empty,
          financial = Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        ),
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "1233",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = sampleDateTime1,
          status = paymentStatus,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = sampleDateTime1, endDate = sampleDateTime1, dueDate = sampleDateTime1, paymentStatus = PaymentStatusEnum.Due
          ),
          communications = Seq.empty,
          financial = Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        )
      )
    ),
    vatOverview = None
  )

  val mockETMPPayloadWithLPPPaidAndUnpaid: ETMPPayload = ETMPPayload(
    pointsTotal = 0, lateSubmissions = 0, adjustmentPointsTotal = 0, fixedPenaltyAmount = 0, penaltyAmountsTotal = 0, otherPenalties = None, penaltyPointsThreshold = 4,
    penaltyPoints = Seq(),
    latePaymentPenalties = Some(
      Seq(
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "1234",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = sampleDateTime1,
          status = PointStatusEnum.Paid,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = sampleDateTime1, endDate = sampleDateTime1, dueDate = sampleDateTime1, paymentStatus = PaymentStatusEnum.Due
          ),
          communications = Seq.empty,
          financial = Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        ),
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "1233",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = sampleDateTime1,
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = sampleDateTime1, endDate = sampleDateTime1, dueDate = sampleDateTime1, paymentStatus = PaymentStatusEnum.Due
          ),
          communications = Seq.empty,
          financial = Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        )
      )
    ),
    vatOverview = None
  )

  val mockETMPPayloadWithLPPAppealed = (appealStatus: AppealStatusEnum.Value) => ETMPPayload(
    pointsTotal = 0, lateSubmissions = 0, adjustmentPointsTotal = 0, fixedPenaltyAmount = 0, penaltyAmountsTotal = 0, otherPenalties = None, penaltyPointsThreshold = 4,
    penaltyPoints = Seq(),
    latePaymentPenalties = Some(
      Seq(
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "1234",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = sampleDateTime1,
          status = PointStatusEnum.Paid,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = sampleDateTime1, endDate = sampleDateTime1, dueDate = sampleDateTime1, paymentStatus = PaymentStatusEnum.Due
          ),
          communications = Seq.empty,
          financial = Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        ),
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "1233",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = sampleDateTime1,
          status = PointStatusEnum.Due,
          appealStatus = Some(appealStatus),
          period = PaymentPeriod(
            startDate = sampleDateTime1, endDate = sampleDateTime1, dueDate = sampleDateTime1, paymentStatus = PaymentStatusEnum.Due
          ),
          communications = Seq.empty,
          financial = Financial(
            amountDue = 200,
            outstandingAmountDue = 200,
            dueDate = sampleDateTime1,
            estimatedInterest = Some(10),
            crystalizedInterest = Some(10)
          )
        )
      )
    ),
    vatOverview = None
  )

  val mockETMPPayloadResponseLSPPaidAndUnpaid: ETMPPayload = ETMPPayload(
    pointsTotal = 1,
    lateSubmissions = 0 ,
    adjustmentPointsTotal = 0,
    fixedPenaltyAmount = 0,
    penaltyAmountsTotal = 1,
    penaltyPointsThreshold = 2,
    penaltyPoints = Seq(
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Financial,
        number = "1",
        id = "123456791",
        appealStatus = None,
        dateCreated = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
        dateExpired = Some(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
        status = PointStatusEnum.Due,
        reason = Some("reason"),
        period = Some(PenaltyPeriod(
          startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
          endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
          submission = Submission(
            dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
            submittedDate = Some(LocalDateTime.of(1970, 2, 7, 0, 0, 0)),
            status = SubmissionStatusEnum.Submitted
          )
        )),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.secureMessage,
            dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            documentId = "123456789"
          )
        ),
        financial = Some(Financial(
          amountDue = 200,
          outstandingAmountDue = 200,
          dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
          outstandingAmountDay15 = None,
          outstandingAmountDay31 = None,
          percentageOfOutstandingAmtCharged = None,
          estimatedInterest = None,
          crystalizedInterest = None
        ))
      ),
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Financial,
        number = "1",
        id = "123456790",
        appealStatus = None,
        dateCreated = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
        dateExpired = Some(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
        status = PointStatusEnum.Removed,
        reason = Some("reason"),
        period = Some(PenaltyPeriod(
          startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
          endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
          submission = Submission(
            dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
            submittedDate = Some(LocalDateTime.of(1970, 2, 7, 0, 0, 0)),
            status = SubmissionStatusEnum.Submitted
          )
        )),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.secureMessage,
            dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            documentId = "123456789"
          )
        ),
        financial = Some(Financial(
          amountDue = 200,
          outstandingAmountDue = 200,
          dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
          outstandingAmountDay15 = None,
          outstandingAmountDay31 = None,
          percentageOfOutstandingAmtCharged = None,
          estimatedInterest = None,
          crystalizedInterest = None
        ))
      ),
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Point,
        number = "1",
        id = "123456789",
        appealStatus = None,
        dateCreated = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
        dateExpired = Some(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
        status = PointStatusEnum.Active,
        reason = Some("reason"),
        period = Some(PenaltyPeriod(
          startDate = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
          endDate = LocalDateTime.of(1970, 1, 31, 0, 0, 0),
          submission = Submission(
            dueDate = LocalDateTime.of(1970, 2, 6, 0, 0, 0),
            submittedDate = Some(LocalDateTime.of(1970, 2, 7, 0, 0, 0)),
            status = SubmissionStatusEnum.Submitted
          )
        )),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.secureMessage,
            dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            documentId = "123456789"
          )
        ),
        financial = None
      )
    )
  )

  val auditModelWithOutstandingParentCharges: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithOutstandingVAT, "1234", "VRN", Some("ARN123"))(fakeRequest)
  val auditModelWithInterest: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithInterest, "1234", "VRN", Some("ARN123"))(fakeRequest)
  val auditModelWithLSPPs: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadResponseAsModelMultiplePoints, "1234", "VRN", None)(fakeRequest)
  val auditModelWithLSPPsUnderReview: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithAppeals(AppealStatusEnum.Under_Review), "1234", "VRN", None)(fakeRequest)
  val auditModelWithLSPPsAcceptedAppeal: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithAppeals(AppealStatusEnum.Accepted), "1234", "VRN", None)(fakeRequest)
  val auditModelWithLSPUnpaidAndRemoved: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadResponseLSPPaidAndUnpaid, "1234", "VRN", None)(fakeRequest)

  val auditModelWithLPPsPaid: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithLPPs(PointStatusEnum.Paid), "1234", "VRN", None)(fakeRequest)
  val auditModelWithLPPsUnpaid: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithLPPs(PointStatusEnum.Due), "1234", "VRN", None)(fakeRequest)
  val auditModelWithLPPsUnpaidAndPaid: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithLPPPaidAndUnpaid, "1234", "VRN", None)(fakeRequest)
  val auditModelWithLPPsUnderReview: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithLPPAppealed(AppealStatusEnum.Under_Review), "1234", "VRN", None)(fakeRequest)
  val auditModelWithLPPsAccepted: UserHasPenaltyAuditModel = UserHasPenaltyAuditModel(mockETMPPayloadWithLPPAppealed(AppealStatusEnum.Accepted), "1234", "VRN", None)(fakeRequest)

  "UserHasPenaltyAuditModel" should {
    "have the correct audit type" in {
      basicModel.auditType shouldBe "PenaltyUserHasPenalty"
    }

    "have the correct transaction name" in {
      basicModel.transactionName shouldBe "penalties-user-has-penalty"
    }

    "show the correct audit details" when {
      "the correct basic detail information is present" in {
        (basicModel.detail \ "taxIdentifier").validate[String].get shouldBe "1234"
        (basicModel.detail \ "identifierType").validate[String].get shouldBe "VRN"
        (basicModel.detail \ "callingService").validate[String].get shouldBe "penalties-frontend"
      }

      "the service is BTA" in {
        (basicModelWithUserAgent("business-account").detail \ "taxIdentifier").validate[String].get shouldBe "1234"
        (basicModelWithUserAgent("business-account").detail \ "identifierType").validate[String].get shouldBe "VRN"
        (basicModelWithUserAgent("business-account").detail \ "callingService").validate[String].get shouldBe "BTA"
      }

      "the service is VATVC" in {
        (basicModelWithUserAgent("vat-through-software").detail \ "taxIdentifier").validate[String].get shouldBe "1234"
        (basicModelWithUserAgent("vat-through-software").detail \ "identifierType").validate[String].get shouldBe "VRN"
        (basicModelWithUserAgent("vat-through-software").detail \ "callingService").validate[String].get shouldBe "VATVC"
      }

      "the user is an agent" in {
        (basicAgentModel.detail \ "agentReferenceNumber").isDefined
        (basicAgentModel.detail \ "agentReferenceNumber").validate[String].get shouldBe "ARN123"
      }

      "the user has parent charges (VAT / ECN etc.) due" in {
        (auditModelWithOutstandingParentCharges.detail \ "penaltyInformation" \ "totalTaxDue").isDefined
        (auditModelWithOutstandingParentCharges.detail \ "penaltyInformation" \ "totalTaxDue").validate[Int].get shouldBe 370
      }

      "the user has interest due" in {
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalInterestDue").isDefined
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalInterestDue").validate[Int].get shouldBe 100
      }

      "the user has financial penalties" in {
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalFinancialPenaltyDue").isDefined
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalFinancialPenaltyDue").validate[Int].get shouldBe 400
      }

      "the user has a combination of all three" in {
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalTaxDue").validate[Int].get shouldBe 370
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalInterestDue").validate[Int].get shouldBe 100
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalFinancialPenaltyDue").validate[Int].get shouldBe 400
        (auditModelWithInterest.detail \ "penaltyInformation" \ "totalDue").validate[Int].get shouldBe 870
      }

      "the user has LSPPs (no appeals)" in {
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 4
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPs.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LSPPs (with appeals)" in {
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 4
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 1
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPsUnderReview.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 1
      }

      "the user has LSPPs (with reviewed appeals)" in {
        (auditModelWithLSPPsAcceptedAppeal.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 4
        (auditModelWithLSPPsAcceptedAppeal.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 1
        (auditModelWithLSPPsAcceptedAppeal.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 0
        (auditModelWithLSPPsAcceptedAppeal.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LSPs that are paid and unpaid" in {
        (auditModelWithLSPUnpaidAndRemoved.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 2
        (auditModelWithLSPUnpaidAndRemoved.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 2
        (auditModelWithLSPUnpaidAndRemoved.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 1
        (auditModelWithLSPUnpaidAndRemoved.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LSPs" in {
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lSPDetail" \ "penaltyPointsThreshold").validate[Int].get shouldBe 4
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lSPDetail" \ "pointsTotal").validate[Int].get shouldBe 1
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lSPDetail" \ "financialPenalties").validate[Int].get shouldBe 1
        (auditModelWithInterest.detail \ "penaltyInformation" \ "lSPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LPPs (all paid)" in {
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has LPPs (all unpaid)" in {
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnpaid.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }

      "the user has a combination of unpaid and paid LPPs" in {
        (auditModelWithLPPsUnpaidAndPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnpaidAndPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnpaidAndPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnpaidAndPaid.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
        (auditModelWithLPPsUnpaidAndPaid.detail \ "penaltyInformation" \ "totalFinancialPenaltyDue").validate[Int].get shouldBe 200
        (auditModelWithLPPsUnpaidAndPaid.detail \ "penaltyInformation" \ "totalInterestDue").validate[Int].get shouldBe 20
        (auditModelWithLPPsUnpaidAndPaid.detail \ "penaltyInformation" \ "totalDue").validate[Int].get shouldBe 220
      }

      "the user has LPPs (with appeals)" in {
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsUnderReview.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 1
      }

      "the user has LPPs (with reviewed appeals)" in {
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfPaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "lPPDetail" \ "numberOfUnpaidPenalties").validate[Int].get shouldBe 1
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "lPPDetail" \ "totalNumberOfPenalties").validate[Int].get shouldBe 2
        (auditModelWithLPPsAccepted.detail \ "penaltyInformation" \ "lPPDetail" \ "underAppeal").validate[Int].get shouldBe 0
      }
    }

    "set the callingService to blank string and log error when the User-Agent can not be matched" in {
      withCaptureOfLoggingFrom(Logger.logger) {
        logs => {
          (basicModelWithUserAgent("").detail \ "callingService").validate[String].get shouldBe ""
          logs.exists(_.getMessage.equals("[UserHasPenaltyAuditModel] - could not distinguish referer for audit")) shouldBe true
        }
      }
    }
  }
}
