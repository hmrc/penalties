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

package models.getFinancialDetails

import base.SpecBase
import play.api.libs.json.{JsObject, Json}

class FinancialDetailsRequestSpec extends SpecBase {

  "be readable from JSON" in {
    val result = Json.fromJson(financialDetailsRequestMaxJson)(FinancialDetailsRequestModel.format)
    result.isSuccess shouldBe true
    result.get shouldBe financialDetailsRequestMaxModel
  }

  "be writable to JSON" in {
    val result = Json.toJson(financialDetailsRequestMaxModel)(FinancialDetailsRequestModel.format)
    result shouldBe financialDetailsRequestMaxJson
  }

  val jsonMinRequest: JsObject = Json.obj(
    "taxRegime" -> enrolmentKey.regime.value,
    "taxpayerInformation" -> Json.obj(
      "idType"   -> enrolmentKey.idType.value,
      "idNumber" -> enrolmentKey.id.value
    )
  )
  val targetedSearch: JsObject = Json.obj(
    "targetedSearch" -> Json.obj(
      "searchType" -> "CHGREF",
      "searchItem" -> "XC00178236592"
    )
  )
  val selectionCriteriaWithoutDateRange: JsObject = Json.obj(
    "selectionCriteria" -> Json.obj(
      "includeClearedItems"     -> true,
      "includeStatisticalItems" -> true,
      "includePaymentOnAccount" -> true
    )
  )
  val selectionCriteriaWithDateRange: JsObject = Json.obj(
    "selectionCriteria" -> Json.obj(
      "includeClearedItems"     -> true,
      "includeStatisticalItems" -> true,
      "includePaymentOnAccount" -> true,
      "dateRange" -> Json.obj(
        "dateType" -> "POSTED",
        "dateFrom" -> "2024-07-12",
        "dateTo"   -> "2025-07-12"
      )
    )
  )
  val dataEnrichment: JsObject = Json.obj(
    "dataEnrichment" -> Json.obj(
      "addRegimeTotalisation"      -> true,
      "addLockInformation"         -> true,
      "addPenaltyDetails"          -> true,
      "addPostedInterestDetails"   -> true,
      "addAccruingInterestDetails" -> true
    )
  )

  val modelWithRequiredSelectionCriteriaParams: FinancialDetailsRequestModel = FinancialDetailsRequestModel.emptyModel.copy(
    includeClearedItems = Some(true),
    includeStatisticalItems = Some(true),
    includePaymentOnAccount = Some(true))
  val modelWithDataEnrichmentParams: FinancialDetailsRequestModel = FinancialDetailsRequestModel.emptyModel.copy(
    addRegimeTotalisation = Some(true),
    addLockInformation = Some(true),
    addPenaltyDetails = Some(true),
    addPostedInterestDetails = Some(true),
    addAccruingInterestDetails = Some(true)
  )

