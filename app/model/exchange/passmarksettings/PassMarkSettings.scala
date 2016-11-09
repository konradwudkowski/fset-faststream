/*
 * Copyright 2016 HM Revenue & Customs
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

package model.exchange.passmarksettings

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.Macros

trait PassMarkSettings {
  def schemes: List[PassMark]
  def version: String
  def createDate: DateTime
  def createdBy: String
}

case class Phase1PassMarkSettings(
  schemes: List[Phase1PassMark],
  version: String,
  createDate: DateTime,
  createdBy: String
) extends PassMarkSettings

object Phase1PassMarkSettings {
  import repositories.BSONDateTimeHandler
  implicit val phase1PassMarkSettingsFormat = Json.format[Phase1PassMarkSettings]
  implicit val phase1PassMarkSettingsHandler = Macros.handler[Phase1PassMarkSettings]
}

case class Phase2PassMarkSettings(
  schemes: List[Phase2PassMark],
  version: String,
  createDate: DateTime,
  createdBy: String
) extends PassMarkSettings

object Phase2PassMarkSettings {
  import repositories.BSONDateTimeHandler
  implicit val phase2PassMarkSettingsFormat = Json.format[Phase2PassMarkSettings]
  implicit val phase2PassMarkSettingsHandler = Macros.handler[Phase2PassMarkSettings]
}