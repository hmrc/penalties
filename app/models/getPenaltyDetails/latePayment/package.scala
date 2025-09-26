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

package models.getPenaltyDetails

package object latePayment {

  object PrincipalChargeMainTr {
    val VATReturnCharge               = "4700"
    val VATReturnFirstLPP             = "4703"
    val VATReturnSecondLPP            = "4704"
    val CentralAssessment             = "4720"
    val CentralAssessmentFirstLPP     = "4723"
    val CentralAssessmentSecondLPP    = "4724"
    val OfficersAssessment            = "4730"
    val OfficersAssessmentFirstLPP    = "4741"
    val OfficersAssessmentSecondLPP   = "4742"
    val ErrorCorrection               = "4731"
    val ErrorCorrectionFirstLPP       = "4743"
    val ErrorCorrectionSecondLPP      = "4744"
    val AdditionalAssessment          = "4732"
    val AdditionalAssessmentFirstLPP  = "4758"
    val AdditionalAssessmentSecondLPP = "4759"
    val ProtectiveAssessment          = "4733"
    val ProtectiveAssessmentFirstLPP  = "4761"
    val ProtectiveAssessmentSecondLPP = "4762"
    val POAReturnCharge               = "4701"
    val POAReturnChargeFirstLPP       = "4716"
    val POAReturnChargeSecondLPP      = "4717"
    val AAReturnCharge                = "4702"
    val AAReturnChargeFirstLPP        = "4718"
    val AAReturnChargeSecondLPP       = "4719"
    val ManualLPP                     = "4787"
    val VATOverpaymentForTax          = "4764"
    val Unknown                       = "UNKNOWN"
  }
}
