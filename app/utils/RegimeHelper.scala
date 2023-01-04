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

object RegimeHelper {

  def getIdentifierTypeFromEnrolmentKey(enrolmentKey: String): String = {
    enrolmentKey match {
      case key if key.contains("MTD-VAT") => "VRN"
      case _ => ""
    }
  }

  def getIdentifierFromEnrolmentKey(enrolmentKey: String): String = {
    enrolmentKey match {
      case key if key.contains("MTD-VAT") => key.split("~").last
      case _ => ""
    }
  }

  def constructMTDVATEnrolmentKey(vrn: String): String = s"HMRC-MTD-VAT~VRN~$vrn"

}
