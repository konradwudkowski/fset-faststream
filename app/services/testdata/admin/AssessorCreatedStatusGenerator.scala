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

package services.testdata.admin

import model.persisted.assessor.AssessorStatus
import model.exchange.testdata.CreateAdminResponse.{ AssessorResponse, CreateAdminResponse }
import model.testdata.CreateAdminData.{ AssessorData, CreateAdminData }
import play.api.mvc.RequestHeader
import services.assessoravailability.AssessorService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object AssessorCreatedStatusGenerator extends AssessorCreatedStatusGenerator {
  override val previousStatusGenerator: AdminUserBaseGenerator = AdminCreatedStatusGenerator
  override val assessorService = AssessorService
}

trait AssessorCreatedStatusGenerator extends AdminUserConstructiveGenerator {

  import scala.concurrent.ExecutionContext.Implicits.global

  val assessorService: AssessorService

  def generate(generationId: Int, createData: CreateAdminData)(implicit hc: HeaderCarrier, rh: RequestHeader): Future[CreateAdminResponse] = {
    previousStatusGenerator.generate(generationId, createData).flatMap { userInPreviousStatus =>
      createData.assessor.map { assessorData =>
        val userId = userInPreviousStatus.userId
        for {
          assessorPersisted <- createAssessor(userId, assessorData)
          availability = assessorData.availability.getOrElse(Nil)
          _ <- assessorService.addAvailability(userId, availability)
        } yield {
          userInPreviousStatus.copy(assessor  = Some(AssessorResponse.apply(assessorPersisted).copy(availability = availability)))
        }
      }.getOrElse(Future.successful(userInPreviousStatus))
    }
  }

  def createAssessor(userId: String, assessor: AssessorData)(
    implicit hc: HeaderCarrier): Future[model.exchange.Assessor] = {
    val assessorE = model.exchange.Assessor(userId, assessor.skills, assessor.sifterSchemes, assessor.civilServant, AssessorStatus.CREATED)
    assessorService.saveAssessor(userId, assessorE).map(_ => assessorE)
  }

}
