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

package model.persisted

import play.api.libs.json.Json
import reactivemongo.bson.{ BSONDocument, BSONDocumentReader, Macros }

case class FsbEvaluation(result: List[SchemeEvaluationResult])

object FsbEvaluation {
  implicit val jsonFormat = Json.format[FsbEvaluation]
  implicit val bsonHandler = Macros.handler[FsbEvaluation]
}

case class FsbTestGroup(evaluation: FsbEvaluation)

object FsbTestGroup {
  implicit val jsonFormat = Json.format[FsbTestGroup]
  implicit val bsonHandler = Macros.handler[FsbTestGroup]

  def apply(results: List[SchemeEvaluationResult]): FsbTestGroup = new FsbTestGroup(FsbEvaluation(results))

}

case class FsbSchemeResult(applicationId: String, results: List[SchemeEvaluationResult])

object FsbSchemeResult {
  implicit val jsonFormat = Json.format[FsbSchemeResult]

  implicit object FsbResultReader extends BSONDocumentReader[Option[FsbSchemeResult]] {
    def read(document: BSONDocument): Option[FsbSchemeResult] = {
      val applicationId = document.getAs[String]("applicationId").get
      val testGroups = document.getAs[BSONDocument]("testGroups").get
      val fsbResult = testGroups.getAs[FsbTestGroup]("FSB").map {fsbTestGroup =>
        FsbSchemeResult(applicationId, fsbTestGroup.evaluation.result)
      }
      fsbResult
    }
  }

}