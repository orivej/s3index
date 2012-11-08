package controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.cache._
import play.api.cache.Cache
import model._
import play.api.libs.json._

object Application extends Controller {

  def index = Action {
    implicit request =>
       
      val uuid = request.session.get("uuid").map { value =>
        value
      }.getOrElse {
        java.util.UUID.randomUUID().toString();
      }
      val props = Cache.getOrElse[BucketProperties](uuid + ".bucket.properties") {
        println("Generating new Bucket")
        new BucketProperties("test")
      }
      println("BUCKET NAME:" + props.name)
      Ok(views.html.index()).withSession(
        request.session + ("uuid" -> uuid))

  }

  def properties = Action {
    request =>
      println(request.body.asFormUrlEncoded)
      Thread.sleep(5000)
      BadRequest(Json.toJson(Seq(Json.toJson(Map("errorId" -> Json.toJson("bucketName"), "errorMessage" -> Json.toJson("Name Error"))), Json.toJson(Map("errorId" -> Json.toJson("excludePath1"), "errorMessage" -> Json.toJson("Path Error"))))))
  }

}