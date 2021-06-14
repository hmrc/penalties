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
      |    "reasonableExcuse": "ENUM_PEGA_LIST",
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

  val crimeAppealJsonWithKeyMissing: JsValue = Json.parse(
    """
      |{
      |    "submittedBy": "client",
      |    "penaltyId": "1234567890",
      |    "reasonableExcuse": "ENUM_PEGA_LIST",
      |    "appealInformation": {
      |						"type": "crime",
      |            "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |            "reportedIssue": true
      |		}
      |}
      |""".stripMargin)

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

  val invalidCrimeAppealInformationJson: JsValue = Json.parse(
    """
      |{
      |   "dateOfEvent": "2021-04-23T18:25:43.511Z",
      |   "reportedIssue": true
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
  }

  "apiReads" should {
    "for crime" must {
      "parse the JSON into a model when all keys are present" in {
        val expectedResult = AppealSubmission(
          submittedBy = "client",
          penaltyId = "1234567890",
          reasonableExcuse = "ENUM_PEGA_LIST",
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
        "lateAppeal"-> false
        )
      }
    }
  }
}
