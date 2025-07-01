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

import models.upload.{UploadDetails, UploadJourney, UploadStatusEnum}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.LocalDateTime

class AppealSubmissionSpec extends AnyWordSpec with Matchers {
  def bereavementAppealJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "sourceSystem": "MDTP",
       |   "taxRegime": "VAT",
       |   "customerReferenceNo": "123456789",
       |   "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "isLPP": true,
       |   "appealSubmittedBy": "customer",
       |   "appealInformation": {
       |             "reasonableExcuse": "bereavement",
       |             "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |             "lateAppeal": true,
       |             "lateAppealReason": "Reason",
       |             "isClientResponsibleForSubmission": false,
       |             "isClientResponsibleForLateSubmission": true,
       |             "honestyDeclaration": true
       |   }
       |}
       |""".stripMargin
  )

  def crimeAppealJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |						 "reasonableExcuse": "crime",
       |            "honestyDeclaration": true,
       |            "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |            "reportedIssueToPolice": "yes",
       |            "lateAppeal": true,
       |            "lateAppealReason": "Reason",
       |            "isClientResponsibleForSubmission": false,
       |            "isClientResponsibleForLateSubmission": true
       |		}
       |}
       |""".stripMargin)

  def lossOfStaffAppealJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |						 "reasonableExcuse": "lossOfEssentialStaff",
       |            "honestyDeclaration": true,
       |            "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |            "lateAppeal": true,
       |            "lateAppealReason": "Reason",
       |            "isClientResponsibleForSubmission": false,
       |            "isClientResponsibleForLateSubmission": true
       |		}
       |}
       |""".stripMargin)

  def technicalIssuesAppealJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |						 "reasonableExcuse": "technicalIssue",
       |            "honestyDeclaration": true,
       |            "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |            "endDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:01Z\"" else "\"2020-01-01T00:00:01\""},
       |            "lateAppeal": true,
       |            "lateAppealReason": "Reason",
       |            "isClientResponsibleForSubmission": false,
       |            "isClientResponsibleForLateSubmission": true
       |		}
       |}
       |""".stripMargin)

  val technicalIssuesAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "appealSubmittedBy": "customer",
      |    "appealInformation": {
      |						 "reasonableExcuse": "technicalIssue",
      |            "honestyDeclaration": true,
      |            "startDateOfEvent": "2021-04-23T00:00:00"
      |		}
      |}
      |""".stripMargin)

  def technicalIssuesAppealInformationJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "technicalIssue",
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "endDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:01Z\"" else "\"2020-01-01T00:00:01\""},
       |   "lateAppeal": true,
       |   "lateAppealReason": "Reason",
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true,
       |   "honestyDeclaration": true
       |}
       |""".stripMargin
  )

  def fireOrFloodAppealJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |						"reasonableExcuse": "fireandflood",
       |           "honestyDeclaration": true,
       |           "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |           "lateAppeal": true,
       |           "lateAppealReason": "Reason",
       |           "isClientResponsibleForSubmission": false,
       |           "isClientResponsibleForLateSubmission": true
       |		}
       |}
       |""".stripMargin)

  def healthAppealNoHospitalStayJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |           "reasonableExcuse": "health",
       |           "honestyDeclaration": true,
       |           "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |           "hospitalStayInvolved": false,
       |           "eventOngoing": false,
       |           "lateAppeal": false,
       |           "isClientResponsibleForSubmission": false,
       |           "isClientResponsibleForLateSubmission": true
       |    }
       |}
       |""".stripMargin)

  def healthAppealHospitalStayOngoingJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |           "reasonableExcuse": "health",
       |           "honestyDeclaration": true,
       |           "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |           "hospitalStayInvolved": true,
       |           "eventOngoing": true,
       |           "lateAppeal": false,
       |           "isClientResponsibleForSubmission": false,
       |           "isClientResponsibleForLateSubmission": true
       |    }
       |}
       |""".stripMargin)

  def healthAppealHospitalStayEndedJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |           "reasonableExcuse": "health",
       |           "honestyDeclaration": true,
       |           "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |           "endDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:01Z\"" else "\"2020-01-01T00:00:01\""},
       |           "hospitalStayInvolved": true,
       |           "eventOngoing": false,
       |           "lateAppeal": false,
       |           "isClientResponsibleForSubmission": false,
       |           "isClientResponsibleForLateSubmission": true
       |    }
       |}
       |""".stripMargin)

  val bereavementAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "appealSubmittedBy": "customer",
      |    "appealInformation": {
      |           "reasonableExcuse": "bereavement",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00:00"
      |    }
      |}
      |""".stripMargin
  )

  val crimeAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "appealSubmittedBy": "customer",
      |    "appealInformation": {
      |						"reasonableExcuse": "crime",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00:00",
      |           "reportedIssueToPolice": "yes"
      |		}
      |}
      |""".stripMargin)

  val lossOfStaffAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "appealSubmittedBy": "customer",
      |    "appealInformation": {
      |						"reasonableExcuse": "lossOfEssentialStaff",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00:00"
      |		}
      |}
      |""".stripMargin)

  def otherAppealJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |						 "reasonableExcuse": "other",
       |            "honestyDeclaration": true,
       |            "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |            "statement": "This is a reason.",
       |            "supportingEvidence": {
       |             "noOfUploadedFiles": 1
       |            },
       |            "lateAppeal": true,
       |            "lateAppealReason": "Reason",
       |            "isClientResponsibleForSubmission": false,
       |            "isClientResponsibleForLateSubmission": true
       |		}
       |}
       |""".stripMargin)

  def otherAppealJsonNoEvidence(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |    "sourceSystem": "MDTP",
       |    "taxRegime": "VAT",
       |    "customerReferenceNo": "123456789",
       |    "dateOfAppeal": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |    "isLPP": true,
       |    "appealSubmittedBy": "agent",
       |    "agentDetails": {
       |       "agentReferenceNo": "AGENT1",
       |       "isExcuseRelatedToAgent": true
       |    },
       |    "appealInformation": {
       |						 "reasonableExcuse": "other",
       |            "honestyDeclaration": true,
       |            "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |            "statement": "This is a reason.",
       |            "lateAppeal": true,
       |            "lateAppealReason": "Reason",
       |            "isClientResponsibleForSubmission": false,
       |            "isClientResponsibleForLateSubmission": true
       |		}
       |}
       |""".stripMargin)

  val otherAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "appealInformation": {
      |						"reasonableExcuse": "other",
      |           "honestyDeclaration": true,
      |           "lateAppeal": true,
      |           "lateAppealReason": "Reason"
      |		}
      |}
      |""".stripMargin)

  def lossOfStaffAppealInformationJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "lossOfEssentialStaff",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "lateAppeal": true,
       |   "lateAppealReason": "Reason",
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  val fireOrFloodAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "customer",
      |    "appealInformation": {
      |						"reasonableExcuse": "fireandflood",
      |           "honestyDeclaration": true,
      |           "lateAppeal": true
      |		}
      |}
      |""".stripMargin
  )

  def bereavementAppealInformationJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "bereavement",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "lateAppeal": true,
       |   "lateAppealReason": "Reason",
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  def crimeAppealInformationJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "crime",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "reportedIssueToPolice": "yes",
       |   "lateAppeal": true,
       |   "lateAppealReason": "Reason",
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  def fireOrFloodAppealInformationJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "fireandflood",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "lateAppeal": true,
       |   "lateAppealReason": "Reason",
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  val invalidBereavementAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |    "startDateOfEvent": "2021-04-23T00:00:00",
      |    "lateAppeal": true
      |}
      |""".stripMargin
  )

  val invalidCrimeAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "startDateOfEvent": "2021-04-23T00:00:00",
      |   "reportedIssueToPolice": "yes"
      |}
      |""".stripMargin
  )

  val invalidFireOrFloodAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "startDateOfEvent": "2021-04-23T00:00:00",
      |   "lateAppeal": true
      |}
      |""".stripMargin
  )

  val invalidTechnicalIssuesAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "startDateOfEvent": "2021-04-23T00:00:00",
      |   "lateAppeal": true
      |}
      |""".stripMargin
  )

  def healthAppealInformationHospitalStayNotOngoingJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "health",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "endDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:01Z\"" else "\"2020-01-01T00:00:01\""},
       |   "eventOngoing": false,
       |   "hospitalStayInvolved": true,
       |   "lateAppeal": false,
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  def healthAppealInformationHospitalStayOngoingJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "health",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "eventOngoing": true,
       |   "hospitalStayInvolved": true,
       |   "lateAppeal": false,
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  def healthAppealInformationNoHospitalStayJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "health",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "hospitalStayInvolved": false,
       |   "eventOngoing": false,
       |   "lateAppeal": false,
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  def otherAppealInformationJson(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "other",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "statement": "This is a statement.",
       |   "supportingEvidence": {
       |     "noOfUploadedFiles": 1
       |   },
       |   "lateAppeal": false,
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  def otherAppealInformationJsonNoEvidence(withZone: Boolean): JsValue = Json.parse(
    s"""
       |{
       |   "reasonableExcuse": "other",
       |   "honestyDeclaration": true,
       |   "startDateOfEvent": ${if (withZone) "\"2020-01-01T00:00:00Z\"" else "\"2020-01-01T00:00:00\""},
       |   "statement": "This is a statement.",
       |   "lateAppeal": false,
       |   "isClientResponsibleForSubmission": false,
       |   "isClientResponsibleForLateSubmission": true
       |}
       |""".stripMargin
  )

  val invalidOtherAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "other",
      |   "lateAppeal": false
      |}
      |""".stripMargin)

  val invalidHealthAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "health",
      |   "eventOngoing": false,
      |   "lateAppeal": false
      |}
      |""".stripMargin)

  "parseAppealInformationFromJson" should {
    "for bereavement" must {
      "parse the appeal information object into the relevant appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("bereavement", bereavementAppealInformationJson(false))
        result.isSuccess shouldBe true
        result.get shouldBe BereavementAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "bereavement"
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("bereavement", invalidBereavementAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }
    "for crime" must {
      "parse the appeal information object into the relevant appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("crime", crimeAppealInformationJson(false))
        result.isSuccess shouldBe true
        result.get shouldBe CrimeAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          reportedIssueToPolice = "yes",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("crime", invalidCrimeAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }

    "for fireOrFlood" must {
      "parse the appeal information object into the relevant appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("fireandflood", fireOrFloodAppealInformationJson(false))
        result.isSuccess shouldBe true
        result.get shouldBe FireOrFloodAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "fireandflood"
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("fireandflood", invalidFireOrFloodAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }

    "for technicalIssues" must {
      "parse the appeal information object into the relevant appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("technicalIssue", technicalIssuesAppealInformationJson(false))
        result.isSuccess shouldBe true
        result.get shouldBe TechnicalIssuesAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          endDateOfEvent = "2020-01-01T00:00:01",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "technicalIssue"
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("technicalIssue", invalidTechnicalIssuesAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }

    "for health" must {
      "parse the appeal information" when {
        "there has been no hospital stay" in {
          val result = AppealSubmission.parseAppealInformationFromJson("health", healthAppealInformationNoHospitalStayJson(false))
          result.isSuccess shouldBe true
          result.get shouldBe HealthAppealInformation(
            startDateOfEvent = Some("2020-01-01T00:00:00"),
            endDateOfEvent = None,
            eventOngoing = false,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = false
          )
        }

        "there is a hospital stay that is ongoing" in {
          val result = AppealSubmission.parseAppealInformationFromJson("health", healthAppealInformationHospitalStayOngoingJson(false))
          result.isSuccess shouldBe true
          result.get shouldBe HealthAppealInformation(
            startDateOfEvent = Some("2020-01-01T00:00:00"),
            endDateOfEvent = None,
            eventOngoing = true,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = true
          )
        }

        "there was a hospital stay that has now ended" in {
          val result = AppealSubmission.parseAppealInformationFromJson("health", healthAppealInformationHospitalStayNotOngoingJson(false))
          result.isSuccess shouldBe true
          result.get shouldBe HealthAppealInformation(
            startDateOfEvent = Some("2020-01-01T00:00:00"),
            endDateOfEvent = Some("2020-01-01T00:00:01"),
            eventOngoing = false,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = true
          )
        }
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("health", invalidHealthAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }

    "for other" must {
      "parse the appeal information object into the relevant appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("other", otherAppealInformationJson(false))
        result.isSuccess shouldBe true
        result.get shouldBe OtherAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = Some("This is a statement."),
          lateAppeal = false,
          lateAppealReason = None,
          supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "other",
          uploadedFiles = None
        )
      }

      "parse the appeal information object into the relevant appeal information case class - no evidence" in {
        val result = AppealSubmission.parseAppealInformationFromJson("other", otherAppealInformationJsonNoEvidence(false))
        result.isSuccess shouldBe true
        result.get shouldBe OtherAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = Some("This is a statement."),
          lateAppeal = false,
          lateAppealReason = None,
          supportingEvidence = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "other",
          uploadedFiles = None
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("other", invalidOtherAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }
  }

  "parseAppealInformationToJson" should {
    "for bereavement" must {
      "parse the appeal information model into a JsObject" in {
        val model = BereavementAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "bereavement"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe bereavementAppealInformationJson(true)
      }
    }

    "for crime" must {
      "parse the appeal information model into a JsObject" in {
        val model = CrimeAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          reportedIssueToPolice = "yes",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe crimeAppealInformationJson(true)
      }
    }

    "for fireOrFlood" must {
      "parse the appeal information model into a JsObject" in {
        val model = FireOrFloodAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "fireandflood"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe fireOrFloodAppealInformationJson(true)
      }
    }

    "for loss of staff" must {
      "parse the appeal information model into a JsObject" in {
        val model = LossOfStaffAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "lossOfEssentialStaff"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe lossOfStaffAppealInformationJson(true)
      }
    }

    "for technical issues" must {
      "parse the appeal information model into a JsObject" in {
        val model = TechnicalIssuesAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          endDateOfEvent = "2020-01-01T00:00:01",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "technicalIssue"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe technicalIssuesAppealInformationJson(true)
      }
    }

    "for health" must {
      "parse the appeal information model into a JsObject (when a startDateOfEvent and endDateOfEvent is present)" in {
        val model = HealthAppealInformation(
          startDateOfEvent = Some("2020-01-01T00:00:00"),
          endDateOfEvent = Some("2020-01-01T00:00:01"),
          eventOngoing = false,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "health",
          hospitalStayInvolved = true
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe healthAppealInformationHospitalStayNotOngoingJson(true).as[JsObject] - "hospitalStayInvolved" ++
          Json.obj("reasonableExcuse" -> "unexpectedHospitalStay")
      }

      "parse the appeal information model into a JsObject (event ongoing hospital stay)" in {
        val model = HealthAppealInformation(
          startDateOfEvent = Some("2020-01-01T00:00:00"),
          endDateOfEvent = None,
          eventOngoing = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "health",
          hospitalStayInvolved = true
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe healthAppealInformationHospitalStayOngoingJson(true).as[JsObject] - "hospitalStayInvolved" ++
          Json.obj("reasonableExcuse" -> "unexpectedHospitalStay")
      }

      "parse the appeal information model into a JsObject (when a startDateOfEvent is present NOT startDateOfEvent AND endDateOfEvent i.e. " +
        "no hospital stay)" in {
        val model = HealthAppealInformation(
          endDateOfEvent = None,
          startDateOfEvent = Some("2020-01-01T00:00:00"),
          eventOngoing = false,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "health",
          hospitalStayInvolved = false
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe healthAppealInformationNoHospitalStayJson(true).as[JsObject] - "hospitalStayInvolved" - "eventOngoing" ++
          Json.obj("reasonableExcuse" -> "seriousOrLifeThreateningIllHealth")
      }
    }

    "for other" must {
      "parse the appeal information model into a JsObject" in {
        val model = OtherAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = Some("This is a statement."),
          supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "other",
          uploadedFiles = None
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe otherAppealInformationJson(true)
      }

      "parse the appeal information model into a JsObject - no evidence" in {
        val model = OtherAppealInformation(
          startDateOfEvent = "2020-01-01T00:00:00",
          statement = Some("This is a statement."),
          supportingEvidence = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "other",
          uploadedFiles = None
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe otherAppealInformationJsonNoEvidence(true)
      }
    }
  }

  "apiReads" should {
    "for bereavement" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "customer",
          agentDetails = None,
          appealInformation = BereavementAppealInformation(
            startDateOfEvent = "2020-01-01T00:00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "bereavement"
          )
        )

        val result = Json.fromJson(bereavementAppealJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "not parse the JSON into a model when some keys are not present" in {
        val result = Json.fromJson(bereavementAppealJsonWithKeyMissing)(AppealSubmission.apiReads)
        result.isSuccess shouldBe false
      }
    }
    "for crime" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = CrimeAppealInformation(
            startDateOfEvent = "2020-01-01T00:00:00",
            reportedIssueToPolice = "yes",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "crime"
          )
        )

        val result = Json.fromJson(crimeAppealJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "not parse the JSON into a model when some keys are not present" in {
        val result = Json.fromJson(crimeAppealJsonWithKeyMissing)(AppealSubmission.apiReads)
        result.isSuccess shouldBe false
      }
    }

    "for fireOrFlood" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = FireOrFloodAppealInformation(
            startDateOfEvent = "2020-01-01T00:00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "fireandflood"
          )
        )

        val result = Json.fromJson(fireOrFloodAppealJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "not parse the JSON into a model when some keys are not present" in {
        val result = Json.fromJson(fireOrFloodAppealJsonWithKeyMissing)(AppealSubmission.apiReads)
        result.isSuccess shouldBe false
      }
    }

    "for loss of staff" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = LossOfStaffAppealInformation(
            startDateOfEvent = "2020-01-01T00:00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "lossOfEssentialStaff"
          )
        )

        val result = Json.fromJson(lossOfStaffAppealJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "not parse the JSON into a model when some keys are not present" in {
        val result = Json.fromJson(lossOfStaffAppealJsonWithKeyMissing)(AppealSubmission.apiReads)
        result.isSuccess shouldBe false
      }
    }

    "for technical issues" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = TechnicalIssuesAppealInformation(
            startDateOfEvent = "2020-01-01T00:00:00",
            endDateOfEvent = "2020-01-01T00:00:01",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "technicalIssue"
          )
        )

        val result = Json.fromJson(technicalIssuesAppealJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "not parse the JSON into a model when some keys are not present" in {
        val result = Json.fromJson(technicalIssuesAppealJsonWithKeyMissing)(AppealSubmission.apiReads)
        result.isSuccess shouldBe false
      }
    }

    "for health" must {
      "read the JSON when there was no hospital stay" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = HealthAppealInformation(
            endDateOfEvent = None,
            startDateOfEvent = Some("2020-01-01T00:00:00"),
            eventOngoing = false,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = false
          )
        )
        val result = Json.fromJson(healthAppealNoHospitalStayJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "read the JSON when there is an ongoing hospital stay" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = HealthAppealInformation(
            startDateOfEvent = Some("2020-01-01T00:00:00"),
            endDateOfEvent = None,
            eventOngoing = true,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = true
          )
        )
        val result = Json.fromJson(healthAppealHospitalStayOngoingJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "read the JSON when there has been a hospital stay" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = HealthAppealInformation(
            startDateOfEvent = Some("2020-01-01T00:00:00"),
            endDateOfEvent = Some("2020-01-01T00:00:01"),
            eventOngoing = false,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = true
          )
        )
        val result = Json.fromJson(healthAppealHospitalStayEndedJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }
    }

    "for other" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2020-01-01T00:00:00",
            statement = Some("This is a reason."),
            supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "other",
            uploadedFiles = None
          )
        )

        val result = Json.fromJson(otherAppealJson(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "parse the JSON into a model when all keys are present - no evidence" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2020-01-01T00:00:00",
            statement = Some("This is a reason."),
            supportingEvidence = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "other",
            uploadedFiles = None
          )
        )

        val result = Json.fromJson(otherAppealJsonNoEvidence(false))(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "not parse the JSON into a model when some keys are not present" in {
        val result = Json.fromJson(otherAppealJsonWithKeyMissing)(AppealSubmission.apiReads)
        result.isSuccess shouldBe false
      }
    }
  }

  "apiWrites" should {
    "for bereavement" must {
      "write the model to JSON" in {
        val modelToCovertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = BereavementAppealInformation(
            startDateOfEvent = "2021-04-23T00:00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "bereavement"
          )
        )
        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00Z",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "bereavement",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true
          )
        )

        val result = Json.toJson(modelToCovertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }
    "for crime" must {
      "write the model to JSON" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = CrimeAppealInformation(
            startDateOfEvent = "2021-04-23T00:00:00",
            reportedIssueToPolice = "yes",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "crime"
          )
        )
        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00Z",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "crime",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "reportedIssueToPolice" -> "yes",
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }

    "for fireOrFlood" must {
      "write the model to Json" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = FireOrFloodAppealInformation(
            startDateOfEvent = "2021-04-23T00:00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "fireandflood"
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00Z",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "fireandflood",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }

    "for loss of staff" must {
      "write the model to JSON" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = LossOfStaffAppealInformation(
            startDateOfEvent = "2021-04-23T00:00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "lossOfEssentialStaff"
          )
        )
        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00Z",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "lossOfEssentialStaff",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }

    "for health" must {
      "write the appeal to JSON" when {
        "there has been a hospital stay - and is no longer ongoing (both start and end date) - write the appeal model to JSON" in {
          val modelToConvertToJson = AppealSubmission(
            taxRegime = "VAT",
            customerReferenceNo = "123456789",
            dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
            isLPP = false,
            appealSubmittedBy = "agent",
            agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
            appealInformation = HealthAppealInformation(
              startDateOfEvent = Some("2020-01-01T00:00:00"),
              endDateOfEvent = Some("2020-01-01T00:00:01"),
              eventOngoing = false,
              statement = None,
              lateAppeal = true,
              lateAppealReason = Some("Reason"),
              isClientResponsibleForSubmission = Some(false),
              isClientResponsibleForLateSubmission = Some(true),
              honestyDeclaration = true,
              reasonableExcuse = "health",
              hospitalStayInvolved = true
            )
          )
          val jsonRepresentingModel: JsValue = Json.obj(
            "appealSubmittedBy" -> "agent",
            "sourceSystem" -> "MDTP",
            "taxRegime" -> "VAT",
            "customerReferenceNo" -> "123456789",
            "dateOfAppeal" -> "2020-01-01T00:00:00Z",
            "isLPP" -> false,
            "agentDetails" -> Json.obj(
              "agentReferenceNo" -> "AGENT1",
              "isExcuseRelatedToAgent" -> true
            ),
            "appealInformation" -> Json.obj(
              "reasonableExcuse" -> "unexpectedHospitalStay",
              "honestyDeclaration" -> true,
              "startDateOfEvent" -> "2020-01-01T00:00:00Z",
              "endDateOfEvent" -> "2020-01-01T00:00:01Z",
              "eventOngoing" -> false,
              "lateAppeal" -> true,
              "lateAppealReason" -> "Reason",
              "isClientResponsibleForSubmission" -> false,
              "isClientResponsibleForLateSubmission" -> true
            )
          )
          val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
          result shouldBe jsonRepresentingModel
        }

        "there has been a hospital stay AND it is ongoing (no end date) - write the appeal model to JSON" in {
          val modelToConvertToJson = AppealSubmission(
            taxRegime = "VAT",
            customerReferenceNo = "123456789",
            dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
            isLPP = false,
            appealSubmittedBy = "agent",
            agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
            appealInformation = HealthAppealInformation(
              startDateOfEvent = Some("2021-04-23T00:00:00"),
              endDateOfEvent = None,
              eventOngoing = true,
              statement = None,
              lateAppeal = true,
              lateAppealReason = Some("Reason"),
              isClientResponsibleForSubmission = Some(false),
              isClientResponsibleForLateSubmission = Some(true),
              honestyDeclaration = true,
              reasonableExcuse = "health",
              hospitalStayInvolved = true
            )
          )
          val jsonRepresentingModel: JsValue = Json.obj(
            "appealSubmittedBy" -> "agent",
            "sourceSystem" -> "MDTP",
            "taxRegime" -> "VAT",
            "customerReferenceNo" -> "123456789",
            "dateOfAppeal" -> "2020-01-01T00:00:00Z",
            "isLPP" -> false,
            "agentDetails" -> Json.obj(
              "agentReferenceNo" -> "AGENT1",
              "isExcuseRelatedToAgent" -> true
            ),
            "appealInformation" -> Json.obj(
              "reasonableExcuse" -> "unexpectedHospitalStay",
              "honestyDeclaration" -> true,
              "startDateOfEvent" -> "2021-04-23T00:00:00Z",
              "eventOngoing" -> true,
              "lateAppeal" -> true,
              "lateAppealReason" -> "Reason",
              "isClientResponsibleForSubmission" -> false,
              "isClientResponsibleForLateSubmission" -> true
            )
          )
          val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
          result shouldBe jsonRepresentingModel
        }

        "there has been NO hospital stay (startDateOfEvent present, eventOngoing = false, hospitalStayInvolved = false) " +
          "write the appeal model to JSON" in {
          val modelToConvertToJson = AppealSubmission(
            taxRegime = "VAT",
            customerReferenceNo = "123456789",
            dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
            isLPP = false,
            appealSubmittedBy = "agent",
            agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
            appealInformation = HealthAppealInformation(
              endDateOfEvent = None,
              eventOngoing = false,
              startDateOfEvent = Some("2021-04-23T00:00:00"),
              statement = None,
              lateAppeal = true,
              lateAppealReason = Some("Reason"),
              isClientResponsibleForSubmission = Some(false),
              isClientResponsibleForLateSubmission = Some(true),
              honestyDeclaration = true,
              reasonableExcuse = "health",
              hospitalStayInvolved = false
            )
          )
          val jsonRepresentingModel: JsValue = Json.obj(
            "appealSubmittedBy" -> "agent",
            "sourceSystem" -> "MDTP",
            "taxRegime" -> "VAT",
            "customerReferenceNo" -> "123456789",
            "dateOfAppeal" -> "2020-01-01T00:00:00Z",
            "isLPP" -> false,
            "agentDetails" -> Json.obj(
              "agentReferenceNo" -> "AGENT1",
              "isExcuseRelatedToAgent" -> true
            ),
            "appealInformation" -> Json.obj(
              "reasonableExcuse" -> "seriousOrLifeThreateningIllHealth",
              "honestyDeclaration" -> true,
              "startDateOfEvent" -> "2021-04-23T00:00:00Z",
              "lateAppeal" -> true,
              "lateAppealReason" -> "Reason",
              "isClientResponsibleForSubmission" -> false,
              "isClientResponsibleForLateSubmission" -> true
            )
          )
          val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
          result shouldBe jsonRepresentingModel
        }
      }
    }

    "for technical issues" must {
      "write the model to JSON" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = TechnicalIssuesAppealInformation(
            startDateOfEvent = "2021-04-23T00:00:00",
            endDateOfEvent = "2021-04-24T00:00:01",
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "technicalIssue"
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00Z",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "technicalIssue",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "endDateOfEvent" -> "2021-04-24T00:00:01Z",
            "lateAppeal" -> false,
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }

    "for other" must {
      "write the model to JSON" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2021-04-23T00:00:00",
            statement = Some("This was the reason"),
            supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "other",
            uploadedFiles = None
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00Z",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "other",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "statement" -> "This was the reason",
            "supportingEvidence" -> Json.obj("noOfUploadedFiles" -> 1),
            "lateAppeal" -> false,
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }

      "write the model to JSON - no evidence" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2021-04-23T00:00:00",
            statement = Some("This was the reason"),
            supportingEvidence = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "other",
            uploadedFiles = None
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00Z",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "other",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "statement" -> "This was the reason",
            "lateAppeal" -> false,
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }

      "write the model to JSON - for late appeal" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.parse("2020-01-01T00:00:00"),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2021-04-23T00:00:00",
            statement = Some("This was the reason"),
            supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
            lateAppeal = true,
            lateAppealReason = Some("Late reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "other",
            uploadedFiles = None
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00Z",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "other",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "statement" -> "This was the reason",
            "supportingEvidence" -> Json.obj("noOfUploadedFiles" -> 1),
            "lateAppeal" -> true,
            "lateAppealReason" -> "Late reason",
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }
  }

  "BereavementAppealInformation" should {
    "bereavementAppealWrites" must {
      "write the appeal model to JSON" in {
        val model = BereavementAppealInformation(
          startDateOfEvent = "2021-04-23T00:00:00",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "bereavement"
        )
        val result = Json.toJson(model)(BereavementAppealInformation.bereavementAppealWrites)
        result shouldBe Json.obj(
          "reasonableExcuse" -> "bereavement",
          "startDateOfEvent" -> "2021-04-23T00:00:00Z",
          "lateAppeal" -> false,
          "isClientResponsibleForSubmission" -> false,
          "isClientResponsibleForLateSubmission" -> true,
          "honestyDeclaration" -> true
        )
      }
    }
  }

  "CrimeAppealInformation" should {
    "crimeAppealWrites" must {
      "write the appeal model to JSON" in {
        val model = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T00:00:00",
          reportedIssueToPolice = "yes",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
        val result = Json.toJson(model)(CrimeAppealInformation.crimeAppealWrites)
        result shouldBe Json.obj(
          "reasonableExcuse" -> "crime",
          "startDateOfEvent" -> "2021-04-23T00:00:00Z",
          "reportedIssueToPolice" -> "yes",
          "lateAppeal" -> false,
          "isClientResponsibleForSubmission" -> false,
          "isClientResponsibleForLateSubmission" -> true,
          "honestyDeclaration" -> true
        )
      }
    }
  }

  "LossOfStaffInformation" should {
    "lossOfStaffAppealWrites" must {
      "write the appeal model to JSON" in {
        val model = LossOfStaffAppealInformation(
          startDateOfEvent = "2021-04-23T00:00:00",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "lossOfEssentialStaff"
        )
        val result = Json.toJson(model)(LossOfStaffAppealInformation.lossOfStaffAppealWrites)
        result shouldBe Json.obj(
          "reasonableExcuse" -> "lossOfEssentialStaff",
          "startDateOfEvent" -> "2021-04-23T00:00:00Z",
          "lateAppeal" -> false,
          "isClientResponsibleForSubmission" -> false,
          "isClientResponsibleForLateSubmission" -> true,
          "honestyDeclaration" -> true
        )
      }
    }
  }

  "FireOrFloodAppealInformation" should {
    "fireOrFloodAppealWrites" must {
      "write the appeal model to Json" in {
        val model = FireOrFloodAppealInformation(
          startDateOfEvent = "2021-04-23T00:00:00",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "fireandflood"
        )
        val result = Json.toJson(model)(FireOrFloodAppealInformation.fireOrFloodAppealWrites)
        result shouldBe Json.obj(
          "reasonableExcuse" -> "fireandflood",
          "startDateOfEvent" -> "2021-04-23T00:00:00Z",
          "lateAppeal" -> false,
          "isClientResponsibleForSubmission" -> false,
          "isClientResponsibleForLateSubmission" -> true,
          "honestyDeclaration" -> true
        )
      }
    }
  }

  "TechnicalIssuesAppealInformation" should {
    "technicalIssuesAppealWrites" must {
      "write the appeal model to JSON" in {
        val model = TechnicalIssuesAppealInformation(
          startDateOfEvent = "2021-04-23T00:00:00",
          endDateOfEvent = "2021-04-24T00:00:01",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "technicalIssue"
        )
        val result = Json.toJson(model)(TechnicalIssuesAppealInformation.technicalIssuesAppealWrites)
        result shouldBe Json.obj(
          "reasonableExcuse" -> "technicalIssue",
          "startDateOfEvent" -> "2021-04-23T00:00:00Z",
          "endDateOfEvent" -> "2021-04-24T00:00:01Z",
          "lateAppeal" -> true,
          "lateAppealReason" -> "Reason",
          "isClientResponsibleForSubmission" -> false,
          "isClientResponsibleForLateSubmission" -> true,
          "honestyDeclaration" -> true
        )
      }
    }
  }

  "HealthAppealInformation" should {
    "healthAppealWrites" must {
      "write the appeal to JSON" when {
        "there has been a hospital stay - and is no longer ongoing (both start and end date) - write the appeal model to JSON" in {
          val model = HealthAppealInformation(
            startDateOfEvent = Some("2020-01-01T00:00:00"),
            endDateOfEvent = Some("2020-01-01T00:00:01"),
            eventOngoing = false,
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = true
          )
          val result = Json.toJson(model)(HealthAppealInformation.healthAppealWrites)
          result shouldBe Json.obj(
            "reasonableExcuse" -> "unexpectedHospitalStay",
            "startDateOfEvent" -> "2020-01-01T00:00:00Z",
            "endDateOfEvent" -> "2020-01-01T00:00:01Z",
            "eventOngoing" -> false,
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true,
            "honestyDeclaration" -> true
          )
        }

        "there has been a hospital stay AND it is ongoing (no end date) - write the appeal model to JSON" in {
          val model = HealthAppealInformation(
            startDateOfEvent = Some("2021-04-23T00:00:00"),
            endDateOfEvent = None,
            eventOngoing = true,
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = true
          )
          val result = Json.toJson(model)(HealthAppealInformation.healthAppealWrites)
          result shouldBe Json.obj(
            "reasonableExcuse" -> "unexpectedHospitalStay",
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "eventOngoing" -> true,
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true,
            "honestyDeclaration" -> true
          )
        }

        "there has been NO hospital stay (startDateOfEvent present, eventOngoing = false, reasonableExcuse = seriousOrLifeThreateningIllHealth) " +
          "write the appeal model to JSON" in {
          val model = HealthAppealInformation(
            endDateOfEvent = None,
            eventOngoing = false,
            startDateOfEvent = Some("2021-04-23T00:00:00"),
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "health",
            hospitalStayInvolved = false
          )
          val result = Json.toJson(model)(HealthAppealInformation.healthAppealWrites)
          result shouldBe Json.obj(
            "reasonableExcuse" -> "seriousOrLifeThreateningIllHealth",
            "startDateOfEvent" -> "2021-04-23T00:00:00Z",
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "isClientResponsibleForSubmission" -> false,
            "isClientResponsibleForLateSubmission" -> true,
            "honestyDeclaration" -> true
          )
        }
      }
    }
  }

  "OtherAppealInformation" should {
    "otherAppealInformationWrites" should {
      "write to JSON - no late appeal" in {
        val modelToConvertToJson = OtherAppealInformation(
          startDateOfEvent = "2022-01-01T13:00:00.000Z",
          statement = Some("I was late. Sorry."),
          supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "other",
          uploadedFiles = None
        )
        val expectedResult = Json.parse(
          """
            |{
            | "reasonableExcuse": "other",
            | "startDateOfEvent": "2022-01-01T13:00:00.000Z",
            | "statement": "I was late. Sorry.",
            | "supportingEvidence": {
            |   "noOfUploadedFiles": 1
            | },
            | "lateAppeal": false,
            | "isClientResponsibleForSubmission": false,
            | "isClientResponsibleForLateSubmission": true,
            | "honestyDeclaration": true
            |}
            |""".stripMargin)
        val result = Json.toJson(modelToConvertToJson)
        result shouldBe expectedResult
      }

      "write to JSON - late appeal" in {
        val modelToConvertToJson = OtherAppealInformation(
          startDateOfEvent = "2022-01-01T13:00:00.000Z",
          statement = Some("I was late. Sorry."),
          supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
          lateAppeal = true,
          lateAppealReason = Some("This is a reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "other",
          uploadedFiles = Some(Seq(
            UploadJourney(
              reference = "reference-3000",
              fileStatus = UploadStatusEnum.READY,
              downloadUrl = Some("download.file"),
              uploadDetails = Some(UploadDetails(
                fileName = "file1.txt",
                fileMimeType = "text/plain",
                uploadTimestamp = LocalDateTime.of(2018, 4, 24, 9, 30),
                checksum = "check12345678",
                size = 987
              )),
              lastUpdated = LocalDateTime.of(2018, 4, 24, 9, 30)
            )
          )
          ))
        val expectedResult = Json.parse(
          """
            |{
            | "reasonableExcuse": "other",
            | "startDateOfEvent": "2022-01-01T13:00:00.000Z",
            | "statement": "I was late. Sorry.",
            | "supportingEvidence": {
            |   "noOfUploadedFiles": 1
            | },
            | "lateAppeal": true,
            | "lateAppealReason": "This is a reason",
            | "isClientResponsibleForSubmission": false,
            | "isClientResponsibleForLateSubmission": true,
            |  "honestyDeclaration": true,
            |   "uploadedFiles":[
            |     {
            |       "reference":"reference-3000",
            |       "fileStatus":"READY",
            |       "downloadUrl":"download.file",
            |       "uploadDetails":
            |           {
            |             "fileName":"file1.txt",
            |             "fileMimeType":"text/plain",
            |             "uploadTimestamp":"2018-04-24T09:30:00",
            |             "checksum":"check12345678","size":987
            |             },
            |         "lastUpdated":"2018-04-24T09:30:00"
            |        }
            |       ]
            |}
            |""".stripMargin)
        val result = Json.toJson(modelToConvertToJson)
        result shouldBe expectedResult
      }

      "write to JSON - no evidence" in {
        val modelToConvertToJson = OtherAppealInformation(
          startDateOfEvent = "2022-01-01T13:00:00.000Z",
          statement = Some("I was late. Sorry."),
          supportingEvidence = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "other",
          uploadedFiles = None
        )
        val expectedResult = Json.parse(
          """
            |{
            | "reasonableExcuse": "other",
            | "startDateOfEvent": "2022-01-01T13:00:00.000Z",
            | "statement": "I was late. Sorry.",
            | "lateAppeal": false,
            | "isClientResponsibleForSubmission": false,
            | "isClientResponsibleForLateSubmission": true,
            | "honestyDeclaration": true
            |}
            |""".stripMargin)
        val result = Json.toJson(modelToConvertToJson)
        result shouldBe expectedResult
      }
    }
  }
}
