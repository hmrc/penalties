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

package models.appeals

import models.upload.UploadJourney
import play.api.libs.json._

import java.time.{LocalDateTime, ZoneOffset}

sealed trait AppealInformation {
  val statement: Option[String]
  val reasonableExcuse: String
  val honestyDeclaration: Boolean
  val isClientResponsibleForSubmission: Option[Boolean]
  val isClientResponsibleForLateSubmission: Option[Boolean]
}

case class BereavementAppealInformation(
                                         startDateOfEvent: String,
                                         statement: Option[String],
                                         lateAppeal: Boolean,
                                         lateAppealReason: Option[String],
                                         reasonableExcuse: String,
                                         honestyDeclaration: Boolean,
                                         isClientResponsibleForSubmission: Option[Boolean],
                                         isClientResponsibleForLateSubmission: Option[Boolean]
                                       ) extends AppealInformation

object BereavementAppealInformation {
  implicit val bereavementAppealInformationFormatter: OFormat[BereavementAppealInformation] = Json.format[BereavementAppealInformation]

  val bereavementAppealWrites: Writes[BereavementAppealInformation] = (bereavementAppealInformation: BereavementAppealInformation) => {
    val startDateOfEventZoned: String = LocalDateTime.parse(bereavementAppealInformation.startDateOfEvent).toInstant(ZoneOffset.UTC).toString
    Json.obj(
      "startDateOfEvent" -> startDateOfEventZoned,
      "lateAppeal" -> bereavementAppealInformation.lateAppeal,
      "reasonableExcuse" -> bereavementAppealInformation.reasonableExcuse,
      "honestyDeclaration" -> bereavementAppealInformation.honestyDeclaration
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
      bereavementAppealInformation.isClientResponsibleForSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForSubmission => Json.obj("isClientResponsibleForSubmission" -> isClientResponsibleForSubmission)
      )
    ).deepMerge(
      bereavementAppealInformation.isClientResponsibleForLateSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForLateSubmission => Json.obj("isClientResponsibleForLateSubmission" -> isClientResponsibleForLateSubmission)
      )
    )
  }
}

case class CrimeAppealInformation(
                                   startDateOfEvent: String,
                                   reportedIssueToPolice: String,
                                   statement: Option[String],
                                   lateAppeal: Boolean,
                                   lateAppealReason: Option[String],
                                   reasonableExcuse: String,
                                   honestyDeclaration: Boolean,
                                   isClientResponsibleForSubmission: Option[Boolean],
                                   isClientResponsibleForLateSubmission: Option[Boolean]
                                 ) extends AppealInformation

object CrimeAppealInformation {
  implicit val crimeAppealInformationFormatter: OFormat[CrimeAppealInformation] = Json.format[CrimeAppealInformation]

  val crimeAppealWrites: Writes[CrimeAppealInformation] = (crimeAppealInformation: CrimeAppealInformation) => {
    val startDateOfEventZoned: String = LocalDateTime.parse(crimeAppealInformation.startDateOfEvent).toInstant(ZoneOffset.UTC).toString
    Json.obj(
      "startDateOfEvent" -> startDateOfEventZoned,
      "reportedIssueToPolice" -> crimeAppealInformation.reportedIssueToPolice,
      "lateAppeal" -> crimeAppealInformation.lateAppeal,
      "reasonableExcuse" -> crimeAppealInformation.reasonableExcuse,
      "honestyDeclaration" -> crimeAppealInformation.honestyDeclaration
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
      crimeAppealInformation.isClientResponsibleForSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForSubmission => Json.obj("isClientResponsibleForSubmission" -> isClientResponsibleForSubmission)
      )
    ).deepMerge(
      crimeAppealInformation.isClientResponsibleForLateSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForLateSubmission => Json.obj("isClientResponsibleForLateSubmission" -> isClientResponsibleForLateSubmission)
      )
    )
  }
}

case class FireOrFloodAppealInformation(
                                         startDateOfEvent: String,
                                         statement: Option[String],
                                         lateAppeal: Boolean,
                                         lateAppealReason: Option[String],
                                         reasonableExcuse: String,
                                         honestyDeclaration: Boolean,
                                         isClientResponsibleForSubmission: Option[Boolean],
                                         isClientResponsibleForLateSubmission: Option[Boolean]
                                       ) extends AppealInformation

