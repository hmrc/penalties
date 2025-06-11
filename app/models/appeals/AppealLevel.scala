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

package models.appeals

import play.api.libs.json._
import play.api.mvc.QueryStringBindable

//object AppealLevelEnum extends Enumeration {
//  val FirstStageAppeal: AppealLevelEnum.Value  = Value("01")
//  val SecondStageAppeal: AppealLevelEnum.Value = Value("02")
//
//  implicit val format: Format[AppealLevelEnum.Value] = new Format[AppealLevelEnum.Value] {
//    override def writes(o: AppealLevelEnum.Value): JsValue = JsString(o.toString)
//
//    override def reads(json: JsValue): JsResult[AppealLevelEnum.Value] =
//      json.as[String] match {
//        case "01" => JsSuccess(FirstStageAppeal)
//        case "02" => JsSuccess(SecondStageAppeal)
//        case e    => JsError(s"AppealLevel '$e' is not recognised")
//      }
//  }
//
//  implicit val queryStringBindable: QueryStringBindable[AppealLevelEnum.Value] = new QueryStringBindable[AppealLevelEnum.Value] {
//    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AppealLevelEnum.Value]] =
//      params.get(key).flatMap(_.headOption).map {
//        case "01"  => Right(FirstStageAppeal)
//        case "02"  => Right(SecondStageAppeal)
//        case other => Left(s"Invalid appealLevel: '$other'. Accepted appealLevel values are: '01' or '02'.")
//      }
//
//    override def unbind(key: String, value: AppealLevelEnum.Value): String = s"$key=${value.toString}"
//  }
//}

sealed abstract class AppealLevel(val value: String) {
  override val toString: String = value
}
//sealed trait AppealLevel {
//  def value: String
//  override def toString: String = value
//}

object AppealLevel {
  case object FirstStageAppeal extends AppealLevel("01")

  case object SecondStageAppeal extends AppealLevel("01")
//  case object FirstStageAppeal extends AppealLevel {
//    val value: String = "01"
//  }
//
//  case object SecondStageAppeal extends AppealLevel {
//    val value: String = "02"
//  }

  val values: Seq[AppealLevel] = Seq(FirstStageAppeal, SecondStageAppeal)

  def fromString(str: String): Option[AppealLevel] =
    values.find(_.value == str)

  implicit val format: Format[AppealLevel] = new Format[AppealLevel] {
    def writes(o: AppealLevel): JsValue = JsString(o.value)

    def reads(json: JsValue): JsResult[AppealLevel] = json match {
      case JsString(str) =>
        fromString(str) match {
          case Some(enum) => JsSuccess(enum)
          case None       => JsError(s"AppealLevel '$str' not recognised")
        }
      case _ => JsError("String value expected for AppealLevel")
    }
  }

  implicit val queryStringBindable: QueryStringBindable[AppealLevel] =
    new QueryStringBindable[AppealLevel] {
      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AppealLevel]] =
        params.get(key).flatMap(_.headOption).map { value =>
          AppealLevel.fromString(value).toRight(s"Invalid appealLevel: '$value'")
        }

      def unbind(key: String, level: AppealLevel): String =
        s"$key=${level.value}"
    }
}
