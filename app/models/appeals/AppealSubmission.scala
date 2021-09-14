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

package models.appeals

import play.api.libs.json._

sealed trait AppealInformation {
  val `type`: String
  val statement: Option[String]
}

case class BereavementAppealInformation(
                                         `type`: String,
                                          dateOfEvent: String,
                                          statement: Option[String],
                                          lateAppeal: Boolean,
                                          lateAppealReason: Option[String],
                                          whoPlannedToSubmit: Option[String],
                                          causeOfLateSubmissionAgent: Option[String]
                                       ) extends AppealInformation
object BereavementAppealInformation {
  implicit val bereavementAppealInformationFormatter: OFormat[BereavementAppealInformation] = Json.format[BereavementAppealInformation]

  val bereavementAppealWrites: Writes[BereavementAppealInformation] = (bereavementAppealInformation: BereavementAppealInformation) => {
    Json.obj(
      "type" -> bereavementAppealInformation.`type`,
      "dateOfEvent" -> bereavementAppealInformation.dateOfEvent,
      "lateAppeal" -> bereavementAppealInformation.lateAppeal
    ).deepMerge(
      bereavementAppealInformation.statement.fold(
        Json.obj()
      )(
        statement => Json.obj("statement" -> statement)
      )
    ).deepMerge(
      bereavementAppealInformation.lateAppealReason.fold(
        Json.obj()
      )(
        lateAppealReason => Json.obj("lateAppealReason" -> lateAppealReason)
      )
    ).deepMerge(
      bereavementAppealInformation.whoPlannedToSubmit.fold(
        Json.obj()
      )(
        whoPlannedToSubmit => Json.obj("whoPlannedToSubmit" -> whoPlannedToSubmit)
      )
    ).deepMerge(
      bereavementAppealInformation.causeOfLateSubmissionAgent.fold(
        Json.obj()
      )(
        causeOfLateSubmissionAgent => Json.obj("causeOfLateSubmissionAgent" -> causeOfLateSubmissionAgent)
      )
    )
  }
}

case class CrimeAppealInformation(
                                   `type`: String,
                                   dateOfEvent: String,
                                   reportedIssue: Boolean,
                                   statement: Option[String],
                                   lateAppeal: Boolean,
                                   lateAppealReason: Option[String],
                                   whoPlannedToSubmit: Option[String],
                                   causeOfLateSubmissionAgent: Option[String]
                                 ) extends AppealInformation

object CrimeAppealInformation {
  implicit val crimeAppealInformationFormatter: OFormat[CrimeAppealInformation] = Json.format[CrimeAppealInformation]

  val crimeAppealWrites: Writes[CrimeAppealInformation] = (crimeAppealInformation: CrimeAppealInformation) => {
    Json.obj(
      "type" -> crimeAppealInformation.`type`,
      "dateOfEvent" -> crimeAppealInformation.dateOfEvent,
      "reportedIssue" -> crimeAppealInformation.reportedIssue,
      "lateAppeal" -> crimeAppealInformation.lateAppeal
    ).deepMerge(
      crimeAppealInformation.statement.fold(
        Json.obj()
      )(
        statement => Json.obj("statement" -> statement)
      )
    ).deepMerge(
      crimeAppealInformation.lateAppealReason.fold(
        Json.obj()
      )(
        lateAppealReason => Json.obj("lateAppealReason" -> lateAppealReason)
      )
    ).deepMerge(
      crimeAppealInformation.whoPlannedToSubmit.fold(
        Json.obj()
      )(
        whoPlannedToSubmit => Json.obj("whoPlannedToSubmit" -> whoPlannedToSubmit)
      )
    ).deepMerge(
      crimeAppealInformation.causeOfLateSubmissionAgent.fold(
        Json.obj()
      )(
        causeOfLateSubmissionAgent => Json.obj("causeOfLateSubmissionAgent" -> causeOfLateSubmissionAgent)
      )
    )
  }
}

