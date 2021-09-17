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
import connectors.ComplianceConnector
import connectors.parsers.ComplianceParser.{GetCompliancePayloadFailureResponse, GetCompliancePayloadMalformed,
  GetCompliancePayloadNoContent, GetCompliancePayloadSuccessResponse}
import models.compliance.{CompliancePayload, MissingReturn, Return, ReturnStatusEnum}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class ComplianceServiceSpec extends SpecBase {
  val mockComplianceConnector: ComplianceConnector = mock(classOf[ComplianceConnector])
  val mockDateHelper: DateHelper = mock(classOf[DateHelper])
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  class Setup {
    val service = new ComplianceService(mockComplianceConnector, mockDateHelper)
    reset(mockDateHelper)
    reset(mockComplianceConnector)
  }

  "getComplianceSummary" should {
    val mtdVatRegime: String = "mtd-vat"
    "return None" when {
      "the connector returns an unknown failure" in new Setup {
        val identifier: String = "123456789"
        when(mockComplianceConnector.getComplianceSummaryForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(mtdVatRegime))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR))))
        val result: Option[JsValue] = await(service.getComplianceSummary(identifier, mtdVatRegime)(implicitly, implicitly, ""))
        result.isDefined shouldBe false
      }

      "the connector returns invalid JSON" in new Setup {
        val identifier: String = "123456789"
        when(mockComplianceConnector.getComplianceSummaryForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(mtdVatRegime))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetCompliancePayloadMalformed)))
        val result: Option[JsValue] = await(service.getComplianceSummary(identifier, mtdVatRegime)(implicitly, implicitly, ""))
        result.isDefined shouldBe false
      }

      "the connector returns NoContent" in new Setup {
        val identifier: String = "123456789"
        when(mockComplianceConnector.getComplianceSummaryForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(mtdVatRegime))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetCompliancePayloadNoContent)))
        val result: Option[JsValue] = await(service.getComplianceSummary(identifier, mtdVatRegime)(implicitly, implicitly, ""))
        result.isDefined shouldBe false
      }
    }

    "return Some" when {
      "the connector returns a successful response" in new Setup {
        val identifier: String = "123456789"
        when(mockComplianceConnector.getComplianceSummaryForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(mtdVatRegime))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(Json.parse("{}")))))
        val result: Option[JsValue] = await(service.getComplianceSummary(identifier, mtdVatRegime)(implicitly, implicitly, ""))
        result.isDefined shouldBe true
        result.get shouldBe Json.parse("{}")
      }
    }
  }

  "getComplianceHistory" should {
    val mtdVatRegime: String = "mtd-vat"
    "return None" when {
      "the connector returns an unknown failure" in new Setup {
        val identifier: String = "123456789"
        val localDateTime: LocalDateTime = LocalDateTime.of(
          2022, 5, 1, 0, 0, 0)
        when(mockComplianceConnector.getPastReturnsForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(localDateTime.minusYears(2)), ArgumentMatchers.eq(localDateTime), ArgumentMatchers.eq("mtd-vat"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR))))
        val result: Option[JsValue] = await(service.getComplianceHistory(identifier, localDateTime.minusYears(2),
          localDateTime, mtdVatRegime)(implicitly, implicitly, ""))
        result.isDefined shouldBe false
      }

      "the connector returns invalid JSON" in new Setup {
        val identifier: String = "123456789"
        val localDateTime: LocalDateTime = LocalDateTime.of(
          2022, 5, 1, 0, 0, 0)
        when(mockComplianceConnector.getPastReturnsForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(localDateTime.minusYears(2)), ArgumentMatchers.eq(localDateTime), ArgumentMatchers.eq("mtd-vat"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetCompliancePayloadMalformed)))
        val result: Option[JsValue] = await(service.getComplianceHistory(identifier, localDateTime.minusYears(2),
          localDateTime, mtdVatRegime)(implicitly, implicitly, ""))
        result.isDefined shouldBe false
      }

      "the connector returns NoContent" in new Setup {
        val identifier: String = "123456789"
        val localDateTime: LocalDateTime = LocalDateTime.of(
          2022, 5, 1, 0, 0, 0)
        when(mockComplianceConnector.getPastReturnsForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(localDateTime.minusYears(2)), ArgumentMatchers.eq(localDateTime), ArgumentMatchers.eq("mtd-vat"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetCompliancePayloadNoContent)))
        val result: Option[JsValue] = await(service.getComplianceHistory(identifier, localDateTime.minusYears(2),
          localDateTime, mtdVatRegime)(implicitly, implicitly, ""))
        result.isDefined shouldBe false
      }
    }

    "return Some" when {
      "the connector returns a successful response - calling the connector with startDate = now - 2 years and endDate = now" in new Setup {
        val identifier: String = "123456789"
        val localDateTime: LocalDateTime = LocalDateTime.of(
          2022, 5, 1, 0, 0, 0)
        when(mockDateHelper.dateTimeNow()).thenReturn(localDateTime)
        when(mockComplianceConnector.getPastReturnsForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(localDateTime.minusYears(2)), ArgumentMatchers.eq(localDateTime), ArgumentMatchers.eq("mtd-vat"))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(Json.parse("{}")))))
        val result: Option[JsValue] = await(service.getComplianceHistory(identifier, localDateTime.minusYears(2),
          localDateTime, mtdVatRegime)(implicitly, implicitly, ""))
        result.isDefined shouldBe true
        result.get shouldBe Json.parse("{}")
      }
    }
  }

  "getComplianceDataForEnrolmentKey" should {
    val identifier: String = "123456789"
    val enrolmentKey: String = s"HMRC-MTD-VAT~VRN~$identifier"
    val mtdVatRegime: String = "mtd-vat"

    "return None" when {
      "the call to retrieve previous compliance data succeeds but the call to retrieve current data does not succeed" in new Setup {
        val localDateTime: LocalDateTime = LocalDateTime.of(
          2022, 5, 1, 0, 0, 0)
        when(mockDateHelper.dateTimeNow()).thenReturn(localDateTime)
        when(mockComplianceConnector.getComplianceSummaryForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(mtdVatRegime))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR))))
        when(mockComplianceConnector.getPastReturnsForEnrolmentKey(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(Json.parse("{}")))))

        val result: Option[CompliancePayload] = await(service.getComplianceDataForEnrolmentKey(enrolmentKey))
        result.isDefined shouldBe false
      }

      "the call to retrieve current data succeeds but the call to retrieve previous compliance data does not succeed" in new Setup {
        val localDateTime: LocalDateTime = LocalDateTime.of(
          2022, 5, 1, 0, 0, 0)
        when(mockDateHelper.dateTimeNow()).thenReturn(localDateTime)
        when(mockComplianceConnector.getComplianceSummaryForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(mtdVatRegime))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(Json.parse("{}")))))
        when(mockComplianceConnector.getPastReturnsForEnrolmentKey(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(GetCompliancePayloadFailureResponse(Status.INTERNAL_SERVER_ERROR))))

        val result: Option[CompliancePayload] = await(service.getComplianceDataForEnrolmentKey(enrolmentKey))
        result.isDefined shouldBe false
      }

      "both calls succeed but the JSON returned is invalid" in new Setup {
        val localDateTime: LocalDateTime = LocalDateTime.of(
          2022, 5, 1, 0, 0, 0)
        when(mockDateHelper.dateTimeNow()).thenReturn(localDateTime)
        when(mockComplianceConnector.getComplianceSummaryForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(mtdVatRegime))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(Json.parse("{}")))))
        when(mockComplianceConnector.getPastReturnsForEnrolmentKey(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(Json.parse("{}")))))

        val result: Option[CompliancePayload] = await(service.getComplianceDataForEnrolmentKey(enrolmentKey))
        result.isDefined shouldBe false
      }
    }

    "return Some" when {
      "the call to retrieve current and previous compliance data succeeds - startDate = now - 2 years, endDate = now" in new Setup {
        val localDateTime: LocalDateTime = LocalDateTime.of(
          2022, 5, 1, 0, 0, 0)
        val previousDataResponse: JsValue = Json.parse(
          """
            |{
            |   "regime":"VAT",
            |   "VRN":"231",
            |   "noOfMissingReturns":"2",
            |   "missingReturns":[
            |      {
            |         "startDate":"2020-10-01T00:00:00.000Z",
            |         "endDate":"2020-12-31T23:59:59.000Z"
            |      }
            |   ]
            |}
            |""".stripMargin)
        val currentComplianceDataResponse: JsValue = Json.parse(
          """
            |{
            |   "regime":"VAT",
            |   "VRN":"231",
            |   "expiryDateOfAllPenaltyPoints":"2023-03-31T00:00:00.000Z",
            |   "noOfSubmissionsReqForCompliance":"4",
            |   "returns":[
            |      {
            |         "startDate":"2020-10-01T00:00:00.000Z",
            |         "endDate":"2020-12-31T23:59:59.000Z",
            |         "dueDate":"2021-05-07T23:59:59.000Z",
            |         "status":"Submitted"
            |      },
            |      {
            |         "startDate":"2021-04-01T00:00:00.000Z",
            |         "endDate":"2021-06-30T23:59:59.000Z",
            |         "dueDate":"2021-08-07T23:59:59.000Z"
            |      }
            |   ]
            |}
            |""".stripMargin)

        val complianceModelRepresentingJSON: CompliancePayload = CompliancePayload(
          noOfMissingReturns = "2",
          noOfSubmissionsReqForCompliance = "4",
          expiryDateOfAllPenaltyPoints = LocalDateTime.of(
            2023, 3, 31, 0, 0, 0, 0),
          missingReturns = Seq(
            MissingReturn(
              startDate = LocalDateTime.of(
                2020, 10, 1, 0, 0, 0, 0),
              endDate = LocalDateTime.of(
                2020, 12, 31, 23, 59, 59, 0)
            )
          ),
          returns = Seq(
            Return(
              startDate = LocalDateTime.of(
                2020, 10, 1, 0, 0, 0, 0),
              endDate = LocalDateTime.of(
                2020, 12, 31, 23, 59, 59, 0),
              dueDate = LocalDateTime.of(
                2021, 5, 7, 23, 59, 59, 0),
              status = Some(ReturnStatusEnum.submitted)
            ),
            Return(
              startDate = LocalDateTime.of(
                2021, 4, 1, 0, 0, 0, 0),
              endDate = LocalDateTime.of(
                2021, 6, 30, 23, 59, 59, 0),
              dueDate = LocalDateTime.of(
                2021, 8, 7, 23, 59, 59, 0),
              status = None
            )
          )
        )
        val startDateCaptor: ArgumentCaptor[LocalDateTime] =  ArgumentCaptor.forClass(classOf[LocalDateTime])
        val endDateCaptor: ArgumentCaptor[LocalDateTime] = ArgumentCaptor.forClass(classOf[LocalDateTime])
        when(mockDateHelper.dateTimeNow()).thenReturn(localDateTime)
        when(mockComplianceConnector.getComplianceSummaryForEnrolmentKey(ArgumentMatchers.eq(identifier),
          ArgumentMatchers.eq(mtdVatRegime))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(previousDataResponse))))
        when(mockComplianceConnector.getPastReturnsForEnrolmentKey(ArgumentMatchers.any(),
          startDateCaptor.capture(), endDateCaptor.capture(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(GetCompliancePayloadSuccessResponse(currentComplianceDataResponse))))

        val result: Option[CompliancePayload] = await(service.getComplianceDataForEnrolmentKey(enrolmentKey))
        result.isDefined shouldBe true
        result.get shouldBe complianceModelRepresentingJSON
        startDateCaptor.getValue shouldBe localDateTime.minusYears(2)
        endDateCaptor.getValue shouldBe localDateTime
      }
    }
  }
}
