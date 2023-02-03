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

package models.auditing

import base.{LogCapturing, SpecBase}
import models.notification._
import play.api.libs.json.Json

class PenaltyAppealFileNotificationStorageFailureModelSpec extends SpecBase with LogCapturing {
  val notifications: Seq[SDESNotification] = Seq(
    SDESNotification(
      informationType = "S18",
      file = SDESNotificationFile(
        recipientOrSender = "123456789012",
        name = "file1.txt",
        location = "download.file",
        checksum = SDESChecksum("md5", "check12345678"),
        size = 987,
        properties = Seq(
          SDESProperties(
            "CaseId",
            "PR-123456789"
          ),
          SDESProperties(
            "SourceFileUploadDate",
            "2018-04-24T09:30"
          )
        )
      ),
      audit = SDESAudit("corr-123456")
    )
  )
  val model = PenaltyAppealFileNotificationStorageFailureModel(notifications)

  "PenaltyAppealFileNotificationStorageFailureModel" should {
    "have the correct audit type" in {
      model.auditType shouldBe "PenaltyAppealFileNotificationStorageFailure"
    }

    "have the correct transaction name" in {
      model.transactionName shouldBe "penalties-file-notification-storage-failure"
    }

    "show the correct audit details" in {
      val expectedDetails = Json.parse(
        """
          |{
          | "notifications": [
          |   {
          |     "informationType": "S18",
          |     "file": {
          |       "recipientOrSender": "123456789012",
          |       "name": "file1.txt",
          |       "location": "download.file",
          |       "checksum": {
          |         "algorithm": "md5",
          |         "value": "check12345678"
          |       },
          |       "size": 987,
          |       "properties": [
          |         {
          |           "CaseId": "PR-123456789"
          |         },
          |         {
          |           "SourceFileUploadDate": "2018-04-24T09:30"
          |         }
          |       ]
          |     },
          |     "audit": {
          |       "correlationID": "corr-123456"
          |     }
          |   }
          | ]
          |}
          |""".stripMargin)
      model.detail shouldBe expectedDetails
    }
  }
}
