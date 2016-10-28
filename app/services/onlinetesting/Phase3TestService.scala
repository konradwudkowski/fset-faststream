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

import _root_.services.AuditService
import config.LaunchpadGatewayConfig
import connectors._
import connectors.launchpadgateway.LaunchpadGatewayClient
import connectors.launchpadgateway.exchangeobjects._
import factories.{ DateTimeFactory, UUIDFactory }
import model.OnlineTestCommands._
import model.ProgressStatuses._
import model.command.ProgressResponse
import model.events.{ AuditEventNoRequest, AuditEvents, DataStoreEventWithAppId, DataStoreEvents }
import model.persisted.{ NotificationExpiringOnlineTest, Phase2TestGroup }
import model.persisted.phase3tests.{ LaunchpadTest, Phase3TestGroup }
import model._
import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import repositories._
import repositories.application.GeneralApplicationRepository
import repositories.onlinetesting.Phase3TestRepository
import services.events.EventService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

object Phase3TestService extends Phase3TestService {
  import config.MicroserviceAppConfig._
  val appRepository = applicationRepository
  val phase3TestRepo = phase3TestRepository
  val cdRepository = faststreamContactDetailsRepository
  val launchpadGatewayClient = LaunchpadGatewayClient
  val tokenFactory = UUIDFactory
  val dateTimeFactory = DateTimeFactory
  val emailClient = Phase3OnlineTestEmailClient
  val auditService = AuditService
  val gatewayConfig = launchpadGatewayConfig
  val eventService = EventService
}

trait Phase3TestService extends OnlineTestService with ResetPhase3Test {
  val appRepository: GeneralApplicationRepository
  val phase3TestRepo: Phase3TestRepository
  val cdRepository: contactdetails.ContactDetailsRepository
  val launchpadGatewayClient: LaunchpadGatewayClient
  val tokenFactory: UUIDFactory
  val dateTimeFactory: DateTimeFactory
  val emailClient: OnlineTestEmailClient
  val auditService: AuditService
  val gatewayConfig: LaunchpadGatewayConfig

  override def nextApplicationReadyForOnlineTesting: Future[List[OnlineTestApplication]] = {
    phase3TestRepo.nextApplicationsReadyForOnlineTesting
  }

  def getTestGroup(applicationId: String): Future[Option[Phase3TestGroup]] = phase3TestRepo.getTestGroup(applicationId)

  override def registerAndInviteForTestGroup(application: List[OnlineTestApplication])
    (implicit hc: HeaderCarrier, rh: RequestHeader): Future[Unit] = Future.failed(new NotImplementedError())

  override def registerAndInviteForTestGroup(application: OnlineTestApplication)
    (implicit hc: HeaderCarrier, rh: RequestHeader): Future[Unit] = {
    registerAndInviteForTestGroup(application, getInterviewIdForApplication(application))
  }

  override def processNextExpiredTest(expiryTest: TestExpirationEvent)(implicit hc: HeaderCarrier, rh: RequestHeader): Future[Unit] = ???

  def registerAndInviteForTestGroup(application: OnlineTestApplication, interviewId: Int)
    (implicit hc: HeaderCarrier): Future[Unit] = {
    val (invitationDate, expirationDate) =
      dateTimeFactory.nowLocalTimeZone -> dateTimeFactory.nowLocalTimeZone.plusDays(gatewayConfig.phase3Tests.timeToExpireInDays)

    for {
      emailAddress <- candidateEmailAddress(application)
      phase3Test <- registerAndInviteApplicant(application, emailAddress, interviewId, invitationDate, expirationDate)
      // TODO: Trigger email when template is available
      // _ <- emailInviteToApplicant(application, emailAddress, invitationDate, expirationDate)
      _ <- markAsInvited(application)(Phase3TestGroup(expirationDate = expirationDate, tests = List(phase3Test)))
      _ <- eventService.handle(
        AuditEvents.VideoInterviewRegistrationAndInviteComplete("userId" -> application.userId) ::
        DataStoreEvents.VideoInterviewRegistrationAndInviteComplete(application.applicationId) :: Nil
      )
    } yield {}
  }