case class FireOrFloodAppealInformation(
                                         `type`: String,
                                         dateOfEvent: String,
                                         statement: Option[String],
                                         lateAppeal: Boolean,
                                         lateAppealReason: Option[String],
                                         whoPlannedToSubmit: Option[String],
                                         causeOfLateSubmissionAgent: Option[String]
                                       ) extends AppealInformation

object FireOrFloodAppealInformation {
  implicit val fireOrFloodAppealInformationFormatter: OFormat[FireOrFloodAppealInformation] = Json.format[FireOrFloodAppealInformation]

  val fireOrFloodAppealWrites: Writes[FireOrFloodAppealInformation] = (fireOrFloodAppealInformation: FireOrFloodAppealInformation) => {
    Json.obj(
      "type" -> fireOrFloodAppealInformation.`type`,
      "dateOfEvent" -> fireOrFloodAppealInformation.dateOfEvent,
      "lateAppeal" -> fireOrFloodAppealInformation.lateAppeal
    ).deepMerge(
      fireOrFloodAppealInformation.statement.fold(
        Json.obj()
      )(
        statement => Json.obj("statement" -> statement)
      )
    ).deepMerge(
      fireOrFloodAppealInformation.lateAppealReason.fold(
        Json.obj()
      )(
        lateAppealReason => Json.obj("lateAppealReason" -> lateAppealReason)
      )
    ).deepMerge(
      fireOrFloodAppealInformation.whoPlannedToSubmit.fold(
        Json.obj()
      )(
        whoPlannedToSubmit => Json.obj("whoPlannedToSubmit" -> whoPlannedToSubmit)
      )
    ).deepMerge(
      fireOrFloodAppealInformation.causeOfLateSubmissionAgent.fold(
        Json.obj()
      )(
        causeOfLateSubmissionAgent => Json.obj("causeOfLateSubmissionAgent" -> causeOfLateSubmissionAgent)
      )
    )
  }
}

case class LossOfStaffAppealInformation(
                                         `type`: String,
                                         dateOfEvent: String,
                                         statement: Option[String],
                                         lateAppeal: Boolean,
                                         lateAppealReason: Option[String],
                                         whoPlannedToSubmit: Option[String],
                                         causeOfLateSubmissionAgent: Option[String]
                                       ) extends AppealInformation

object LossOfStaffAppealInformation {
  implicit val lossOfStaffAppealInformationFormatter: OFormat[LossOfStaffAppealInformation] = Json.format[LossOfStaffAppealInformation]

  val lossOfStaffAppealWrites: Writes[LossOfStaffAppealInformation] = (lossOfStaffAppealInformation: LossOfStaffAppealInformation) => {
    Json.obj(
      "type" -> lossOfStaffAppealInformation.`type`,
      "dateOfEvent" -> lossOfStaffAppealInformation.dateOfEvent,
      "lateAppeal" -> lossOfStaffAppealInformation.lateAppeal
    ).deepMerge(
      lossOfStaffAppealInformation.statement.fold(
        Json.obj()
      )(
        statement => Json.obj("statement" -> statement)
      )
    ).deepMerge(
      lossOfStaffAppealInformation.lateAppealReason.fold(
        Json.obj()
      )(
        lateAppealReason => Json.obj("lateAppealReason" -> lateAppealReason)
      )
    ).deepMerge(
      lossOfStaffAppealInformation.whoPlannedToSubmit.fold(
        Json.obj()
      )(
        whoPlannedToSubmit => Json.obj("whoPlannedToSubmit" -> whoPlannedToSubmit)
      )
    ).deepMerge(
      lossOfStaffAppealInformation.causeOfLateSubmissionAgent.fold(
        Json.obj()
      )(
        causeOfLateSubmissionAgent => Json.obj("causeOfLateSubmissionAgent" -> causeOfLateSubmissionAgent)
      )
    )
  }
}

