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

package base

import java.time.LocalDateTime

import config.AppConfig
import models.ETMPPayload
import models.appeals.AgentDetails
import models.communication.{Communication, CommunicationTypeEnum}
import models.financial.Financial
import models.payment.{LatePaymentPenalty, PaymentPeriod, PaymentStatusEnum}
import models.penalty.PenaltyPeriod
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import models.reason.PaymentPenaltyReasonEnum
import models.submission.{Submission, SubmissionStatusEnum}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.EmailAddress

trait SpecBase extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  lazy val injector: Injector = app.injector

  implicit val appConfig: AppConfig = injector.instanceOf[AppConfig]

  val fakeRequest: FakeRequest[AnyContent] = FakeRequest("GET", "/")

  val mockETMPPayloadResponseAsModel: ETMPPayload = ETMPPayload(
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

  val mockETMPPayloadResponseAsModelForLPP: ETMPPayload = ETMPPayload(
    pointsTotal = 1,
    lateSubmissions = 0 ,
    adjustmentPointsTotal = 0,
    fixedPenaltyAmount = 0,
    penaltyAmountsTotal = 1,
    penaltyPointsThreshold = 3,
    penaltyPoints = Seq(
    ),
    latePaymentPenalties = Some(
      Seq(
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "123456800",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            paymentStatus = PaymentStatusEnum.Paid
          ),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.letter,
              dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
              documentId = "DOC1"
            )
          ),
          financial = Financial(
            amountDue = 144,
            outstandingAmountDue = 0,
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0)
          )
        )
      )
    )
  )

  val mockETMPPayloadResponseAsModelForLPPWithAdditionalPenalties: ETMPPayload = ETMPPayload(
    pointsTotal = 1,
    lateSubmissions = 0 ,
    adjustmentPointsTotal = 0,
    fixedPenaltyAmount = 0,
    penaltyAmountsTotal = 1,
    penaltyPointsThreshold = 3,
    penaltyPoints = Seq(
    ),
    latePaymentPenalties = Some(
      Seq(
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Additional,
          id = "123456801",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_AFTER_30_DAYS,
          dateCreated = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            paymentStatus = PaymentStatusEnum.Paid
          ),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.letter,
              dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
              documentId = "DOC1"
            )
          ),
          financial = Financial(
            amountDue = 144,
            outstandingAmountDue = 0,
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0)
          )
        ),
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "123456800",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            paymentStatus = PaymentStatusEnum.Paid
          ),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.letter,
              dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
              documentId = "DOC1"
            )
          ),
          financial = Financial(
            amountDue = 144,
            outstandingAmountDue = 0,
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0)
          )
        )
      )
    )
  )

  val mockETMPPayloadResponseAsModelMultiplePoints: ETMPPayload = ETMPPayload(
    pointsTotal = 2,
    lateSubmissions = 2,
    adjustmentPointsTotal = 0,
    fixedPenaltyAmount = 0,
    penaltyAmountsTotal = 2,
    penaltyPointsThreshold = 4,
    penaltyPoints = Seq(
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Point,
        number = "2",
        id = "123456790",
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

  val mockETMPPayloadResponseAsModelMultipleSubmissionAndPaymentPoints: ETMPPayload = ETMPPayload(
    pointsTotal = 2,
    lateSubmissions = 2,
    adjustmentPointsTotal = 0,
    fixedPenaltyAmount = 0,
    penaltyAmountsTotal = 2,
    penaltyPointsThreshold = 4,
    penaltyPoints = Seq(
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Point,
        number = "2",
        id = "123456790",
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
    ),
    latePaymentPenalties = Some(
      Seq(
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "123456800",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            paymentStatus = PaymentStatusEnum.Paid
          ),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.letter,
              dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
              documentId = "DOC1"
            )
          ),
          financial = Financial(
            amountDue = 144,
            outstandingAmountDue = 0,
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0)
          )
        ),
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "123456799",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            paymentStatus = PaymentStatusEnum.Paid
          ),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.letter,
              dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
              documentId = "DOC1"
            )
          ),
          financial = Financial(
            amountDue = 144,
            outstandingAmountDue = 0,
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0)
          )
        )
      )
    )
  )

  val sampleMTDVATEnrolmentKey: String = "HMRC-MTD-VAT~VRN~123456789"

  val mockETMPPayloadForAPIResponseData: ETMPPayload = ETMPPayload(
    pointsTotal = 4,
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
      ),
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Point,
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
          dateCreated = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
          status = PointStatusEnum.Due,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            paymentStatus = PaymentStatusEnum.Paid
          ),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.letter,
              dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
              documentId = "DOC1"
            )
          ),
          financial = Financial(
            amountDue = 144,
            outstandingAmountDue = 0,
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0)
          )
        ),
        LatePaymentPenalty(
          `type` = PenaltyTypeEnum.Financial,
          id = "123456800",
          reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
          dateCreated = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
          status = PointStatusEnum.Estimated,
          appealStatus = None,
          period = PaymentPeriod(
            startDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            endDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
            paymentStatus = PaymentStatusEnum.Paid
          ),
          communications = Seq(
            Communication(
              `type` = CommunicationTypeEnum.letter,
              dateSent = LocalDateTime.of(1970, 2, 8, 0, 0, 0),
              documentId = "DOC1"
            )
          ),
          financial = Financial(
            amountDue = 144,
            outstandingAmountDue = 0,
            dueDate = LocalDateTime.of(1970, 2, 8, 0, 0, 0)
          )
        )
      )

    )
  )

  val mockETMPPayloadWithNoEstimatedPenaltiesForAPIResponseData: ETMPPayload = ETMPPayload(
    pointsTotal = 4,
    lateSubmissions = 0 ,
    adjustmentPointsTotal = 0,
    fixedPenaltyAmount = 0,
    penaltyAmountsTotal = 1,
    penaltyPointsThreshold = 3,
    penaltyPoints = Seq()
  )

  val agentDetails = Some(AgentDetails(
    agentReferenceNo = "AGENT1",
    name = "Jack",
    addressLine1 = "Flat 20",
    addressLine2 = Some("123 Jack street"),
    addressLine3 = None,
    addressLine4 = Some("Birmingham"),
    addressLine5 = Some("UK"),
    postCode = "AAA AAA",
    agentEmailID = Some(EmailAddress("Jack@aaa.com"))
  ))
}