object FireOrFloodAppealInformation {
  implicit val fireOrFloodAppealInformationFormatter: OFormat[FireOrFloodAppealInformation] = Json.format[FireOrFloodAppealInformation]

  val fireOrFloodAppealWrites: Writes[FireOrFloodAppealInformation] = (fireOrFloodAppealInformation: FireOrFloodAppealInformation) => {
    val startDateOfEventZoned: String = LocalDateTime.parse(fireOrFloodAppealInformation.startDateOfEvent).toInstant(ZoneOffset.UTC).toString
    Json.obj(
      "startDateOfEvent" -> startDateOfEventZoned,
      "lateAppeal" -> fireOrFloodAppealInformation.lateAppeal,
      "reasonableExcuse" -> fireOrFloodAppealInformation.reasonableExcuse,
      "honestyDeclaration" -> fireOrFloodAppealInformation.honestyDeclaration
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
      fireOrFloodAppealInformation.isClientResponsibleForSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForSubmission => Json.obj("isClientResponsibleForSubmission" -> isClientResponsibleForSubmission)
      )
    ).deepMerge(
      fireOrFloodAppealInformation.isClientResponsibleForLateSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForLateSubmission => Json.obj("isClientResponsibleForLateSubmission" -> isClientResponsibleForLateSubmission)
      )
    )
  }
}

case class LossOfStaffAppealInformation(
                                         startDateOfEvent: String,
                                         statement: Option[String],
                                         lateAppeal: Boolean,
                                         lateAppealReason: Option[String],
                                         reasonableExcuse: String,
                                         honestyDeclaration: Boolean,
                                         isClientResponsibleForSubmission: Option[Boolean],
                                         isClientResponsibleForLateSubmission: Option[Boolean]
                                       ) extends AppealInformation

object LossOfStaffAppealInformation {
  implicit val lossOfStaffAppealInformationFormatter: OFormat[LossOfStaffAppealInformation] = Json.format[LossOfStaffAppealInformation]

  val lossOfStaffAppealWrites: Writes[LossOfStaffAppealInformation] = (lossOfStaffAppealInformation: LossOfStaffAppealInformation) => {
    val startDateOfEventZoned: String = LocalDateTime.parse(lossOfStaffAppealInformation.startDateOfEvent).toInstant(ZoneOffset.UTC).toString
    Json.obj(
      "startDateOfEvent" -> startDateOfEventZoned,
      "lateAppeal" -> lossOfStaffAppealInformation.lateAppeal,
      "reasonableExcuse" -> lossOfStaffAppealInformation.reasonableExcuse,
      "honestyDeclaration" -> lossOfStaffAppealInformation.honestyDeclaration
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
      lossOfStaffAppealInformation.isClientResponsibleForSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForSubmission => Json.obj("isClientResponsibleForSubmission" -> isClientResponsibleForSubmission)
      )
    ).deepMerge(
      lossOfStaffAppealInformation.isClientResponsibleForLateSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForLateSubmission => Json.obj("isClientResponsibleForLateSubmission" -> isClientResponsibleForLateSubmission)
      )
    )
  }
}

case class TechnicalIssuesAppealInformation(
                                             startDateOfEvent: String,
                                             endDateOfEvent: String,
                                             statement: Option[String],
                                             lateAppeal: Boolean,
                                             lateAppealReason: Option[String],
                                             reasonableExcuse: String,
                                             honestyDeclaration: Boolean,
                                             isClientResponsibleForSubmission: Option[Boolean],
                                             isClientResponsibleForLateSubmission: Option[Boolean]
                                           ) extends AppealInformation

object TechnicalIssuesAppealInformation {
  implicit val technicalIssuesAppealInformationFormatter: OFormat[TechnicalIssuesAppealInformation] = Json.format[TechnicalIssuesAppealInformation]

  val technicalIssuesAppealWrites: Writes[TechnicalIssuesAppealInformation] = (technicalIssuesAppealInformation: TechnicalIssuesAppealInformation) => {
    val startDateOfEventZoned: String = LocalDateTime.parse(technicalIssuesAppealInformation.startDateOfEvent).toInstant(ZoneOffset.UTC).toString
    val endDateOfEventZoned: String = LocalDateTime.parse(technicalIssuesAppealInformation.endDateOfEvent).toInstant(ZoneOffset.UTC).toString
    Json.obj(
      "startDateOfEvent" -> startDateOfEventZoned,
      "endDateOfEvent" -> endDateOfEventZoned,
      "lateAppeal" -> technicalIssuesAppealInformation.lateAppeal,
      "reasonableExcuse" -> technicalIssuesAppealInformation.reasonableExcuse,
      "honestyDeclaration" -> technicalIssuesAppealInformation.honestyDeclaration
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
      technicalIssuesAppealInformation.isClientResponsibleForSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForSubmission => Json.obj("isClientResponsibleForSubmission" -> isClientResponsibleForSubmission)
      )
    ).deepMerge(
      technicalIssuesAppealInformation.isClientResponsibleForLateSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForLateSubmission => Json.obj("isClientResponsibleForLateSubmission" -> isClientResponsibleForLateSubmission)
      )
    )
  }
}

