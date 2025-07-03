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
import connectors.getFinancialDetails.FinancialDetailsConnector
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import models.getFinancialDetails.{FinancialDetails, FinancialDetailsHIP}
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import models.{AgnosticEnrolmentKey, Id, IdType, Regime, getFinancialDetails}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration
import play.api.http.Status.IM_A_TEAPOT
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class FinancialDetailsServiceSpec extends SpecBase with FeatureSwitching with LogCapturing {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val config: Configuration = mock(classOf[Configuration])

  val regime = Regime("VATC")
  val idType = IdType("VRN")
  val id = Id("123456789")
  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    regime, idType, id
  )

  val mockGetFinancialDetailsConnector: FinancialDetailsConnector = mock(classOf[FinancialDetailsConnector])
  class Setup {
    val service = new FinancialDetailsService(mockGetFinancialDetailsConnector)
    reset(mockGetFinancialDetailsConnector)
    reset(config)
    sys.props -= TIME_MACHINE_NOW
  }

  "getFinancialDetails" should {
    val mockGetFinancialDetailsResponseAsModel: FinancialDetailsHIP = FinancialDetailsHIP(
      processingDate = "2025-05-06",
      financialData = FinancialDetails(
      documentDetails = Some(Seq(getFinancialDetails.DocumentDetails(
        chargeReferenceNumber = None,
        documentOutstandingAmount = Some(0.00),
        lineItemDetails = Some(Seq(getFinancialDetails.LineItemDetails(None))),
        documentTotalAmount = Some(100.00),
        issueDate = Some(LocalDate.of(2023, 1, 1)))
      )),
      totalisation = Some(FinancialDetailsTotalisation(
        regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
      ))
    ))

    s"call the connector and return a $GetFinancialDetailsSuccessResponse when the request is successful" in new Setup {
      when(config.getOptional[String](ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(any())(any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsHipSuccessResponse(mockGetFinancialDetailsResponseAsModel))))
      val result: GetFinancialDetailsResponse = await(service.getFinancialDetails(vrn123456789))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetFinancialDetailsHipSuccessResponse(mockGetFinancialDetailsResponseAsModel)
    }

    s"call the connector and return a $GetFinancialDetailsSuccessResponse when the request is successful (with the time machine date)" in new Setup {
      setTimeMachineDate(Some(LocalDateTime.of(2024, 1, 1, 0, 0, 0)))
      when(mockGetFinancialDetailsConnector.getFinancialDetails(any())(any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsHipSuccessResponse(mockGetFinancialDetailsResponseAsModel))))
      val result: GetFinancialDetailsResponse = await(service.getFinancialDetails(vrn123456789))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetFinancialDetailsHipSuccessResponse(mockGetFinancialDetailsResponseAsModel)
    }

    s"call the connector and return $GetFinancialDetailsNoContent when the response body contains NO_DATA_FOUND" in new Setup {
      when(config.getOptional[String](ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(any())(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetFinancialDetailsResponse = await(service.getFinancialDetails(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetFinancialDetailsNoContent
          logs.map(_.getMessage) should contain ("[FinancialDetailsService][getDataFromFinancialService][VATC] - Got a 404 response and no data was found for GetFinancialDetails call")
        }
      }
    }

    s"call the connector and return $GetFinancialDetailsMalformed when the response body is malformed" in new Setup {
      when(config.getOptional[String](ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(any())(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsMalformed)))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetFinancialDetailsResponse = await(service.getFinancialDetails(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetFinancialDetailsMalformed
          logs.map(_.getMessage) should contain ("[FinancialDetailsService][getDataFromFinancialService][VATC] - Failed to parse HTTP response into model for VATC~VRN~123456789")
        }
      }
    }

    s"call the connector and return $GetFinancialDetailsFailureResponse when an unknown status is returned" in new Setup {
      when(config.getOptional[String](ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(any())(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(IM_A_TEAPOT))))
      withCaptureOfLoggingFrom(logger) {
        logs => {
          val result: GetFinancialDetailsResponse = await(service.getFinancialDetails(vrn123456789))
          result.isLeft shouldBe true
          result.left.getOrElse(GetFinancialDetailsFailureResponse(INTERNAL_SERVER_ERROR)) shouldBe GetFinancialDetailsFailureResponse(IM_A_TEAPOT)
          logs.map(_.getMessage) should contain ("[FinancialDetailsService][getDataFromFinancialService][VATC] - Unknown status returned from connector for VATC~VRN~123456789")
        }
      }
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(config.getOptional[String](ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(any())(any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))
      val result: Exception = intercept[Exception](await(service.getFinancialDetails(vrn123456789)))
      result.getMessage shouldBe "Something has gone wrong."
    }
  }
}
