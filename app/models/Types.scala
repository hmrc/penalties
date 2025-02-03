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

package models

import play.api.mvc.PathBindable

case class AgnosticEnrolmentKey(regime: Regime, idType: IdType, id: Id) {
  override def toString: String = s"${regime.value}~${idType.value}~${id.value}"
}
object AgnosticEnrolmentKey {
  def fromEnrolmentKey(enrolmentKey: EnrolmentKey): AgnosticEnrolmentKey = {
    AgnosticEnrolmentKey(Regime(enrolmentKey.regime.toString), IdType(enrolmentKey.keyType.name), Id(enrolmentKey.key))
  }
}

case class Regime(value: String) extends AnyVal

object Regime {
  implicit val pathBindable: PathBindable[Regime] = new PathBindable[Regime] {
    override def bind(key: String, value: String): Either[String, Regime] = {
      Right(Regime(value))
    }

    override def unbind(key: String, regime: Regime): String = {
      regime.value
    }
  }
}

case class IdType(value: String) extends AnyVal

object IdType {
  implicit val pathBindable: PathBindable[IdType] = new PathBindable[IdType] {
    override def bind(key: String, value: String): Either[String, IdType] = {
      Right(IdType(value)) 
    }

    override def unbind(key: String, idType: IdType): String = {
      idType.value
    }
  }
}

case class Id(value: String) extends AnyVal

object Id {
  implicit val pathBindable: PathBindable[Id] = new PathBindable[Id] {
    override def bind(key: String, value: String): Either[String, Id] = {
      Right(Id(value))
    }

    override def unbind(key: String, id: Id): String = {
      id.value
    }
  }
}
