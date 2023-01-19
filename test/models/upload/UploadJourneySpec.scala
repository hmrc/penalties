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

package models.upload

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime

class UploadJourneySpec extends AnyWordSpec with Matchers {
  val mockDateTime: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0 , 0, 0)

  val uploadJourneyAsJson: JsValue = Json.parse(
    """
      |{
      | "reference": "123456789",
      | "fileStatus": "READY",
      | "downloadUrl": "www.test.com",
      | "uploadDetails": {
      |   "fileName": "file1.txt",
      |   "fileMimeType": "text/plain",
      |   "uploadTimestamp": "2018-04-24T09:30:00",
      |   "checksum": "check123456789",
      |   "size": 1
      | },
      | "lastUpdated": "2020-01-01T00:00:00",
      | "uploadFields": {
      |   "key": "abcxyz",
      |   "algo": "md5"
      | }
      |}
      |""".stripMargin
  )

  val uploadJourneyWithoutUrlOrUploadDetailsAsJson: JsValue = Json.parse(
    """
      |{
      | "reference": "123456789",
      | "fileStatus": "READY",
      | "lastUpdated": "2020-01-01T00:00:00",
      |  "uploadFields": {
      |   "key": "abcxyz",
      |   "algo": "md5"
      | }
      |}
      |""".stripMargin
  )

  val uploadJourneyWithoutUrlOrUploadDetailsAndNoStatusAsJson: JsValue = Json.parse(
    """
      |{
      | "reference": "123456789",
      | "lastUpdated": "2020-01-01T00:00:00",
      |  "uploadFields": {
      |   "key": "abcxyz",
      |   "algo": "md5"
      | }
      |}
      |""".stripMargin
  )

  val uploadJourneyWithFailureStatusAsJson: JsValue = Json.parse(
    """
      |{
      | "reference": "123456789",
      | "fileStatus": "FAILED",
      | "failureDetails": {
      |   "failureReason": "REJECTED",
      |   "message": "this file was rejected"
      | },
      | "lastUpdated": "2020-01-01T00:00:00"
      |}
      |""".stripMargin
  )

  val uploadJourneyModel: UploadJourney = UploadJourney(
    reference = "123456789",
    fileStatus = UploadStatusEnum.READY,
    downloadUrl = Some("www.test.com"),
    uploadDetails = Some(
      UploadDetails(
        fileName = "file1.txt",
        fileMimeType = "text/plain",
        uploadTimestamp = LocalDateTime.of(2018,4,24,9,30,0),
        checksum = "check123456789",
        size = 1
      )),
    lastUpdated = mockDateTime,
    uploadFields = Some(Map(
      "key" -> "abcxyz",
      "algo" -> "md5"
    ))
  )

  val uploadJourneyWithoutUrlOrUploadDetailsModel: UploadJourney = uploadJourneyModel.copy(downloadUrl = None, uploadDetails = None, lastUpdated = mockDateTime)

  val uploadJourneyWithoutUrlOrUploadWithDefaultStatusDetailsModel: UploadJourney = uploadJourneyModel.copy(
    downloadUrl = None, uploadDetails = None, fileStatus = UploadStatusEnum.WAITING, lastUpdated = mockDateTime)

  val uploadJourneyWithFailureStatusModel: UploadJourney = uploadJourneyModel.copy(
    fileStatus = UploadStatusEnum.FAILED, downloadUrl = None, uploadDetails = None,
    failureDetails = Some(FailureDetails(
      failureReason = FailureReasonEnum.REJECTED,
      message = "this file was rejected"
    )),
    lastUpdated = mockDateTime,
    uploadFields = None
  )

  "UploadJourneySpec" should {
    "be readable from JSON" in {
      val result = Json.fromJson(uploadJourneyAsJson)(UploadJourney.reads)
      result.isSuccess shouldBe true
      result.get shouldBe uploadJourneyModel
    }

    "be readable from JSON when there is no url or upload details" in {
      val result = Json.fromJson(uploadJourneyWithoutUrlOrUploadDetailsAsJson)(UploadJourney.reads)
      result.isSuccess shouldBe true
      result.get shouldBe uploadJourneyWithoutUrlOrUploadDetailsModel
    }

    "be readable from JSON when there is no url, upload details or status (set to WAITING)" in {
      val result = Json.fromJson(uploadJourneyWithoutUrlOrUploadDetailsAndNoStatusAsJson)(UploadJourney.reads)
      result.isSuccess shouldBe true
      result.get shouldBe uploadJourneyWithoutUrlOrUploadWithDefaultStatusDetailsModel
    }

    "be readable from JSON when the upload failed" in {
      val result = Json.fromJson(uploadJourneyWithFailureStatusAsJson)(UploadJourney.reads)
      result.isSuccess shouldBe true
      result.get shouldBe uploadJourneyWithFailureStatusModel
    }

    "be writable to JSON" in {
      val result = Json.toJson(uploadJourneyModel)(UploadJourney.writes)
      result shouldBe uploadJourneyAsJson
    }

    "be writable to JSON when there is no url or upload details" in {
      val result = Json.toJson(uploadJourneyWithoutUrlOrUploadDetailsModel)(UploadJourney.writes)
      result shouldBe uploadJourneyWithoutUrlOrUploadDetailsAsJson
    }

    "be writable to JSON the upload failed" in {
      val result = Json.toJson(uploadJourneyWithFailureStatusModel)(UploadJourney.writes)
      result shouldBe uploadJourneyWithFailureStatusAsJson
    }
  }
}
