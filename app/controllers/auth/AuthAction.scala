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

package controllers.auth

import com.google.inject.Inject
import models.{CurrentUser, EnrolmentKey}
import utils.SessionKeys._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.EnrolmentUtil
import utils.EnrolmentUtil.{AuthReferenceExtractor, MtdVatEnrolmentKey, delegatedAuthRule, itsaEnrolmentKey, itsaRegex, vatRegex}
import utils.Logger.logger

import scala.concurrent.{ExecutionContext, Future}

class AuthAction @Inject()(val authConnector: AuthConnector, cc: ControllerComponents)
                          (implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {



  def authenticate(identifier: String)(f: Request[AnyContent] => CurrentUser => Future[Result]): Action[AnyContent] = Action.async{
    implicit request =>
      authorised().retrieve(affinityGroup and allEnrolments) {
        case Some(AffinityGroup.Agent) ~ _ =>
          logger.debug("Auth check - Authorising user as Agent")
          request.session.get(agentSessionId(identifier)) match {
            case Some(id) =>
              logger.debug("hello")
              authorised(delegatedAuthRule(identifier)).retrieve(allEnrolments) {
                enrolments =>
                  enrolments.agentReferenceNumber match {
                    case Some(arn) =>
                      f(request)(CurrentUser(id, Some(arn)))
                    case _ =>
                      logger.error("Auth check - Agent does not have HMRC-AS-AGENT enrolment")
                      throw InsufficientEnrolments("User does not have Agent Enrolment")
                  }
              }
          }
        case Some(_) ~ enrolments =>
          logger.debug("Auth check - Authorising user as Individual")
          enrolments.identifierId(identifier) match {
            case Some(identifierId) =>
              f(request)(CurrentUser(identifierId))
            case _ =>
              if (vatRegex.matches(identifier)) {
                logger.error(s"Auth check - User does not have an $MtdVatEnrolmentKey enrolment")
                throw InsufficientEnrolments(s"User does not have a $MtdVatEnrolmentKey Enrolment")

              } else {
                logger.error(s"Auth check - User does not have an $itsaEnrolmentKey enrolment")
                throw InsufficientEnrolments(s"User does not have a $itsaEnrolmentKey Enrolment")
              }
          }
        case _ =>
          logger.error("Auth check - Invalid affinity group")
          throw UnsupportedAffinityGroup("Invalid Affinity Group")
      }.recover {
        case _: NoActiveSession =>
          logger.error(s"Auth check - No active session. Redirecting to http://localhost:9949/auth-login-stub/gg-sign-in")
          Redirect("http://localhost:9949/auth-login-stub/gg-sign-in")
        case _: AuthorisationException =>
          logger.error(s"Auth check - User not authorised. Redirecting to http://localhost:9949/auth-login-stub/gg-sign-in")
          Redirect("http://localhost:9949/auth-login-stub/gg-sign-in")
      }
  }
}
