/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors.parsers

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import utils.Logger.logger

import scala.util.{Try, Success, Failure}

trait SafeHttpReads {

  protected val MaxBodyLength = 300

  protected def parseJson(body: String): Option[JsValue] =
    Try(Json.parse(body)) match {
      case Success(json) => Some(json)
      case Failure(_)    => None
    }

  protected def safeBody(response: HttpResponse): String =
    Option(response.body).getOrElse("").trim

  protected def classifyNonJson(body: String): String = body match {
    case b if b.contains("Send timeout")      => "Timeout from downstream"
    case b if b.contains("502 Bad Gateway")   => "Bad Gateway"
    case b if b.toLowerCase.contains("<html") => "HTML response"
    case ""                                   => "Empty response"
    case b                                    => b.take(MaxBodyLength)
  }

  protected def handleErrorResponseSafe(response: HttpResponse, parserName: String): Unit = {
    val body = safeBody(response)
    parseJson(body) match {
      case Some(json) =>
        logger.info(s"[$parserName] Parsed downstream error: ${json.toString().take(MaxBodyLength)}")

      case None =>
        logger.info(s"[$parserName] Non-JSON error: ${classifyNonJson(body)}")
    }
  }
}