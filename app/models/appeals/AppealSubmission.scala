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
  val lateAppeal: Boolean
}

case class CrimeAppealInformation(
                                   `type`: String,
                                   dateOfEvent: String,
                                   reportedIssue: Boolean,
                                   statement: Option[String],
                                   lateAppeal: Boolean,
                                   lateAppealReason: Option[String]
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
    )
  }
}

case class FireOrFloodAppealInformation(
                                         `type`: String,
                                         dateOfEvent: String,
                                         statement: Option[String],
                                         lateAppeal: Boolean,
                                         lateAppealReason: Option[String]
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
    )
  }
}

case class LossOfStaffAppealInformation(
                                         `type`: String,
                                         dateOfEvent: String,
                                         statement: Option[String],
                                         lateAppeal: Boolean,
                                         lateAppealReason: Option[String]
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
    )
  }
}

case class TechnicalIssuesAppealInformation(
                                             `type`: String,
                                             startDateOfEvent: String,
                                             endDateOfEvent: String,
                                             statement: Option[String],
                                             lateAppeal: Boolean,
                                             lateAppealReason: Option[String]
                                           ) extends AppealInformation

object TechnicalIssuesAppealInformation {
  implicit val technicalIssuesAppealInformationFormatter: OFormat[TechnicalIssuesAppealInformation] = Json.format[TechnicalIssuesAppealInformation]

  val technicalIssuesAppealWrites: Writes[TechnicalIssuesAppealInformation] = (lossOfStaffAppealInformation: TechnicalIssuesAppealInformation) => {
    Json.obj(
      "type" -> lossOfStaffAppealInformation.`type`,
      "startDateOfEvent" -> lossOfStaffAppealInformation.startDateOfEvent,
      "endDateOfEvent" -> lossOfStaffAppealInformation.endDateOfEvent,
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
                                    lateAppealReason: Option[String]
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
        case (true, true) => {
          Json.obj(
            "startDateOfEvent" -> healthAppealInformation.startDateOfEvent.get
          )
        }
        case (true, false) => {
          Json.obj(
            "startDateOfEvent" -> healthAppealInformation.startDateOfEvent.get,
            "endDateOfEvent" -> healthAppealInformation.endDateOfEvent.get
          )
        }
        case _ => {
          Json.obj(
            "dateOfEvent" -> healthAppealInformation.dateOfEvent.get
          )
        }
      }
    )
  }
}

case class OtherAppealInformation(
                                   `type`: String,
                                   dateOfEvent: String,
                                   statement: Option[String],
                                   supportingEvidence: Evidence,
                                   lateAppeal: Boolean,
                                   lateAppealReason: Option[String]
                                 ) extends AppealInformation

object OtherAppealInformation {
  implicit val evidenceFormatter: OFormat[Evidence] = Evidence.format
  implicit val otherAppealInformationFormatter: OFormat[OtherAppealInformation] = Json.format[OtherAppealInformation]

  val otherAppealInformationWrites: Writes[OtherAppealInformation] = (otherAppealInformation: OtherAppealInformation) => {
    Json.obj(
      "type" -> otherAppealInformation.`type`,
      "dateOfEvent" -> otherAppealInformation.dateOfEvent,
      "statement" -> otherAppealInformation.statement.get,
      "supportingEvidence" -> Json.toJson(otherAppealInformation.supportingEvidence),
      "lateAppeal" -> otherAppealInformation.lateAppeal
    ).deepMerge(
      otherAppealInformation.lateAppealReason.fold(
        Json.obj()
      )(
        lateAppealReason => Json.obj("lateAppealReason" -> lateAppealReason)
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
      case "crime" => {
        Json.fromJson(payload)(CrimeAppealInformation.crimeAppealInformationFormatter)
      }
      case "fireOrFlood" => {
        Json.fromJson(payload)(FireOrFloodAppealInformation.fireOrFloodAppealInformationFormatter)
      }
      case "lossOfStaff" => {
        Json.fromJson(payload)(LossOfStaffAppealInformation.lossOfStaffAppealInformationFormatter)
      }
      case "technicalIssues" => {
        Json.fromJson(payload)(TechnicalIssuesAppealInformation.technicalIssuesAppealInformationFormatter)
      }
      case "health" => {
        Json.fromJson(payload)(HealthAppealInformation.healthAppealInformationFormatter)
      }
      case "other" => {
        Json.fromJson(payload)(OtherAppealInformation.otherAppealInformationFormatter)
      }
    }
  }

  def parseAppealInformationToJson(payload: AppealInformation): JsValue = {
    payload.`type` match {
      case "crime" => {
        Json.toJson(payload.asInstanceOf[CrimeAppealInformation])(CrimeAppealInformation.crimeAppealWrites)
      }
      case "fireOrFlood" => {
        Json.toJson(payload.asInstanceOf[FireOrFloodAppealInformation])(FireOrFloodAppealInformation.fireOrFloodAppealWrites)
      }
      case "lossOfStaff" => {
        Json.toJson(payload.asInstanceOf[LossOfStaffAppealInformation])(LossOfStaffAppealInformation.lossOfStaffAppealWrites)
      }
      case "technicalIssues" => {
        Json.toJson(payload.asInstanceOf[TechnicalIssuesAppealInformation])(TechnicalIssuesAppealInformation.technicalIssuesAppealWrites)
      }
      case "health" => {
        Json.toJson(payload.asInstanceOf[HealthAppealInformation])(HealthAppealInformation.healthAppealWrites)
      }
      case "other" => {
        Json.toJson(payload.asInstanceOf[OtherAppealInformation])(OtherAppealInformation.otherAppealInformationWrites)
      }
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
