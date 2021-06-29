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

package controllers

import config.AppConfig
import connectors.parsers.ETMPPayloadParser.GetETMPPayloadNoContent
import models.ETMPPayload
import models.appeals.{AppealData, AppealSubmission, AppealTypeEnum}
import models.appeals.AppealTypeEnum._
import models.appeals.reasonableExcuses.ReasonableExcuse
import utils.Logger.logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.ETMPService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AppealsController @Inject()(appConfig: AppConfig,
                                  etmpService: ETMPService,
                                  cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def getAppealsDataForLateSubmissionPenalty(penaltyId: String, enrolmentKey: String): Action[AnyContent] = Action.async {
    implicit request => {
      etmpService.getPenaltyDataFromETMPForEnrolment(enrolmentKey).map {
        result => {
          result._1.fold {
            result._2 match {
              case Left(GetETMPPayloadNoContent) => NotFound(s"Could not retrieve ETMP penalty data for $enrolmentKey")
              case Left(_) => InternalServerError("Something went wrong.")
            }
          }(
            etmpData => {
              checkAndReturnResponseForPenaltyData(etmpData, penaltyId, enrolmentKey, Late_Submission)
            }
          )
        }
      }
    }
  }

  private def checkAndReturnResponseForPenaltyData(etmpData: ETMPPayload,
                                                   penaltyIdToCheck: String,
                                                   enrolmentKey: String,
                                                   appealType: AppealTypeEnum.Value): Result = {
    val isPenaltyIdInETMPPayload: Boolean = etmpData.penaltyPoints.exists(_.id == penaltyIdToCheck)
    if (isPenaltyIdInETMPPayload) {
      logger.debug(s"[AppealsController][getAppealsData] Penalty ID: $penaltyIdToCheck for enrolment key: $enrolmentKey found in ETMP.")
      val penaltyBasedOnId = etmpData.penaltyPoints.find(_.id == penaltyIdToCheck).get
      val dataToReturn: AppealData = AppealData(`type` = appealType,
        startDate = penaltyBasedOnId.period.get.startDate,
        endDate = penaltyBasedOnId.period.get.endDate,
        dueDate = penaltyBasedOnId.period.get.submission.dueDate,
        dateCommunicationSent = penaltyBasedOnId.communications.head.dateSent
      )
      Ok(Json.toJson(dataToReturn))
    } else {
      logger.info("[AppealsController][getAppealsData] Data retrieved for enrolment but provided penalty ID was not found. Returning 404.")
      NotFound("Penalty ID was not found in users penalties.")
    }
  }

  def getReasonableExcuses: Action[AnyContent] = Action {
    Ok(ReasonableExcuse.allExcusesToJson(appConfig))
  }

  def submitAppeal(enrolmentKey: String): Action[AnyContent] = Action.async {
    implicit request => {
      request.body.asJson.fold({
        logger.error("[AppealsController][submitAppeal] Failed to validate request body as JSON")
        Future(BadRequest("Invalid body received i.e. could not be parsed to JSON"))
      })(
        jsonBody => {
          val parseResultToModel = Json.fromJson(jsonBody)(AppealSubmission.apiReads)
          parseResultToModel.fold(
            failure => {
              logger.error("[AppealsController][submitAppeal] Fail to parse request body to model")
              logger.debug(s"[AppealsController][submitAppeal] Parse failure(s): $failure")
              Future(BadRequest("Failed to parse to model"))
            },
            appealSubmission => {
              etmpService.submitAppeal(appealSubmission, enrolmentKey).map {
                response =>
                  response.status match {
                    case OK => {
                      Ok("")
                    }
                    case _ => {
                      logger.error(s"[AppealsController][submitAppeal] Connector returned unknown status code: ${response.status} ")
                      logger.debug(s"[AppealsController][submitAppeal] Failure response body: ${response.body}")
                      InternalServerError("Something went wrong.")
                    }
                  }
              } recover {
                case e => {
                  logger.error(s"[AppealsController][submitAppeal] Unknown exception occurred with message: ${e.getMessage}")
                  InternalServerError("Something went wrong.")
                }
              }
            }
          )
        }
      )
    }
  }
}
