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

import common.FutureEx
import config.ScheduledJobConfig
import model.Phase
import model.Phase.Phase
import model.exchange.passmarksettings.{ PassMarkSettings, Phase1PassMarkSettings, Phase2PassMarkSettings }
import model.persisted.ApplicationReadyForEvaluation
import play.api.Logger
import scheduler.clustering.SingleInstanceScheduledJob
import services.onlinetesting.{ EvaluatePhase1ResultService, EvaluatePhase2ResultService }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

object EvaluatePhase1ResultJob extends EvaluateOnlineTestResultJob[Phase1PassMarkSettings] with EvaluatePhase1ResultJobConfig {
  val evaluateService = EvaluatePhase1ResultService
  val phase = Phase.PHASE1
}

object EvaluatePhase2ResultJob extends EvaluateOnlineTestResultJob[Phase2PassMarkSettings] with EvaluatePhase2ResultJobConfig {
  val evaluateService = EvaluatePhase2ResultService
  val phase = Phase.PHASE2
}

trait EvaluateOnlineTestResultJob[T <: PassMarkSettings] extends SingleInstanceScheduledJob {
  val evaluateService: EvaluateOnlineTestResultService[T]
  val batchSize: Int
  val phase: Phase

  def tryExecute()(implicit ec: ExecutionContext): Future[Unit] = {
    evaluateService.nextCandidatesReadyForEvaluation(batchSize) flatMap {
      case Some((apps, passmarkSettings)) =>
        evaluateInBatch(apps, passmarkSettings)
      case None =>
        Logger.info(s"Passmark settings or an application to evaluate $phase result not found")
        Future.successful(())
    }
  }

  private def evaluateInBatch(apps: List[ApplicationReadyForEvaluation],
                              passmarkSettings: T)(implicit ec: ExecutionContext): Future[Unit] = {
    Logger.debug(s"Evaluate $phase Job found ${apps.size} application(s), the passmarkVersion=${passmarkSettings.version}")
    val evaluationResultsFut = FutureEx.traverseToTry(apps) { app =>
      Try(evaluateService.evaluate(app, passmarkSettings)) match {
        case Success(fut) => fut
        case Failure(e) => Future.failed(e)
      }
    }

    evaluationResultsFut flatMap { evaluationResults =>
      val errors = evaluationResults flatMap {
        case Failure(e) => Some(e)
        case _ => None
      }

      if (errors.nonEmpty) {
        val errorMsg = apps.map {a =>
          s"${a.applicationId}, cubiks Ids: ${a.activeTests.map(_.cubiksUserId).mkString(",")}"
        }.mkString("\n")

        Logger.error(s"There were ${errors.size} errors in batch Phase 1 evaluation:\n$errorMsg")
        Future.failed(errors.head)
      } else {
        Future.successful(())
      }
    }
  }
}

trait EvaluatePhase1ResultJobConfig extends BasicJobConfig[ScheduledJobConfig] {
  this: SingleInstanceScheduledJob =>
  val conf = config.MicroserviceAppConfig.evaluatePhase1ResultJobConfig
  val configPrefix = "scheduling.online-testing.evaluate-phase1-result-job."
  val name = "EvaluatePhase1ResultJob"

  val batchSize = conf.batchSize.getOrElse(throw new IllegalArgumentException("Batch size must be defined"))
  Logger.debug(s"Max number of applications in scheduler: $batchSize")
}

trait EvaluatePhase2ResultJobConfig extends BasicJobConfig[ScheduledJobConfig] {
  this: SingleInstanceScheduledJob =>
  val conf = config.MicroserviceAppConfig.evaluatePhase2ResultJobConfig
  val configPrefix = "scheduling.online-testing.evaluate-phase2-result-job."
  val name = "EvaluatePhase2ResultJob"

  val batchSize = conf.batchSize.getOrElse(throw new IllegalArgumentException("Batch size must be defined"))
  Logger.debug(s"Max number of applications in scheduler: $batchSize")
}
