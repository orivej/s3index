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

object Application extends Controller {

  def index = Action {
    Redirect(routes.Application.properties)
  }

  def properties = Action {
    implicit request =>
      val uuid = getOrInitializeUUID(request)
      val bucketProperties = getOrInitializeBucketProperties(uuid)
      Ok(views.html.properties("Generate index.html for all files in Amazon S3 bucket. Step 1",
          bucketProperties.name,
          bucketProperties.depthLevel,
          if(bucketProperties.credentials != None) bucketProperties.credentials.get.accessKeyId else "",
          if(bucketProperties.credentials != None) bucketProperties.credentials.get.secretKey else "",
          bucketProperties.excludedPaths.toList,
          bucketProperties.includedPaths.toList
       ))

  }

  def viewProperties = TODO
  
  def generatorStatus = Action {
    Ok(Json.toJson(Map( "status" -> Json.toJson(0), "message" -> Json.toJson("Please wait, we will start processing of your request shortly..."), "percents" -> Json.toJson(math.abs(new Random().nextInt()) % 100) )))
  }

  def generate = Action {
    Ok(views.html.generate("Generate index.html for all files in Amazon S3 bucket. Step 3"))
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

        bucketProperties.credentials = if (parameters.contains("accessKeyID") && parameters.contains("secretAccessKey")) {
          Option(new AWSCredentials(parameters.get("accessKeyID").get(0), parameters.get("secretAccessKey").get(0)))
        } else bucketProperties.credentials

        bucketProperties.includedPaths = if (parameters.exists(_._1.matches("includeKey\\d+"))) {
          parameters.filter(_._1.matches("includeKey\\d+")).foldLeft(List[String]())((l, v) => v._2(0) :: l).toSet
        } else {
          bucketProperties.includedPaths
        }

        bucketProperties.excludedPaths = if (parameters.exists(_._1.matches("excludeKey\\d+"))) {
          parameters.filter(_._1.matches("excludeKey\\d+")).foldLeft(List[String]())((l, v) => v._2(0) :: l).toSet
        } else {
          bucketProperties.includedPaths
        }

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