case class TechnicalIssuesAppealInformation(
                                             `type`: String,
                                             startDateOfEvent: String,
                                             endDateOfEvent: String,
                                             statement: Option[String],
                                             lateAppeal: Boolean,
                                             lateAppealReason: Option[String],
                                             whoPlannedToSubmit: Option[String],
                                             causeOfLateSubmissionAgent: Option[String]
                                           ) extends AppealInformation

object TechnicalIssuesAppealInformation {
  implicit val technicalIssuesAppealInformationFormatter: OFormat[TechnicalIssuesAppealInformation] = Json.format[TechnicalIssuesAppealInformation]

  val technicalIssuesAppealWrites: Writes[TechnicalIssuesAppealInformation] = (technicalIssuesAppealInformation: TechnicalIssuesAppealInformation) => {
    Json.obj(
      "type" -> technicalIssuesAppealInformation.`type`,
      "startDateOfEvent" -> technicalIssuesAppealInformation.startDateOfEvent,
      "endDateOfEvent" -> technicalIssuesAppealInformation.endDateOfEvent,
      "lateAppeal" -> technicalIssuesAppealInformation.lateAppeal
    ).deepMerge(
      technicalIssuesAppealInformation.statement.fold(
        Json.obj()
      )(
        statement => Json.obj("statement" -> statement)
      )
    ).deepMerge(
      technicalIssuesAppealInformation.lateAppealReason.fold(
        Json.obj()
      )(
        lateAppealReason => Json.obj("lateAppealReason" -> lateAppealReason)
      )
    ).deepMerge(
      technicalIssuesAppealInformation.whoPlannedToSubmit.fold(
        Json.obj()
      )(
        whoPlannedToSubmit => Json.obj("whoPlannedToSubmit" -> whoPlannedToSubmit)
      )
    ).deepMerge(
      technicalIssuesAppealInformation.causeOfLateSubmissionAgent.fold(
        Json.obj()
      )(
        causeOfLateSubmissionAgent => Json.obj("causeOfLateSubmissionAgent" -> causeOfLateSubmissionAgent)
      )
    )
  }
}

case class HealthAppealInformation(
                                    `type`: String,
                                    hospitalStayInvolved: Boolean,
                                    dateOfEvent: Option[String],
                                    startDateOfEvent: Option[String],
                                    endDateOfEvent: Option[String],
                                    eventOngoing: Boolean,
                                    statement: Option[String],
                                    lateAppeal: Boolean,
                                    lateAppealReason: Option[String],
                                    whoPlannedToSubmit: Option[String],
                                    causeOfLateSubmissionAgent: Option[String]
                                  ) extends AppealInformation

object HealthAppealInformation {
  implicit val healthAppealInformationFormatter: OFormat[HealthAppealInformation] = Json.format[HealthAppealInformation]

  val healthAppealWrites: Writes[HealthAppealInformation] = (healthAppealInformation: HealthAppealInformation) => {
    Json.obj(
      "type" -> healthAppealInformation.`type`,
      "hospitalStayInvolved" -> healthAppealInformation.hospitalStayInvolved,
      "eventOngoing" -> healthAppealInformation.eventOngoing,
      "lateAppeal" -> healthAppealInformation.lateAppeal
    ).deepMerge(
      healthAppealInformation.statement.fold(
        Json.obj()
      )(
        statement => Json.obj("statement" -> statement)
      )
    ).deepMerge(
      healthAppealInformation.lateAppealReason.fold(
        Json.obj()
      )(
        lateAppealReason => Json.obj("lateAppealReason" -> lateAppealReason)
      )
    ).deepMerge(
      (healthAppealInformation.hospitalStayInvolved, healthAppealInformation.eventOngoing) match {
        case (true, true) =>
          Json.obj(
            "startDateOfEvent" -> healthAppealInformation.startDateOfEvent.get
          )
        case (true, false) =>
          Json.obj(
            "startDateOfEvent" -> healthAppealInformation.startDateOfEvent.get,
            "endDateOfEvent" -> healthAppealInformation.endDateOfEvent.get
          )
        case _ =>
          Json.obj(
            "dateOfEvent" -> healthAppealInformation.dateOfEvent.get
          )
      }
    ).deepMerge(
      healthAppealInformation.whoPlannedToSubmit.fold(
        Json.obj()
      )(
        whoPlannedToSubmit => Json.obj("whoPlannedToSubmit" -> whoPlannedToSubmit)
      )
    ).deepMerge(
      healthAppealInformation.causeOfLateSubmissionAgent.fold(
        Json.obj()
      )(
        causeOfLateSubmissionAgent => Json.obj("causeOfLateSubmissionAgent" -> causeOfLateSubmissionAgent)
      )
    )
  }
}

