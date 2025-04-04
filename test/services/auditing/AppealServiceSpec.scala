/*
 * Copyright 2025 HM Revenue & Customs
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

package services.auditing

import base.SpecBase
import org.scalatest.prop.TableDrivenPropertyChecks
import services.AppealService

class AppealServiceSpec extends SpecBase with TableDrivenPropertyChecks {

  private def longNameAppended(fileName: String, fileExtension: String): String = fileName + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + fileExtension
  private def truncatedLongNameAppended(fileName: String, fileExtension: String): String = fileName + "aaaaaaaaaaaaaaaaaaaaaaaaaaa" +
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      .substring(fileName.length) + fileExtension

  private val validFileTypes = Table(
    ("mimeType", "fileExtension"),
    ("image.jpeg", ".jpeg"),
    ("image.png", ".png"),
    ("image.tiff", ".tiff"),
    ("text.plain", ".txt"),
    ("application.pdf", ".pdf"),
    ("application.vnd.ms-outlook", ".msg"),
    ("application.msword", ".doc"),
    ("application.vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    ("application.vnd.vnd.ms-excel", ".xls"),
    ("application.vnd.vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    ("application.vnd.oasis.opendocument.text", ".odt"),
    ("application.vnd.oasis.opendocument.spreadsheet", ".ods"),
    ("application.vnd.ms-powerpoint", ".ppt"),
    ("application.vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
    ("application.vnd.oasis.opendocument.presentation", ".odp")
  )

  private val sanitisedFilename = "file_name"
  private val unsanitisedTestCases = Seq(
    "file/name",
    "file*name",
    "file:name",
    "file<name",
    "file>name",
    "file?name",
    "file|name",
    "file‘name",
    """file"name""",
    """file\name"""
  )
  private val nonRegexTestCases = Seq(
    "filename",
    "file!name",
    "file@@name"
  )

  val service: AppealService = injector.instanceOf[AppealService]

  "sanitisedAndTruncatedFileName" when {
    "the filename is not greater than maximumFilenameLength" should {
      "return a sanitised filename when filename contains characters in the regexToSanitiseFileName" in {
        unsanitisedTestCases.foreach(filename =>
          service.sanitisedAndTruncatedFileName(s"$filename.txt")("text.plain")("") shouldBe s"$sanitisedFilename.txt"
        )
      }
      "return the original filename when filename contains no characters in the regexToSanitiseFileName" in {
        nonRegexTestCases.foreach(filename =>
          service.sanitisedAndTruncatedFileName(s"$filename.txt")("text.plain")("") shouldBe s"$filename.txt"
        )
      }
    }
    "the filename is greater than maximumFilenameLength" should {
      "return a truncated filename, maintaining the correct file type extension for all valid MIME types" in
        forAll(validFileTypes) { case (mimeType, fileExtension) =>
          service.sanitisedAndTruncatedFileName(longNameAppended("filename", fileExtension))(mimeType)("") shouldBe
            truncatedLongNameAppended("filename", fileExtension)
        }
      "return a sanitised and truncated filename when filename contains characters in the regexToSanitiseFileName" in {
        service.sanitisedAndTruncatedFileName(longNameAppended("file<>name", ""))("text.plain")("") shouldBe
          truncatedLongNameAppended("file__name", ".txt")
      }
    }
  }

}
