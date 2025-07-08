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

import models.AgnosticEnrolmentKey
import play.api.libs.json.{Format, JsObject, Json}

case class FinancialDetailsRequestModel(searchType: Option[String],
                                        searchItem: Option[String],
                                        dateType: Option[String],
                                        dateFrom: Option[String],
                                        dateTo: Option[String],
                                        includeClearedItems: Option[Boolean],
                                        includeStatisticalItems: Option[Boolean],
                                        includePaymentOnAccount: Option[Boolean],
                                        addRegimeTotalisation: Option[Boolean],
                                        addLockInformation: Option[Boolean],
                                        addPenaltyDetails: Option[Boolean],
                                        addPostedInterestDetails: Option[Boolean],
                                        addAccruingInterestDetails: Option[Boolean]) {

  def toJsonRequest(enrolmentKey: AgnosticEnrolmentKey): JsObject = {
    val baseBody: JsObject = Json.obj(
      "taxRegime" -> enrolmentKey.regime.value,
      "taxpayerInformation" -> Json.obj(
        "idType"   -> enrolmentKey.idType.value,
        "idNumber" -> enrolmentKey.id.value
      )
    )
    val targetedSearchObj: Option[JsObject] = for {
      sType <- searchType
      sItem <- searchItem
    } yield Json.obj(
      "targetedSearch" -> Json.obj(
        "searchType" -> sType,
        "searchItem" -> sItem
      )
    )
    val selectionCriteriaObj: Option[JsObject] = {
      val dateRangeObj: Option[JsObject] = for {
        dType <- dateType
        dFrom <- dateFrom
        dTo   <- dateTo
      } yield Json.obj(
        "dateRange" -> Json.obj(
          "dateType" -> dType,
          "dateFrom" -> dFrom,
          "dateTo"   -> dTo
        )
      )
      for {
        clearedItems     <- includeClearedItems
        statisticalItems <- includeStatisticalItems
        paymentOnAccount <- includePaymentOnAccount
      } yield {
        val innerJson = Json.obj(
          "includeClearedItems"     -> clearedItems,
          "includeStatisticalItems" -> statisticalItems,
          "includePaymentOnAccount" -> paymentOnAccount
        ) ++ dateRangeObj.getOrElse(Json.obj())
        Json.obj("selectionCriteria" -> innerJson)
      }
    }
    val dataEnrichmentObj: Option[JsObject] = for {
      regimeTotalisation      <- addRegimeTotalisation
      lockInformation         <- addLockInformation
      penaltyDetails          <- addPenaltyDetails
      postedInterestDetails   <- addPostedInterestDetails
      accruingInterestDetails <- addAccruingInterestDetails
    } yield Json.obj(
      "dataEnrichment" -> Json.obj(
        "addRegimeTotalisation"      -> regimeTotalisation,
        "addLockInformation"         -> lockInformation,
        "addPenaltyDetails"          -> penaltyDetails,
        "addPostedInterestDetails"   -> postedInterestDetails,
        "addAccruingInterestDetails" -> accruingInterestDetails
      )
    )

    def combine(base: JsObject, extras: Option[JsObject]*): JsObject =
      extras.foldLeft(base)(_ ++ _.getOrElse(Json.obj()))

    combine(baseBody, targetedSearchObj, selectionCriteriaObj, dataEnrichmentObj)
  }

  def toJsonRequestQueryParams: String = {
    val params: Seq[(String, Option[_])] = Seq(
      "searchType"                 -> searchType,
      "searchItem"                 -> searchItem,
      "dateType"                   -> dateType,
      "dateFrom"                   -> dateFrom,
      "dateTo"                     -> dateTo,
      "includeClearedItems"        -> includeClearedItems,
      "includeStatisticalItems"    -> includeStatisticalItems,
      "includePaymentOnAccount"    -> includePaymentOnAccount,
      "addRegimeTotalisation"      -> addRegimeTotalisation,
      "addLockInformation"         -> addLockInformation,
      "addPenaltyDetails"          -> addPenaltyDetails,
      "addPostedInterestDetails"   -> addPostedInterestDetails,
      "addAccruingInterestDetails" -> addAccruingInterestDetails
    )
    params.foldLeft("?")((prevString, paramToValue) => prevString + paramToValue._2.fold("")(param => s"${paramToValue._1}=$param&")).dropRight(1)
  }
  def toJsonRequestQueryParamsMap: Seq[(String, String)] =
    Seq(
      "searchType"                 -> searchType,
      "searchItem"                 -> searchItem,
      "dateType"                   -> dateType,
      "dateFrom"                   -> dateFrom,
      "dateTo"                     -> dateTo,
      "includeClearedItems"        -> includeClearedItems,
      "includeStatisticalItems"    -> includeStatisticalItems,
      "includePaymentOnAccount"    -> includePaymentOnAccount,
      "addRegimeTotalisation"      -> addRegimeTotalisation,
      "addLockInformation"         -> addLockInformation,
      "addPenaltyDetails"          -> addPenaltyDetails,
      "addPostedInterestDetails"   -> addPostedInterestDetails,
      "addAccruingInterestDetails" -> addAccruingInterestDetails
    ).collect { case (key, Some(value)) =>
      key -> value.toString
    }
}

object FinancialDetailsRequestModel {
  implicit val format: Format[FinancialDetailsRequestModel] = Json.format[FinancialDetailsRequestModel]

  val emptyModel: FinancialDetailsRequestModel =
    FinancialDetailsRequestModel(None, None, None, None, None, None, None, None, None, None, None, None, None)
}