case class HealthAppealInformation(
                                    startDateOfEvent: Option[String],
                                    endDateOfEvent: Option[String],
                                    eventOngoing: Boolean,
                                    statement: Option[String],
                                    lateAppeal: Boolean,
                                    hospitalStayInvolved: Boolean,
                                    lateAppealReason: Option[String],
                                    reasonableExcuse: String,
                                    honestyDeclaration: Boolean,
                                    isClientResponsibleForSubmission: Option[Boolean],
                                    isClientResponsibleForLateSubmission: Option[Boolean]
                                  ) extends AppealInformation

object HealthAppealInformation {
  implicit val healthAppealInformationFormatter: OFormat[HealthAppealInformation] = Json.format[HealthAppealInformation]

  val healthAppealWrites: Writes[HealthAppealInformation] = (healthAppealInformation: HealthAppealInformation) => {
    val healthReason = if (healthAppealInformation.hospitalStayInvolved) "unexpectedHospitalStay" else "seriousOrLifeThreateningIllHealth"

    val baseHealthInfoJson = Json.obj(
      "lateAppeal" -> healthAppealInformation.lateAppeal,
      "reasonableExcuse" -> healthReason,
      "honestyDeclaration" -> healthAppealInformation.honestyDeclaration
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
      healthAppealInformation.isClientResponsibleForSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForSubmission => Json.obj("isClientResponsibleForSubmission" -> isClientResponsibleForSubmission)
      )
    ).deepMerge(
      healthAppealInformation.isClientResponsibleForLateSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForLateSubmission => Json.obj("isClientResponsibleForLateSubmission" -> isClientResponsibleForLateSubmission)
      )
    )
    val startDateOfEventZoned: String = LocalDateTime.parse(healthAppealInformation.startDateOfEvent.get).toInstant(ZoneOffset.UTC).toString
    val additionalHealthInfo = (healthAppealInformation.hospitalStayInvolved, healthAppealInformation.eventOngoing) match {
      case (true, true) =>
        Json.obj(
          "startDateOfEvent" -> startDateOfEventZoned,
          "eventOngoing" -> healthAppealInformation.eventOngoing
        )
      case (true, false) =>
        val endDateOfEventZoned: String = LocalDateTime.parse(healthAppealInformation.endDateOfEvent.get).toInstant(ZoneOffset.UTC).toString
        Json.obj(
          "startDateOfEvent" -> startDateOfEventZoned,
          "eventOngoing" -> healthAppealInformation.eventOngoing,
          "endDateOfEvent" -> endDateOfEventZoned
        )
      case _ =>
        Json.obj(
          "startDateOfEvent" -> startDateOfEventZoned
        )
    }

    baseHealthInfoJson deepMerge additionalHealthInfo
  }
}

case class OtherAppealInformation(
                                   startDateOfEvent: String,
                                   statement: Option[String],
                                   supportingEvidence: Option[Evidence],
                                   lateAppeal: Boolean,
                                   lateAppealReason: Option[String],
                                   reasonableExcuse: String,
                                   honestyDeclaration: Boolean,
                                   isClientResponsibleForSubmission: Option[Boolean],
                                   isClientResponsibleForLateSubmission: Option[Boolean],
                                   uploadedFiles: Option[Seq[UploadJourney]]
                                 ) extends AppealInformation

object OtherAppealInformation {
  implicit val evidenceFormatter: OFormat[Evidence] = Evidence.format
  implicit val otherAppealInformationFormatter: OFormat[OtherAppealInformation] = Json.format[OtherAppealInformation]

