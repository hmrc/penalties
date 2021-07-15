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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class AppealSubmissionSpec extends AnyWordSpec with Matchers {
  val crimeAppealJson: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "crime",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |						"type": "crime",
      |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |            "reportedIssue": true,
      |            "lateAppeal": true,
      |            "lateAppealReason": "Reason",
      |            "whoPlannedToSubmit": "agent",
      |            "causeOfLateSubmissionAgent": "client"
      |		}
      |}
      |""".stripMargin)

  val lossOfStaffAppealJson: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "lossOfStaff",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |						"type": "lossOfStaff",
      |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |            "lateAppeal": true,
      |            "lateAppealReason": "Reason",
      |            "whoPlannedToSubmit": "agent",
      |            "causeOfLateSubmissionAgent": "client"
      |		}
      |}
      |""".stripMargin)

  val technicalIssuesAppealJson: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "technicalIssues",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |						"type": "technicalIssues",
      |            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
      |            "endDateOfEvent": "2021-04-24T18:25:43.511Z",
      |            "lateAppeal": true,
      |            "lateAppealReason": "Reason",
      |            "whoPlannedToSubmit": "agent",
      |            "causeOfLateSubmissionAgent": "client"
      |		}
      |}
      |""".stripMargin)

  val technicalIssuesAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "technicalIssues",
      |    "appealInformation": {
      |						"type": "technicalIssues",
      |            "startDateOfEvent": "2021-04-23T18:25:43.511Z"
      |		}
      |}
      |""".stripMargin)

  val technicalIssuesAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "technicalIssues",
      |   "startDateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "endDateOfEvent": "2021-04-24T18:25:43.511Z",
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason",
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val fireOrFloodAppealJson: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "ENUM_PEGA_LIST",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |						"type": "fireOrFlood",
      |           "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |           "lateAppeal": true,
      |           "lateAppealReason": "Reason",
      |           "whoPlannedToSubmit": "agent",
      |           "causeOfLateSubmissionAgent": "client"
      |		}
      |}
      |""".stripMargin)

  val healthAppealNoHospitalStayJson: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "health",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |           "type": "health",
      |           "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |           "hospitalStayInvolved": false,
      |           "eventOngoing": false,
      |           "lateAppeal": false,
      |           "whoPlannedToSubmit": "agent",
      |           "causeOfLateSubmissionAgent": "client"
      |    }
      |}
      |""".stripMargin)

  val healthAppealHospitalStayOngoingJson: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "health",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |           "type": "health",
      |           "startDateOfEvent": "2021-04-23T18:25:43.511Z",
      |           "hospitalStayInvolved": true,
      |           "eventOngoing": true,
      |           "lateAppeal": false,
      |           "whoPlannedToSubmit": "agent",
      |           "causeOfLateSubmissionAgent": "client"
      |    }
      |}
      |""".stripMargin)

  val healthAppealHospitalStayEndedJson: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "health",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |           "type": "health",
      |           "startDateOfEvent": "2021-04-23T18:25:43.511Z",
      |           "endDateOfEvent": "2021-04-24T18:25:43.511Z",
      |           "hospitalStayInvolved": true,
      |           "eventOngoing": false,
      |           "lateAppeal": false,
      |           "whoPlannedToSubmit": "agent",
      |           "causeOfLateSubmissionAgent": "client"
      |    }
      |}
      |""".stripMargin)


  val crimeAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "crime",
      |    "appealInformation": {
      |						"type": "crime",
      |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |            "reportedIssue": true
      |		}
      |}
      |""".stripMargin)

  val lossOfStaffAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "lossOfStaff",
      |    "appealInformation": {
      |						"type": "lossOfStaff",
      |            "dateOfEvent": "2021-04-23T18:25:43.511Z"
      |		}
      |}
      |""".stripMargin)

  val otherAppealJson: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "other",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |						"type": "other",
      |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |            "statement": "This is a reason.",
      |            "supportingEvidence": {
      |             "noOfUploadedFiles": 1,
      |             "referenceId": "ref1"
      |            },
      |            "lateAppeal": true,
      |            "lateAppealReason": "Reason",
      |            "whoPlannedToSubmit": "agent",
      |            "causeOfLateSubmissionAgent": "client"
      |		}
      |}
      |""".stripMargin)

  val otherAppealJsonNoEvidence: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "other",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |						"type": "other",
      |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |            "statement": "This is a reason.",
      |            "lateAppeal": true,
      |            "lateAppealReason": "Reason",
      |            "whoPlannedToSubmit": "agent",
      |            "causeOfLateSubmissionAgent": "client"
      |		}
      |}
      |""".stripMargin)

  val otherAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "other",
      |    "honestyDeclaration": true,
      |    "appealInformation": {
      |						"type": "other",
      |            "lateAppeal": true,
      |            "lateAppealReason": "Reason"
      |		}
      |}
      |""".stripMargin)

  val lossOfStaffAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "lossOfStaff",
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason",
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val fireOrFloodAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "ENUM_PEGA_LIST",
      |    "appealInformation": {
      |						"type": "fireOrFlood",
      |           "lateAppeal": true
      |		}
      |}
      |""".stripMargin
  )

  val crimeAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "crime",
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "reportedIssue": true,
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason",
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val fireOrFloodAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "fireOrFlood",
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason",
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val invalidCrimeAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "reportedIssue": true
      |}
      |""".stripMargin
  )

  val invalidFireOrFloodAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "lateAppeal": true
      |}
      |""".stripMargin
  )

  val invalidTechnicalIssuesAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "startDateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "lateAppeal": true
      |}
      |""".stripMargin
  )

  val healthAppealInformationHospitalStayNotOngoingJson: JsValue = Json.parse(
    """
      |{
      |   "type": "health",
      |   "startDateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "endDateOfEvent": "2021-04-24T18:25:43.511Z",
      |   "eventOngoing": false,
      |   "hospitalStayInvolved": true,
      |   "lateAppeal": false,
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val healthAppealInformationHospitalStayOngoingJson: JsValue = Json.parse(
    """
      |{
      |   "type": "health",
      |   "startDateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "eventOngoing": true,
      |   "hospitalStayInvolved": true,
      |   "lateAppeal": false,
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val healthAppealInformationNoHospitalStayJson: JsValue = Json.parse(
    """
      |{
      |   "type": "health",
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "hospitalStayInvolved": false,
      |   "eventOngoing": false,
      |   "lateAppeal": false,
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val otherAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "other",
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "statement": "This is a statement.",
      |   "supportingEvidence": {
      |     "noOfUploadedFiles": 1,
      |     "referenceId": "ref1"
      |   },
      |   "lateAppeal": false,
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val otherAppealInformationJsonNoEvidence: JsValue = Json.parse(
    """
      |{
      |   "type": "other",
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "statement": "This is a statement.",
      |   "lateAppeal": false,
      |   "whoPlannedToSubmit": "agent",
      |   "causeOfLateSubmissionAgent": "client"
      |}
      |""".stripMargin
  )

  val invalidOtherAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "other",
      |   "lateAppeal": false
      |}
      |""".stripMargin)

  val invalidHealthAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "health",
      |   "eventOngoing": false,
      |   "lateAppeal": false
      |}
      |""".stripMargin)

  "parseAppealInformationFromJson" should {
    "for crime" must {
      "parse the appeal information object into the relevant appeal information case class" in {
        val result = AppealSubmission.parseAppealInformationFromJson("crime", crimeAppealInformationJson)
        result.isSuccess shouldBe true
        result.get shouldBe CrimeAppealInformation(
          `type` = "crime",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssue = true,
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
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
          `type` = "fireOrFlood",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
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
          `type` = "technicalIssues",
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          endDateOfEvent = "2021-04-24T18:25:43.511Z",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
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
            `type` = "health",
            dateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            startDateOfEvent = None,
            endDateOfEvent = None,
            eventOngoing = false,
            hospitalStayInvolved = false,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        }

        "there is a hospital stay that is ongoing" in {
          val result = AppealSubmission.parseAppealInformationFromJson("health", healthAppealInformationHospitalStayOngoingJson)
          result.isSuccess shouldBe true
          result.get shouldBe HealthAppealInformation(
            `type` = "health",
            dateOfEvent = None,
            startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            endDateOfEvent = None,
            eventOngoing = true,
            hospitalStayInvolved = true,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        }

        "there was a hospital stay that has now ended" in {
          val result = AppealSubmission.parseAppealInformationFromJson("health", healthAppealInformationHospitalStayNotOngoingJson)
          result.isSuccess shouldBe true
          result.get shouldBe HealthAppealInformation(
            `type` = "health",
            dateOfEvent = None,
            startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            endDateOfEvent = Some("2021-04-24T18:25:43.511Z"),
            eventOngoing = false,
            hospitalStayInvolved = true,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
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
          `type` = "other",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = Some("This is a statement."),
          lateAppeal = false,
          lateAppealReason = None,
          supportingEvidence = Some(Evidence(
            noOfUploadedFiles = 1, referenceId = "ref1"
          )),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
      }

      "parse the appeal information object into the relevant appeal information case class - no evidence" in {
        val result = AppealSubmission.parseAppealInformationFromJson("other", otherAppealInformationJsonNoEvidence)
        result.isSuccess shouldBe true
        result.get shouldBe OtherAppealInformation(
          `type` = "other",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = Some("This is a statement."),
          lateAppeal = false,
          lateAppealReason = None,
          supportingEvidence = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("other", invalidOtherAppealInformationJson)
        result.isSuccess shouldBe false
      }
    }
  }

  "parseAppealInformationToJson" should {
    "for crime" must {
      "parse the appeal information model into a JsObject" in {
        val model = CrimeAppealInformation(
          `type` = "crime",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssue = true,
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe crimeAppealInformationJson
      }
    }

    "for fireOrFlood" must {
      "parse the appeal information model into a JsObject" in {
        val model = FireOrFloodAppealInformation(
          `type` = "fireOrFlood",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe fireOrFloodAppealInformationJson
      }
    }

    "for loss of staff" must {
      "parse the appeal information model into a JsObject" in {
        val model = LossOfStaffAppealInformation(
          `type` = "lossOfStaff",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe lossOfStaffAppealInformationJson
      }
    }

    "for technical issues" must {
      "parse the appeal information model into a JsObject" in {
        val model = TechnicalIssuesAppealInformation(
          `type` = "technicalIssues",
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          endDateOfEvent = "2021-04-24T18:25:43.511Z",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe technicalIssuesAppealInformationJson
      }
    }

    "for health" must {
      "parse the appeal information model into a JsObject (when a startDateOfEvent and endDateOfEvent is present NOT dateOfEvent)" in {
        val model = HealthAppealInformation(
          `type` = "health",
          startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
          endDateOfEvent = Some("2021-04-24T18:25:43.511Z"),
          dateOfEvent = None,
          eventOngoing = false,
          hospitalStayInvolved = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe healthAppealInformationHospitalStayNotOngoingJson
      }

      "parse the appeal information model into a JsObject (when a startDateOfEvent is present NOT dateOfEvent AND endDateOfEvent i.e. " +
        "event ongoing hospital stay)" in {
        val model = HealthAppealInformation(
          `type` = "health",
          startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
          endDateOfEvent = None,
          dateOfEvent = None,
          eventOngoing = true,
          hospitalStayInvolved = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe healthAppealInformationHospitalStayOngoingJson
      }

      "parse the appeal information model into a JsObject (when a dateOfEvent is present NOT startDateOfEvent AND endDateOfEvent i.e. " +
        "no hospital stay)" in {
        val model = HealthAppealInformation(
          `type` = "health",
          startDateOfEvent = None,
          endDateOfEvent = None,
          dateOfEvent = Some("2021-04-23T18:25:43.511Z"),
          eventOngoing = false,
          hospitalStayInvolved = false,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe healthAppealInformationNoHospitalStayJson
      }
    }

    "for other" must {
      "parse the appeal information model into a JsObject" in {
        val model = OtherAppealInformation(
          `type` = "other",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = Some("This is a statement."),
          supportingEvidence = Some(Evidence(
            noOfUploadedFiles = 1, referenceId = "ref1"
          )),
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe otherAppealInformationJson
      }

      "parse the appeal information model into a JsObject - no evidence" in {
        val model = OtherAppealInformation(
          `type` = "other",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = Some("This is a statement."),
          supportingEvidence = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe otherAppealInformationJsonNoEvidence
      }
    }
  }

  "apiReads" should {
    "for crime" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "crime",
          honestyDeclaration = true,
          appealInformation = CrimeAppealInformation(
            `type` = "crime",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            reportedIssue = true,
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
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
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "ENUM_PEGA_LIST",
          honestyDeclaration = true,
          appealInformation = FireOrFloodAppealInformation(
            `type` = "fireOrFlood",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
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
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "lossOfStaff",
          honestyDeclaration = true,
          appealInformation = LossOfStaffAppealInformation(
            `type` = "lossOfStaff",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
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
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "technicalIssues",
          honestyDeclaration = true,
          appealInformation = TechnicalIssuesAppealInformation(
            `type` = "technicalIssues",
            startDateOfEvent = "2021-04-23T18:25:43.511Z",
            endDateOfEvent = "2021-04-24T18:25:43.511Z",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
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
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "health",
          honestyDeclaration = true,
          appealInformation = HealthAppealInformation(
            `type` = "health",
            startDateOfEvent = None,
            endDateOfEvent = None,
            dateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            eventOngoing = false,
            hospitalStayInvolved = false,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )
        val result = Json.fromJson(healthAppealNoHospitalStayJson)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "read the JSON when there is an ongoing hospital stay" in {
        val expectedResult = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "health",
          honestyDeclaration = true,
          appealInformation = HealthAppealInformation(
            `type` = "health",
            startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            endDateOfEvent = None,
            dateOfEvent = None,
            eventOngoing = true,
            hospitalStayInvolved = true,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )
        val result = Json.fromJson(healthAppealHospitalStayOngoingJson)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "read the JSON when there has been a hospital stay" in {
        val expectedResult = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "health",
          honestyDeclaration = true,
          appealInformation = HealthAppealInformation(
            `type` = "health",
            startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            endDateOfEvent = Some("2021-04-24T18:25:43.511Z"),
            dateOfEvent = None,
            eventOngoing = false,
            hospitalStayInvolved = true,
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
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
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "other",
          honestyDeclaration = true,
          appealInformation = OtherAppealInformation(
            `type` = "other",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = Some("This is a reason."),
            supportingEvidence = Some(Evidence(
              noOfUploadedFiles = 1, referenceId = "ref1"
            )),
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )

        val result = Json.fromJson(otherAppealJson)(AppealSubmission.apiReads)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

      "parse the JSON into a model when all keys are present - no evidence" in {
        val expectedResult = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "other",
          honestyDeclaration = true,
          appealInformation = OtherAppealInformation(
            `type` = "other",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = Some("This is a reason."),
            supportingEvidence = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
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
  }

  "apiWrites" should {
    "for crime" must {
      "write the model to JSON" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234",
          reasonableExcuse = "crime",
          honestyDeclaration = true,
          appealInformation = CrimeAppealInformation(
            `type` = "crime",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            reportedIssue = true,
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )
        val jsonRepresentingModel: JsValue = Json.obj(
          "submittedBy" -> "client",
          "penaltyId" -> "1234",
          "reasonableExcuse" -> "crime",
          "honestyDeclaration" -> true,
          "appealInformation" -> Json.obj(
            "type" -> "crime",
            "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "reportedIssue" -> true,
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }

    "for fireOrFlood" must {
      "write the model to Json" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234",
          reasonableExcuse = "fireOrFlood",
          honestyDeclaration = true,
          appealInformation = FireOrFloodAppealInformation(
            `type` = "fireOrFlood",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "submittedBy" -> "client",
          "penaltyId" -> "1234",
          "reasonableExcuse" -> "fireOrFlood",
          "honestyDeclaration" -> true,
          "appealInformation" -> Json.obj(
            "type" -> "fireOrFlood",
            "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }

    "for loss of staff" must {
      "write the model to JSON" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234",
          reasonableExcuse = "lossOfStaff",
          honestyDeclaration = true,
          appealInformation = LossOfStaffAppealInformation(
            `type` = "lossOfStaff",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )
        val jsonRepresentingModel: JsValue = Json.obj(
          "submittedBy" -> "client",
          "penaltyId" -> "1234",
          "reasonableExcuse" -> "lossOfStaff",
          "honestyDeclaration" -> true,
          "appealInformation" -> Json.obj(
            "type" -> "lossOfStaff",
            "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
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
            submittedBy = "client",
            penaltyId = "1234",
            reasonableExcuse = "health",
            honestyDeclaration = true,
            appealInformation = HealthAppealInformation(
              `type` = "health",
              startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
              endDateOfEvent = Some("2021-04-24T18:25:43.511Z"),
              eventOngoing = false,
              hospitalStayInvolved = true,
              dateOfEvent = None,
              statement = None,
              lateAppeal = true,
              lateAppealReason = Some("Reason"),
              whoPlannedToSubmit = Some("agent"),
              causeOfLateSubmissionAgent = Some("client")
            )
          )
          val jsonRepresentingModel: JsValue = Json.obj(
            "submittedBy" -> "client",
            "penaltyId" -> "1234",
            "reasonableExcuse" -> "health",
            "honestyDeclaration" -> true,
            "appealInformation" -> Json.obj(
              "type" -> "health",
              "startDateOfEvent" -> "2021-04-23T18:25:43.511Z",
              "endDateOfEvent" -> "2021-04-24T18:25:43.511Z",
              "eventOngoing" -> false,
              "hospitalStayInvolved" -> true,
              "lateAppeal" -> true,
              "lateAppealReason" -> "Reason",
              "whoPlannedToSubmit" -> "agent",
              "causeOfLateSubmissionAgent" -> "client"
            )
          )
          val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
          result shouldBe jsonRepresentingModel
        }

        "there has been a hospital stay AND it is ongoing (no end date) - write the appeal model to JSON" in {
          val modelToConvertToJson = AppealSubmission(
            submittedBy = "client",
            penaltyId = "1234",
            reasonableExcuse = "health",
            honestyDeclaration = true,
            appealInformation = HealthAppealInformation(
              `type` = "health",
              startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
              endDateOfEvent = None,
              eventOngoing = true,
              hospitalStayInvolved = true,
              dateOfEvent = None,
              statement = None,
              lateAppeal = true,
              lateAppealReason = Some("Reason"),
              whoPlannedToSubmit = Some("agent"),
              causeOfLateSubmissionAgent = Some("client")
            )
          )
          val jsonRepresentingModel: JsValue = Json.obj(
            "submittedBy" -> "client",
            "penaltyId" -> "1234",
            "reasonableExcuse" -> "health",
            "honestyDeclaration" -> true,
            "appealInformation" -> Json.obj(
              "type" -> "health",
              "startDateOfEvent" -> "2021-04-23T18:25:43.511Z",
              "eventOngoing" -> true,
              "hospitalStayInvolved" -> true,
              "lateAppeal" -> true,
              "lateAppealReason" -> "Reason",
              "whoPlannedToSubmit" -> "agent",
              "causeOfLateSubmissionAgent" -> "client"
            )
          )
          val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
          result shouldBe jsonRepresentingModel
        }

        "there has been NO hospital stay (dateOfEvent present, eventOngoing = false, hospitalStayInvolved = false) " +
          "write the appeal model to JSON" in {
          val modelToConvertToJson = AppealSubmission(
            submittedBy = "client",
            penaltyId = "1234",
            reasonableExcuse = "health",
            honestyDeclaration = true,
            appealInformation = HealthAppealInformation(
              `type` = "health",
              startDateOfEvent = None,
              endDateOfEvent = None,
              eventOngoing = false,
              hospitalStayInvolved = false,
              dateOfEvent = Some("2021-04-23T18:25:43.511Z"),
              statement = None,
              lateAppeal = true,
              lateAppealReason = Some("Reason"),
              whoPlannedToSubmit = Some("agent"),
              causeOfLateSubmissionAgent = Some("client")
            )
          )
          val jsonRepresentingModel: JsValue = Json.obj(
            "submittedBy" -> "client",
            "penaltyId" -> "1234",
            "reasonableExcuse" -> "health",
            "honestyDeclaration" -> true,
            "appealInformation" -> Json.obj(
              "type" -> "health",
              "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
              "eventOngoing" -> false,
              "hospitalStayInvolved" -> false,
              "lateAppeal" -> true,
              "lateAppealReason" -> "Reason",
              "whoPlannedToSubmit" -> "agent",
              "causeOfLateSubmissionAgent" -> "client"
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
          submittedBy = "client",
          penaltyId = "1234",
          reasonableExcuse = "technicalIssues",
          honestyDeclaration = true,
          appealInformation = TechnicalIssuesAppealInformation(
            `type` = "technicalIssues",
            startDateOfEvent = "2021-04-23T18:25:43.511Z",
            endDateOfEvent = "2021-04-24T18:25:43.511Z",
            statement = None,
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "submittedBy" -> "client",
          "penaltyId" -> "1234",
          "reasonableExcuse" -> "technicalIssues",
          "honestyDeclaration" -> true,
          "appealInformation" -> Json.obj(
            "type" -> "technicalIssues",
            "startDateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "endDateOfEvent" -> "2021-04-24T18:25:43.511Z",
            "lateAppeal" -> false,
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }

    "for other" must {
      "write the model to JSON" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234",
          reasonableExcuse = "other",
          honestyDeclaration = true,
          appealInformation = OtherAppealInformation(
            `type` = "other",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = Some("This was the reason"),
            supportingEvidence = Some(Evidence(
              noOfUploadedFiles = 1,
              referenceId = "ref1"
            )),
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "submittedBy" -> "client",
          "penaltyId" -> "1234",
          "reasonableExcuse" -> "other",
          "honestyDeclaration" -> true,
          "appealInformation" -> Json.obj(
            "type" -> "other",
            "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "statement" -> "This was the reason",
            "supportingEvidence" -> Json.obj(
              "noOfUploadedFiles" -> 1,
              "referenceId" -> "ref1"
            ),
            "lateAppeal" -> false,
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }

      "write the model to JSON - no evidence" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234",
          reasonableExcuse = "other",
          honestyDeclaration = true,
          appealInformation = OtherAppealInformation(
            `type` = "other",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = Some("This was the reason"),
            supportingEvidence = None,
            lateAppeal = false,
            lateAppealReason = None,
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "submittedBy" -> "client",
          "penaltyId" -> "1234",
          "reasonableExcuse" -> "other",
          "honestyDeclaration" -> true,
          "appealInformation" -> Json.obj(
            "type" -> "other",
            "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "statement" -> "This was the reason",
            "lateAppeal" -> false,
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }

      "write the model to JSON - for late appeal" in {
        val modelToConvertToJson: AppealSubmission = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234",
          reasonableExcuse = "other",
          honestyDeclaration = true,
          appealInformation = OtherAppealInformation(
            `type` = "other",
            dateOfEvent = "2021-04-23T18:25:43.511Z",
            statement = Some("This was the reason"),
            supportingEvidence = Some(Evidence(
              noOfUploadedFiles = 1,
              referenceId = "ref1"
            )),
            lateAppeal = true,
            lateAppealReason = Some("Late reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
        )

        val jsonRepresentingModel: JsValue = Json.obj(
          "submittedBy" -> "client",
          "penaltyId" -> "1234",
          "reasonableExcuse" -> "other",
          "honestyDeclaration" -> true,
          "appealInformation" -> Json.obj(
            "type" -> "other",
            "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "statement" -> "This was the reason",
            "supportingEvidence" -> Json.obj(
              "noOfUploadedFiles" -> 1,
              "referenceId" -> "ref1"
            ),
            "lateAppeal" -> true,
            "lateAppealReason" -> "Late reason",
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
      }
    }
  }


  "CrimeAppealInformation" should {
    "crimeAppealWrites" must {
      "write the appeal model to JSON" in {
        val model = CrimeAppealInformation(
          `type` = "crime",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          reportedIssue = true,
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = Json.toJson(model)(CrimeAppealInformation.crimeAppealWrites)
        result shouldBe Json.obj(
          "type" -> "crime",
          "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
          "reportedIssue" -> true,
          "lateAppeal" -> false,
          "whoPlannedToSubmit" -> "agent",
          "causeOfLateSubmissionAgent" -> "client"
        )
      }
    }
  }

  "LossOfStaffInformation" should {
    "lossOfStaffAppealWrites" must {
      "write the appeal model to JSON" in {
        val model = LossOfStaffAppealInformation(
          `type` = "lossOfStaff",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = Json.toJson(model)(LossOfStaffAppealInformation.lossOfStaffAppealWrites)
        result shouldBe Json.obj(
          "type" -> "lossOfStaff",
          "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
          "lateAppeal" -> false,
          "whoPlannedToSubmit" -> "agent",
          "causeOfLateSubmissionAgent" -> "client"
        )
      }
    }
  }

  "FireOrFloodAppealInformation" should {
    "fireOrFloodAppealWrites" must {
      "write the appeal model to Json" in {
        val model = FireOrFloodAppealInformation(
          `type` = "fireOrFlood",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = Json.toJson(model)(FireOrFloodAppealInformation.fireOrFloodAppealWrites)
        result shouldBe Json.obj(
          "type" -> "fireOrFlood",
          "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
          "lateAppeal" -> false,
          "whoPlannedToSubmit" -> "agent",
          "causeOfLateSubmissionAgent" -> "client"
        )
      }
    }
  }

  "TechnicalIssuesAppealInformation" should {
    "technicalIssuesAppealWrites" must {
      "write the appeal model to JSON" in {
        val model = TechnicalIssuesAppealInformation(
          `type` = "technicalIssues",
          startDateOfEvent = "2021-04-23T18:25:43.511Z",
          endDateOfEvent = "2021-04-24T18:25:43.511Z",
          statement = None,
          lateAppeal = true,
          lateAppealReason = Some("Reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val result = Json.toJson(model)(TechnicalIssuesAppealInformation.technicalIssuesAppealWrites)
        result shouldBe Json.obj(
          "type" -> "technicalIssues",
          "startDateOfEvent" -> "2021-04-23T18:25:43.511Z",
          "endDateOfEvent" -> "2021-04-24T18:25:43.511Z",
          "lateAppeal" -> true,
          "lateAppealReason" -> "Reason",
          "whoPlannedToSubmit" -> "agent",
          "causeOfLateSubmissionAgent" -> "client"
        )
      }
    }
  }

  "HealthAppealInformation" should {
    "healthAppealWrites" must {
      "write the appeal to JSON" when {
        "there has been a hospital stay - and is no longer ongoing (both start and end date) - write the appeal model to JSON" in {
          val model = HealthAppealInformation(
            `type` = "health",
            startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            endDateOfEvent = Some("2021-04-24T18:25:43.511Z"),
            eventOngoing = false,
            hospitalStayInvolved = true,
            dateOfEvent = None,
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
          val result = Json.toJson(model)(HealthAppealInformation.healthAppealWrites)
          result shouldBe Json.obj(
            "type" -> "health",
            "startDateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "endDateOfEvent" -> "2021-04-24T18:25:43.511Z",
            "eventOngoing" -> false,
            "hospitalStayInvolved" -> true,
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        }

        "there has been a hospital stay AND it is ongoing (no end date) - write the appeal model to JSON" in {
          val model = HealthAppealInformation(
            `type` = "health",
            startDateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            endDateOfEvent = None,
            eventOngoing = true,
            hospitalStayInvolved = true,
            dateOfEvent = None,
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
          val result = Json.toJson(model)(HealthAppealInformation.healthAppealWrites)
          result shouldBe Json.obj(
            "type" -> "health",
            "startDateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "eventOngoing" -> true,
            "hospitalStayInvolved" -> true,
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        }

        "there has been NO hospital stay (dateOfEvent present, eventOngoing = false, hospitalStayInvolved = false) " +
          "write the appeal model to JSON" in {
          val model = HealthAppealInformation(
            `type` = "health",
            startDateOfEvent = None,
            endDateOfEvent = None,
            eventOngoing = false,
            hospitalStayInvolved = false,
            dateOfEvent = Some("2021-04-23T18:25:43.511Z"),
            statement = None,
            lateAppeal = true,
            lateAppealReason = Some("Reason"),
            whoPlannedToSubmit = Some("agent"),
            causeOfLateSubmissionAgent = Some("client")
          )
          val result = Json.toJson(model)(HealthAppealInformation.healthAppealWrites)
          result shouldBe Json.obj(
            "type" -> "health",
            "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
            "eventOngoing" -> false,
            "hospitalStayInvolved" -> false,
            "lateAppeal" -> true,
            "lateAppealReason" -> "Reason",
            "whoPlannedToSubmit" -> "agent",
            "causeOfLateSubmissionAgent" -> "client"
          )
        }
      }
    }
  }

  "OtherAppealInformation" should {
    "otherAppealInformationWrites" should {
      "write to JSON - no late appeal" in {
        val modelToConvertToJson = OtherAppealInformation(
          `type` = "other",
          dateOfEvent = "2022-01-01T13:00:00.000Z",
          statement = Some("I was late. Sorry."),
          supportingEvidence = Some(Evidence(1, "reference-3000")),
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val expectedResult = Json.parse(
          """
            |{
            | "type": "other",
            | "dateOfEvent": "2022-01-01T13:00:00.000Z",
            | "statement": "I was late. Sorry.",
            | "supportingEvidence": {
            |   "noOfUploadedFiles": 1,
            |   "referenceId": "reference-3000"
            | },
            | "lateAppeal": false,
            | "whoPlannedToSubmit": "agent",
            | "causeOfLateSubmissionAgent": "client"
            |}
            |""".stripMargin)
        val result = Json.toJson(modelToConvertToJson)
        result shouldBe expectedResult
      }

      "write to JSON - late appeal" in {
        val modelToConvertToJson = OtherAppealInformation(
          `type` = "other",
          dateOfEvent = "2022-01-01T13:00:00.000Z",
          statement = Some("I was late. Sorry."),
          supportingEvidence = Some(Evidence(1, "reference-3000")),
          lateAppeal = true,
          lateAppealReason = Some("This is a reason"),
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val expectedResult = Json.parse(
          """
            |{
            | "type": "other",
            | "dateOfEvent": "2022-01-01T13:00:00.000Z",
            | "statement": "I was late. Sorry.",
            | "supportingEvidence": {
            |   "noOfUploadedFiles": 1,
            |   "referenceId": "reference-3000"
            | },
            | "lateAppeal": true,
            | "lateAppealReason": "This is a reason",
            | "whoPlannedToSubmit": "agent",
            | "causeOfLateSubmissionAgent": "client"
            |}
            |""".stripMargin)
        val result = Json.toJson(modelToConvertToJson)
        result shouldBe expectedResult
      }

      "write to JSON - no evidence" in {
        val modelToConvertToJson = OtherAppealInformation(
          `type` = "other",
          dateOfEvent = "2022-01-01T13:00:00.000Z",
          statement = Some("I was late. Sorry."),
          supportingEvidence = None,
          lateAppeal = false,
          lateAppealReason = None,
          whoPlannedToSubmit = Some("agent"),
          causeOfLateSubmissionAgent = Some("client")
        )
        val expectedResult = Json.parse(
          """
            |{
            | "type": "other",
            | "dateOfEvent": "2022-01-01T13:00:00.000Z",
            | "statement": "I was late. Sorry.",
            | "lateAppeal": false,
            | "whoPlannedToSubmit": "agent",
            | "causeOfLateSubmissionAgent": "client"
            |}
            |""".stripMargin)
        val result = Json.toJson(modelToConvertToJson)
        result shouldBe expectedResult
      }
    }
  }
}
