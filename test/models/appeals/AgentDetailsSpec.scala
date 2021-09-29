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

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.emailaddress.EmailAddress

class AgentDetailsSpec extends SpecBase {

  val agentDetailsJson = Json.parse(
    """
      |{
      |   "agentReferenceNo": "1234567890",
      |   "name": "Jack",
      |   "addressLine1": "Flat 20",
      |   "addressLine2": "123 Jack street",
      |   "addressLine4": "Birmingham",
      |   "addressLine5": "UK",
      |   "postCode": "AAA AAA",
      |   "agentEmailID": "Jack@aaa.com"
      |}
      |""".stripMargin
  )
  val agentDetailsModel = AgentDetails(
    agentReferenceNo = "1234567890",
    name = "Jack",
    addressLine1 = "Flat 20",
    addressLine2 = Some("123 Jack street"),
    addressLine3 = None,
    addressLine4 = Some("Birmingham"),
    addressLine5 = Some("UK"),
    postCode = "AAA AAA",
    agentEmailID = Some(EmailAddress("Jack@aaa.com"))
  )

  "AgentDetails" should {
    "be readable from Json" in {
      val result = agentDetailsJson.as[AgentDetails]
      result shouldBe agentDetailsModel
    }

    "be writable to Json" in {
      val result = Json.toJson(agentDetailsModel)
      result shouldBe agentDetailsJson
    }
  }

}
