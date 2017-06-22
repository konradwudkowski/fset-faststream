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

import model.Exceptions.EventNotFoundException
import model.persisted.eventschedules.{ EventType, VenueType }
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent }
import repositories.events.EventsRepository
import services.events.{ EventsParsingService, EventsService }
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

object EventsController extends EventsController {
  val assessmentEventsRepository: EventsRepository = repositories.eventsRepository
  val assessmentCenterParsingService: EventsParsingService = EventsParsingService
  val eventsService: EventsService = EventsService
}

trait EventsController extends BaseController {
  def assessmentEventsRepository: EventsRepository
  def assessmentCenterParsingService: EventsParsingService
  def eventsService: EventsService

  def saveAssessmentEvents(): Action[AnyContent] = Action.async { implicit request =>
    eventsService.saveAssessmentEvents().map(_ => Created("Events saved")).recover { case _ => UnprocessableEntity }
  }

  def getEvent(eventId: String): Action[AnyContent] = Action.async { implicit request =>
    eventsService.getEvent(eventId).map { event =>
      Ok(Json.toJson(event))
    }.recover {
      case _: EventNotFoundException => NotFound(s"No event found with id $eventId")
    }
  }

  def fetchEvents(eventTypeParam: String, venueParam: String): Action[AnyContent] = Action.async { implicit request =>
    val eventType = EventType.withName(eventTypeParam.toUpperCase)
    val venue = VenueType.withName(venueParam.toUpperCase)

    assessmentEventsRepository.fetchEvents(eventType, venue)
      .map(events => Ok(Json.toJson(events)))
  }
}