case class OtherAppealInformation(
                                   `type`: String,
                                   dateOfEvent: String,
                                   statement: Option[String],
                                   supportingEvidence: Option[Evidence],
                                   lateAppeal: Boolean,
                                   lateAppealReason: Option[String],
                                   whoPlannedToSubmit: Option[String],
                                   causeOfLateSubmissionAgent: Option[String]
                                 ) extends AppealInformation

object OtherAppealInformation {
  implicit val evidenceFormatter: OFormat[Evidence] = Evidence.format
  implicit val otherAppealInformationFormatter: OFormat[OtherAppealInformation] = Json.format[OtherAppealInformation]

  val otherAppealInformationWrites: Writes[OtherAppealInformation] = (otherAppealInformation: OtherAppealInformation) => {
    Json.obj(
      "type" -> otherAppealInformation.`type`,
      "dateOfEvent" -> otherAppealInformation.dateOfEvent,
      "statement" -> otherAppealInformation.statement.get,
      "lateAppeal" -> otherAppealInformation.lateAppeal
    ).deepMerge(
      otherAppealInformation.lateAppealReason.fold(
        Json.obj()
      )(
        lateAppealReason => Json.obj("lateAppealReason" -> lateAppealReason)
      )
    ).deepMerge(
      otherAppealInformation.supportingEvidence.fold(
        Json.obj()
      )(
        evidence => Json.obj("supportingEvidence" -> evidence)
      )
    ).deepMerge(
      otherAppealInformation.whoPlannedToSubmit.fold(
        Json.obj()
      )(
        whoPlannedToSubmit => Json.obj("whoPlannedToSubmit" -> whoPlannedToSubmit)
      )
    ).deepMerge(
      otherAppealInformation.causeOfLateSubmissionAgent.fold(
        Json.obj()
      )(
        causeOfLateSubmissionAgent => Json.obj("causeOfLateSubmissionAgent" -> causeOfLateSubmissionAgent)
      )
    )
  }
}

case class ObligationAppealInformation(
                                        `type`: String,
                                        statement: Option[String],
                                        supportingEvidence: Option[Evidence]
                                      ) extends AppealInformation

object ObligationAppealInformation{
  implicit val evidenceFormatter: OFormat[Evidence] = Evidence.format
  implicit val obligationAppealInformationFormatter: OFormat[ObligationAppealInformation] = Json.format[ObligationAppealInformation]

  val obligationAppealInformationWrites: Writes[ObligationAppealInformation] = (obligationAppealInformation: ObligationAppealInformation) => {
    Json.obj(
      "type" -> obligationAppealInformation.`type`,
      "statement" -> obligationAppealInformation.statement.get
    ).deepMerge(
      obligationAppealInformation.supportingEvidence.fold(
        Json.obj()
      )(
        supportingEvidence => Json.obj("supportingEvidence" -> supportingEvidence)
      )
    )
  }
}

case class AppealSubmission(
                             submittedBy: String,
                             penaltyId: String,
                             reasonableExcuse: String,
                             honestyDeclaration: Boolean,
                             appealInformation: AppealInformation
                           )

