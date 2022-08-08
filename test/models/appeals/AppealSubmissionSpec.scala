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

package models.appeals

import models.upload.{UploadDetails, UploadJourney, UploadStatusEnum}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.LocalDateTime

class AppealSubmissionSpec extends AnyWordSpec with Matchers {
  val bereavementAppealJson: JsValue = Json.parse(
    """
      |{
      |   "sourceSystem": "MDTP",
      |   "taxRegime": "VAT",
      |   "customerReferenceNo": "123456789",
      |   "dateOfAppeal": "2020-01-01T00:00:00",
      |   "isLPP": true,
      |   "appealSubmittedBy": "client",
      |   "appealInformation": {
      |             "reasonableExcuse": "bereavement",
      |             "startDateOfEvent": "2021-04-23T00:00",
      |             "lateAppeal": true,
      |             "lateAppealReason": "Reason",
      |             "isClientResponsibleForSubmission": false,
      |             "isClientResponsibleForLateSubmission": true,
      |             "honestyDeclaration": true
      |   }
      |}
      |""".stripMargin
  )

  val crimeAppealJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |						 "reasonableExcuse": "crime",
      |            "honestyDeclaration": true,
      |            "startDateOfEvent": "2021-04-23T00:00",
      |            "reportedIssueToPolice": true,
      |            "lateAppeal": true,
      |            "lateAppealReason": "Reason",
      |            "isClientResponsibleForSubmission": false,
      |            "isClientResponsibleForLateSubmission": true
      |		}
      |}
      |""".stripMargin)

  val lossOfStaffAppealJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |						 "reasonableExcuse": "lossOfStaff",
      |            "honestyDeclaration": true,
      |            "startDateOfEvent": "2021-04-23T00:00",
      |            "lateAppeal": true,
      |            "lateAppealReason": "Reason",
      |            "isClientResponsibleForSubmission": false,
      |            "isClientResponsibleForLateSubmission": true
      |		}
      |}
      |""".stripMargin)

  val technicalIssuesAppealJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |						 "reasonableExcuse": "technicalIssues",
      |            "honestyDeclaration": true,
      |            "startDateOfEvent": "2021-04-23T00:00",
      |            "endDateOfEvent": "2021-04-24T23:59:59.999999999",
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
      |    "appealSubmittedBy": "client",
      |    "appealInformation": {
      |						 "reasonableExcuse": "technicalIssues",
      |            "honestyDeclaration": true,
      |            "startDateOfEvent": "2021-04-23T00:00"
      |		}
      |}
      |""".stripMargin)

  val technicalIssuesAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "technicalIssues",
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "endDateOfEvent": "2021-04-24T23:59:59.999999999",
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason",
      |   "isClientResponsibleForSubmission": false,
      |   "isClientResponsibleForLateSubmission": true,
      |   "honestyDeclaration": true
      |}
      |""".stripMargin
  )

  val fireOrFloodAppealJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |						"reasonableExcuse": "fireOrFlood",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00",
      |           "lateAppeal": true,
      |           "lateAppealReason": "Reason",
      |           "isClientResponsibleForSubmission": false,
      |           "isClientResponsibleForLateSubmission": true
      |		}
      |}
      |""".stripMargin)

  val healthAppealNoHospitalStayJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |           "reasonableExcuse": "health",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00",
      |           "hospitalStayInvolved": false,
      |           "eventOngoing": false,
      |           "lateAppeal": false,
      |           "isClientResponsibleForSubmission": false,
      |           "isClientResponsibleForLateSubmission": true
      |    }
      |}
      |""".stripMargin)

  val healthAppealHospitalStayOngoingJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |           "reasonableExcuse": "health",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00",
      |           "hospitalStayInvolved": true,
      |           "eventOngoing": true,
      |           "lateAppeal": false,
      |           "isClientResponsibleForSubmission": false,
      |           "isClientResponsibleForLateSubmission": true
      |    }
      |}
      |""".stripMargin)

  val healthAppealHospitalStayEndedJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |           "reasonableExcuse": "health",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00",
      |           "endDateOfEvent": "2021-04-24T23:59:59.999999999",
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
      |    "appealSubmittedBy": "client",
      |    "appealInformation": {
      |           "reasonableExcuse": "bereavement",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00"
      |    }
      |}
      |""".stripMargin
  )

  val crimeAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "appealSubmittedBy": "client",
      |    "appealInformation": {
      |						"reasonableExcuse": "crime",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00",
      |           "reportedIssueToPolice": true
      |		}
      |}
      |""".stripMargin)

  val lossOfStaffAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "appealSubmittedBy": "client",
      |    "appealInformation": {
      |						"reasonableExcuse": "lossOfStaff",
      |           "honestyDeclaration": true,
      |           "startDateOfEvent": "2021-04-23T00:00"
      |		}
      |}
      |""".stripMargin)

  val otherAppealJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |						 "reasonableExcuse": "other",
      |            "honestyDeclaration": true,
      |            "startDateOfEvent": "2021-04-23T00:00",
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

  val otherAppealJsonNoEvidence: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "agent",
      |    "agentDetails": {
      |       "agentReferenceNo": "AGENT1",
      |       "isExcuseRelatedToAgent": true
      |    },
      |    "appealInformation": {
      |						 "reasonableExcuse": "other",
      |            "honestyDeclaration": true,
      |            "startDateOfEvent": "2021-04-23T00:00",
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

  val lossOfStaffAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "lossOfStaff",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
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
      |    "appealSubmittedBy": "client",
      |    "appealInformation": {
      |						"reasonableExcuse": "fireOrFlood",
      |           "honestyDeclaration": true,
      |           "lateAppeal": true
      |		}
      |}
      |""".stripMargin
  )

  val bereavementAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "bereavement",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason",
      |   "isClientResponsibleForSubmission": false,
      |   "isClientResponsibleForLateSubmission": true
      |}
      |""".stripMargin
  )

  val crimeAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "crime",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "reportedIssueToPolice": true,
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason",
      |   "isClientResponsibleForSubmission": false,
      |   "isClientResponsibleForLateSubmission": true
      |}
      |""".stripMargin
  )

  val fireOrFloodAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "fireOrFlood",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
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
      |    "startDateOfEvent": "2021-04-23T00:00",
      |    "lateAppeal": true
      |}
      |""".stripMargin
  )

  val invalidCrimeAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "reportedIssueToPolice": true
      |}
      |""".stripMargin
  )

  val invalidFireOrFloodAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "lateAppeal": true
      |}
      |""".stripMargin
  )

  val invalidTechnicalIssuesAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "lateAppeal": true
      |}
      |""".stripMargin
  )

  val healthAppealInformationHospitalStayNotOngoingJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "health",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "endDateOfEvent": "2021-04-24T23:59:59.999999999",
      |   "eventOngoing": false,
      |   "hospitalStayInvolved": true,
      |   "lateAppeal": false,
      |   "isClientResponsibleForSubmission": false,
      |   "isClientResponsibleForLateSubmission": true
      |}
      |""".stripMargin
  )

  val healthAppealInformationHospitalStayOngoingJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "health",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "eventOngoing": true,
      |   "hospitalStayInvolved": true,
      |   "lateAppeal": false,
      |   "isClientResponsibleForSubmission": false,
      |   "isClientResponsibleForLateSubmission": true
      |}
      |""".stripMargin
  )

  val healthAppealInformationNoHospitalStayJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "health",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
      |   "hospitalStayInvolved": false,
      |   "eventOngoing": false,
      |   "lateAppeal": false,
      |   "isClientResponsibleForSubmission": false,
      |   "isClientResponsibleForLateSubmission": true
      |}
      |""".stripMargin
  )

  val otherAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "other",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
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

  val otherAppealInformationJsonNoEvidence: JsValue = Json.parse(
    """
      |{
      |   "reasonableExcuse": "other",
      |   "honestyDeclaration": true,
      |   "startDateOfEvent": "2021-04-23T00:00",
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

  val obligationAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "honestyDeclaration": true,
      |   "reasonableExcuse": "obligation",
      |   "statement": "A valid statement",
      |   "supportingEvidence": {
      |     "noOfUploadedFiles": 1
      |   }
      |}
      |""".stripMargin
  )

  val obligationAppealInformationJsonNoEvidence: JsValue = Json.parse(
    """
      |{
      |   "statement": "A valid statement",
      |   "reasonableExcuse": "obligation",
      |   "honestyDeclaration": true
      |}
      |""".stripMargin)

  val invalidObligationAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "statement": 1
      |}
      |""".stripMargin)

  val obligationAppealJson: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "client",
      |    "appealInformation": {
      |       "reasonableExcuse": "obligation",
      |       "honestyDeclaration": true,
      |       "statement": "A valid statement",
      |       "supportingEvidence": {
      |         "noOfUploadedFiles": 1
      |       }
      |    }
      |}
      |""".stripMargin
  )

  val obligationAppealJsonNoEvidence: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": false,
      |    "appealSubmittedBy": "client",
      |    "appealInformation": {
      |       "reasonableExcuse": "obligation",
      |       "honestyDeclaration": true,
      |       "statement": "A valid statement"
      |    }
      |}
      |""".stripMargin
  )

  val obligationAppealJsonWithKeysMissing: JsValue = Json.parse(
    """
      |{
      |    "sourceSystem": "MDTP",
      |    "taxRegime": "VAT",
      |    "customerReferenceNo": "123456789",
      |    "dateOfAppeal": "2020-01-01T00:00:00",
      |    "isLPP": true,
      |    "appealSubmittedBy": "client",
      |    "appealInformation": {
      |           "reasonableExcuse": "obligation",
      |						"statement": "a statement"
      |     }
      |}
      |""".stripMargin
  )

  "parseAppealInformationFromJson" should {
    "for bereavement" must {
      "parse the appeal information object into the relevant appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("bereavement", bereavementAppealInformationJson)
        result.isSuccess shouldBe true
        result.get shouldBe BereavementAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
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
        val result = AppealSubmission.parseAppealInformationFromJson("crime", crimeAppealInformationJson)
        result.isSuccess shouldBe true
        result.get shouldBe CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          reportedIssueToPolice = true,
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
        val result = AppealSubmission.parseAppealInformationFromJson("fireOrFlood", fireOrFloodAppealInformationJson)
        result.isSuccess shouldBe true
        result.get shouldBe FireOrFloodAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "fireOrFlood"
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("fireOrFlood", invalidFireOrFloodAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }

    "for technicalIssues" must {
      "parse the appeal information object into the relevant appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("technicalIssues", technicalIssuesAppealInformationJson)
        result.isSuccess shouldBe true
        result.get shouldBe TechnicalIssuesAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          endDateOfEvent = "2021-04-24T23:59:59.999999999",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "technicalIssues"
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("technicalIssues", invalidTechnicalIssuesAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }

    "for health" must {
      "parse the appeal information" when {
        "there has been no hospital stay" in {
          val result = AppealSubmission.parseAppealInformationFromJson("health", healthAppealInformationNoHospitalStayJson)
          result.isSuccess shouldBe true
          result.get shouldBe HealthAppealInformation(
            startDateOfEvent = Some("2021-04-23T00:00"),
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
          val result = AppealSubmission.parseAppealInformationFromJson("health", healthAppealInformationHospitalStayOngoingJson)
          result.isSuccess shouldBe true
          result.get shouldBe HealthAppealInformation(
            startDateOfEvent = Some("2021-04-23T00:00"),
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
          val result = AppealSubmission.parseAppealInformationFromJson("health", healthAppealInformationHospitalStayNotOngoingJson)
          result.isSuccess shouldBe true
          result.get shouldBe HealthAppealInformation(
            startDateOfEvent = Some("2021-04-23T00:00"),
            endDateOfEvent = Some("2021-04-24T23:59:59.999999999"),
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
        val result = AppealSubmission.parseAppealInformationFromJson("other", otherAppealInformationJson)
        result.isSuccess shouldBe true
        result.get shouldBe OtherAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
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
        val result = AppealSubmission.parseAppealInformationFromJson("other", otherAppealInformationJsonNoEvidence)
        result.isSuccess shouldBe true
        result.get shouldBe OtherAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
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

    "for obligation" must {
      "parse the appeal information object to the appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("obligation", obligationAppealInformationJson)
        result.isSuccess shouldBe true
        result.get shouldBe ObligationAppealInformation(
          statement = Some("A valid statement"),
          supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None,
          honestyDeclaration = true,
          reasonableExcuse = "obligation",
          uploadedFiles = None
        )
      }

      "parse the appeal information object to the appeal information case class - no evidence" in {
        val result = AppealSubmission.parseAppealInformationFromJson("obligation", obligationAppealInformationJsonNoEvidence)
        result.isSuccess shouldBe true
        result.get shouldBe ObligationAppealInformation(
          statement = Some("A valid statement"),
          supportingEvidence = None,
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None,
          honestyDeclaration = true,
          reasonableExcuse = "obligation",
          uploadedFiles = None
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("obligation", invalidObligationAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }
  }

  "parseAppealInformationToJson" should {
    "for bereavement" must {
      "parse the appeal information model into a JsObject" in {
        val model = BereavementAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "bereavement"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe bereavementAppealInformationJson
      }
    }
    "for crime" must {
      "parse the appeal information model into a JsObject" in {
        val model = CrimeAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          reportedIssueToPolice = true,
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "crime"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe crimeAppealInformationJson
      }
    }

    "for fireOrFlood" must {
      "parse the appeal information model into a JsObject" in {
        val model = FireOrFloodAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "fireOrFlood"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe fireOrFloodAppealInformationJson
      }
    }

    "for loss of staff" must {
      "parse the appeal information model into a JsObject" in {
        val model = LossOfStaffAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "lossOfStaff"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe lossOfStaffAppealInformationJson
      }
    }

    "for technical issues" must {
      "parse the appeal information model into a JsObject" in {
        val model = TechnicalIssuesAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
          endDateOfEvent = "2021-04-24T23:59:59.999999999",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "technicalIssues"
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe technicalIssuesAppealInformationJson
      }
    }

    "for health" must {
      "parse the appeal information model into a JsObject (when a startDateOfEvent and endDateOfEvent is present)" in {
        val model = HealthAppealInformation(
          startDateOfEvent = Some("2021-04-23T00:00"),
          endDateOfEvent = Some("2021-04-24T23:59:59.999999999"),
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
        result shouldBe healthAppealInformationHospitalStayNotOngoingJson.as[JsObject] - "hospitalStayInvolved" ++
          Json.obj("reasonableExcuse" -> "unexpectedHospitalStay")
      }

      "parse the appeal information model into a JsObject (event ongoing hospital stay)" in {
        val model = HealthAppealInformation(
          startDateOfEvent = Some("2021-04-23T00:00"),
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
        result shouldBe healthAppealInformationHospitalStayOngoingJson.as[JsObject] - "hospitalStayInvolved" ++
          Json.obj("reasonableExcuse" -> "unexpectedHospitalStay")
      }

      "parse the appeal information model into a JsObject (when a startDateOfEvent is present NOT startDateOfEvent AND endDateOfEvent i.e. " +
        "no hospital stay)" in {
        val model = HealthAppealInformation(
          endDateOfEvent = None,
          startDateOfEvent = Some("2021-04-23T00:00"),
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
        result shouldBe healthAppealInformationNoHospitalStayJson.as[JsObject] - "hospitalStayInvolved" - "eventOngoing" ++
          Json.obj("reasonableExcuse" -> "seriousOrLifeThreateningIllHealth")
      }
    }

    "for other" must {
      "parse the appeal information model into a JsObject" in {
        val model = OtherAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
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
        result shouldBe otherAppealInformationJson
      }

      "parse the appeal information model into a JsObject - no evidence" in {
        val model = OtherAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
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
        result shouldBe otherAppealInformationJsonNoEvidence
      }
    }

    "for obligation" must {
      "parse the appeal information model to a JsObject" in {
        val model = ObligationAppealInformation(
          statement = Some("A valid statement"),
          supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None,
          honestyDeclaration = true,
          reasonableExcuse = "obligation",
          uploadedFiles = None
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe obligationAppealInformationJson
      }

      "parse the appeal information model to a JsObject - no evidence" in {
        val model = ObligationAppealInformation(
          statement = Some("A valid statement"),
          supportingEvidence = None,
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None,
          honestyDeclaration = true,
          reasonableExcuse = "obligation",
          uploadedFiles = None
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe obligationAppealInformationJsonNoEvidence
      }
    }
  }

  "apiReads" should {
    "for bereavement" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "client",
          agentDetails = None,
          appealInformation = BereavementAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "bereavement"
          )
        )

        val result = Json.fromJson(bereavementAppealJson)(AppealSubmission.apiReads)
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = CrimeAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            reportedIssueToPolice = true,
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "crime"
          )
        )

        val result = Json.fromJson(crimeAppealJson)(AppealSubmission.apiReads)
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = FireOrFloodAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "fireOrFlood"
          )
        )

        val result = Json.fromJson(fireOrFloodAppealJson)(AppealSubmission.apiReads)
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = LossOfStaffAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "lossOfStaff"
          )
        )

        val result = Json.fromJson(lossOfStaffAppealJson)(AppealSubmission.apiReads)
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = TechnicalIssuesAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            endDateOfEvent = "2021-04-24T23:59:59.999999999",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "technicalIssues"
          )
        )

        val result = Json.fromJson(technicalIssuesAppealJson)(AppealSubmission.apiReads)
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = HealthAppealInformation(
            endDateOfEvent = None,
            startDateOfEvent = Some("2021-04-23T00:00"),
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
        val result = Json.fromJson(healthAppealNoHospitalStayJson)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "read the JSON when there is an ongoing hospital stay" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = HealthAppealInformation(
            startDateOfEvent = Some("2021-04-23T00:00"),
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
        val result = Json.fromJson(healthAppealHospitalStayOngoingJson)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "read the JSON when there has been a hospital stay" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = HealthAppealInformation(
            startDateOfEvent = Some("2021-04-23T00:00"),
            endDateOfEvent = Some("2021-04-24T23:59:59.999999999"),
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
        val result = Json.fromJson(healthAppealHospitalStayEndedJson)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }
    }

    "for other" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
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

        val result = Json.fromJson(otherAppealJson)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "parse the JSON into a model when all keys are present - no evidence" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
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

        val result = Json.fromJson(otherAppealJsonNoEvidence)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "not parse the JSON into a model when some keys are not present" in {
        val result = Json.fromJson(otherAppealJsonWithKeyMissing)(AppealSubmission.apiReads)
        result.isSuccess shouldBe false
      }
    }

    "for obligation" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = true,
          appealSubmittedBy = "client",
          agentDetails = None,
          appealInformation = ObligationAppealInformation(
            statement = Some("A valid statement"),
            supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
            isClientResponsibleForSubmission = None,
            isClientResponsibleForLateSubmission = None,
            honestyDeclaration = true,
            reasonableExcuse = "obligation",
            uploadedFiles = None
          )
        )

        val result = Json.fromJson(obligationAppealJson)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "parse the JSON into a model when all keys are present - no evidence" in {
        val expectedResult = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "client",
          agentDetails = None,
          appealInformation = ObligationAppealInformation(
            statement = Some("A valid statement"),
            supportingEvidence = None,
            honestyDeclaration = true,
            reasonableExcuse = "obligation",
            uploadedFiles = None
          )
        )

        val result = Json.fromJson(obligationAppealJsonNoEvidence)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "not parse the JSON into a model when some keys are not present" in {
        val result = Json.fromJson(obligationAppealJsonWithKeysMissing)(AppealSubmission.apiReads)
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = BereavementAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
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
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "bereavement",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00",
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = CrimeAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            reportedIssueToPolice = true,
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
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "crime",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00",
            "reportedIssueToPolice" -> true,
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = FireOrFloodAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "fireOrFlood"
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "fireOrFlood",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00",
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = LossOfStaffAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "lossOfStaff"
          )
        )
        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "lossOfStaff",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00",
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
            dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            isLPP = false,
            appealSubmittedBy = "agent",
            agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
            appealInformation = HealthAppealInformation(
              startDateOfEvent = Some("2021-04-23T00:00"),
              endDateOfEvent = Some("2021-04-24T23:59:59.999999999"),
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
            "dateOfAppeal" -> "2020-01-01T00:00:00",
            "isLPP" -> false,
            "agentDetails" -> Json.obj(
              "agentReferenceNo" -> "AGENT1",
              "isExcuseRelatedToAgent" -> true
            ),
            "appealInformation" -> Json.obj(
              "reasonableExcuse" -> "unexpectedHospitalStay",
              "honestyDeclaration" -> true,
              "startDateOfEvent" -> "2021-04-23T00:00",
              "endDateOfEvent" -> "2021-04-24T23:59:59.999999999",
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
            dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            isLPP = false,
            appealSubmittedBy = "agent",
            agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
            appealInformation = HealthAppealInformation(
              startDateOfEvent = Some("2021-04-23T00:00"),
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
            "dateOfAppeal" -> "2020-01-01T00:00:00",
            "isLPP" -> false,
            "agentDetails" -> Json.obj(
              "agentReferenceNo" -> "AGENT1",
              "isExcuseRelatedToAgent" -> true
            ),
            "appealInformation" -> Json.obj(
              "reasonableExcuse" -> "unexpectedHospitalStay",
              "honestyDeclaration" -> true,
              "startDateOfEvent" -> "2021-04-23T00:00",
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
            dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            isLPP = false,
            appealSubmittedBy = "agent",
            agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
            appealInformation = HealthAppealInformation(
              endDateOfEvent = None,
              eventOngoing = false,
              startDateOfEvent = Some("2021-04-23T00:00"),
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
            "dateOfAppeal" -> "2020-01-01T00:00:00",
            "isLPP" -> false,
            "agentDetails" -> Json.obj(
              "agentReferenceNo" -> "AGENT1",
              "isExcuseRelatedToAgent" -> true
            ),
            "appealInformation" -> Json.obj(
              "reasonableExcuse" -> "seriousOrLifeThreateningIllHealth",
              "honestyDeclaration" -> true,
              "startDateOfEvent" -> "2021-04-23T00:00",
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = TechnicalIssuesAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
            endDateOfEvent = "2021-04-24T23:59:59.999999999",
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            isClientResponsibleForSubmission = Some(false),
            isClientResponsibleForLateSubmission = Some(true),
            honestyDeclaration = true,
            reasonableExcuse = "technicalIssues"
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "agent",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "technicalIssues",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00",
            "endDateOfEvent" -> "2021-04-24T23:59:59.999999999",
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
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
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "other",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00",
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
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
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "other",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00",
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
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "agent",
          agentDetails = Some(AgentDetails(agentReferenceNo = "AGENT1", isExcuseRelatedToAgent = true)),
          appealInformation = OtherAppealInformation(
            startDateOfEvent = "2021-04-23T00:00",
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
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "agentDetails" -> Json.obj(
            "agentReferenceNo" -> "AGENT1",
            "isExcuseRelatedToAgent" -> true
          ),
          "appealInformation" -> Json.obj(
            "reasonableExcuse" -> "other",
            "honestyDeclaration" -> true,
            "startDateOfEvent" -> "2021-04-23T00:00",
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

    "for obligation" must {
      "write the model to JSON" in {
        val modelToCovertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "client",
          agentDetails = None,
          appealInformation = ObligationAppealInformation(
            statement = Some("A valid statement"),
            supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
            honestyDeclaration = true,
            reasonableExcuse = "obligation",
            uploadedFiles = None
          )
        )

        val jsonModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "client",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "appealInformation" -> Json.obj(
            "statement" -> "A valid statement",
            "supportingEvidence" -> Json.obj(
              "noOfUploadedFiles" -> 1
            ),
            "honestyDeclaration" -> true,
            "reasonableExcuse" -> "obligation"
          )
        )

        val result = Json.toJson(modelToCovertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonModel
      }

      "write the model to JSON - no evidence" in {
        val modelToCovertToJson: AppealSubmission = AppealSubmission(
          taxRegime = "VAT",
          customerReferenceNo = "123456789",
          dateOfAppeal = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
          isLPP = false,
          appealSubmittedBy = "client",
          agentDetails = None,
          appealInformation = ObligationAppealInformation(
            statement = Some("A valid statement"),
            supportingEvidence = None,
            honestyDeclaration = true,
            reasonableExcuse = "obligation",
            uploadedFiles = None
          )
        )

        val jsonModel: JsValue = Json.obj(
          "appealSubmittedBy" -> "client",
          "sourceSystem" -> "MDTP",
          "taxRegime" -> "VAT",
          "customerReferenceNo" -> "123456789",
          "dateOfAppeal" -> "2020-01-01T00:00:00",
          "isLPP" -> false,
          "appealInformation" -> Json.obj(
            "statement" -> "A valid statement",
            "honestyDeclaration" -> true,
            "reasonableExcuse" -> "obligation"
          )
        )

        val result = Json.toJson(modelToCovertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonModel
      }
    }
  }

  "BereavementAppealInformation" should {
    "bereavementAppealWrites" must {
      "write the appeal model to JSON" in {
        val model = BereavementAppealInformation(
          startDateOfEvent = "2021-04-23T00:00",
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
          "startDateOfEvent" -> "2021-04-23T00:00",
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
          startDateOfEvent = "2021-04-23T00:00",
          reportedIssueToPolice = true,
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
          "startDateOfEvent" -> "2021-04-23T00:00",
          "reportedIssueToPolice" -> true,
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
          startDateOfEvent = "2021-04-23T00:00",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "lossOfStaff"
        )
        val result = Json.toJson(model)(LossOfStaffAppealInformation.lossOfStaffAppealWrites)
        result shouldBe Json.obj(
          "reasonableExcuse" -> "lossOfStaff",
          "startDateOfEvent" -> "2021-04-23T00:00",
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
          startDateOfEvent = "2021-04-23T00:00",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "fireOrFlood"
        )
        val result = Json.toJson(model)(FireOrFloodAppealInformation.fireOrFloodAppealWrites)
        result shouldBe Json.obj(
          "reasonableExcuse" -> "fireOrFlood",
          "startDateOfEvent" -> "2021-04-23T00:00",
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
          startDateOfEvent = "2021-04-23T00:00",
          endDateOfEvent = "2021-04-24T23:59:59.999999999",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          isClientResponsibleForSubmission = Some(false),
          isClientResponsibleForLateSubmission = Some(true),
          honestyDeclaration = true,
          reasonableExcuse = "technicalIssues"
        )
        val result = Json.toJson(model)(TechnicalIssuesAppealInformation.technicalIssuesAppealWrites)
        result shouldBe Json.obj(
          "reasonableExcuse" -> "technicalIssues",
          "startDateOfEvent" -> "2021-04-23T00:00",
          "endDateOfEvent" -> "2021-04-24T23:59:59.999999999",
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
            startDateOfEvent = Some("2021-04-23T00:00"),
            endDateOfEvent = Some("2021-04-24T23:59:59.999999999"),
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
            "startDateOfEvent" -> "2021-04-23T00:00",
            "endDateOfEvent" -> "2021-04-24T23:59:59.999999999",
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
            startDateOfEvent = Some("2021-04-23T00:00"),
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
            "startDateOfEvent" -> "2021-04-23T00:00",
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
            startDateOfEvent = Some("2021-04-23T00:00"),
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
            "startDateOfEvent" -> "2021-04-23T00:00",
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

  "ObligationAppealInformation" should {
    "obligationAppealInformationWrites" should {
      "write to JSON" in {
        val model = ObligationAppealInformation(
          statement = Some("A valid statement"),
          supportingEvidence = Some(Evidence(noOfUploadedFiles = 1)),
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None,
          honestyDeclaration = true,
          reasonableExcuse = "obligation",
          uploadedFiles = Some(Seq(
            UploadJourney(
              reference = "ref",
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
            |   "statement": "A valid statement",
            |   "supportingEvidence": {
            |       "noOfUploadedFiles": 1
            |   },
            |   "honestyDeclaration": true,
            |   "reasonableExcuse": "obligation",
            |   "uploadedFiles":[
            |     {
            |       "reference":"ref",
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
            |""".stripMargin
        )

        val result = Json.toJson(model)
        result shouldBe expectedResult
      }

      "write to JSON - no evidence" in {
        val model = ObligationAppealInformation(
          statement = Some("A valid statement"),
          supportingEvidence = None,
          isClientResponsibleForSubmission = None,
          isClientResponsibleForLateSubmission = None,
          honestyDeclaration = true,
          reasonableExcuse = "obligation",
          uploadedFiles = None
        )

        val expectedResult = Json.parse(
          """
            |{
            |   "statement": "A valid statement",
            |   "reasonableExcuse": "obligation",
            |   "honestyDeclaration": true
            |}
            |""".stripMargin
        )

        val result = Json.toJson(model)
        result shouldBe expectedResult
      }
    }
  }
}
