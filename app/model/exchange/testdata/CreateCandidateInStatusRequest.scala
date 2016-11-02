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

package model.exchange.testdata

import model.ApplicationRoute
import play.api.libs.json.Json


case class AssistanceDetailsRequest(hasDisability: Option[String] = None,
  hasDisabilityDescription: Option[String] = None,
  setGis: Option[Boolean] = None,
  onlineAdjustments: Option[Boolean] = None,
  onlineAdjustmentsDescription: Option[String] = None,
  assessmentCentreAdjustments: Option[Boolean] = None,
  assessmentCentreAdjustmentsDescription: Option[String] = None
)

object AssistanceDetailsRequest {
  implicit val assistanceDetailsRequestFormat = Json.format[AssistanceDetailsRequest]
}

trait TestDatesRequest {
  def start: Option[String]
  def expiry: Option[String]
  def completion: Option[String]
}

trait TestResultRequest {
  def tscore: Option[String]
}

case class Phase1TestDataRequest(
  start: Option[String] = None,
  expiry: Option[String] = None,
  completion: Option[String] = None
) extends TestDatesRequest with TestResultRequest

object Phase1TestDataRequest {
  implicit val phase1TestDataFormat = Json.format[Phase1TestDataRequest]
}

case class Phase2TestDataRequest(
  start: Option[String] = None,
  expiry: Option[String] = None,
  completion: Option[String] = None
) extends TestDatesRequest with TestResultRequest

object Phase2TestDataRequest {
  implicit val phase2TestDataFormat = Json.format[Phase2TestDataRequest]
}

case class PersonalDataRequest(
  emailPrefix: Option[String] = None,
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  preferedName: Option[String] = None,
  dateOfBirth: Option[String] = None,
  postCode: Option[String] = None,
  country: Option[String] = None
)

object PersonalDataRequest {
  implicit val personalDataFormat = Json.format[PersonalDataRequest]
}

case class StatusDataRequest(
  applicationStatus: String = "registered",
  previousApplicationStatus: Option[String] = None,
  progressStatus: Option[String] = Some("registered"),
  applicationRoute: Option[String] = Some("faststream")
)

object StatusDataRequest{
  implicit def statusDataformat = Json.format[StatusDataRequest]
}


case class CreateCandidateInStatusRequest(statusData: StatusDataRequest = new StatusDataRequest,
  numberToGenerate: Int = 1,
  personalData: Option[PersonalDataRequest],
  assistanceDetails: Option[AssistanceDetailsRequest],
  isCivilServant: Option[Boolean],
  hasDegree: Option[Boolean],
  region: Option[String],
  loc1scheme1EvaluationResult: Option[String],
  loc1scheme2EvaluationResult: Option[String],
  confirmedAllocation: Option[Boolean],
  phase1TestData: Option[Phase1TestDataRequest],
  phase2TestData: Option[Phase2TestDataRequest]
)

object CreateCandidateInStatusRequest {
  implicit val createCandidateInStatusRequestFormat = Json.format[CreateCandidateInStatusRequest]
}
