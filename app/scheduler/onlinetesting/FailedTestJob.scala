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

package scheduler.onlinetesting

import config.ScheduledJobConfig
import model._
import scheduler.BasicJobConfig
import scheduler.clustering.SingleInstanceScheduledJob
import services.onlinetesting.{ OnlineTestService, Phase1TestService, Phase2TestService, Phase3TestService }
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

object FailedPhase1TestJob extends FailedTestJob {
  override val service = Phase1TestService
  override val failedType: FailedTestType = Phase1FailedTestType
  val config = FailedPhase1TestJobConfig
}

object FailedPhase2TestJob extends FailedTestJob {
  override val service = Phase2TestService
  override val failedType: FailedTestType = Phase2FailedTestType
  val config = FailedPhase2TestJobConfig
}

object FailedPhase3TestJob extends FailedTestJob {
  override val service = Phase3TestService
  override val failedType: FailedTestType = Phase3FailedTestType
  val config = FailedPhase3TestJobConfig
}

trait FailedTestJob extends SingleInstanceScheduledJob[BasicJobConfig[ScheduledJobConfig]] {
  val service: OnlineTestService
  val failedType: FailedTestType

  def tryExecute()(implicit ec: ExecutionContext): Future[Unit] = {
    implicit val rh = EmptyRequestHeader
    implicit val hc = new HeaderCarrier()
    service.processNextTestForNotification(failedType)
  }
}

object FailedPhase1TestJobConfig extends BasicJobConfig[ScheduledJobConfig](
  configPrefix = "scheduling.online-testing.failed-phase1-test-job",
  name = "FailedPhase1TestJob"
)

object FailedPhase2TestJobConfig extends BasicJobConfig[ScheduledJobConfig](
  configPrefix = "scheduling.online-testing.failed-phase2-test-job",
  name = "FailedPhase2TestJob"
)

object FailedPhase3TestJobConfig extends BasicJobConfig[ScheduledJobConfig](
  configPrefix = "scheduling.online-testing.failed-phase3-test-job",
  name = "FailedPhase3TestJob"
)
