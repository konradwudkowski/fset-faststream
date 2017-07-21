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

package controllers

import config.TestFixtureBase
import factories.DateTimeFactory
import model.Exceptions.CannotUpdateRecord
import model.assessmentscores.{ AssessmentScoresAllExercisesExamples, AssessmentScoresExerciseExamples }
import model.command.AssessmentScoresCommands._
import org.joda.time.DateTimeZone
import org.mockito.ArgumentMatchers.{ eq => eqTo, _ }
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import repositories.AssessmentScoresRepository
import services.assessmentscores.AssessmentScoresService
import testkit.UnitWithAppSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.language.postfixOps

class AssessmentScoresControllerSpec extends UnitWithAppSpec {

  "submit" should {
    "save exercise, send AssessmentScoresOneExerciseSubmitted audit event and return OK" in new TestFixture {
      val exerciseScores = AssessmentScoresExerciseExamples.Example1.copy(
        submittedDate = AssessmentScoresExerciseExamples.Example1.submittedDate.map(_.withZone(DateTimeZone.forOffsetHours(1))))
      val request = fakeRequest(AssessmentScoresSubmitRequest(appId, "analysisExercise", exerciseScores))

      when(mockService.saveExercise(eqTo(appId), eqTo(AssessmentExerciseType.analysisExercise),
        any())).thenReturn(Future.successful(()))
      val auditDetails = Map(
        "applicationId" -> appId.toString(),
        "exercise" -> AssessmentExerciseType.analysisExercise.toString,
        "assessorId" -> exerciseScores.updatedBy.toString())


      val result = controller.submit()(request)

      status(result) must be(OK)
      verify(mockService).saveExercise(eqTo(appId), eqTo(AssessmentExerciseType.analysisExercise), any())
      verify(mockAuditService).logEvent(
        eqTo(AssessmentScoresController.AssessmentScoresOneExerciseSubmitted),
        eqTo(auditDetails))(any[HeaderCarrier], any[RequestHeader])

    }
  }

  "findAssessmentScoresWithCandidateSummary" should {
    "return OK with corresponding assessment scores" in new TestFixture {
      val expectedResponse = AssessmentScoresFindResponse(
        RecordCandidateScores("firstName", "lastName", "venue", DateTimeFactory.nowLocalDate),
        Some(AssessmentScoresAllExercisesExamples.Example1))
      when(mockService.findAssessmentScoresWithCandidateSummary(appId)).thenReturn(
        Future.successful(expectedResponse))

      val result = controller.findAssessmentScoresWithCandidateSummary(appId)(fakeRequest)

      status(result) must be (OK)
      contentAsJson(result) mustBe Json.toJson(expectedResponse)
    }

    "return NOT_FOUND if there is any error" in new TestFixture {
      when(mockService.findAssessmentScoresWithCandidateSummary(appId)).thenReturn(Future.failed(CannotUpdateRecord(appId.toString())))
      val response = controller.findAssessmentScoresWithCandidateSummary(appId)(fakeRequest)
      status(response) mustBe NOT_FOUND
    }
  }

  trait TestFixture extends TestFixtureBase {
    val mockService = mock[AssessmentScoresService]
    val mockAssessmentScoresRepository = mock[AssessmentScoresRepository]

    val controller = new AssessmentScoresController {
      override val service = mockService
      override val repository = mockAssessmentScoresRepository
      override val auditService = mockAuditService
    }

    val appId = AssessmentScoresAllExercisesExamples.Example1.applicationId
  }
}
