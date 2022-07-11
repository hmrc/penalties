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

package services

import connectors.PEGAConnector
import connectors.parsers.AppealsParser
import models.appeals.{AppealResponseModel, AppealSubmission}
import utils.Logger.logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ETMPService @Inject()(appealsConnector: PEGAConnector)
                           (implicit ec: ExecutionContext) {

  def submitAppeal(appealSubmission: AppealSubmission,
                   enrolmentKey: String, isLPP: Boolean, penaltyNumber: String, correlationId: String): Future[Either[AppealsParser.ErrorResponse, AppealResponseModel]]= {

    appealsConnector.submitAppeal(appealSubmission, enrolmentKey, isLPP, penaltyNumber, correlationId).flatMap {
      _.fold(
        error => {
          logger.error(s"[ETMPService][submitAppeal] - Submit appeal call failed with error: ${error.body} and status: ${error.status}")
          Future(Left(error))
        },
        responseModel => {
          logger.debug(s"[ETMPService][submitAppeal] - Retrieving response model for penalty: $penaltyNumber")
          Future(Right(responseModel))
        }
      )
    }
  }
}
