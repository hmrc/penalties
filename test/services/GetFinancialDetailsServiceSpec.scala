/*
 * Copyright 2023 HM Revenue & Customs
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
import config.featureSwitches.FeatureSwitching
import connectors.getFinancialDetails.GetFinancialDetailsConnector
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import models.getFinancialDetails
import models.getFinancialDetails.FinancialDetails
import models.getFinancialDetails.totalisation.{FinancialDetailsTotalisation, InterestTotalisation, RegimeTotalisation}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.Configuration
import play.api.http.Status.IM_A_TEAPOT
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class GetFinancialDetailsServiceSpec extends SpecBase with FeatureSwitching {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val config: Configuration = mock(classOf[Configuration])
  val mockGetFinancialDetailsConnector: GetFinancialDetailsConnector = mock(classOf[GetFinancialDetailsConnector])
  class Setup {
    val service = new GetFinancialDetailsService(mockGetFinancialDetailsConnector)
    reset(mockGetFinancialDetailsConnector)
    reset(config)
    sys.props -= TIME_MACHINE_NOW
  }

  "getFinancialDetails" should {
    val mockGetFinancialDetailsResponseAsModel: FinancialDetails = FinancialDetails(
      documentDetails = Some(Seq(getFinancialDetails.DocumentDetails(
        chargeReferenceNumber = None,
        documentOutstandingAmount = Some(0.00),
        lineItemDetails = Some(Seq(getFinancialDetails.LineItemDetails(None))))
      )),
      totalisation = Some(FinancialDetailsTotalisation(
        regimeTotalisations = Some(RegimeTotalisation(totalAccountOverdue = Some(1000))),
        interestTotalisations = Some(InterestTotalisation(totalAccountPostedInterest = Some(123.45), totalAccountAccruingInterest = Some(23.45)))
      ))
    )

    s"call the connector and return a $GetFinancialDetailsSuccessResponse when the request is successful" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))
      val result: GetFinancialDetailsResponse = await(service.getFinancialDetails("123456789"))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
    }

    s"call the connector and return a $GetFinancialDetailsSuccessResponse when the request is successful (with the time machine date)" in new Setup {
      setTimeMachineDate(Some(LocalDate.of(2024, 1, 1)))
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Right(GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel))))
      val result: GetFinancialDetailsResponse = await(service.getFinancialDetails("123456789"))
      result.isRight shouldBe true
      result.toOption.get shouldBe GetFinancialDetailsSuccessResponse(mockGetFinancialDetailsResponseAsModel)
    }

    s"call the connector and return $GetFinancialDetailsNoContent when the response body contains NO_DATA_FOUND" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsNoContent)))
      val result: GetFinancialDetailsResponse = await(service.getFinancialDetails("123456789"))
      result.isLeft shouldBe true
      result.left.getOrElse(false) shouldBe GetFinancialDetailsNoContent
    }

    s"call the connector and return $GetFinancialDetailsMalformed when the response body is malformed" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsMalformed)))
      val result: GetFinancialDetailsResponse = await(service.getFinancialDetails("123456789"))
      result.isLeft shouldBe true
      result.left.getOrElse(false) shouldBe GetFinancialDetailsMalformed
    }

    s"call the connector and return $GetFinancialDetailsFailureResponse when an unknown status is returned" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.successful(Left(GetFinancialDetailsFailureResponse(IM_A_TEAPOT))))
      val result: GetFinancialDetailsResponse = await(service.getFinancialDetails("123456789"))
      result.isLeft shouldBe true
      result.left.getOrElse(false) shouldBe GetFinancialDetailsFailureResponse(IM_A_TEAPOT)
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(config.getOptional[String](Matchers.any())(Matchers.any()))
        .thenReturn(None)
      when(mockGetFinancialDetailsConnector.getFinancialDetails(Matchers.eq("123456789"))(any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))
      val result: Exception = intercept[Exception](await(service.getFinancialDetails("123456789")))
      result.getMessage shouldBe "Something has gone wrong."
    }
  }
}
