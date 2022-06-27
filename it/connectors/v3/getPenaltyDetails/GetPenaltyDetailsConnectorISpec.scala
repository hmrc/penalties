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

package connectors.v3.getPenaltyDetails

import config.featureSwitches.{CallAPI1812ETMP, FeatureSwitching}
import connectors.parsers.v3.getPenaltyDetails.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsNoContent, GetPenaltyDetailsResponse}
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{ETMPWiremock, IntegrationSpecCommonBase}

class GetPenaltyDetailsConnectorISpec extends IntegrationSpecCommonBase with ETMPWiremock with FeatureSwitching {

  class Setup {
    val connector: GetPenaltyDetailsConnector = injector.instanceOf[GetPenaltyDetailsConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()

  }

  "getPenaltyDetails" should {
    "return a successful response when called" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsv3(Status.OK, "123456789")
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789")(hc))
      result.isRight shouldBe true
    }

    s"return a $GetPenaltyDetailsMalformed response when called" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      val malformedBody =  """
          {
           "lateSubmissionPenalty": {
             "summary": {}
             }
           }
          """
      mockResponseForGetPenaltyDetailsv3(Status.OK, "123456789", body = Some(malformedBody))
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetPenaltyDetailsMalformed
    }

    s"return a $GetPenaltyDetailsFailureResponse when the response status is ISE (${Status.INTERNAL_SERVER_ERROR})" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsv3(Status.INTERNAL_SERVER_ERROR, "123456789")
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return a $GetPenaltyDetailsFailureResponse when the response status is ISE (${Status.SERVICE_UNAVAILABLE})" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsv3(Status.SERVICE_UNAVAILABLE, "123456789")
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.SERVICE_UNAVAILABLE
    }

    s"return a $GetPenaltyDetailsFailureResponse when the response status is NOT FOUND (${Status.NOT_FOUND})" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsv3(Status.NOT_FOUND, "123456789")
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.NOT_FOUND
    }

    s"return a $GetPenaltyDetailsNoContent when the response status is NOT FOUND (${Status.NOT_FOUND}) but with NO_DATA_FOUND in JSON body" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      val noDataFoundBody =
        """
          |{
          | "failures": [
          |   {
          |     "code": "NO_DATA_FOUND",
          |     "reason": "Some reason"
          |   }
          | ]
          |}
          |""".stripMargin
      mockResponseForGetPenaltyDetailsv3(Status.NOT_FOUND, "123456789", body = Some(noDataFoundBody))
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get shouldBe GetPenaltyDetailsNoContent
    }

    s"return a $GetPenaltyDetailsFailureResponse when the response status is NO CONTENT (${Status.NO_CONTENT})" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsv3(Status.NO_CONTENT, "123456789")
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.NO_CONTENT
    }

    s"return a $GetPenaltyDetailsFailureResponse when the response status is CONFLICT (${Status.CONFLICT})" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsv3(Status.CONFLICT, "123456789")
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.CONFLICT
    }

    s"return a $GetPenaltyDetailsFailureResponse when the response status is UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY})" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsv3(Status.UNPROCESSABLE_ENTITY, "123456789")
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.UNPROCESSABLE_ENTITY
    }

    s"return a $GetPenaltyDetailsFailureResponse when the response status is ISE (${Status.BAD_REQUEST})" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsv3(Status.BAD_REQUEST, "123456789")
      val result: GetPenaltyDetailsResponse = await(connector.getPenaltyDetails("123456789"))
      result.isLeft shouldBe true
      result.left.get.asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.BAD_REQUEST
    }
  }

  "getPenaltyDetailsForAPI" should {
    "return a 200 response" in new Setup {
      enableFeatureSwitch(CallAPI1812ETMP)
      mockResponseForGetPenaltyDetailsForAPIv3(Status.OK, "123456789?dateLimit=09")
      val result: HttpResponse = await(connector.getPenaltyDetailsForAPI("123456789", dateLimit = Some("09")))
      result.status shouldBe Status.OK
    }

    "handle a UpstreamErrorResponse" when {
      "a 4xx error is returned" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetailsForAPIv3(Status.FORBIDDEN, "123456789?dateLimit=09")
        val result: HttpResponse = await(connector.getPenaltyDetailsForAPI("123456789", dateLimit = Some("09")))
        result.status shouldBe Status.FORBIDDEN
      }

      "a 5xx error is returned" in new Setup {
        enableFeatureSwitch(CallAPI1812ETMP)
        mockResponseForGetPenaltyDetailsForAPIv3(Status.BAD_GATEWAY, "123456789?dateLimit=09")
        val result: HttpResponse = await(connector.getPenaltyDetailsForAPI("123456789", dateLimit = Some("09")))
        result.status shouldBe Status.BAD_GATEWAY
      }
    }
  }
}

