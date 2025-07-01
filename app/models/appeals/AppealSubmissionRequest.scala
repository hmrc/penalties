/*
 * Copyright 2025 HM Revenue & Customs
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

package models.appeals

import play.api.libs.json.{JsObject, JsValue, Json, Writes}

import java.time.{LocalDateTime, ZoneOffset}

case class AppealSubmissionRequest(taxRegime: String,
                                   appealLevel: AppealLevel,
                                   penaltyId: String,
                                   dateOfAppeal: LocalDateTime,
                                   isLPP: Boolean,
                                   appealSubmittedBy: String,
                                   appealInformation: AppealInformation,
                                   agentDetails: Option[AgentDetails]) {

  val sourceSystem: String = "MDTP"
}

object AppealSubmissionRequest {

  def apply(appealSubmission: AppealSubmission, penaltyId: String): AppealSubmissionRequest =
    AppealSubmissionRequest(
      taxRegime = appealSubmission.taxRegime,
      appealLevel = appealSubmission.appealLevel,
      penaltyId = penaltyId,
      dateOfAppeal = appealSubmission.dateOfAppeal,
      isLPP = appealSubmission.isLPP,
      appealSubmittedBy = appealSubmission.appealSubmittedBy,
      appealInformation = appealSubmission.appealInformation,
      agentDetails = appealSubmission.agentDetails
    )

  def parseAppealInformationToJson(payload: AppealInformation): JsValue = {
    val value = payload.reasonableExcuse match {
      case "bereavement" =>
        Json.toJson(payload.asInstanceOf[BereavementAppealInformation])(BereavementAppealInformation.bereavementAppealWrites)
      case "crime" =>
        Json.toJson(payload.asInstanceOf[CrimeAppealInformation])(CrimeAppealInformation.crimeAppealWrites)
      case "fireandflood" =>
        Json.toJson(payload.asInstanceOf[FireOrFloodAppealInformation])(FireOrFloodAppealInformation.fireOrFloodAppealWrites)
      case "lossOfEssentialStaff" =>
        Json.toJson(payload.asInstanceOf[LossOfStaffAppealInformation])(LossOfStaffAppealInformation.lossOfStaffAppealWrites)
      case "technicalIssue" =>
        Json.toJson(payload.asInstanceOf[TechnicalIssuesAppealInformation])(TechnicalIssuesAppealInformation.technicalIssuesAppealWrites)
      case "health" =>
        Json.toJson(payload.asInstanceOf[HealthAppealInformation])(HealthAppealInformation.healthAppealWrites)
      case "other" =>
        Json.toJson(payload.asInstanceOf[OtherAppealInformation])(OtherAppealInformation.otherAppealInformationWrites)
    }
    value.asOpt[JsObject] match { // TODO: remove once the HIP migration is done
      case Some(appealInformation) => appealInformation - "honestyDeclaration"
    }
  }

  implicit val apiWrites: Writes[AppealSubmissionRequest] = (appealSubmission: AppealSubmissionRequest) => {
    val dateOfAppealZoned: String = appealSubmission.dateOfAppeal.toInstant(ZoneOffset.UTC).toString
    Json
      .obj(
        "sourceSystem"      -> appealSubmission.sourceSystem,
        "penaltyId"         -> appealSubmission.penaltyId,
        "appealLevel"       -> appealSubmission.appealLevel,
        "sourceSystem"      -> appealSubmission.sourceSystem,
        "taxRegime"         -> appealSubmission.taxRegime,
        "dateOfAppeal"      -> dateOfAppealZoned,
        "isLPP"             -> appealSubmission.isLPP,
        "appealSubmittedBy" -> appealSubmission.appealSubmittedBy,
        "appealInformation" -> parseAppealInformationToJson(appealSubmission.appealInformation)
      )
      .deepMerge(
        appealSubmission.agentDetails.fold(Json.obj())(agentDetails => Json.obj("agentDetails" -> agentDetails))
      )
  }
}
