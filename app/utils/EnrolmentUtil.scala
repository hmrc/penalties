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

package utils

import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}


object EnrolmentUtil {

  val vatRegex = "^[0-9]{9}|[0-9]{12}$".r
  val itsaRegex = "^[ABCEGHJKLMNOPRSTWXYZ][ABCEGHJKLMNPRSTWXYZ][0-9]{6}[A-D\\s]$".r

  val MtdVatEnrolmentKey: String = "HMRC-MTD-VAT"
  val MtdVatReferenceKey: String = "VRN"
  val MtdVatDelegatedAuth: String = "mtd-vat-auth"

  val itsaEnrolmentKey = "HMRC-MTD-IT"
  val itsaReferenceKey = "MTDITID"
  val itsaDelegatedAuth: String = "mtd-it-auth"


  val AgentServicesEnrolment: String = "HMRC-AS-AGENT"
  val AgentServicesReference: String = "AgentReferenceNumber"

   def delegatedAuthRule(regime: String): Enrolment = {
    if (vatRegex.matches(regime)) {
      Enrolment(EnrolmentUtil.MtdVatEnrolmentKey)
        .withIdentifier(EnrolmentUtil.MtdVatReferenceKey, regime)
        .withDelegatedAuthRule(EnrolmentUtil.MtdVatDelegatedAuth)
    } else if (itsaRegex.matches(regime)){
      Enrolment(EnrolmentUtil.itsaEnrolmentKey)
        .withIdentifier(EnrolmentUtil.itsaReferenceKey, regime)
        .withDelegatedAuthRule(EnrolmentUtil.itsaDelegatedAuth)
    } else {
      Enrolment(EnrolmentUtil.itsaEnrolmentKey)
        .withIdentifier(EnrolmentUtil.itsaReferenceKey, regime)
        .withDelegatedAuthRule(EnrolmentUtil.itsaDelegatedAuth)
    }
  }

  implicit class AuthReferenceExtractor(enrolments: Enrolments) {

    def agentReferenceNumber: Option[String] =
      for {
        agentEnrolment <- enrolments.getEnrolment(AgentServicesEnrolment)
        identifier <- agentEnrolment.getIdentifier(AgentServicesReference)
        arn = identifier.value
      } yield arn

    def identifierId(regime: String): Option[String] = {
      if (vatRegex.matches(regime)) {
        for {
          incomeTaxEnrolment <- enrolments.getEnrolment(MtdVatEnrolmentKey)
          identifier <- incomeTaxEnrolment.getIdentifier(MtdVatReferenceKey)
          vrn = identifier.value
        } yield vrn
      } else if (itsaRegex.matches(regime)) {
        for {
          incomeTaxEnrolment <- enrolments.getEnrolment(itsaEnrolmentKey)
          identifier <- incomeTaxEnrolment.getIdentifier(itsaReferenceKey)
          mtdItId = identifier.value
        } yield mtdItId
      } else {
        for {
          incomeTaxEnrolment <- enrolments.getEnrolment(itsaEnrolmentKey)
          identifier <- incomeTaxEnrolment.getIdentifier(itsaReferenceKey)
          mtdItId = identifier.value
        } yield mtdItId
      }
    }
  }

}
