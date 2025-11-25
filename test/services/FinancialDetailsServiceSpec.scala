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
import config.featureSwitches.FeatureSwitching
import connectors.getFinancialDetails.FinancialDetailsHipConnector
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import connectors.parsers.getFinancialDetails.HIPFinancialDetailsParser.{
  HIPFinancialDetailsFailureResponse,
  HIPFinancialDetailsMalformed,
  HIPFinancialDetailsNoContent,
  HIPFinancialDetailsResponse,
  HIPFinancialDetailsSuccessResponse
}
import models._
import models.getFinancialDetails.FinancialDetails
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.Configuration
import play.api.http.Status.IM_A_TEAPOT
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Logger.logger

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsServiceSpec extends SpecBase with FeatureSwitching with LogCapturing {
  implicit val ec: ExecutionContext  = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier     = HeaderCarrier()
  implicit val config: Configuration = mock(classOf[Configuration])

  val vatcEnrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("VATC"), IdType("VRN"), Id("123456789"))
  val itsaEnrolmentKey: AgnosticEnrolmentKey = AgnosticEnrolmentKey(Regime("ITSA"), IdType("NINO"), Id("AA000000A"))

  val mockGetFinancialDetailsHipConnector: FinancialDetailsHipConnector = mock(classOf[FinancialDetailsHipConnector])

  def buildGetFinancialDetailsHIPMock(enrolmentKey: AgnosticEnrolmentKey,
                                      mockedResponse: Future[HIPFinancialDetailsResponse]): OngoingStubbing[Future[HIPFinancialDetailsResponse]] =
    when(mockGetFinancialDetailsHipConnector.getFinancialDetails(ArgumentMatchers.eq(enrolmentKey), ArgumentMatchers.any())(any()))
      .thenReturn(mockedResponse)

  def buildGetFinancialDetailsForApiMock(enrolmentKey: AgnosticEnrolmentKey,
                                         mockedResponse: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] =
    when(
      mockGetFinancialDetailsHipConnector.getFinancialDetailsForAPI(
        ArgumentMatchers.eq(enrolmentKey),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )(any()))
      .thenReturn(mockedResponse)

  trait Setup {
    val service = new FinancialDetailsService(mockGetFinancialDetailsHipConnector)
    reset(config)
    sys.props -= TIME_MACHINE_NOW
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

  "getFinancialDetails" should {
    def getErrorLogPrefix(enrolmentKey: AgnosticEnrolmentKey) = s"[FinancialDetailsService][getFinancialDetails][$enrolmentKey]"
    Seq(vatcEnrolmentKey, itsaEnrolmentKey).foreach { enrolmentKey =>
      s"calling the HIP service for ${enrolmentKey.regime} regime" should {
        val errorLogPrefix = getErrorLogPrefix(enrolmentKey)

        s"return a $FinancialDetailsSuccessResponse from the connector when successful" in new Setup {
          buildGetFinancialDetailsHIPMock(
            enrolmentKey,
            Future.successful(Right(HIPFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))

          val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

          result shouldBe Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))
        }

        s"return a $FinancialDetailsSuccessResponse from the connector when successful (with the time machine date)" in new Setup {
          setTimeMachineDate(Some(LocalDateTime.of(2024, 1, 1, 0, 0, 0)))
          buildGetFinancialDetailsHIPMock(
            enrolmentKey,
            Future.successful(Right(HIPFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))

          val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

          result.isRight shouldBe true
          result.toOption.get shouldBe FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
        }

        s"return $FinancialDetailsNoContent from the connector when the response from the API is no data was found" in new Setup {
          buildGetFinancialDetailsHIPMock(enrolmentKey, Future.successful(Left(HIPFinancialDetailsNoContent)))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsNoContent)
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - No data found for ID")
          }
        }

        s"return $FinancialDetailsMalformed from the connector when the response body is malformed" in new Setup {
          buildGetFinancialDetailsHIPMock(enrolmentKey, Future.successful(Left(HIPFinancialDetailsMalformed)))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsMalformed)
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Failed to parse HTTP response into model for ID")
          }
        }

        s"return $FinancialDetailsFailureResponse from the connector when an unknown status is returned" in new Setup {
          buildGetFinancialDetailsHIPMock(enrolmentKey, Future.successful(Left(HIPFinancialDetailsFailureResponse(IM_A_TEAPOT))))

          withCaptureOfLoggingFrom(logger) { logs =>
            val result: FinancialDetailsResponse = await(service.getFinancialDetails(enrolmentKey, None))

            result shouldBe Left(FinancialDetailsFailureResponse(IM_A_TEAPOT))
            logs.map(_.getMessage) should contain(s"$errorLogPrefix - Unknown status returned from connector for ID")
          }
        }

        s"throw an exception from the connector when something unknown has happened" in new Setup {
          buildGetFinancialDetailsHIPMock(enrolmentKey, Future.failed(new Exception("Something has gone wrong.")))

          val result: Exception = intercept[Exception](await(service.getFinancialDetails(enrolmentKey, None)))

          result.getMessage shouldBe "Something has gone wrong."
        }
      }
    }

    val errorLogPrefix = getErrorLogPrefix(vatcEnrolmentKey)
    s"return a $FinancialDetailsSuccessResponse from the connector when successful" in new Setup {
      buildGetFinancialDetailsHIPMock(
        vatcEnrolmentKey,
        Future.successful(Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))

      val result: FinancialDetailsResponse = await(service.getFinancialDetails(vatcEnrolmentKey, None))

      result shouldBe Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))
    }

    s"return a $FinancialDetailsSuccessResponse from the connector when successful (with the time machine date)" in new Setup {
      buildGetFinancialDetailsHIPMock(
        vatcEnrolmentKey,
        Future.successful(Right(FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))

      val result: FinancialDetailsResponse = await(service.getFinancialDetails(vatcEnrolmentKey, None))

      result.isRight shouldBe true
      result.toOption.get shouldBe FinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
    }

    s"return $FinancialDetailsNoContent from the connector when the response from the API is no data was found" in new Setup {
      buildGetFinancialDetailsHIPMock(vatcEnrolmentKey, Future.successful(Left(FinancialDetailsNoContent)))

      withCaptureOfLoggingFrom(logger) { logs =>
        val result: FinancialDetailsResponse = await(service.getFinancialDetails(vatcEnrolmentKey, None))

        result shouldBe Left(FinancialDetailsNoContent)
        logs.map(_.getMessage) should contain(s"$errorLogPrefix - No data found for ID")
      }
    }

    s"return $FinancialDetailsMalformed from the connector when the response body is malformed" in new Setup {
      buildGetFinancialDetailsHIPMock(vatcEnrolmentKey, Future.successful(Left(FinancialDetailsMalformed)))

      withCaptureOfLoggingFrom(logger) { logs =>
        val result: FinancialDetailsResponse = await(service.getFinancialDetails(vatcEnrolmentKey, None))

        result shouldBe Left(FinancialDetailsMalformed)
        logs.map(_.getMessage) should contain(s"$errorLogPrefix - Failed to parse HTTP response into model for ID")
      }
    }

    s"return $FinancialDetailsFailureResponse from the connector when an unknown status is returned" in new Setup {
      buildGetFinancialDetailsHIPMock(vatcEnrolmentKey, Future.successful(Left(FinancialDetailsFailureResponse(IM_A_TEAPOT))))

      withCaptureOfLoggingFrom(logger) { logs =>
        val result: FinancialDetailsResponse = await(service.getFinancialDetails(vatcEnrolmentKey, None))

        result shouldBe Left(FinancialDetailsFailureResponse(IM_A_TEAPOT))
        logs.map(_.getMessage) should contain(s"$errorLogPrefix - Unknown status returned from connector for ID")
      }
    }

    s"throw an exception from the connector when something unknown has happened" in new Setup {
      buildGetFinancialDetailsHIPMock(vatcEnrolmentKey, Future.failed(new Exception("Something has gone wrong.")))

      val result: Exception = intercept[Exception](await(service.getFinancialDetails(vatcEnrolmentKey, None)))

      result.getMessage shouldBe "Something has gone wrong."
    }
  }

  "getFinancialDetailsForAPI" should {
    "return 200 HttpResponse from the connector when call is successful" in new Setup {
      buildGetFinancialDetailsForApiMock(vatcEnrolmentKey, Future.successful(HttpResponse(200, "")))

      val result: HttpResponse =
        await(service.getFinancialDetailsForAPI(vatcEnrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None))

      result.status shouldBe 200
    }

    Seq(BAD_REQUEST, UNAUTHORIZED, NOT_FOUND, INTERNAL_SERVER_ERROR).foreach { errorStatus =>
      s"return $errorStatus HttpResponse from connector when $errorStatus error is returned" in new Setup {
        buildGetFinancialDetailsForApiMock(vatcEnrolmentKey, Future.successful(HttpResponse(errorStatus, "")))

        val result: HttpResponse =
          await(service.getFinancialDetailsForAPI(vatcEnrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None))

        result.status shouldBe errorStatus
      }
    }

    s"throw an exception from the connector when something unknown has happened" in new Setup {
      buildGetFinancialDetailsForApiMock(vatcEnrolmentKey, Future.failed(new Exception("Something has gone wrong.")))

      val result: Exception = intercept[Exception](
        await(service.getFinancialDetailsForAPI(vatcEnrolmentKey, None, None, None, None, None, None, None, None, None, None, None, None, None)))

      result.getMessage shouldBe "Something has gone wrong."
    }
  }
}
