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

import connectors.ComplianceConnector
import connectors.parsers.ComplianceParser._
import connectors.parsers.DESComplianceParser
import featureSwitches.FeatureSwitching
import models.compliance.{CompliancePayload, CompliancePayloadObligationAPI}
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger
import utils.{DateHelper, RegimeHelper}

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ComplianceService @Inject()(complianceConnector: ComplianceConnector,
                                  dateHelper: DateHelper) extends FeatureSwitching {

  def getComplianceDataForEnrolmentKey(enrolmentKey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CompliancePayload]] = {
    implicit val startOfLogMsg: String = "[ComplianceService][getComplianceDataForEnrolmentKey]"
    val regime: String = RegimeHelper.getRegimeFromEnrolmentKey(enrolmentKey)
    val identifierFromEnrolmentKey: String = RegimeHelper.getIdentifierFromEnrolmentKey(enrolmentKey)
    val startDate: LocalDateTime = RegimeHelper.getDateTimeBasedOnRegimeFromEnrolmentKey(enrolmentKey, dateHelper)
    val endDate: LocalDateTime = dateHelper.dateTimeNow()
    for {
      pastReturns <- getComplianceHistory(identifierFromEnrolmentKey, startDate, endDate, regime)
      complianceSummary <- getComplianceSummary(identifierFromEnrolmentKey, regime)
    } yield {
      pastReturns.flatMap(
        previousReturns => {
          complianceSummary.flatMap(
            futureReturns => {
              Json.fromJson(previousReturns.as[JsObject].deepMerge(futureReturns.as[JsObject]))(CompliancePayload.format).fold(
                failure => {
                  logger.debug(s"$startOfLogMsg - Failed to merge 2 responses with error: $failure")
                  None
                },
                complianceData => Some(complianceData)
              )
            }
          )
        }
      )
    }
  }

  private def connectorFailureHandler(failure: GetCompliancePayloadFailure)(implicit startOfLogMsg: String, callType: String): Option[JsValue] = {
    failure match {
      case _@GetCompliancePayloadFailureResponse(status) =>
        logger.error(s"$startOfLogMsg - Received status $status back from ETMP for $callType data")
        None
      case _@GetCompliancePayloadMalformed =>
        logger.error(s"$startOfLogMsg - Received malformed JSON back from ETMP for $callType data")
        None
      case _@GetCompliancePayloadNoContent =>
        logger.warn(s"$startOfLogMsg - Received no content back from ETMP for $callType data")
        None
    }
  }

  def getComplianceHistory(identifier: String, startDate: LocalDateTime, endDate: LocalDateTime, regime: String)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext, startOfLogMsg: String): Future[Option[JsValue]] = {
    complianceConnector.getPastReturnsForEnrolmentKey(identifier, startDate, endDate, regime).map {
      _.fold({
        connectorFailureHandler(_)(implicitly, "previous compliance")
      },
        pastReturns => {
          logger.debug(s"$startOfLogMsg - Received JSON: ${pastReturns.jsValue} from connector for past returns")
          Some(pastReturns.jsValue)
        }
      )
    }
  }

  def getComplianceSummary(identifier: String, regime: String)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext, startOfLogMsg: String): Future[Option[JsValue]] = {
    complianceConnector.getComplianceSummaryForEnrolmentKey(identifier, regime).map {
      _.fold[Option[JsValue]]({
        connectorFailureHandler(_)(implicitly, "compliance summary")
      },
        complianceSummary => {
          logger.debug(s"$startOfLogMsg - Received JSON: ${complianceSummary.jsValue} from connector for summary data")
          Some(complianceSummary.jsValue)
        }
      )
    }
  }

  def getComplianceDataFromDES(vrn: String, startDate: String, endDate: String)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Int, CompliancePayloadObligationAPI]] = {
    complianceConnector.getComplianceDataFromDES(vrn, startDate, endDate).map {
      _.fold[Either[Int, CompliancePayloadObligationAPI]]({
        failureModel =>
          logger.error(s"[ComplianceService][getComplianceDataFromDES] - Received error back from DES for compliance data")
          failureModel match {
            case DESComplianceParser.DESCompliancePayloadFailureResponse(status) => Left(status)
            case DESComplianceParser.DESCompliancePayloadNoData => Left(NOT_FOUND)
            case DESComplianceParser.DESCompliancePayloadMalformed => Left(INTERNAL_SERVER_ERROR)
          }
      },
        complianceData => {
          logger.debug(s"[ComplianceService][getComplianceDataFromDES] - Received model: ${complianceData.model} from connector for compliance data")
          Right(complianceData.model)
        }
      )
    }
  }
}