object AppealSubmission {
  def parseAppealInformationFromJson(reason: String, payload: JsValue): JsResult[AppealInformation] = {
    reason match {
      case "bereavement" =>
        Json.fromJson(payload)(BereavementAppealInformation.bereavementAppealInformationFormatter)
      case "crime" =>
        Json.fromJson(payload)(CrimeAppealInformation.crimeAppealInformationFormatter)
      case "fireOrFlood" =>
        Json.fromJson(payload)(FireOrFloodAppealInformation.fireOrFloodAppealInformationFormatter)
      case "lossOfStaff" =>
        Json.fromJson(payload)(LossOfStaffAppealInformation.lossOfStaffAppealInformationFormatter)
      case "technicalIssues" =>
        Json.fromJson(payload)(TechnicalIssuesAppealInformation.technicalIssuesAppealInformationFormatter)
      case "health" =>
        Json.fromJson(payload)(HealthAppealInformation.healthAppealInformationFormatter)
      case "other" =>
        Json.fromJson(payload)(OtherAppealInformation.otherAppealInformationFormatter)
      case "obligation" =>
        Json.fromJson(payload)(ObligationAppealInformation.obligationAppealInformationFormatter)
    }
  }

  def parseAppealInformationToJson(payload: AppealInformation): JsValue = {
    payload.`type` match {
      case "bereavement" =>
        Json.toJson(payload.asInstanceOf[BereavementAppealInformation])(BereavementAppealInformation.bereavementAppealWrites)
      case "crime" =>
        Json.toJson(payload.asInstanceOf[CrimeAppealInformation])(CrimeAppealInformation.crimeAppealWrites)
      case "fireOrFlood" =>
        Json.toJson(payload.asInstanceOf[FireOrFloodAppealInformation])(FireOrFloodAppealInformation.fireOrFloodAppealWrites)
      case "lossOfStaff" =>
        Json.toJson(payload.asInstanceOf[LossOfStaffAppealInformation])(LossOfStaffAppealInformation.lossOfStaffAppealWrites)
      case "technicalIssues" =>
        Json.toJson(payload.asInstanceOf[TechnicalIssuesAppealInformation])(TechnicalIssuesAppealInformation.technicalIssuesAppealWrites)
      case "health" =>
        Json.toJson(payload.asInstanceOf[HealthAppealInformation])(HealthAppealInformation.healthAppealWrites)
      case "other" =>
        Json.toJson(payload.asInstanceOf[OtherAppealInformation])(OtherAppealInformation.otherAppealInformationWrites)
      case "obligation" =>
        Json.toJson(payload.asInstanceOf[ObligationAppealInformation])(ObligationAppealInformation.obligationAppealInformationWrites)
    }
  }

  val apiReads: Reads[AppealSubmission] = (json: JsValue) => {
    for {
      submittedBy <- (json \ "submittedBy").validate[String]
      penaltyId <- (json \ "penaltyId").validate[String]
      reasonableExcuse <- (json \ "reasonableExcuse").validate[String]
      honestyDeclaration <- (json \ "honestyDeclaration").validate[Boolean]
      appealInformationType <- (json \ "appealInformation" \ "type").validate[String]
      appealInformation <- parseAppealInformationFromJson(appealInformationType, (json \ "appealInformation").get)
    } yield {
      AppealSubmission(
        submittedBy,
        penaltyId,
        reasonableExcuse,
        honestyDeclaration,
        appealInformation
      )
    }
  }

  val apiWrites: Writes[AppealSubmission] = (appealSubmission: AppealSubmission) => {
    Json.obj(
      "submittedBy" -> appealSubmission.submittedBy,
      "penaltyId" -> appealSubmission.penaltyId,
      "reasonableExcuse" -> appealSubmission.reasonableExcuse,
      "honestyDeclaration" -> appealSubmission.honestyDeclaration,
      "appealInformation" -> parseAppealInformationToJson(appealSubmission.appealInformation)
    )
  }
}
