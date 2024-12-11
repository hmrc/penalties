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

package models

import play.api.mvc.PathBindable

object TaxRegime extends Enumeration {
  type TaxRegime = Value

  val VAT: Value = Value("VATC")
  val ITSA: Value = Value("ITSA")
  //val CT: Value = Value("CT")

  /**
   * Support using this enum in route paths
   * @see "https://cjwebb.com/play-framework-path-binders/"
   */
  implicit val pathBindable: PathBindable[TaxRegime] = new PathBindable[TaxRegime] {
    override def bind(key: String, value: String): Either[String, TaxRegime] =
      TaxRegime.values.find(_.toString == value).toRight("invalid value")

    override def unbind(key: String, value: TaxRegime): String =
      implicitly[PathBindable[String]].unbind(key, value.toString)
  }
}