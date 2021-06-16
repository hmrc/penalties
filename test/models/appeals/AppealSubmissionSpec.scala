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
      |            "lateAppealReason": "Reason"
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
      |            "lateAppealReason": "Reason"
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
      |            "lateAppealReason": "Reason"
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
      |   "lateAppealReason": "Reason"
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
      |           "lateAppealReason": "Reason"
      |		}
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

  val lossOfStaffAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "lossOfStaff",
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason"
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
      |    "lateAppealReason": "Reason"
      |}
      |""".stripMargin
  )

  val fireOrFloodAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "type": "fireOrFlood",
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "lateAppeal": true,
      |   "lateAppealReason": "Reason"
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
          lateAppealReason = Some("Reason")
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
          lateAppealReason = Some("Reason")
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
          lateAppealReason = Some("Reason")
        )
      }

      "return a JsError when the appeal information payload is incorrect" in {
        val result = AppealSubmission.parseAppealInformationFromJson("technicalIssues", invalidTechnicalIssuesAppealInformationJson)
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
          lateAppealReason = Some("Reason")
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
          lateAppealReason = Some("Reason")
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
          lateAppealReason = Some("Reason")
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
          lateAppealReason = Some("Reason")
        )
        val result = AppealSubmission.parseAppealInformationToJson(model)
        result shouldBe technicalIssuesAppealInformationJson
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
            lateAppealReason = Some("Reason")
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
            lateAppealReason = Some("Reason")
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
            lateAppealReason = Some("Reason")
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
            lateAppealReason = Some("Reason")
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
            lateAppealReason = Some("Reason")
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
            "lateAppealReason" -> "Reason"
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
            lateAppealReason = Some("Reason")
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
            "lateAppealReason" -> "Reason"
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
            lateAppealReason = Some("Reason")
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
            "lateAppealReason" -> "Reason"
          )
        )

        val result = Json.toJson(modelToConvertToJson)(AppealSubmission.apiWrites)
        result shouldBe jsonRepresentingModel
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
            lateAppealReason = None
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
            "lateAppeal" -> false
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
          lateAppealReason = None
        )
        val result = Json.toJson(model)(CrimeAppealInformation.crimeAppealWrites)
        result shouldBe Json.obj(
          "type" -> "crime",
          "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
          "reportedIssue" -> true,
        "lateAppeal" -> false
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
          lateAppealReason = None
        )
        val result = Json.toJson(model)(LossOfStaffAppealInformation.lossOfStaffAppealWrites)
        result shouldBe Json.obj(
          "type" -> "lossOfStaff",
          "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
          "lateAppeal" -> false
        )
      }
    }
  }

  "FireOrFloodAppealInformation" should {
    "fireOrFloodAppealWrites" must{
      "write the appeal model to Json" in {
        val model = FireOrFloodAppealInformation(
          `type` = "fireOrFlood",
          dateOfEvent = "2021-04-23T18:25:43.511Z",
          statement = None,
          lateAppeal = false,
          lateAppealReason = None
        )
        val result = Json.toJson(model)(FireOrFloodAppealInformation.fireOrFloodAppealWrites)
        result shouldBe Json.obj(
          "type" -> "fireOrFlood",
          "dateOfEvent" -> "2021-04-23T18:25:43.511Z",
          "lateAppeal"-> false
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
          lateAppealReason = Some("Reason")
        )
        val result = Json.toJson(model)(TechnicalIssuesAppealInformation.technicalIssuesAppealWrites)
        result shouldBe Json.obj(
          "type" -> "technicalIssues",
          "startDateOfEvent" -> "2021-04-23T18:25:43.511Z",
          "endDateOfEvent" -> "2021-04-24T18:25:43.511Z",
          "lateAppeal" -> true,
          "lateAppealReason" -> "Reason"
        )
      }
    }
  }
}
