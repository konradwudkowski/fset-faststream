/*
 * Copyright 2017 HM Revenue & Customs
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

package model.exchange.candidateevents

import play.api.libs.json.{ Format, Json }

case class CandidateRemoveReason(key: String, description: String)

object CandidateRemoveReason {

  implicit val candidateRemoveReason: Format[CandidateRemoveReason] = Json.format[CandidateRemoveReason]

  val NoShow = "No-show"

  val Values = List(
    CandidateRemoveReason(NoShow, NoShow),
    CandidateRemoveReason("Candidate_contacted", "Candidate contacted"),
    CandidateRemoveReason("Expired", "Expired"),
    CandidateRemoveReason("Other", "Other")
  )

  def find(key: String): Option[CandidateRemoveReason] = Values.find(_.key == key)

}