  val otherAppealInformationWrites: Writes[OtherAppealInformation] = (otherAppealInformation: OtherAppealInformation) => {
    val startDateOfEventZoned: String = LocalDateTime.parse(otherAppealInformation.startDateOfEvent).toInstant(ZoneOffset.UTC).toString
    Json.obj(
      "startDateOfEvent" -> startDateOfEventZoned,
      "statement" -> otherAppealInformation.statement.get,
      "lateAppeal" -> otherAppealInformation.lateAppeal,
      "reasonableExcuse" -> otherAppealInformation.reasonableExcuse,
      "honestyDeclaration" -> otherAppealInformation.honestyDeclaration
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
      otherAppealInformation.isClientResponsibleForSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForSubmission => Json.obj("isClientResponsibleForSubmission" -> isClientResponsibleForSubmission)
      )
    ).deepMerge(
      otherAppealInformation.isClientResponsibleForLateSubmission.fold(
        Json.obj()
      )(
        isClientResponsibleForLateSubmission => Json.obj("isClientResponsibleForLateSubmission" -> isClientResponsibleForLateSubmission)
      )
    )
  }
}

case class AppealSubmission(
                             taxRegime: String,
                             appealSubmittedBy: String,
                             customerReferenceNo: String,
                             dateOfAppeal: LocalDateTime,
                             agentDetails: Option[AgentDetails],
                             isLPP: Boolean,
                             appealInformation: AppealInformation
                           ) {
  val sourceSystem: String = "MDTP"
}

object AppealSubmission {
  def parseAppealInformationFromJson(reason: String, payload: JsValue): JsResult[AppealInformation] = {
    reason match {
      case "bereavement" =>
        Json.fromJson(payload)(BereavementAppealInformation.bereavementAppealInformationFormatter)
      case "crime" =>
        Json.fromJson(payload)(CrimeAppealInformation.crimeAppealInformationFormatter)
      case "fireandflood" =>
        Json.fromJson(payload)(FireOrFloodAppealInformation.fireOrFloodAppealInformationFormatter)
      case "lossOfEssentialStaff" =>
        Json.fromJson(payload)(LossOfStaffAppealInformation.lossOfStaffAppealInformationFormatter)
      case "technicalIssue" =>
        Json.fromJson(payload)(TechnicalIssuesAppealInformation.technicalIssuesAppealInformationFormatter)
      case "health" =>
        Json.fromJson(payload)(HealthAppealInformation.healthAppealInformationFormatter)
      case "other" =>
        Json.fromJson(payload)(OtherAppealInformation.otherAppealInformationFormatter)
    }
  }

  def parseAppealInformationToJson(payload: AppealInformation): JsValue = {
    payload.reasonableExcuse match {
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
  }

  val apiReads: Reads[AppealSubmission] = (json: JsValue) => {
    for {
      taxRegime <- (json \ "taxRegime").validate[String]
      appealSubmittedBy <- (json \ "appealSubmittedBy").validate[String]
      customerReferenceNo <- (json \ "customerReferenceNo").validate[String]
      dateOfAppeal <- (json \ "dateOfAppeal").validate[LocalDateTime]
      isLPP <- (json \ "isLPP").validate[Boolean]
      appealInformationType <- (json \ "appealInformation" \ "reasonableExcuse").validate[String]
      agentDetails <- (json \ "agentDetails").validateOpt[AgentDetails]
      appealInformation <- parseAppealInformationFromJson(appealInformationType, (json \ "appealInformation").get)
    } yield {
      AppealSubmission(
        taxRegime,
        appealSubmittedBy,
        customerReferenceNo,
        dateOfAppeal,
        agentDetails,
        isLPP,
        appealInformation
      )
    }
  }

  implicit val apiWrites: Writes[AppealSubmission] = (appealSubmission: AppealSubmission) => {
    val dateOfAppealZoned: String = appealSubmission.dateOfAppeal.toInstant(ZoneOffset.UTC).toString
    Json.obj(
      "sourceSystem" -> appealSubmission.sourceSystem,
      "taxRegime" -> appealSubmission.taxRegime,
      "customerReferenceNo" -> appealSubmission.customerReferenceNo,
      "dateOfAppeal" -> dateOfAppealZoned,
      "isLPP" -> appealSubmission.isLPP,
      "appealSubmittedBy" -> appealSubmission.appealSubmittedBy,
      "appealInformation" -> parseAppealInformationToJson(appealSubmission.appealInformation)
    ).deepMerge(
      appealSubmission.agentDetails.fold(Json.obj())(agentDetails => Json.obj("agentDetails" -> agentDetails))
    )
  }
}
