package controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.cache._
import play.api.cache.Cache
import model._
import play.api.libs.json._
import com.codeminders.s3simpleclient.AWSCredentials
import java.util.Random
import play.api.GlobalSettings
import java.io.OutputStream
import java.io.InputStream
import play.api.libs.iteratee.Enumerator
import java.io.ByteArrayInputStream

object Application extends Controller {

  def index = Action {
    Redirect(routes.Application.generalPropertiesPage)
  }

  def generalPropertiesPage = Action {
    Ok(views.html.properties("Generate index.html for all files in Amazon S3 bucket. Step 1"))
  }

  def viewPropertiesPage = Action {
    Ok(views.html.viewProperties("Generate index.html for all files in Amazon S3 bucket. Step 2"))
  }

  def generatorPage = Action {
    request =>
      val uuid = getOrInitializeUUID(request)
      val bucketProperties = getOrInitializeS3IndexTask(uuid)
      Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + bucketProperties.toString())
      bucketProperties.status.set(bucketProperties.status.get() % 0 info ("Please wait. We will start processing of your bucket shortly"))
      IndexGenerator ! bucketProperties
      Ok(views.html.generate("Generate index.html for all files in Amazon S3 bucket. Step 3"))
  }

  def status = Action {
    request =>
      val uuid = getOrInitializeUUID(request)
      val bucketProperties = getOrInitializeS3IndexTask(uuid)
      Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + bucketProperties.toString())
      Ok(bucketProperties.status.get().toJSON)
  }

  def properties = Action {
    implicit request =>
      val uuid = getOrInitializeUUID(request)
      val task = getOrInitializeS3IndexTask(uuid)
      val taskProperties = task.properties.get().getOrElse(new Properties(""))
      Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + taskProperties.toString())

      Ok(taskProperties.toJSON())
  }

  def setProperties = Action {
    request =>
      try {
        val uuid = getOrInitializeUUID(request)
        val task = getOrInitializeS3IndexTask(uuid)
        val parameters = request.body.asFormUrlEncoded.getOrElse(throw new InternalError(Json.toJson("Please specify at least one parameter"), "Request body should not be empty"))
        Logger.debug("parameters -> " + parameters.toString())
        val validator = new PropertiesValidator(parameters).
          isLengthInRange("bucketName", 3 to 63).
          isNumber("depthLevel").
          isNumberInRange("depthLevel", 1 to 100).
          isLengthInRange("excludeKey", 1 to 1024).
          isLengthInRange("includeKey", 1 to 1024).
          oneOf("template", List("Simple", "Slim", "Blue", "Orange")).
          oneOf("fileListFormat", List("Full", "Brief")).
          oneOf("outputTo", List("ZipArchive", "Bucket")).
          isLengthInRange("accessKeyID", 1 to 255).
          isLengthInRange("secretAccessKey", 1 to 255)

        if (validator.anyErrors) throw new BadRequestError(validator.toJSON(), "Form validation errors: " + validator.toString)
        else {
          val taskProperties = validator.toProperties(task.properties.get().getOrElse(new Properties("")))
          Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + taskProperties.toString())
          task.properties.set(Option(taskProperties))
        }
        
        Ok("OK").withSession(
          request.session + ("uuid" -> uuid))

      } catch {
        case e: S3IndexException => {
          BadRequest(e.response)
        }
      }
  }

  def getIndex(id: String) = Action {
    request =>
      val uuid = getOrInitializeUUID(request)
      val task = getOrInitializeS3IndexTask(uuid)
      task.result match {
        case None => NotFound("")
        case Some(a) => sendByteArrayAsFile(a, fileName = "s3index.zip")
      }
  }

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(controllers.routes.javascript.Application.getIndex)).as("text/javascript")
  }

  private def getOrInitializeS3IndexTask(uuid: String): S3IndexTask = {
    Cache.getOrElse[S3IndexTask](uuid + ".bucket.properties") {
      new S3IndexTask(uuid)
    }
  }

  private def getOrInitializeUUID(request: Request[AnyContent]): String = {
    request.session.get("uuid").map { value =>
      value
    }.getOrElse {
      java.util.UUID.randomUUID().toString();
    }
  }

  private def sendByteArrayAsFile(content: Array[Byte], fileName: String): SimpleResult[Array[Byte]] = {
    SimpleResult(
      header = ResponseHeader(OK, Map(
        CONTENT_LENGTH -> content.length.toString,
        CONTENT_TYPE -> play.api.libs.MimeTypes.forFileName(fileName).getOrElse(play.api.http.ContentTypes.BINARY)) ++ (Map(CONTENT_DISPOSITION -> ("attachment; filename=" + fileName)))),
      Enumerator.fromStream(new ByteArrayInputStream(content)))
  }

}