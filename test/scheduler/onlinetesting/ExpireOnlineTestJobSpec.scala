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

package scheduler.onlinetesting

import model.{ ExpiryTest, Phase1ExpiryTest }
import org.mockito.Matchers.{ eq => eqTo, _ }
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.WithApplication
import services.onlinetesting.OnlineTestService
import testkit.ShortTimeout
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.concurrent.{ ExecutionContext, Future }

class ExpireOnlineTestJobSpec extends PlaySpec with MockitoSugar with ScalaFutures with ShortTimeout {
  implicit val ec: ExecutionContext = ExecutionContext.global

  val serviceMock = mock[OnlineTestService]

  object TestableExpireTestJob extends ExpireOnlineTestJob {
    val onlineTestingService = serviceMock
    val expiryTest = Phase1ExpiryTest
    val lockId: String = "1"
    val forceLockReleaseAfter: Duration = mock[Duration]
    implicit val ec: ExecutionContext = mock[ExecutionContext]
    def name: String = "test"
    def initialDelay: FiniteDuration = mock[FiniteDuration]
    def interval: FiniteDuration = mock[FiniteDuration]
  }

  "expire test phase 1 job" should {
    "complete successfully when service completes successfully" in new WithApplication {
      when(serviceMock.processNextExpiredTest(eqTo(Phase1ExpiryTest))(any[HeaderCarrier])).thenReturn(Future.successful(()))
      TestableExpireTestJob.tryExecute().futureValue mustBe (())
    }

    "fail when the service fails" in new WithApplication {
      when(serviceMock.processNextExpiredTest(eqTo(Phase1ExpiryTest))(any[HeaderCarrier])).thenReturn(Future.failed(new Exception))
      TestableExpireTestJob.tryExecute().failed.futureValue mustBe an[Exception]
    }
  }
}
