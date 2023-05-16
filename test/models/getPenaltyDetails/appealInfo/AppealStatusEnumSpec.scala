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

package models.getPenaltyDetails.appealInfo

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class AppealStatusEnumSpec extends SpecBase {

  "AppealStatusEnum" should {

    def writableTest(friendlyName: String, appealStatusEnum: AppealStatusEnum.Value, expectedResult: String): Unit = {
      s"be writable to JSON for $friendlyName" in {
        val result = Json.toJson(appealStatusEnum)(AppealStatusEnum.format)
        result shouldBe JsString(expectedResult)
      }
    }

    def readableTest(friendlyName: String, appealStatusValue: String, expectedResult: AppealStatusEnum.Value): Unit = {
      s"be readable from JSON for $friendlyName" in {
        val result = Json.fromJson(JsString(appealStatusValue))(AppealStatusEnum.format)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }
    }

    writableTest("UNDER_APPEAL", AppealStatusEnum.Under_Appeal, "A")
    writableTest("UPHELD", AppealStatusEnum.Upheld, "B")
    writableTest("REJECTED", AppealStatusEnum.Rejected, "C")
    writableTest("UNAPPEALABLE", AppealStatusEnum.Unappealable, "99")
    writableTest("CHARGE ALREADY REVERSED - Appeal Rejection", AppealStatusEnum.AppealRejectedChargeAlreadyReversed, "91")
    writableTest("POINT ALREADY REMOVED - Appeal Upheld", AppealStatusEnum.AppealUpheldPointAlreadyRemoved, "92")
    writableTest("CHARGE ALREADY REVERSED - Appeal Upheld", AppealStatusEnum.AppealUpheldChargeAlreadyReversed, "93")
    writableTest("POINT ALREADY REMOVED - Appeal Rejection", AppealStatusEnum.AppealRejectedPointAlreadyRemoved, "94")

    readableTest("UNDER_APPEAL", "A", AppealStatusEnum.Under_Appeal)
    readableTest("UPHELD", "B", AppealStatusEnum.Upheld)
    readableTest("REJECTED", "C", AppealStatusEnum.Rejected)
    readableTest("UNAPPEALABLE", "99", AppealStatusEnum.Unappealable)
    readableTest("CHARGE ALREADY REVERSED - Appeal Rejection", "91", AppealStatusEnum.AppealRejectedChargeAlreadyReversed)
    readableTest("POINT ALREADY REMOVED - Appeal Upheld", "92", AppealStatusEnum.AppealUpheldPointAlreadyRemoved)
    readableTest("CHARGE ALREADY REVERSED - Appeal Upheld", "93", AppealStatusEnum.AppealUpheldChargeAlreadyReversed)
    readableTest("POINT ALREADY REMOVED - Appeal Rejection", "94", AppealStatusEnum.AppealRejectedPointAlreadyRemoved)

    "return JsError when the enum is not recognised" in {
      val result = Json.fromJson(JsString("error"))(AppealStatusEnum.format)
      result.isError shouldBe true
    }
  }

}
