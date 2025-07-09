/*
 * Copyright 2024 HM Revenue & Customs
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

import base.{LogCapturing, SpecBase}
import config.featureSwitches.{CallAPI1811HIP, FeatureSwitching}
import connectors.getFinancialDetails.{FinancialDetailsConnector, FinancialDetailsHipConnector}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.getFinancialDetails.FinancialDetails
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.Configuration
import play.api.http.Status.IM_A_TEAPOT
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsServiceSpec extends SpecBase with FeatureSwitching with LogCapturing {
  implicit val ec: ExecutionContext  = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier     = HeaderCarrier()
  implicit val config: Configuration = mock(classOf[Configuration])

  val vatcEnrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("VATC"), IdType("VRN"), Id("123456789"))
  val itsaEnrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("ITSA"), IdType("NINO"), Id("AA000000A"))

  val mockGetFinancialDetailsConnector: FinancialDetailsConnector       = mock(classOf[FinancialDetailsConnector])
  val mockGetFinancialDetailsHipConnector: FinancialDetailsHipConnector = mock(classOf[FinancialDetailsHipConnector])

  private def buildIfConnectorMock(mockedResponse: Future[FinancialDetailsResponse],
                                   enrolmentKey: AgnosticEnrolmentKey): OngoingStubbing[Future[FinancialDetailsResponse]] =
    when(mockGetFinancialDetailsConnector.getFinancialDetails(ArgumentMatchers.eq(enrolmentKey), ArgumentMatchers.any())(any()))
      .thenReturn(mockedResponse)
  private def buildHipConnectorMock(mockedResponse: Future[FinancialDetailsResponse],
                                    enrolmentKey: AgnosticEnrolmentKey): OngoingStubbing[Future[FinancialDetailsResponse]] =
    when(mockGetFinancialDetailsHipConnector.getFinancialDetails(ArgumentMatchers.eq(enrolmentKey), ArgumentMatchers.any())(any()))
      .thenReturn(mockedResponse)

  class Setup(upstreamService: String, enrolmentKey: AgnosticEnrolmentKey) {
    val isHip: Boolean = upstreamService == "HIP"

    val service = new FinancialDetailsService(mockGetFinancialDetailsConnector, mockGetFinancialDetailsHipConnector)
    reset(mockGetFinancialDetailsConnector)
    reset(config)
    sys.props -= TIME_MACHINE_NOW

    def buildConnectorMock(mockedResponse: Future[FinancialDetailsResponse]): OngoingStubbing[Future[FinancialDetailsResponse]] =
      if (isHip) buildHipConnectorMock(mockedResponse, enrolmentKey) else buildIfConnectorMock(mockedResponse, enrolmentKey)

    if (isHip) enableFeatureSwitch(CallAPI1811HIP) else disableFeatureSwitch(CallAPI1811HIP)
  }

  val mockGetFinancialDetailsResponseAsModel: FinancialDetails = FinancialDetails(
    documentDetails = Some(
      Seq(getFinancialDetails.DocumentDetails(
        chargeReferenceNumber = None,
        documentOutstandingAmount = Some(0.00),
        lineItemDetails = Some(Seq(getFinancialDetails.LineItemDetails(None))),
        documentTotalAmount = Some(100.00),
        issueDate = Some(LocalDate.of(2023, 1, 1))
      ))),
    totalisation = Some(
      FinancialDetailsTotalisation(
        regimeTotalisation = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
      ))
  )

  private val testCases = Seq(("IF", vatcEnrolmentKey), ("HIP", vatcEnrolmentKey), ("HIP", itsaEnrolmentKey))

  "getFinancialDetails" when {
    testCases.foreach { case (upstreamService, enrolmentKey) =>
      val errorLogPrefix = s"[FinancialDetailsService][getFinancialDetails][$enrolmentKey]"

      s"calling the $upstreamService service for ${enrolmentKey.regime} regime" should {
        s"return a $FinancialDetailsSuccessResponse from the connector when successful" in new Setup(upstreamService, enrolmentKey) {
          buildConnectorMock(Future.successful(Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))

          val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

          result shouldBe Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))
        }

        s"return a $FinancialDetailsSuccessResponse from the connector when successful (with the time machine date)" in new Setup(
          upstreamService,
          enrolmentKey) {
          setTimeMachineDate(Some(LocalDateTime.of(2024, 1, 1, 0, 0, 0)))
          buildConnectorMock(Future.successful(Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))

          val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

          result.isRight shouldBe true
          result.toOption.get shouldBe FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
        }

        s"return $FinancialDetailsNoContent from the connector when the response body contains NO_DATA_FOUND" in new Setup(
          upstreamService,
          enrolmentKey) {
          buildConnectorMock(Future.successful(Left(FinancialDetailsNoContent)))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsNoContent)
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Got a 404 response and no data was found for GetFinancialDetails call")
          }
        }

        s"return $FinancialDetailsMalformed from the connector when the response body is malformed" in new Setup(upstreamService, enrolmentKey) {
          buildConnectorMock(Future.successful(Left(FinancialDetailsMalformed)))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsMalformed)
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Failed to parse HTTP response into model for $enrolmentKey")
          }
        }

        s"return $FinancialDetailsFailureResponse from the connector when an unknown status is returned" in new Setup(
          upstreamService,
          enrolmentKey) {
          buildConnectorMock(Future.successful(Left(FinancialDetailsFailureResponse(IM_A_TEAPOT))))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsFailureResponse(IM_A_TEAPOT))
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Unknown status returned from connector for $enrolmentKey")
          }
        }

        s"throw an exception from the connector when something unknown has happened" in new Setup(upstreamService, enrolmentKey) {
          buildConnectorMock(Future.failed(new Exception("Something has gone wrong.")))

          val result: Exception = intercept[Exception](await(service.getFinancialDetails(enrolmentKey, None)))

          result.getMessage shouldBe "Something has gone wrong."
        }
      }
    }
  }

//  "getFinancialDetailsForAPI" when {
//    testCases.foreach { case (upstreamService, enrolmentKey) =>
//      val errorLogPrefix = s"[FinancialDetailsService][getFinancialDetailsForAPI][$enrolmentKey]"
//
//      s"calling the $upstreamService service for ${enrolmentKey.regime} regime" should {
//        s"return a $GetFinancialDetailsSuccessResponse from the connector when successful" in new Setup(upstreamService, enrolmentKey) {
//          buildConnectorMock(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))
//
//          val result: GetFinancialDetailsResponse = await(service.getFinancialDetailsForAPI(enrolmentKey, None))
//
//          result shouldBe Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))
//        }
//
//        s"return a $GetFinancialDetailsSuccessResponse from the connector when successful (with the time machine date)" in new Setup(
//          upstreamService,
//          enrolmentKey) {
//          setTimeMachineDate(Some(LocalDateTime.of(2024, 1, 1, 0, 0, 0)))
//          buildConnectorMock(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))
//
//          val result: GetFinancialDetailsResponse = await(service.getFinancialDetailsForAPI(enrolmentKey, None))
//
//          result.isRight shouldBe true
//          result.toOption.get shouldBe GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
//        }
//
//        s"return $GetFinancialDetailsNoContent from the connector when the response body contains NO_DATA_FOUND" in new Setup(
//          upstreamService,
//          enrolmentKey) {
//          buildConnectorMock(Future.successful(Left(GetFinancialDetailsNoContent)))
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(service.getFinancialDetailsForAPI(enrolmentKey, None))
//
//            result shouldBe Left(GetFinancialDetailsNoContent)
//            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Got a 404 response and no data was found for GetFinancialDetailsForAPI call")
//          }
//        }
//
//        s"return $GetFinancialDetailsMalformed from the connector when the response body is malformed" in new Setup(upstreamService, enrolmentKey) {
//          buildConnectorMock(Future.successful(Left(GetFinancialDetailsMalformed)))
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(service.getFinancialDetailsForAPI(enrolmentKey, None))
//
//            result shouldBe Left(GetFinancialDetailsMalformed)
//            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Failed to parse HTTP response into model for $enrolmentKey")
//          }
//        }
//
//        s"return $GetFinancialDetailsFailureResponse from the connector when an unknown status is returned" in new Setup(
//          upstreamService,
//          enrolmentKey) {
//          buildConnectorMock(Future.successful(Left(GetFinancialDetailsFailureResponse(IM_A_TEAPOT))))
//
//          withCaptureOfLoggingFrom(logger) { logs =>
//            val result: GetFinancialDetailsResponse = await(service.getFinancialDetailsForAPI(enrolmentKey, None))
//
//            result shouldBe Left(GetFinancialDetailsFailureResponse(IM_A_TEAPOT))
//            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Unknown status returned from connector for $enrolmentKey")
//          }
//        }
//
//        s"throw an exception from the connector when something unknown has happened" in new Setup(upstreamService, enrolmentKey) {
//          buildConnectorMock(Future.failed(new Exception("Something has gone wrong.")))
//
//          val result: Exception = intercept[Exception](await(service.getFinancialDetailsForAPI(enrolmentKey, None)))
//
//          result.getMessage shouldBe "Something has gone wrong."
//        }
//      }
//    }
//  }
}
