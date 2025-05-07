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

package connectors.getFinancialDetails

import config.featureSwitches.{CallAPI1811ETMP, FeatureSwitching}
import connectors.parsers.getFinancialDetails.FinancialDetailsParser._
import play.api.http.Status
import play.api.http.Status.IM_A_TEAPOT
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{ETMPWiremock, IntegrationSpecCommonBase}
import models.{AgnosticEnrolmentKey, Regime, IdType, Id}

class FinancialDetailsConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {

  val regime = Regime("VATC") 
  val idType = IdType("VRN")
  val id = Id("123456789")


  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    regime,
    idType,
    id
  )

  class Setup {
    val connector: FinancialDetailsConnector = injector.instanceOf[FinancialDetailsConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "getFinancialDetails" should {
    "return a successful response when called" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.OK, "123456789")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(hc))
      result.isRight shouldBe true
    }

    s"return a $GetFinancialDetailsMalformed response when called" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      val malformedBody =  """
          {
           "documentDetails": [{
              "documentOutstandingAmount": "xyz"
             }]
           }
          """
      mockResponseForGetFinancialDetails(Status.OK, "123456789", body = Some(malformedBody))
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)) shouldBe GetFinancialDetailsMalformed
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.INTERNAL_SERVER_ERROR, "123456789")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789)(HeaderCarrier()))

      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is ISE (${Status.SERVICE_UNAVAILABLE})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.SERVICE_UNAVAILABLE, "123456789")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.SERVICE_UNAVAILABLE
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is NOT FOUND (${Status.NOT_FOUND})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.NOT_FOUND, "123456789")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.NOT_FOUND
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is CONFLICT (${Status.CONFLICT})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.CONFLICT, "123456789")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.CONFLICT
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.UNPROCESSABLE_ENTITY, "1234567689")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.UNPROCESSABLE_ENTITY
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is BAD REQUEST (${Status.BAD_REQUEST})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.BAD_REQUEST, "123456789")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.BAD_REQUEST
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is FORBIDDEN (${Status.FORBIDDEN})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.FORBIDDEN, "123456789")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails(vrn123456789))
      result.isLeft shouldBe true
      result.left.getOrElse(GetFinancialDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.FORBIDDEN
    }
  }

  "getFinancialDetailsForAPI" should {
    "return a 200 response" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.OK, "123456789")

      val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
        enrolmentKey = vrn123456789,
        searchType = Some("CHGREF"),
        searchItem = Some("XC00178236592"),
        dateType = Some("BILLING"),
        dateFrom = Some("2020-10-03"),
        dateTo = Some("2021-07-12"),
        includeClearedItems = Some(false),
        includeStatisticalItems = Some(true),
        includePaymentOnAccount = Some(true),
        addRegimeTotalisation = Some(false),
        addLockInformation = Some(true),
        addPenaltyDetails = Some(true),
        addPostedInterestDetails = Some(true),
        addAccruingInterestDetails = Some(true)
      ))
      result.status shouldBe Status.OK
    }

    "handle a UpstreamErrorResponse" when {
      "a 4xx error is returned" in new Setup {
        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetails(Status.FORBIDDEN, "123456789")

        val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
          enrolmentKey = vrn123456789,
          searchType = Some("CHGREF"),
          searchItem = Some("XC00178236592"),
          dateType = Some("BILLING"),
          dateFrom = Some("2020-10-03"),
          dateTo = Some("2021-07-12"),
          includeClearedItems = Some(false),
          includeStatisticalItems = Some(true),
          includePaymentOnAccount = Some(true),
          addRegimeTotalisation = Some(false),
          addLockInformation = Some(true),
          addPenaltyDetails = Some(true),
          addPostedInterestDetails = Some(true),
          addAccruingInterestDetails = Some(true)
        ))
        result.status shouldBe Status.FORBIDDEN
      }

      "a 5xx error is returned" in new Setup {
        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetails(Status.BAD_GATEWAY, "123456789")

        val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
          enrolmentKey = vrn123456789,
          searchType = Some("CHGREF"),
          searchItem = Some("XC00178236592"),
          dateType = Some("BILLING"),
          dateFrom = Some("2020-10-03"),
          dateTo = Some("2021-07-12"),
          includeClearedItems = Some(false),
          includeStatisticalItems = Some(true),
          includePaymentOnAccount = Some(true),
          addRegimeTotalisation = Some(false),
          addLockInformation = Some(true),
          addPenaltyDetails = Some(true),
          addPostedInterestDetails = Some(true),
          addAccruingInterestDetails = Some(true)
        ))
        result.status shouldBe Status.BAD_GATEWAY
      }
    }
  }
}