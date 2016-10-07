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

// TODO FIX ME!!! Once Cubiks callbacks are implemented
package scheduler.onlinetesting

import config.WaitingScheduledJobConfig
import scheduler.clustering.SingleInstanceScheduledJob
import services.onlinetesting.Phase1TestService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

object RetrieveResultsJob extends RetrieveResultsJob {
  val onlineTestingService = Phase1TestService
}

trait RetrieveResultsJob extends SingleInstanceScheduledJob with RetrieveResultsJobConfig {
  val onlineTestingService: Phase1TestService

  def tryExecute()(implicit ec: ExecutionContext): Future[Unit] = {
    onlineTestingService.nextTestGroupWithReportReady.flatMap {
      case Some(phase1TestProfile) =>
        implicit val hc = new HeaderCarrier()
        onlineTestingService.retrieveTestResult(phase1TestProfile)
      case None => Future.successful(())
    }
  }
}

trait RetrieveResultsJobConfig extends BasicJobConfig[WaitingScheduledJobConfig] {
  this: SingleInstanceScheduledJob =>
  override val conf = config.MicroserviceAppConfig.retrieveResultsJobConfig
  override val configPrefix = "scheduling.online-testing.retrieve-results-job."
  override val name = "RetrieveResultsJob"
}
