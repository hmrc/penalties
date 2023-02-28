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

import config.featureSwitches.FeatureSwitching
import connectors.ComplianceConnector
import connectors.parsers.ComplianceParser
import models.compliance.CompliancePayload
import play.api.Configuration
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ComplianceService @Inject()(complianceConnector: ComplianceConnector)(implicit val config: Configuration) extends FeatureSwitching {

  def getComplianceData(vrn: String, startDate: String, endDate: String)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Int, CompliancePayload]] = {
    complianceConnector.getComplianceData(vrn, startDate, endDate).map {
      _.fold[Either[Int, CompliancePayload]]({
        failureModel =>
          logger.error(s"[ComplianceService][getComplianceData] - Received error back from DES for compliance data for VRN: $vrn with error: ${failureModel.message}")
          failureModel match {
            case ComplianceParser.CompliancePayloadFailureResponse(status) => Left(status)
            case ComplianceParser.CompliancePayloadNoData => Left(NOT_FOUND)
            case ComplianceParser.CompliancePayloadMalformed => Left(INTERNAL_SERVER_ERROR)
          }
      },
        complianceData => {
          logger.debug(s"[ComplianceService][getComplianceData] - Received model: ${complianceData.model} from connector for compliance data")
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
