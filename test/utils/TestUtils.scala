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

package utils

import org.scalactic.source.Position.here
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Assertion, Succeeded}

import scala.util.{Failure, Success, Try}

object TestUtils {
  implicit class TryPlus[T](t: Try[T]) {
    def shouldFailWith(message: String): Assertion = {
      t match {
        case Failure(value) if value.getMessage == message => Succeeded
        case Failure(value) =>
          throw new TestFailedException(_ => Some(s""""${value.getMessage}" did not match "$message""""), None, here)
        case Success(value) =>
          throw new TestFailedException(_ => Some(s"""Succeeded unexpectedly with $value"""), None, here)
      }
    }

    def shouldFailWith(exception: Throwable): Assertion = {
      t match {
        case Failure(value) if value.getClass == exception.getClass && value.getMessage == exception.getMessage => Succeeded
        case Failure(value) =>
          val message = s"""${value.getClass.getSimpleName} "${value.getMessage}" did not match ${exception.getClass.getSimpleName} "${exception.getMessage}""""
          throw new TestFailedException(_ => Some(message), None, here)
        case Success(value) =>
          throw new TestFailedException(_ => Some(s"""Succeeded unexpectedly with $value"""), None, here)
      }
    }
  }
}
