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
import connectors.ETMPConnector
import connectors.parsers.ETMPPayloadParser._
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ETMPServiceSpec extends SpecBase {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockEtmpConnector: ETMPConnector = mock[ETMPConnector]

  class Setup {
    reset(mockEtmpConnector)
    val service = new ETMPService(mockEtmpConnector)
  }

  "getPenaltyDataFromETMPForEnrolment" should {
    s"call the connector and return a $Some when the request is successful" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(GetETMPPayloadSuccessResponse(mockETMPPayloadResponseAsModel))))

      val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe true
      result._1.get shouldBe mockETMPPayloadResponseAsModel
    }

    s"return $None when the connector returns No Content (${NO_CONTENT})" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadNoContent)))

      val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadNoContent
    }

    s"return $None when the response body is malformed" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadMalformed)))

      val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadMalformed
    }

    s"return $None when the connector receives an unmatched status code" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Left(GetETMPPayloadFailureResponse(IM_A_TEAPOT))))

      val result = await(service.getPenaltyDataFromETMPForEnrolment("123456789"))
      result._1.isDefined shouldBe false
      result._2.isLeft shouldBe true
      result._2.left.get shouldBe GetETMPPayloadFailureResponse(IM_A_TEAPOT)
    }

    s"throw an exception when something unknown has happened" in new Setup {
      when(mockEtmpConnector.getPenaltiesDataForEnrolmentKey(ArgumentMatchers.eq("123456789"))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("Something has gone wrong.")))

      val result = intercept[Exception](await(service.getPenaltyDataFromETMPForEnrolment("123456789")))
      result.getMessage shouldBe "Something has gone wrong."
    }
  }
}
