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

package utils

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import com.codahale.metrics.SharedMetricRegistries
import helpers.WiremockHelper
import models.ETMPPayload
import models.appeals.AppealStatusEnum
import models.communication.{Communication, CommunicationTypeEnum}
import models.financial.{AmountTypeEnum, Financial, OverviewElement}
import models.payment.{LatePaymentPenalty, PaymentPeriod, PaymentStatusEnum}
import models.penalty.PenaltyPeriod
import models.point.{PenaltyPoint, PenaltyTypeEnum, PointStatusEnum}
import models.reason.PaymentPenaltyReasonEnum
import models.submission.{Submission, SubmissionStatusEnum}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestSuite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.http.HeaderCarrier

trait IntegrationSpecCommonBase extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with
  BeforeAndAfterAll with BeforeAndAfterEach with TestSuite with WiremockHelper with DatastreamWiremock {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val injector: Injector = app.injector

  override def afterEach(): Unit = {
    resetAll()
    super.afterEach()
    SharedMetricRegistries.clear()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockAuditResponse()
    mockMergedAuditResponse()
    SharedMetricRegistries.clear()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    start()
    SharedMetricRegistries.clear()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    resetAll()
    stop()
    SharedMetricRegistries.clear()
  }

  val configForApp: Map[String, Any] = Map(
    "auditing.enabled" -> true,
    "auditing.traceRequests" -> false,
    "microservice.services.penalties-stub.host" -> stubHost,
    "microservice.services.penalties-stub.port" -> stubPort,
    "microservice.services.etmp.host" -> stubHost,
    "microservice.services.etmp.port" -> stubPort,
    "microservice.services.etmp.host" -> stubHost,
    "auditing.consumer.baseUri.host" -> stubHost,
    "auditing.consumer.baseUri.port" -> stubPort
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(configForApp)
    .build()

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  def buildClientForRequestToApp(baseUrl: String = "/penalties", uri: String): WSRequest = {
    ws.url(s"http://localhost:$port$baseUrl$uri").withFollowRedirects(false)
  }

  val sampleDate: LocalDateTime = LocalDateTime.of(2021, 4, 23, 18, 25, 43).plus(511, ChronoUnit.MILLIS)

  val etmpPayloadModel: ETMPPayload = ETMPPayload(
    pointsTotal = 1,
    lateSubmissions = 1,
    adjustmentPointsTotal = 1,
    fixedPenaltyAmount = 200.00,
    penaltyAmountsTotal = 400.00,
    penaltyPointsThreshold = 4,
    penaltyPoints = Seq(
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Financial,
        number = "2",
        id = "1235",
        appealStatus = Some(AppealStatusEnum.Under_Review),
        dateCreated = sampleDate,
        dateExpired = Some(sampleDate),
        status = PointStatusEnum.Active,
        reason = None,
        period = Some(PenaltyPeriod(
          startDate = sampleDate,
          endDate = sampleDate,
          submission = Submission(
            dueDate = sampleDate,
            submittedDate = Some(sampleDate),
            status = SubmissionStatusEnum.Submitted
          )
        )),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.secureMessage,
            dateSent = sampleDate,
            documentId = "1234567890"
          )
        ),
        financial = Some(Financial(
          amountDue = 400.00,
          outstandingAmountDue = 400.00,
          dueDate = sampleDate
        ))
      ),
      PenaltyPoint(
        `type` = PenaltyTypeEnum.Point,
        number = "1",
        id = "1234",
        appealStatus = None,
        dateCreated = sampleDate,
        dateExpired = Some(sampleDate),
        status = PointStatusEnum.Active,
        reason = None,
        period = Some(PenaltyPeriod(
          startDate = sampleDate,
          endDate = sampleDate,
          submission = Submission(
            dueDate = sampleDate,
            submittedDate = Some(sampleDate),
            status = SubmissionStatusEnum.Submitted
          )
        )),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.letter,
            dateSent = sampleDate,
            documentId = "1234567890")
        ),
        financial = None
      )
    )
  )

  val etmpPayloadModelWithLPP: ETMPPayload = etmpPayloadModel.copy(latePaymentPenalties = Some(Seq(
    LatePaymentPenalty(
      `type` = PenaltyTypeEnum.Financial,
      id = "1234567891",
      reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
      dateCreated = sampleDate,
      status = PointStatusEnum.Active,
      appealStatus = Some(AppealStatusEnum.Under_Review),
      period = PaymentPeriod(
        startDate = sampleDate,
        endDate = sampleDate,
        dueDate = sampleDate,
        paymentStatus = PaymentStatusEnum.Paid
      ),
      communications = Seq(
        Communication(
          `type` = CommunicationTypeEnum.letter,
          dateSent = sampleDate,
          documentId = "1234567890"
        )
      ),
      financial = Financial(
        amountDue = 400.00,
        outstandingAmountDue = 0.00,
        dueDate = sampleDate
      )
    )
  )))

  val etmpPayloadModelWithLPPAndAdditionalPenalties: ETMPPayload = etmpPayloadModel.copy(latePaymentPenalties = Some(Seq(
    LatePaymentPenalty(
      `type` = PenaltyTypeEnum.Additional,
      id = "1234567892",
      reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_AFTER_30_DAYS,
      dateCreated = sampleDate,
      status = PointStatusEnum.Active,
      period = PaymentPeriod(
        startDate = sampleDate,
        endDate = sampleDate,
        dueDate = sampleDate,
        paymentStatus = PaymentStatusEnum.Paid
      ),
      communications = Seq(
        Communication(
          `type` = CommunicationTypeEnum.letter,
          dateSent = sampleDate,
          documentId = "1234567890"
        )
      ),
      financial = Financial(
        amountDue = 123.45,
        outstandingAmountDue = 0.00,
        dueDate = sampleDate
      )
    ),
    LatePaymentPenalty(
      `type` = PenaltyTypeEnum.Financial,
      id = "1234567891",
      reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
      dateCreated = sampleDate,
      status = PointStatusEnum.Active,
      appealStatus = Some(AppealStatusEnum.Under_Review),
      period = PaymentPeriod(
        startDate = sampleDate,
        endDate = sampleDate,
        dueDate = sampleDate,
        paymentStatus = PaymentStatusEnum.Paid
      ),
      communications = Seq(
        Communication(
          `type` = CommunicationTypeEnum.letter,
          dateSent = sampleDate,
          documentId = "1234567890"
        )
      ),
      financial = Financial(
        amountDue = 400.00,
        outstandingAmountDue = 0.00,
        dueDate = sampleDate
      )
    )
  )))

  val etmpPayloadModelWithVATOverview: ETMPPayload = etmpPayloadModel.copy(
    otherPenalties = Some(false),
    vatOverview = Some(
      Seq(
        OverviewElement(
          `type` = AmountTypeEnum.VAT,
          amount = 100.00,
          estimatedInterest = Some(10.00),
          crystalizedInterest = Some(10.00)
        ),
        OverviewElement(
          `type` = AmountTypeEnum.Central_Assessment,
          amount = 100.00,
          estimatedInterest = Some(10.00),
          crystalizedInterest = Some(10.00)
        )
      )
    ),
    latePaymentPenalties = Some(Seq(
      LatePaymentPenalty(
        `type` = PenaltyTypeEnum.Additional,
        id = "1234567892",
        reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_AFTER_30_DAYS,
        dateCreated = sampleDate,
        status = PointStatusEnum.Active,
        period = PaymentPeriod(
          startDate = sampleDate,
          endDate = sampleDate,
          dueDate = sampleDate,
          paymentStatus = PaymentStatusEnum.Paid
        ),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.letter,
            dateSent = sampleDate,
            documentId = "1234567890"
          )
        ),
        financial = Financial(
          amountDue = 123.45,
          outstandingAmountDue = 0.00,
          dueDate = sampleDate
        )
      ),
      LatePaymentPenalty(
        `type` = PenaltyTypeEnum.Financial,
        id = "1234567891",
        reason = PaymentPenaltyReasonEnum.VAT_NOT_PAID_WITHIN_30_DAYS,
        dateCreated = sampleDate,
        status = PointStatusEnum.Active,
        appealStatus = Some(AppealStatusEnum.Under_Review),
        period = PaymentPeriod(
          startDate = sampleDate,
          endDate = sampleDate,
          dueDate = sampleDate,
          paymentStatus = PaymentStatusEnum.Paid
        ),
        communications = Seq(
          Communication(
            `type` = CommunicationTypeEnum.letter,
            dateSent = sampleDate,
            documentId = "1234567890"
          )
        ),
        financial = Financial(
          amountDue = 400.00,
          outstandingAmountDue = 0.00,
          dueDate = sampleDate
        )
      )
  )))
}
