package controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.cache._
import play.api.cache.Cache
import model._
import S3IndexException._
import play.api.libs.json._
import java.util.Random
import play.api.GlobalSettings
import java.io.OutputStream
import java.io.InputStream
import play.api.libs.iteratee.Enumerator
import java.io.ByteArrayInputStream
import play.api.templates.Html
import java.net.URL
import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import com.codeminders.scalaws.s3.AWSS3
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringEscapeUtils
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import com.codeminders.scalaws.s3.model.S3ObjectSummary
import com.codeminders.scalaws.s3.model.StorageClass._
import com.codeminders.scalaws.s3.model.Owner
import java.util.Date
import com.codeminders.scalaws.s3.model.StorageClass
import org.apache.commons.lang.StringUtils
import com.codeminders.scalaws.AmazonServiceException
import com.codeminders.scalaws.AmazonClientException
import com.yahoo.platform.yui.compressor.JavaScriptCompressor
import com.yahoo.platform.yui.compressor.YUICompressor
import com.googlecode.htmlcompressor.compressor.ClosureJavaScriptCompressor
import play.api.i18n.Messages

object Application extends Controller {

  private val s3Client = AWSS3()

  private val htmlCompressor = new HtmlCompressor()

  private val javascriptCompressor = new ClosureJavaScriptCompressor

  htmlCompressor.setRemoveIntertagSpaces(true)

  private val indexGenerator = new IndexGenerator(s3Client, globals.settings.backreferenceUrl.toString(), 20)

  private val generalPropertiesPageTemplate = views.html.pages.generalProperties(globals.settings)

  private val viewPropertiesPageTemplate = views.html.pages.viewProperties(globals.settings)(_, _)

  private val finalPageTemplate = views.html.pages.finalPage(globals.settings)(_)

  private val propertiesValidator = new PropertiesValidator().
    isLengthInRange("bucketName", 3 to 63).
    matches("bucketName", """[a-zA-Z].[a-zA-Z0-9\-\.]*""", "The value of this parameter should conform with DNS requirements.").
    isNumber("maxKeys").
    isNumberInRange("maxKeys", 1 to 2000000000).
    isLengthInRange("excludeKey", 1 to 1024).
    isLengthInRange("includeKey", 1 to 1024).
    oneOf("template", Template.values.foldLeft(List[String]())((l, v) => v.toString() :: l)).
    oneOf("filesformat", FilesListFormat.values.foldLeft(List[String]())((l, v) => v.toString() :: l))

  private val compressedAPI1 = javascriptCompressor.compress(views.html.api1(globals.settings.backreferenceUrl.toString()).body)

  def index = Action {
    Redirect(routes.Application.generalPropertiesPage)
  }

  def generalPropertiesPage = Action {
    Ok(generalPropertiesPageTemplate)
  }

  def viewPropertiesPage = Action {
    request =>
      if (!arePrimaryPropertiesSet(request)) Redirect(routes.Application.generalPropertiesPage)
      else Ok(viewPropertiesPageTemplate(Template.values.map(_.toString).toSeq, FilesListFormat.values.map(_.toString).toSeq))
  }

