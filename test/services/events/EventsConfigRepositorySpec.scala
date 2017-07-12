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

package services.events

import model.persisted.eventschedules._
import org.scalatest.concurrent.ScalaFutures
import repositories.events.EventsConfigRepository
import services.BaseServiceSpec
import org.joda.time.{ LocalDate, LocalTime }
import org.scalatest.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.test.UnitSpec

class EventsConfigRepositorySpec extends UnitSpec with Matchers with ScalaFutures {
  "events" must {
    "successfully parse the config" in {
      val input = """- eventType: FSAC
                    |  description: PDFS FSB
                    |  location: London
                    |  venue: LONDON_FSAC
                    |  date: 2017-04-03
                    |  capacity: 36
                    |  minViableAttendees: 12
                    |  attendeeSafetyMargin: 2
                    |  startTime: 11:00
                    |  endTime: 12:00
                    |  skillRequirements:
                    |    ASSESSOR: 6
                    |    CHAIR: 3
                    |    DEPARTMENTAL_ASSESSOR: 3
                    |    EXERCISE_MARKER: 3
                    |    QUALITY_ASSURANCE_COORDINATOR: 1
                    |  sessions:
                    |    - description: AM
                    |      startTime: 11:00
                    |      endTime: 12:00
                    |- eventType: FSAC
                    |  description: PDFS FSB
                    |  location: London
                    |  venue: LONDON_FSAC
                    |  date: 2017-04-03
                    |  capacity: 36
                    |  minViableAttendees: 12
                    |  attendeeSafetyMargin: 2
                    |  startTime: 9:00
                    |  endTime: 12:00
                    |  skillRequirements:
                    |    ASSESSOR: 6
                    |    CHAIR: 3
                    |    DEPARTMENTAL_ASSESSOR: 3
                    |    EXERCISE_MARKER: 2
                    |    QUALITY_ASSURANCE_COORDINATOR: 1
                    |  sessions:
                    |    - description: First
                    |      startTime: 9:00
                    |      endTime: 10:30
                    |    - description: Second
                    |      startTime: 10:30
                    |      endTime: 12:00
                    |- eventType: FSAC
                    |  description: PDFS FSB
                    |  location: Newcastle
                    |  venue: NEWCASTLE_LONGBENTON
                    |  date: 2017-04-03
                    |  capacity: 36
                    |  minViableAttendees: 12
                    |  attendeeSafetyMargin: 2
                    |  startTime: 09:00
                    |  endTime: 12:00
                    |  skillRequirements:
                    |    ASSESSOR: 6
                    |    CHAIR: 3
                    |    DEPARTMENTAL_ASSESSOR: 2
                    |    EXERCISE_MARKER: 3
                    |    QUALITY_ASSURANCE_COORDINATOR: 1
                    |  sessions:
                    |    - description: First
                    |      startTime: 9:00
                    |      endTime: 10:30
                    |    - description: Second
                    |      startTime: 10:30
                    |      endTime: 12:00""".stripMargin

      val repo = new EventsConfigRepository {
        override protected def rawConfig = input
      }

      implicit val patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

      whenReady(repo.events) { result =>
        result shouldBe List(
          Event(result(0).id,EventType.withName("FSAC"),"PDFS FSB",Location("London"),Venue("LONDON_FSAC",""),LocalDate.parse("2017-04-03"),36,
            12,2,LocalTime.parse("11:00:00.000"),LocalTime.parse("12:00:00.000"),
            Map("DEPARTMENTAL_ASSESSOR" -> 3, "EXERCISE_MARKER" -> 3, "ASSESSOR" -> 6, "QUALITY_ASSURANCE_COORDINATOR" -> 1, "CHAIR" -> 3),
            List(Session("AM",LocalTime.parse("11:00:00.000"),LocalTime.parse("12:00:00.000")))),
          Event(result(1).id,EventType.withName("FSAC"),"PDFS FSB",Location("London"),Venue("LONDON_FSAC",""),LocalDate.parse("2017-04-03"),36,
            12,2,LocalTime.parse("09:00:00.000"),LocalTime.parse("12:00:00.000"),
            Map("DEPARTMENTAL_ASSESSOR" -> 3, "EXERCISE_MARKER" -> 2, "ASSESSOR" -> 6, "QUALITY_ASSURANCE_COORDINATOR" -> 1, "CHAIR" -> 3),
            List(Session("First",LocalTime.parse("09:00:00.000"),LocalTime.parse("10:30:00.000")),
              Session("Second",LocalTime.parse("10:30:00.000"),LocalTime.parse("12:00:00.000")))),
          Event(result(2).id,EventType.withName("FSAC"),"PDFS FSB",Location("Newcastle"),Venue("NEWCASTLE_LONGBENTON",""),
            LocalDate.parse("2017-04-03"),36,12,2,LocalTime.parse("09:00:00.000"),LocalTime.parse("12:00:00.000"),
            Map("DEPARTMENTAL_ASSESSOR" -> 2, "EXERCISE_MARKER" -> 3, "ASSESSOR" -> 6, "QUALITY_ASSURANCE_COORDINATOR" -> 1, "CHAIR" -> 3),
            List(Session("First",LocalTime.parse("09:00:00.000"),LocalTime.parse("10:30:00.000")),
              Session("Second",LocalTime.parse("10:30:00.000"),LocalTime.parse("12:00:00.000")))))
      }
    }
  }
}
