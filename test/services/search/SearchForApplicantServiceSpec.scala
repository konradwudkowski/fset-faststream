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

package services.search

import model.Address
import model.Commands.{ Candidate, SearchCandidate }
import model.PersistedObjects.ContactDetailsWithId
import org.joda.time.LocalDate
import repositories.ContactDetailsRepository
import repositories.application.{ GeneralApplicationRepository, PersonalDetailsRepository }
import services.BaseServiceSpec
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SearchForApplicantServiceSpec extends BaseServiceSpec {

  val appRepositoryMock = mock[GeneralApplicationRepository]
  val psRepositoryMock = mock[PersonalDetailsRepository]
  val cdRepositoryMock = mock[ContactDetailsRepository]

  val searchForApplicantService = new SearchForApplicantService {
    override val appRepository = appRepositoryMock
    override val psRepository = psRepositoryMock
    override val cdRepository = cdRepositoryMock
  }

  "find by criteria" should {
    "filter by post code" in {
      val testAddress = Address(line1 = "1 Test Street", line2 = None, line3 = None, line4 = None)
      when(cdRepositoryMock.findByPostCode(any[String])).thenReturn(
        Future(List(ContactDetailsWithId(userId = "123", postCode = "QQ1 1QQ", address = testAddress, email = "",
          phone = None)))
      )

      when(appRepositoryMock.findByCriteria(any[Option[String]], any[Option[String]],
        any[Option[LocalDate]], any[List[String]])
      ).thenReturn(Future(List(Candidate("123", None, None, Some("Leia"), Some("Amadala"), None ,None, None, None, None))))

      val actual = searchForApplicantService.findByCriteria(SearchCandidate(firstOrPreferredName = Some("Leia"),
        lastName = Some("Amadala"), dateOfBirth = None, postCode = Some("QQ1 1QQ"))).futureValue

      val expected = Candidate("123", None, None, Some("Leia"), Some("Amadala"),
        None, None, Some(testAddress), Some("QQ1 1QQ"), None
      )

      actual mustBe List(expected)
    }

  }
}
