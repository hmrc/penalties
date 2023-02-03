/*
 * Copyright 2023 HM Revenue & Customs
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

package models.notification

import play.api.libs.json.{Json, Writes}

case class SDESNotificationFile(
                                 recipientOrSender: String,
                                 name: String,
                                 location: String,
                                 checksum: SDESChecksum,
                                 size: Int,
                                 properties: Seq[SDESProperties]
                               )

object SDESNotificationFile {
  implicit val writes: Writes[SDESNotificationFile] = Json.writes[SDESNotificationFile]
  val auditWrites: Writes[SDESNotificationFile] = (notificationFile: SDESNotificationFile) => Json.obj(
    "recipientOrSender" -> notificationFile.recipientOrSender,
    "name" -> notificationFile.name,
    "location" -> notificationFile.location,
    "checksum" -> notificationFile.checksum,
    "size" -> notificationFile.size,
    "properties" -> Json.toJson(notificationFile.properties)(SDESProperties.auditSeqWrites)
  )
}

case class SDESProperties(name: String, value: String)

object SDESProperties {
  implicit val writes: Writes[SDESProperties] = Json.writes[SDESProperties]
  val auditSeqWrites: Writes[Seq[SDESProperties]] =
    (properties: Seq[SDESProperties]) => Json.toJson(properties)(Writes.seq(auditWrites))
  private val auditWrites: Writes[SDESProperties] = (properties: SDESProperties) => Json.obj(properties.name -> properties.value)
}

case class SDESChecksum(algorithm: String, value: String)

object SDESChecksum {
  implicit val writes: Writes[SDESChecksum] = Json.writes[SDESChecksum]
}
