package repositories.parity

import config.{ LaunchpadGatewayConfig, Phase3TestsConfig }
import model.ApplicationStatus.ApplicationStatus
import model.EvaluationResults.Green
import model.Exceptions.PassMarkEvaluationNotFound
import model.SchemeType._
import model.persisted._
import model.persisted.phase3tests.Phase3TestGroup
import model.{ ApplicationStatus, ProgressStatuses, SchemeType }
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsArray
import reactivemongo.bson.BSONDocument
import reactivemongo.json.ImplicitBSONHandlers
import repositories.CommonRepository
import repositories.parity.ParityExportRepository.ApplicationIdNotFoundException
import testkit.MongoRepositorySpec


class ParityExportMongoRepositorySpec extends MongoRepositorySpec with CommonRepository with MockitoSugar {

  import ImplicitBSONHandlers._
  import model.Phase3TestProfileExamples._

  val collectionName: String = "application"

  "next Applications Ready For export" must {

    val resultToSave = List(SchemeEvaluationResult(SchemeType.Commercial, Green.toString))

    "return nothing if application does not have READY_FOR_EXPORT" in {
      insertApplication("app1", ApplicationStatus.PHASE3_TESTS)
      val result = parityExportMongoRepo.nextApplicationsForExport(batchSize = 1).futureValue
      result mustBe empty
    }

    "return application id in READY_FOR_EXPORT" in {
      insertApplication("app1", ApplicationStatus.READY_FOR_EXPORT, None)

      val result = parityExportMongoRepo.nextApplicationsForExport(batchSize = 1).futureValue

      result.size mustBe 1
      result.head.applicationId mustBe "app1"
    }

    "return nothing when no applications are in READY_FOR_EXPORT" in {
      val result = parityExportMongoRepo.nextApplicationsForExport(batchSize = 1).futureValue
      result mustBe empty
    }

    "limit number of next applications to the batch size limit" in {
      val batchSizeLimit = 5
      1 to 6 foreach { id =>
        insertApplication(s"app$id", ApplicationStatus.READY_FOR_EXPORT)
      }
      val result = parityExportMongoRepo.nextApplicationsForExport(batchSizeLimit).futureValue
      result.size mustBe batchSizeLimit
    }

    "return less number of applications than batch size limit" in {
      val batchSizeLimit = 5
      1 to 2 foreach { id =>
        insertApplication(s"app$id", ApplicationStatus.READY_FOR_EXPORT)
      }
      val result = parityExportMongoRepo.nextApplicationsForExport(batchSizeLimit).futureValue
      result.size mustBe 2
    }
  }

  "get application for export" must {

    "return a full application as a JsValue when valid" in {
      insertApplication("app1", ApplicationStatus.READY_FOR_EXPORT)

      val result = parityExportMongoRepo.getApplicationForExport("app1").futureValue
      (result \ "applicationId").as[String] mustBe "app1"
      (result \ "userId").as[String] must not be empty
      (result \ "applicationStatus").as[String] must not be empty
      (result \ "scheme-preferences" \ "schemes").as[JsArray].value.head.as[String] mustBe Commercial.toString
    }

    "throw an exception when an applicationId is invalid" in {
        val result = parityExportMongoRepo.getApplicationForExport("app1").failed.futureValue mustBe a[ApplicationIdNotFoundException]
    }
  }
}