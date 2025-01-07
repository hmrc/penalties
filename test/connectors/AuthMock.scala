/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors.mock

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments, InternalError, MissingBearerToken}
import utils.EnrolmentUtil

import scala.concurrent.Future

trait AuthMock extends MockitoSugar {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  def mockAuthenticatedAgent(): Unit =
    when(mockAuthConnector.authorise[~[Option[AffinityGroup], Enrolments]](
      eqTo(EmptyPredicate), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(
      any(), any())
    ).thenReturn(
      Future.successful(new ~(Some(AffinityGroup.Agent), Enrolments(
        Set(Enrolment(
          key = "HMRC-AS-AGENT",
          identifiers = Seq(EnrolmentIdentifier("AgentReferenceNumber", "1234567")),
          state = "Activated",
          delegatedAuthRule = Some("mtd-it-auth")
        ))
      )))
    )

  def mockAuthenticatedAgentEnrolment(mtdItId: String): Unit =
    when(mockAuthConnector.authorise[Enrolments](
      eqTo(EnrolmentUtil.delegatedAuthRule(mtdItId)), any[Retrieval[Enrolments]]())(
      any(), any())
    ).thenReturn(
      Future.successful( Enrolments(
        Set(Enrolment(
          key = "HMRC-AS-AGENT",
          identifiers = Seq(EnrolmentIdentifier("AgentReferenceNumber", "1234567")),
          state = "Activated",
          delegatedAuthRule = Some("mtd-it-auth")
        ))
      ))
    )

  def mockAuthenticatedIndividual(): Unit =
    when(mockAuthConnector.authorise[~[Option[AffinityGroup], Enrolments]](
      any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(
      any(), any())
    ).thenReturn(
      Future.successful(new ~(Some(AffinityGroup.Individual), Enrolments(
        Set(Enrolment(
          key = "HMRC-MTD-IT",
          identifiers = Seq(EnrolmentIdentifier("MTDITID", "1234567")),
          state = "Activated"
        ))
      )))
    )

  def mockAuthenticatedWithNoAffinityGroup(): Unit =
    when(mockAuthConnector.authorise[~[Option[AffinityGroup], Enrolments]](
      any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(
      any(), any())
    ).thenReturn(
      Future.successful(new ~(None, Enrolments(
        Set(Enrolment(
          key = "HMRC-MTD-IT",
          identifiers = Seq(EnrolmentIdentifier("MTDITID", "1234567")),
          state = "Activated"
        ))
      )))
    )

  def mockAuthenticatedNoActiveSession(): Unit =
    when(mockAuthConnector.authorise[~[Option[AffinityGroup], Enrolments]](
      any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(
      any(), any())
    ).thenReturn(Future.failed(MissingBearerToken("No token")))


  def mockAuthenticatedFailure(): Unit =
    when(mockAuthConnector.authorise[~[Option[AffinityGroup], Enrolments]](
      any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(
      any(), any())
    ).thenReturn(Future.failed(InternalError("There has been an error")))


}
