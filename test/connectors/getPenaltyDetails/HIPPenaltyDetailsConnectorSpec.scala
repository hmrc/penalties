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

package connectors.getPenaltyDetails

import base.{LogCapturing, SpecBase}
import config.AppConfig
import config.featureSwitches.FeatureSwitching
import connectors.parsers.getPenaltyDetails.HIPPenaltyDetailsParser.{HIPPenaltyDetailsFailureResponse, HIPPenaltyDetailsResponse, HIPPenaltyDetailsSuccessResponse}
import models.hipPenaltyDetails.PenaltyDetails
import models.{AgnosticEnrolmentKey, Id, IdType, Regime}
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpResponse,
  UpstreamErrorResponse
}
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class HIPPenaltyDetailsConnectorSpec
    extends SpecBase
    with LogCapturing
    with FeatureSwitching {
  override implicit val config: Configuration =
    injector.instanceOf[Configuration]

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])
  implicit val mockConfiguration: Configuration = mock(classOf[Configuration])
  val instant = Instant.now()

  val regime = Regime("VATC")
  val idType = IdType("VRN")
  val id = Id("123456789")

  val vrn123456789: AgnosticEnrolmentKey = AgnosticEnrolmentKey(
    regime,
    idType,
    id
  )

  class Setup {
    reset(mockHttpClient)
    reset(mockAppConfig)
    reset(mockConfiguration)

    val connector = new HIPPenaltyDetailsConnector(mockHttpClient, mockAppConfig)(
      implicitly,
      mockConfiguration
    )

    when(
      mockHttpClient.GET[HttpResponse](
        ArgumentMatchers.eq(
          "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789&dateLimit=09"
        ),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )(
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )
    ).thenReturn(
      Future.successful(
        HttpResponse.apply(
          status = Status.OK,
          json = Json.toJson(mockPenaltyDetailsModelAPI1812),
          headers = Map.empty
        )
      )
    )

    when(mockAppConfig.getHIPPenaltyDetailsUrl(vrn123456789, None))
      .thenReturn(
        "http://localhost:1234/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
      )
    when(mockAppConfig.getHIPPenaltyDetailsUrl(vrn123456789))
      .thenReturn(
        "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
      )

    when(mockAppConfig.getHIPPenaltyDetailsUrl(vrn123456789, None))
      .thenReturn(
        "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
      )

    when(
      mockAppConfig.getHIPPenaltyDetailsUrl(vrn123456789, Some("09"))
    )
      .thenReturn(
        "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789&dateLimit=09"
      )

    when(mockAppConfig.hipAuthorisationToken).thenReturn("encodedToken")
    when(mockAppConfig.hipServiceOriginatorIdKeyV1).thenReturn("OriginatorId")
    when(mockAppConfig.hipServiceOriginatorIdV1).thenReturn("ServiceXYZ")
    when(mockAppConfig.hipEnvironment).thenReturn("env")

    when(
      mockConfiguration.getOptional[String](
        ArgumentMatchers.eq("feature.switch.time-machine-now")
      )(ArgumentMatchers.any())
    )
      .thenReturn(None)
    sys.props -= TIME_MACHINE_NOW
  }

  val mockPenaltyDetailsModelAPI1812: PenaltyDetails = PenaltyDetails(
    instant,
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = None,
    breathingSpace = None
  )

  "getPenaltiesDetails" should {
    "return a 200 when the call succeeds" in new Setup {
      when(
        mockHttpClient.GET[HIPPenaltyDetailsResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            Right(HIPPenaltyDetailsSuccessResponse(mockPenaltyDetailsModelAPI1812))
          )
        )

      val result: HIPPenaltyDetailsResponse =
        await(connector.getPenaltyDetails(vrn123456789))
      result.isRight shouldBe true
    }

    "send all required headers for the penalty details request" in new Setup {
      val headersCaptor =
        ArgumentCaptor.forClass(classOf[Seq[(String, String)]])

      when(mockAppConfig.hipAuthorisationToken).thenReturn("encodedToken")
      when(mockAppConfig.hipServiceOriginatorIdKeyV1).thenReturn("OriginatorId")
      when(mockAppConfig.hipServiceOriginatorIdV1).thenReturn("ServiceXYZ")
      when(mockAppConfig.hipEnvironment).thenReturn("env")

      when(
        mockHttpClient.GET[HIPPenaltyDetailsResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          headersCaptor.capture()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            Right(HIPPenaltyDetailsSuccessResponse(mockPenaltyDetailsModelAPI1812))
          )
        )

      await(connector.getPenaltyDetails(vrn123456789))

      val headers = headersCaptor.getValue.toMap

      headers should contain key "Authorization"
      headers("Authorization") shouldBe "Basic encodedToken"

      headers should contain key "correlationid"
      headers("correlationid").length should be >= 36

      headers should contain key "X-Originating-System"
      headers("X-Originating-System") shouldBe "MDTP"

      headers should contain key "X-Receipt-Date"
      noException should be thrownBy Instant.parse(headers("X-Receipt-Date"))

      headers should contain key "X-Transmitting-System"
      headers("X-Transmitting-System") shouldBe "HIP"

      headers should contain key "OriginatorId"
      headers("OriginatorId") shouldBe "ServiceXYZ"

      headers should contain key "Environment"
      headers("Environment") shouldBe "env"
    }

    s"return a 404 when the call fails for Not Found" in new Setup {
      when(
        mockHttpClient.GET[HIPPenaltyDetailsResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            Left(HIPPenaltyDetailsFailureResponse(Status.NOT_FOUND))
          )
        )

      val result: HIPPenaltyDetailsResponse =
        await(connector.getPenaltyDetails(vrn123456789))
      result.isLeft shouldBe true
    }

    s"return a 400 when the call fails for Bad Request" in new Setup {
      when(
        mockHttpClient.GET[HIPPenaltyDetailsResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            Left(HIPPenaltyDetailsFailureResponse(Status.BAD_REQUEST))
          )
        )

      val result: HIPPenaltyDetailsResponse =
        await(connector.getPenaltyDetails(vrn123456789))
      result.isLeft shouldBe true
    }

    s"return a 409 when the call fails for Conflict" in new Setup {
      when(
        mockHttpClient.GET[HIPPenaltyDetailsResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            Left(HIPPenaltyDetailsFailureResponse(Status.CONFLICT))
          )
        )

      val result: HIPPenaltyDetailsResponse =
        await(connector.getPenaltyDetails(vrn123456789))
      result.isLeft shouldBe true
    }

    s"return a 422 when the call fails for Unprocessable Entity" in new Setup {
      when(
        mockHttpClient.GET[HIPPenaltyDetailsResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            Left(HIPPenaltyDetailsFailureResponse(Status.UNPROCESSABLE_ENTITY))
          )
        )

      val result: HIPPenaltyDetailsResponse =
        await(connector.getPenaltyDetails(vrn123456789))
      result.isLeft shouldBe true
    }

    s"return a 500 when the call fails for Internal Server Error" in new Setup {
      when(
        mockHttpClient.GET[HIPPenaltyDetailsResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            Left(HIPPenaltyDetailsFailureResponse(Status.INTERNAL_SERVER_ERROR))
          )
        )

      val result: HIPPenaltyDetailsResponse =
        await(connector.getPenaltyDetails(vrn123456789))
      result.isLeft shouldBe true
    }

    s"return a 503 when the call fails" in new Setup {
      when(
        mockHttpClient.GET[HIPPenaltyDetailsResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            Left(HIPPenaltyDetailsFailureResponse(Status.SERVICE_UNAVAILABLE))
          )
        )

      val result: HIPPenaltyDetailsResponse =
        await(connector.getPenaltyDetails(vrn123456789))
      result.isLeft shouldBe true
    }

    "return a 500 when the call fails due to an UpstreamErrorResponse(5xx) exception" in new Setup {
      when(
        mockHttpClient.GET[HttpResponse](
          ArgumentMatchers.eq(
            s"/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.failed(
            UpstreamErrorResponse.apply("", Status.INTERNAL_SERVER_ERROR)
          )
        )
      withCaptureOfLoggingFrom(logger) { logs =>
        {
          val result: HIPPenaltyDetailsResponse =
            await(connector.getPenaltyDetails(vrn123456789))
          logs.exists(
            _.getMessage.contains(
              PagerDutyKeys.RECEIVED_5XX_FROM_1812_API.toString
            )
          ) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }

    "return a 400 when the call fails due to an UpstreamErrorResponse(4xx) exception" in new Setup {
      when(
        mockHttpClient.GET[HttpResponse](
          ArgumentMatchers.eq(
            s"/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.failed(UpstreamErrorResponse.apply("", Status.BAD_REQUEST))
        )
      withCaptureOfLoggingFrom(logger) { logs =>
        {
          val result: HIPPenaltyDetailsResponse =
            await(connector.getPenaltyDetails(vrn123456789))
          logs.exists(
            _.getMessage.contains(
              PagerDutyKeys.RECEIVED_4XX_FROM_1812_API.toString
            )
          ) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }

    "return a 500 when the call fails due to an unexpected exception" in new Setup {
      when(
        mockHttpClient.GET[HttpResponse](
          ArgumentMatchers.eq(
            s"/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(Future.failed(new Exception("Something weird happened")))
      withCaptureOfLoggingFrom(logger) { logs =>
        {
          val result: HIPPenaltyDetailsResponse =
            await(connector.getPenaltyDetails(vrn123456789)(HeaderCarrier()))
          logs.exists(
            _.getMessage.contains(
              PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1812_API.toString
            )
          ) shouldBe true
          result.isLeft shouldBe true
        }
      }
    }
  }

  "getPenaltyDetailsForAPI" should {
    val queryParam = "?dateLimit=09"

    "return a 200 when the call succeeds" in new Setup {
      when(
        mockHttpClient.GET[HttpResponse](
          ArgumentMatchers.eq(
            s"/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789$queryParam"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            HttpResponse.apply(
              status = Status.OK,
              json = Json.toJson(mockPenaltyDetailsModelAPI1812),
              headers = Map.empty
            )
          )
        )

      val result: HttpResponse = await(
        connector.getPenaltyDetailsForAPI(vrn123456789, dateLimit = Some("09"))(
          HeaderCarrier()
        )
      )
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe Json.toJson(
        mockPenaltyDetailsModelAPI1812
      )
    }

    "return a 200 when the call succeeds - with only vrn" in new Setup {
      when(
        mockHttpClient.GET[HttpResponse](
          ArgumentMatchers.eq(
            s"/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(
          Future.successful(
            HttpResponse.apply(
              status = Status.OK,
              json = Json.toJson(mockPenaltyDetailsModelAPI1812),
              headers = Map.empty
            )
          )
        )

      val result: HttpResponse = await(
        connector.getPenaltyDetailsForAPI(vrn123456789, dateLimit = None)(
          HeaderCarrier()
        )
      )
      result.status shouldBe Status.OK
      Json.parse(result.body) shouldBe Json.toJson(
        mockPenaltyDetailsModelAPI1812
      )
    }

    s"return a 403 when the call fails for Not Found (for 4xx errors)" in new Setup {
      when(
        mockHttpClient.GET[HttpResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789&dateLimit=09"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      ).thenReturn(
        Future.failed(
          UpstreamErrorResponse("You shall not pass", Status.FORBIDDEN)
        )
      )

      val result: HttpResponse = await(
        connector.getPenaltyDetailsForAPI(vrn123456789, dateLimit = Some("09"))(
          HeaderCarrier()
        )
      )
      result.status shouldBe Status.FORBIDDEN
    }

    s"return a 500 when the call fails for Internal Server Error (for 5xx errors)" in new Setup {
      when(
        mockHttpClient.GET[HttpResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789&dateLimit=09"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      ).thenReturn(
        Future.failed(
          UpstreamErrorResponse(
            "Internal Server Error",
            Status.INTERNAL_SERVER_ERROR
          )
        )
      )

      val result: HttpResponse = await(
        connector.getPenaltyDetailsForAPI(vrn123456789, dateLimit = Some("09"))(
          HeaderCarrier()
        )
      )
      result.status shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return a 500 when the call fails due to an unexpected exception" in new Setup {
      when(
        mockHttpClient.GET[HttpResponse](
          ArgumentMatchers.eq(
            "/etmp/RESTAdapter/cross-regime/taxpayer/penalties?taxRegime=VATC&idType=VRN&idNumber=123456789&dateLimit=09"
          ),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(Future.failed(new Exception("Something weird happened")))

      withCaptureOfLoggingFrom(logger) { logs =>
        val result: HttpResponse = await(
          connector.getPenaltyDetailsForAPI(
            vrn123456789,
            dateLimit = Some("09")
          )(HeaderCarrier())
        )

        result.status shouldBe Status.INTERNAL_SERVER_ERROR

        logs.exists(log =>
          log.getMessage.contains(
            PagerDutyKeys.UNKNOWN_EXCEPTION_CALLING_1812_API.toString
          )
        ) shouldBe true
      }
    }

  }
}