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

object Application extends Controller {

  def index = Action {
    Redirect(routes.Application.generalPropertiesPage)
  }

  def generalPropertiesPage = Action {
    Ok(views.html.properties("Generate index.html for all files in Amazon S3 bucket. Step 1"))
  }

  def viewPropertiesPage = TODO
  
  def generatorPage = Action {
    request => 
    val uuid = getOrInitializeUUID(request)
    val bucketProperties = getOrInitializeBucketProperties(uuid)
    Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + bucketProperties.toString())
    bucketProperties.status.set(bucketProperties.status.get() % 0 info ("Please wait. We will start processing of your bucket shortly")) 
    IndexGenerator ! bucketProperties
    Ok(views.html.generate("Generate index.html for all files in Amazon S3 bucket. Step 3"))
  }
  
  def status = Action {
    request =>
    val uuid = getOrInitializeUUID(request)
    val bucketProperties = getOrInitializeBucketProperties(uuid)
    Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + bucketProperties.toString())
    Ok(bucketProperties.status.get().toJSON)
  }

  def properties = Action {
    implicit request =>
      val uuid = getOrInitializeUUID(request)
      val bucketProperties = getOrInitializeBucketProperties(uuid)
      Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + bucketProperties.toString())
      val response = Json.toJson(
        Map("bucketName" -> Json.toJson(bucketProperties.name),
          "accessKeyID" -> Json.toJson(if(bucketProperties.credentials != None) bucketProperties.credentials.get.accessKeyId else ""),
          "secretAccessKey" -> Json.toJson(if(bucketProperties.credentials != None) bucketProperties.credentials.get.secretKey else ""),
          "depthLevel" -> Json.toJson(bucketProperties.depthLevel),
          "includeKey" -> Json.toJson(bucketProperties.includedPaths.toList),
          "excludeKey" -> Json.toJson(bucketProperties.excludedPaths.toList)))

      Ok(response)
  }

  def setProperties = Action {
    request =>
      try {
        val uuid = getOrInitializeUUID(request)
        val bucketProperties = getOrInitializeBucketProperties(uuid)
        val parameters = request.body.asFormUrlEncoded.getOrElse(throw new InternalError(Json.toJson("Please specify at least one parameter"), "Request body should not be empty"))
        val validator = new PropertiesValidator(parameters).
          isLengthInRange("bucketName", 3 to 63).
          isNumber("depthLevel").
          isNumberInRange("depthLevel", 1 to 100).
          isLengthInRange(parameters.foldLeft(List[String]())((l, p) => if (p._1.matches("excludeKey\\d+")) p._1 :: l else l), 1 to 1024).
          isLengthInRange(parameters.foldLeft(List[String]())((l, p) => if (p._1.matches("includeKey\\d+")) p._1 :: l else l), 1 to 1024)

        if (validator.anyErrors) throw new BadRequestError(validator.toJSON(), "Form validation errors: " + validator.toString)

        bucketProperties.name = parameters.getOrElse("bucketName", List(bucketProperties.name))(0);

        bucketProperties.depthLevel = if (parameters.contains("depthLevel")) parameters.get("depthLevel").get(0).toInt else bucketProperties.depthLevel

        bucketProperties.credentials = if (parameters.contains("accessKeyID") &&
            !parameters("accessKeyID")(0).isEmpty &&
            parameters.contains("secretAccessKey") && 
            !parameters("secretAccessKey")(0).isEmpty) {
        		Option(new AWSCredentials(parameters("accessKeyID")(0), parameters("secretAccessKey")(0)))
        } else bucketProperties.credentials

        bucketProperties.includedPaths = if (parameters.contains("includeKey")) parameters.get("includeKey").get.filter(!_.isEmpty()).toSet[String] else bucketProperties.includedPaths

        bucketProperties.excludedPaths = if (parameters.contains("excludeKey")) parameters.get("excludeKey").get.filter(!_.isEmpty()).toSet[String] else bucketProperties.excludedPaths
        
        Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + bucketProperties.toString())
        
        Ok("OK").withSession(
          request.session + ("uuid" -> uuid))

      } catch {
        case e: S3IndexException => {
          BadRequest(e.response)
        }
      }
  }

  private def getOrInitializeBucketProperties(uuid: String): BucketProperties = {
    Cache.getOrElse[BucketProperties](uuid + ".bucket.properties") {
      new BucketProperties("")
    }
  }

  private def getOrInitializeUUID(request: Request[AnyContent]): String = {
    request.session.get("uuid").map { value =>
      value
    }.getOrElse {
      java.util.UUID.randomUUID().toString();
    }
  }

}