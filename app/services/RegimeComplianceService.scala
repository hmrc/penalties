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

import config.featureSwitches.FeatureSwitching
import connectors. RegimeComplianceConnector
import connectors.parsers.ComplianceParser

import models.compliance.CompliancePayload
import play.api.Configuration
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger
import models.AgnosticEnrolmentKey
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegimeComplianceService @Inject()(complianceConnector: RegimeComplianceConnector)(implicit val config: Configuration) extends FeatureSwitching {

  def getComplianceData(enrolmentKey: AgnosticEnrolmentKey, startDate: String, endDate: String)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Int, CompliancePayload]] = {
    complianceConnector.getComplianceData(enrolmentKey, startDate, endDate).map {
      _.fold[Either[Int, CompliancePayload]]({
        failureModel =>
          logger.error(s"[RegimeComplianceService][getComplianceData] - Received error back from DES for compliance data for ${enrolmentKey} with error: ${failureModel.message}")
          failureModel match {
            case ComplianceParser.CompliancePayloadFailureResponse(status) => Left(status)
            case ComplianceParser.CompliancePayloadNoData => Left(NOT_FOUND)
            case ComplianceParser.CompliancePayloadMalformed => Left(INTERNAL_SERVER_ERROR)
          }
      },
        complianceData => {
          logger.debug(s"[RegimeComplianceService][getComplianceData] - Received model: ${complianceData.model} from connector for compliance data")
          val orderedModel = complianceData.model.copy(
            obligationDetails = complianceData.model.obligationDetails.sortWith((d1, d2) =>
              d1.inboundCorrespondenceDueDate.isBefore(d2.inboundCorrespondenceDueDate)
            )
          )
          Right(orderedModel)
        }
      )
    }
  }
}