  override def processNextTestForReminder(reminder: model.ReminderNotice)(implicit hc: HeaderCarrier, rh: RequestHeader): Future[Unit] = ???

  override def emailCandidateForExpiringTestReminder(expiringTest: NotificationExpiringOnlineTest,
    emailAddress: String,
    reminder: ReminderNotice)(implicit hc: HeaderCarrier, rh: RequestHeader): Future[Unit] = ???

  private def registerAndInviteApplicant(application: OnlineTestApplication, emailAddress: String, interviewId: Int, invitationDate: DateTime,
    expirationDate: DateTime
  )(implicit hc: HeaderCarrier): Future[LaunchpadTest] = {
    val customCandidateId = "FSCND-" + tokenFactory.generateUUID()

    for {
      candidateId <- registerApplicant(application, emailAddress, customCandidateId)
      invitation <- inviteApplicant(application, interviewId, candidateId)
    } yield {
      LaunchpadTest(interviewId = interviewId,
        usedForResults = true,
        testUrl = invitation.testUrl,
        token = invitation.customInviteId,
        candidateId = candidateId,
        customCandidateId = invitation.customCandidateId,
        invitationDate = invitationDate,
        startedDateTime = None,
        completedDateTime = None
      )
    }
  }

  def extendTestGroupExpiryTime(applicationId: String, extraDays: Int, actionTriggeredBy: String)
                               (implicit hc: HeaderCarrier, rh: RequestHeader): Future[Unit] = {
    val progressFut = appRepository.findProgress(applicationId)
    val phase3TestGroup = phase3TestRepo.getTestGroup(applicationId)
      .map(tg => tg.getOrElse(throw new IllegalStateException("Expiration date for Phase 3 cannot be extended. Test group not found.")))

    for {
      progress <- progressFut
      phase3 <- phase3TestGroup
      isAlreadyExpired = progress.phase3ProgressResponse.phase3TestsExpired
      extendDays = extendTime(isAlreadyExpired, phase3.expirationDate)
      newExpiryDate = extendDays(extraDays)
      _ <- phase3TestRepo.updateGroupExpiryTime(applicationId, newExpiryDate, phase3TestRepo.phaseName)
      _ <- progressStatusesToRemoveWhenExtendTime(newExpiryDate, phase3, progress)
        .fold(Future.successful(()))(p => appRepository.removeProgressStatuses(applicationId, p))
      _ <- eventService.handle(AuditEvents.VideoInterviewExtended(
        "isAlreadyExpired" -> isAlreadyExpired.toString,
        "applicationId" -> applicationId) ::
        DataStoreEvents.VideoInterviewExtended(applicationId, actionTriggeredBy) :: Nil)
    } yield {}
  }

  def logEvent(auditClass: Map[String, String] => AuditEventNoRequest,
                        dataStoreEvent: Map[String, String] => DataStoreEventWithAppId,
                        params: (String, String)*): Future[Unit] =
    eventService.handle(auditClass(params.toMap) :: dataStoreEvent(params.toMap) :: Nil)

  private def registerApplicant(application: OnlineTestApplication,
                        emailAddress: String, customCandidateId: String)(implicit hc: HeaderCarrier): Future[String] = {
    val registerApplicant = RegisterApplicantRequest(emailAddress, customCandidateId, application.preferredName, application.lastName)
    launchpadGatewayClient.registerApplicant(registerApplicant).map { registration =>
      eventService.handle(
        AuditEvents.VideoInterviewCandidateRegistered("userId" -> application.userId) ::
        DataStoreEvents.VideoInterviewCandidateRegistered(application.applicationId) :: Nil
      )
      registration.candidateId
    }
  }

  private def inviteApplicant(application: OnlineTestApplication, interviewId: Int, candidateId: String)
    (implicit hc: HeaderCarrier): Future[InviteApplicantResponse] = {

    val customInviteId = "FSINV-" + tokenFactory.generateUUID()

    val completionRedirectUrl = s"${gatewayConfig.phase3Tests.candidateCompletionRedirectUrl}/fset-fast-stream/" +
      s"phase3-tests/complete/$customInviteId"

    val inviteApplicant = InviteApplicantRequest(interviewId, candidateId, customInviteId, completionRedirectUrl)

    launchpadGatewayClient.inviteApplicant(inviteApplicant)
  }

