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

package utils

import base.SpecBase
import config.AppConfig
import org.mockito.Matchers
import org.mockito.Mockito.{mock, when}

class FileHelperSpec extends SpecBase {
  val mockAppConfig: AppConfig = mock(classOf[AppConfig])

  "appendFileExtension" when {
    "the file name does not have an extension" should {
      "add the extension to the file name" in {
        when(mockAppConfig.getMimeType(Matchers.eq("text.plain"))).thenReturn(Some(".txt"))
        val result = FileHelper.appendFileExtension("file1")("text/plain")("ref1")(mockAppConfig)
        result shouldBe "file1.txt"
      }

      "throw an exception when there is no config entry for the mime type" in {
        when(mockAppConfig.getMimeType(Matchers.eq("fake.mime"))).thenReturn(None)
        val result = intercept[Exception](FileHelper.appendFileExtension("file1")("fake/mime")("ref1")(mockAppConfig))
        result.getMessage shouldBe "[FileHelper][appendFileExtension] - Unknown mime type: fake/mime for file reference: ref1"
      }
    }

    "the file name has an extension" should {
      "return the original file name" in {
        val result = FileHelper.appendFileExtension("file1.csv.txt")("text/plain")("ref1")(mockAppConfig)
        result shouldBe "file1.csv.txt"
      }
    }
  }

}
