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

package services.onlinetesting

import connectors.EmailClient
import factories.{ DateTimeFactory, UUIDFactory }
import model.OnlineTestCommands.OnlineTestApplication
import org.joda.time.DateTime
import play.api.Logger
import services.AuditService
import services.events.EventSink
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }


trait OnlineTestService extends EventSink  {
  val emailClient: EmailClient
  val auditService: AuditService
  val tokenFactory: UUIDFactory
  val dateTimeFactory: DateTimeFactory

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def nextApplicationReadyForOnlineTesting: Future[Option[OnlineTestApplication]]
  def registerAndInviteForTestGroup(application: OnlineTestApplication)(implicit hc: HeaderCarrier): Future[Unit]
  def registerAndInviteForTestGroup(applications: List[OnlineTestApplication])(implicit hc: HeaderCarrier): Future[Unit]

  protected def emailInviteToApplicant(application: OnlineTestApplication, emailAddress: String,
    invitationDate: DateTime, expirationDate: DateTime
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val preferredName = application.preferredName
    emailClient.sendOnlineTestInvitation(emailAddress, preferredName, expirationDate).map { _ =>
      audit("OnlineTestInvitationEmailSent", application.userId, Some(emailAddress))
    }
  }

  protected def audit(event: String, userId: String, emailAddress: Option[String] = None): Unit = {
    Logger.info(s"$event for user $userId")

    auditService.logEventNoRequest(
      event,
      Map("userId" -> userId) ++ emailAddress.map("email" -> _).toMap
    )
  }
}