  "toJsonRequest" should {
    "build the Json request body" which {
      "only contains the base body" when {
        "no other parameters are given" in {
          val result = FinancialDetailsRequestModel.emptyModel.toJsonRequest(enrolmentKey)

          result shouldBe jsonMinRequest
        }
        "some but not all required targetedSearch parameters are given, so they are not included" in {
          val resultWithoutSearchItem = FinancialDetailsRequestModel.emptyModel.copy(searchType = Some("CHGREF")).toJsonRequest(enrolmentKey)
          val resultWithoutSearchType = FinancialDetailsRequestModel.emptyModel.copy(searchItem = Some("XC00178236592")).toJsonRequest(enrolmentKey)

          resultWithoutSearchItem shouldBe jsonMinRequest
          resultWithoutSearchType shouldBe jsonMinRequest
        }
        "some but not all required selectionCriteria parameters are given, so they are not included" in {
          val resultWithoutIncludeClearedItems =
            modelWithRequiredSelectionCriteriaParams.copy(includeClearedItems = None).toJsonRequest(enrolmentKey)
          val resultWithoutIncludeStatisticalItems =
            modelWithRequiredSelectionCriteriaParams.copy(includeStatisticalItems = None).toJsonRequest(enrolmentKey)
          val resultWithoutIncludePaymentOnAccount =
            modelWithRequiredSelectionCriteriaParams.copy(includePaymentOnAccount = None).toJsonRequest(enrolmentKey)

          val errorResults = Seq(
            resultWithoutIncludeClearedItems,
            resultWithoutIncludeStatisticalItems,
            resultWithoutIncludePaymentOnAccount
          ).filterNot(_ == jsonMinRequest)
          errorResults shouldBe empty
        }
        "some but not all required dataEnrichment parameters are given, so they are not included" in {
          val resultWithoutAddRegimeTotalisation    = modelWithDataEnrichmentParams.copy(addRegimeTotalisation = None).toJsonRequest(enrolmentKey)
          val resultWithoutAddLockInformation       = modelWithDataEnrichmentParams.copy(addLockInformation = None).toJsonRequest(enrolmentKey)
          val resultWithoutAddPenaltyDetails        = modelWithDataEnrichmentParams.copy(addPenaltyDetails = None).toJsonRequest(enrolmentKey)
          val resultWithoutAddPostedInterestDetails = modelWithDataEnrichmentParams.copy(addPostedInterestDetails = None).toJsonRequest(enrolmentKey)
          val resultWithoutAddAccruingInterestDetails = modelWithDataEnrichmentParams.copy(addRegimeTotalisation = None).toJsonRequest(enrolmentKey)

          val errorResults = Seq(
            resultWithoutAddRegimeTotalisation,
            resultWithoutAddLockInformation,
            resultWithoutAddPenaltyDetails,
            resultWithoutAddPostedInterestDetails,
            resultWithoutAddAccruingInterestDetails
          ).filterNot(_ == jsonMinRequest)
          errorResults shouldBe empty
        }
      }
      "includes targetedSearch when the correct targetedSearch parameters are given" in {
        val result =
          FinancialDetailsRequestModel.emptyModel.copy(searchType = Some("CHGREF"), searchItem = Some("XC00178236592")).toJsonRequest(enrolmentKey)

        result shouldBe jsonMinRequest ++ targetedSearch
      }
      "includes selectionCriteria when the full correct selectionCriteria parameters are given" in {
        val result =
          modelWithRequiredSelectionCriteriaParams
            .copy(dateType = Some("POSTED"), dateFrom = Some("2024-07-12"), dateTo = Some("2025-07-12"))
            .toJsonRequest(enrolmentKey)

        result shouldBe jsonMinRequest ++ selectionCriteriaWithDateRange
      }
      "includes selectionCriteria but not selectionCriteria's inner dateRange when some parameter's are missing" in {
        val modelWithFullSelectionCriteriaParams =
          modelWithRequiredSelectionCriteriaParams.copy(dateType = Some("POSTED"), dateFrom = Some("2024-07-12"), dateTo = Some("2025-07-12"))
        val resultWithoutDateType = modelWithFullSelectionCriteriaParams.copy(dateType = None).toJsonRequest(enrolmentKey)
        val resultWithoutDateFrom = modelWithFullSelectionCriteriaParams.copy(dateFrom = None).toJsonRequest(enrolmentKey)
        val resultWithoutDateTo   = modelWithFullSelectionCriteriaParams.copy(dateTo = None).toJsonRequest(enrolmentKey)

        val errorResults = Seq(
          resultWithoutDateType,
          resultWithoutDateFrom,
          resultWithoutDateTo
        ).filterNot(_ == jsonMinRequest ++ selectionCriteriaWithoutDateRange)
        errorResults shouldBe empty
      }
      "includes dataEnrichment when the correct dataEnrichment parameters are given" in {
        val result = modelWithDataEnrichmentParams.toJsonRequest(enrolmentKey)

        result shouldBe jsonMinRequest ++ dataEnrichment
      }
    }
  }
}
