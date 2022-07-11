/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import config.featureSwitches.{CallAPI1811ETMP, FeatureSwitching}
import connectors.parsers.getFinancialDetails.GetFinancialDetailsParser._
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class GetFinancialDetailsConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {

  class Setup {
    val connector: GetFinancialDetailsConnector = injector.instanceOf[GetFinancialDetailsConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "getFinancialDetails" should {
    "return a successful response when called" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.OK,
        s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now())(hc))
      result.isRight shouldBe true
    }

    s"return a $GetFinancialDetailsMalformed response when called" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      val malformedBody =  """
          {
           "documentDetails": [{
             "summary": {}
             }]
           }
          """
      mockResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false", body = Some(malformedBody))
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now()))
      result.isLeft shouldBe true
      result.left.get shouldBe GetFinancialDetailsMalformed
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.INTERNAL_SERVER_ERROR, s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now()))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is ISE (${Status.SERVICE_UNAVAILABLE})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.SERVICE_UNAVAILABLE, s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now()))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.SERVICE_UNAVAILABLE
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is NOT FOUND (${Status.NOT_FOUND})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.NOT_FOUND, s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now()))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.NOT_FOUND
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is CONFLICT (${Status.CONFLICT})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.CONFLICT, s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now()))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.CONFLICT
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.UNPROCESSABLE_ENTITY, s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now()))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.UNPROCESSABLE_ENTITY
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is BAD REQUEST (${Status.BAD_REQUEST})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.BAD_REQUEST, s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now()))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.BAD_REQUEST
    }

    s"return a $GetFinancialDetailsFailureResponse when the response status is FORBIDDEN (${Status.FORBIDDEN})" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.FORBIDDEN, s"VRN/123456789/VATC?dateFrom=${LocalDate.now()}&dateTo=${LocalDate.now()}&onlyOpenItems=false&includeStatistical=true&includeLocks=false&calculateAccruedInterest=true&removePOA=false&customerPaymentInformation=false")
      val result: GetFinancialDetailsResponse = await(connector.getFinancialDetails("123456789", LocalDate.now(), LocalDate.now()))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetFinancialDetailsFailureResponse].status shouldBe Status.FORBIDDEN
    }
  }

  "getFinancialDetailsForAPI" should {
    "return a 200 response" in new Setup {
      enableFeatureSwitch(CallAPI1811ETMP)
      mockResponseForGetFinancialDetails(Status.OK, s"VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true")
      val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
        vrn = "123456789",
        docNumber = Some("DOC1"),
        dateFrom = Some("2022-01-01"),
        dateTo = Some("2024-01-01"),
        onlyOpenItems = false,
        includeStatistical = false,
        includeLocks = false,
        calculateAccruedInterest = false,
        removePOA = false,
        customerPaymentInformation = true
      ))
      result.status shouldBe Status.OK
    }

    "handle a UpstreamErrorResponse" when {
      "a 4xx error is returned" in new Setup {
        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetails(Status.FORBIDDEN, s"VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true")
        val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
          vrn = "123456789",
          docNumber = Some("DOC1"),
          dateFrom = Some("2022-01-01"),
          dateTo = Some("2024-01-01"),
          onlyOpenItems = false,
          includeStatistical = false,
          includeLocks = false,
          calculateAccruedInterest = false,
          removePOA = false,
          customerPaymentInformation = true
        ))
        result.status shouldBe Status.FORBIDDEN
      }

      "a 5xx error is returned" in new Setup {
        enableFeatureSwitch(CallAPI1811ETMP)
        mockResponseForGetFinancialDetails(Status.BAD_GATEWAY, s"VRN/123456789/VATC?docNumber=DOC1&dateFrom=2022-01-01&dateTo=2024-01-01&onlyOpenItems=false&includeStatistical=false&includeLocks=false&calculateAccruedInterest=false&removePOA=false&customerPaymentInformation=true")
        val result: HttpResponse = await(connector.getFinancialDetailsForAPI(
          vrn = "123456789",
          docNumber = Some("DOC1"),
          dateFrom = Some("2022-01-01"),
          dateTo = Some("2024-01-01"),
          onlyOpenItems = false,
          includeStatistical = false,
          includeLocks = false,
          calculateAccruedInterest = false,
          removePOA = false,
          customerPaymentInformation = true
        ))
        result.status shouldBe Status.BAD_GATEWAY
      }
    }
  }
}