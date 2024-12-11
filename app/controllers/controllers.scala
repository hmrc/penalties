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

import models.EnrolmentKey
import models.EnrolmentKey._
import models.TaxRegime._
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest

import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.util.{Failure, Try}

package object controllers {
  implicit class TryPlus(t: Try[EnrolmentKey]) {
    def andThen(block: EnrolmentKey => Future[Result]): Future[Result] = {
      t.fold(e=>successful(BadRequest(e.getMessage)), block)
    }
  }

  private[controllers]
  def composeEnrolmentKey(regime: String, keyType: String, key: String): Try[EnrolmentKey] = {
    (regime.toUpperCase, keyType.toUpperCase) match {
      case ("VAT", "VRN") | ("VATC", "VRN") => Try(EnrolmentKey(VAT, VRN, key))
      case ("VAT", _) | ("VATC", _) => Failure(new Exception(s"Unsupported id type: $keyType"))
      //case ("CT", "UTR") | ("COTAX", "UTR") => Try(EnrolmentKey(CT, UTR, key))
      //case ("CT", _) | ("COTAX", _)  => Failure(new Exception(s"Unsupported id type: $keyType"))
      case /*("IT", "NINO") |*/ ("ITSA", "NINO")  => Try(EnrolmentKey(ITSA, NINO, key))
      case /*("IT", _) |*/ ("ITSA", _)  => Failure(new Exception(s"Unsupported id type: $keyType"))
      case (other, _) => Failure(new Exception(s"Unsupported regime: $other"))
    }
  }

  private[controllers]
  def composeEnrolmentKey(regime: String, key: String): Try[EnrolmentKey] = {
    regime.toUpperCase match {
      case "VAT" | "VATC" => Try(EnrolmentKey(VAT, VRN, key))
      case "IT" | "ITSA" => Try(EnrolmentKey(ITSA, NINO, key))
      case other => Failure(new Exception(s"Unsupported regime: $other"))
    }
  }
}