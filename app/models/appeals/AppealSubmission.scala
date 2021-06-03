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
  val dateOfEvent: String
  val statement: Option[String]
}

case class CrimeAppealInformation(
                                   `type`: String,
                                   dateOfEvent: String,
                                   reportedIssue: Boolean,
                                   statement: Option[String]
                                 ) extends AppealInformation

object CrimeAppealInformation {
  implicit val crimeAppealInformationFormatter: OFormat[CrimeAppealInformation] = Json.format[CrimeAppealInformation]

  val crimeAppealWrites: Writes[CrimeAppealInformation] = (crimeAppealInformation: CrimeAppealInformation) => {
    Json.obj(
      "type" -> crimeAppealInformation.`type`,
      "dateOfEvent" -> crimeAppealInformation.dateOfEvent,
      "reportedIssue" -> crimeAppealInformation.reportedIssue
    ).deepMerge(
      crimeAppealInformation.statement.fold(
        Json.obj()
      )(
        statement => Json.obj("statement" -> statement)
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
    }
  }

  def parseAppealInformationToJson(payload: AppealInformation): JsValue = {
    payload.`type` match {
      case "crime" => {
        Json.toJson(payload.asInstanceOf[CrimeAppealInformation])(CrimeAppealInformation.crimeAppealWrites)
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
      appealInformationReportedIssue <- parseAppealInformationFromJson(appealInformationType, (json \ "appealInformation").get)
    } yield {
      AppealSubmission(
        submittedBy,
        penaltyId,
        reasonableExcuse,
        honestyDeclaration,
        appealInformationReportedIssue
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