  override def emailInviteToApplicant(application: OnlineTestApplication, emailAddress: String,
    invitationDate: DateTime, expirationDate: DateTime
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val preferredName = application.preferredName
    emailClient.sendOnlineTestInvitation(emailAddress, preferredName, expirationDate).map { _ =>
     eventService.handle(
       AuditEvents.VideoInterviewInvitationEmailSent(
        "userId" -> application.userId,
        "emailAddress" -> emailAddress
      ) ::
       DataStoreEvents.VideoInterviewInvitationEmailSent(
        application.applicationId
       ) :: Nil
     )
    }
  }

  private def merge(currentTestGroup: Option[Phase3TestGroup],
                    newTestGroup: Phase3TestGroup): Phase3TestGroup = currentTestGroup match {
    case None =>
      newTestGroup
    case Some(profile) =>
      val interviewIdsToArchive = newTestGroup.tests.map(_.interviewId)
      val existingTestsAfterUpdate = profile.tests.map { t =>
        if (interviewIdsToArchive.contains(t.interviewId)) {
          t.copy(usedForResults = false)
        } else {
          t
        }
      }
      Phase3TestGroup(newTestGroup.expirationDate, existingTestsAfterUpdate ++ newTestGroup.tests)
  }

  private def markAsInvited(application: OnlineTestApplication)
                           (newPhase3TestGroup: Phase3TestGroup): Future[Unit] = {
    for {
      currentPhase3TestGroup <- phase3TestRepo.getTestGroup(application.applicationId)
      updatedPhase3TestGroup = merge(currentPhase3TestGroup, newPhase3TestGroup)
      _ <- phase3TestRepo.insertOrUpdateTestGroup(application.applicationId, updatedPhase3TestGroup)
      _ <- appRepository.removeProgressStatuses(application.applicationId, determineStatusesToRemove(updatedPhase3TestGroup))
      _ <- eventService.handle(AuditEvents.VideoInterviewInvited("userId" -> application.userId) ::
        DataStoreEvents.VideoInterviewInvited(application.applicationId) :: Nil)
    } yield {}
  }

  private def progressStatusesToRemoveWhenExtendTime(extendedExpiryDate: DateTime,
                                                     profile: Phase3TestGroup,
                                                     progress: ProgressResponse): Option[List[ProgressStatus]] = {
    val shouldRemoveExpired = progress.phase3ProgressResponse.phase3TestsExpired
    val today = dateTimeFactory.nowLocalTimeZone
    val shouldRemoveSecondReminder = extendedExpiryDate.minusHours(Phase3SecondReminder.hoursBeforeReminder).isAfter(today)
    val shouldRemoveFirstReminder = extendedExpiryDate.minusHours(Phase3FirstReminder.hoursBeforeReminder).isAfter(today)

    val progressStatusesToRemove = (Set.empty[ProgressStatus]
      ++ (if (shouldRemoveExpired) Set(PHASE3_TESTS_EXPIRED) else Set.empty)
      ++ (if (shouldRemoveSecondReminder) Set(PHASE3_TESTS_SECOND_REMINDER) else Set.empty)
      ++ (if (shouldRemoveFirstReminder) Set(PHASE3_TESTS_FIRST_REMINDER) else Set.empty)).toList

    if (progressStatusesToRemove.isEmpty) { None } else { Some(progressStatusesToRemove) }
  }

  private def candidateEmailAddress(application: OnlineTestApplication): Future[String] =
    cdRepository.find(application.userId).map(_.email)

  // TODO: This needs to cater for 10% extra, 33% extra etc. See FSET-656
  private def getInterviewIdForApplication(application: OnlineTestApplication): Int = {
      gatewayConfig.phase3Tests.interviewsByAdjustmentPercentage("0pc")
  }
}

trait ResetPhase3Test {
  import ProgressStatuses._

  // TODO: Implement for resets/extends
  def determineStatusesToRemove(testGroup: Phase3TestGroup): List[ProgressStatus] = {
    List(PHASE3_TESTS_STARTED, PHASE3_TESTS_COMPLETED)
  }
}