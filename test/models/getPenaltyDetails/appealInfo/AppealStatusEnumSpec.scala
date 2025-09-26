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
import models.hipPenaltyDetails.appealInfo.AppealStatusEnum
import play.api.libs.json._

import scala.util.Try

class AppealStatusEnumSpec extends SpecBase {

  "AppealStatusEnum" should {

    def writableTest(friendlyName: String, appealStatusEnum: AppealStatusEnum.Value, expectedResult: String): Unit =
      s"be writable to JSON for $friendlyName" in {
        val result = Json.toJson(appealStatusEnum)(AppealStatusEnum.format)
        result shouldBe JsString(expectedResult)
      }

    def readableTest(friendlyName: String, appealStatusValue: JsValue, expectedResult: AppealStatusEnum.Value): Unit =
      s"be readable from JSON for $friendlyName" in {
        val result = Json.fromJson(appealStatusValue)(AppealStatusEnum.format)
        result.isSuccess shouldBe true
        result.get shouldBe expectedResult
      }

    writableTest("UNDER_APPEAL", AppealStatusEnum.Under_Appeal, "A")
    writableTest("UPHELD", AppealStatusEnum.Upheld, "B")
    writableTest("REJECTED", AppealStatusEnum.Rejected, "C")
    writableTest("UNAPPEALABLE", AppealStatusEnum.Unappealable, "99")
    writableTest("CHARGE ALREADY REVERSED - Appeal Rejection", AppealStatusEnum.AppealUpheldChargeAlreadyReversed, "91")
    writableTest("POINT ALREADY REMOVED - Appeal Upheld", AppealStatusEnum.AppealCancelledPointAlreadyRemoved, "92")
    writableTest("CHARGE ALREADY REVERSED - Appeal Upheld", AppealStatusEnum.AppealCancelledChargeAlreadyReversed, "93")
    writableTest("POINT ALREADY REMOVED - Appeal Rejection", AppealStatusEnum.AppealUpheldPointAlreadyRemoved, "94")

    readableTest("UNDER_APPEAL", JsString("A"), AppealStatusEnum.Under_Appeal)
    readableTest("UPHELD", JsString("B"), AppealStatusEnum.Upheld)
    readableTest("REJECTED", JsString("C"), AppealStatusEnum.Rejected)
    readableTest("UNAPPEALABLE", JsString("99"), AppealStatusEnum.Unappealable)
    readableTest("CHARGE ALREADY REVERSED - Appeal Rejection", JsString("91"), AppealStatusEnum.AppealUpheldChargeAlreadyReversed)
    readableTest("POINT ALREADY REMOVED - Appeal Upheld", JsString("92"), AppealStatusEnum.AppealCancelledPointAlreadyRemoved)
    readableTest("CHARGE ALREADY REVERSED - Appeal Upheld", JsString("93"), AppealStatusEnum.AppealCancelledChargeAlreadyReversed)
    readableTest("POINT ALREADY REMOVED - Appeal Rejection", JsString("94"), AppealStatusEnum.AppealUpheldPointAlreadyRemoved)

    "return JsError when the enum is not recognised" in {
      val result = Json.fromJson(JsString("error"))(AppealStatusEnum.format)
      result.isError shouldBe true
    }
    "return JsError when the JsValue is not a recognised format" in {
      val booleanResult = Try(Json.fromJson(JsBoolean(true))(AppealStatusEnum.format))
      val numberResult  = Try(Json.fromJson(JsNumber(99))(AppealStatusEnum.format))

      booleanResult.isFailure shouldBe true
      numberResult.isFailure shouldBe true
    }
  }

}