  def finalPage = Action {
    request =>
      if (!arePrimaryPropertiesSet(request)) Redirect(routes.Application.generalPropertiesPage)
      else {
        val uuid = getOrInitializeUUID(request)
        val properties = getOrInitializeProperties(uuid)
        Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + properties.toString())
        Ok(finalPageTemplate(properties.toJSON.toString))
      }
  }

  def preview(template: String, fileListFormat: String) = Action {

    val objs = Seq(
      new S3ObjectSummary("my-image.jpg") {
        val size: Long = 1024 * 1024 * 3
        val etag: String = "fba9dede5f27731c9771645a39863328"
        val lastModified: Date = new Date()
        val storageClass: StorageClass = StorageClass.STANDARD
        val owner: Owner = new Owner("75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a", "mtd@amazon.com")
      },
      new S3ObjectSummary("my-second-image.jpg") {
        val size: Long = 1024 * 1024 * 3
        val etag: String = "fba9dede5f27731c9771645a39863328"
        val lastModified: Date = new Date()
        val storageClass: StorageClass = StorageClass.STANDARD
        val owner: Owner = new Owner("75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a", "mtd@amazon.com")
      })

    Ok(indexGenerator.html(
      "mybucket",
      "",
      objs,
      Seq("January/", "February/"),
      Template.withName(template),
      FilesListFormat.withName(fileListFormat),
      (1, Array.empty[Int])))
  }

  def jsonp = Action {
    request =>
      val callback = request.queryString.getOrElse("callback", Seq("S3Index.JSONP.success"))(0)
      try {
        val prefix = request.queryString.getOrElse("prefix", Seq(""))(0)
        val marker = request.queryString.getOrElse("marker", Seq(""))(0)

        val json = Json.toJson(request.queryString.filter(!_._2.isEmpty).map {
          e =>
            (e._1 -> (e._2 match {
              case Seq(s) => Json.toJson(s)
              case Seq(x, xs @ _*) => Json.toJson(e._2)
            }))
        })
        propertiesValidator.validate(json)
        val properties = Properties(json)
        Ok(Cache.getOrElse[Html]("%s.%s.%s".format(properties.toId, prefix, marker), globals.settings.apiCacheExpirationPeriodSec) {
          Html("""var data = {"html": "%s"}; %s(data);""".format(StringEscapeUtils.escapeJavaScript(
            try {
              Logger.debug("Cache miss, properties -> " + properties)
              val index = indexGenerator.generate(properties, prefix, marker.toInt).body
              htmlCompressor.compress(index)
            } catch {
              case e: AmazonServiceException => {
                Logger.error("Could not process request %s".format(request), e)
                views.html.templates.error("S3 request failed: " + e.message).body
              }
            }), callback))
        }).as("text/javascript")
      } catch {
        case e: PropertiesValidationError => {
          Logger.error(e.getMessage, e)
          Ok(Html("""var data = {"html": "%s"}; %s(data);""".format(
            StringEscapeUtils.escapeJavaScript(views.html.templates.error(e.errors.toString).body), callback)))
        }
        case e: Throwable => {
          Logger.error("Could not process request %s".format(request), e)
          Ok(Html("""var data = {"html": "%s"}; %s(data);""".format(
            StringEscapeUtils.escapeJavaScript(views.html.templates.error("Oops, an iternal error occured. Please try again later.").body), callback)))
        }
      }
  }

  def api1() = Action {
    Ok(compressedAPI1).as("text/javascript")
  }

  def properties = Action {
    implicit request =>
      val uuid = getOrInitializeUUID(request)
      val properties = getOrInitializeProperties(uuid)
      Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + properties.toString())

      Ok(properties.toJSON()).withSession(request.session + ("uuid" -> uuid))
  }

  def validateProperties() = Action {
    request =>
      val json = request.body.asJson.getOrElse(throw new Exception("Could not parse empty POST request"))
      try {
        propertiesValidator.validate(json)
        val properties = new Properties().update(json)
        s3Client(properties.bucketName).list("", "", 1, "").take(1) // ensure that service can access specified bucket
        Ok(Json.toJson(""))
      } catch {
        case e: AmazonServiceException => BadRequest(Json.toJson(Map("bucketName" -> "Could not list objects in this bucket, %s".format(Json.toJson(e.message)))))
        case e: AmazonClientException => BadRequest(Json.toJson(Map("bucketName" -> "Could not list objects in this bucket, %s".format(Json.toJson(e.getMessage())))))
        case e: PropertiesValidationError => BadRequest(e.errors)
      }
  }

  def setProperties() = Action {
    request =>
      val uuid = getOrInitializeUUID(request)
      try {
        val json = request.body.asJson.getOrElse(throw new Exception("Could not parse empty POST request"))
        propertiesValidator.validate(json)
        val np = getOrInitializeProperties(uuid).update(json)
        updateProperties(uuid, np)
        Ok(Json.toJson("")).withSession(request.session + ("uuid" -> uuid))
      } catch {
        case e: PropertiesValidationError => BadRequest(e.errors).withSession(request.session + ("uuid" -> uuid))
      }
  }

  private def getOrInitializeProperties(uuid: String): Properties = {
    Cache.getOrElse[Properties](uuid + ".properties") {
      new Properties()
    }
  }

  private def updateProperties(uuid: String, properties: Properties) {
    Cache.set(uuid + ".properties", properties)
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

  private def arePrimaryPropertiesSet(request: Request[AnyContent]): Boolean = {
    val bucketName = getOrInitializeProperties(getOrInitializeUUID(request)).bucketName
    !bucketName.isEmpty()
  }

}