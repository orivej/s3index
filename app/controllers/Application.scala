package controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.cache._
import play.api.cache.Cache
import model._
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

object Application extends Controller {

  private val s3Client = AWSS3()

  private val htmlCompressor = new HtmlCompressor()

  htmlCompressor.setRemoveIntertagSpaces(true)

  private val indexGenerator = new IndexGenerator(s3Client, globals.settings.backreferenceUrl.toString())

  private val codeTemplate = "<div id=\"s3index-root\" indexid=\"%s\"></div>" +
    "<script src=\"%s/api\" type=\"text/javascript\"></script>" +
    "<script>(function() {S3Index.load()}());</script>"

  def index = Action {
    Redirect(routes.Application.generalPropertiesPage)
  }

  def generalPropertiesPage = Action {
    Ok(views.html.pages.generalProperties(globals.settings.applicationName, globals.settings.applicationDescription, globals.settings.brandName, globals.settings.brandLink, globals.settings.yearUpdated))
  }

  def viewPropertiesPage = Action {
    request =>
      if (!arePrimaryPropertiesSet(request)) Redirect(routes.Application.generalPropertiesPage)
      else Ok(views.html.pages.viewProperties(globals.settings.applicationName, globals.settings.applicationDescription, globals.settings.brandName, globals.settings.brandLink, globals.settings.yearUpdated, Template.values.map(_.toString).toSeq, FilesListFormat.values.map(_.toString).toSeq))
  }

  def finalPage = Action {
    request =>
      if (!arePrimaryPropertiesSet(request)) Redirect(routes.Application.generalPropertiesPage)
      else {
        val uuid = getOrInitializeUUID(request)
        val properties = getOrInitializeProperties(uuid)
        Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + properties.toString())
        val code = codeTemplate.format(properties.toId(), globals.settings.backreferenceUrl)
        Ok(views.html.pages.finalPage(code, globals.settings.applicationName, globals.settings.applicationDescription, globals.settings.brandName, globals.settings.brandLink, globals.settings.yearUpdated))
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
      "",
      ""))
  }

  def jsonp(indexId: String, prefix: String, marker: String, callback: String) = Action {
    Logger.debug(Seq("indexId -> " + indexId, "prefix -> " + prefix, "marker -> " + marker, "callback -> " + callback).mkString(", "))
    Ok(Cache.getOrElse[Html]("%s.%s.%s".format(indexId, prefix, marker)) {
      Html("""var data = {"html": "%s"}; %s(data);""".format(StringEscapeUtils.escapeJavaScript(try {
        val properties = Properties.fromId(indexId)
        Logger.debug("Cache miss, properties -> " + properties)
        val index = indexGenerator.generate(properties, prefix, marker).body
        htmlCompressor.compress(index)
      } catch {
        case e: AmazonServiceException => views.html.templates.error("S3 request failed: " + e.message).body
        case e: Throwable => views.html.templates.error("Oops, an iternal error occured. Please try again later.").body
      }), callback))
    }).as("text/javascript")
  }

  def api() = Action {
    Ok(views.html.api(globals.settings.backreferenceUrl.toString())).as("text/javascript")
  }

  def properties = Action {
    implicit request =>
      val uuid = getOrInitializeUUID(request)
      val properties = getOrInitializeProperties(uuid)
      Logger.debug("UUID -> " + uuid.toString() + ", " + "properties -> " + properties.toString())

      Ok(properties.toJSON())
  }

  def setProperties = Action {
    request =>
      try {
        val uuid = getOrInitializeUUID(request)
        val parameters = request.body.asFormUrlEncoded.getOrElse(throw new InternalError(Json.toJson("Please specify at least one parameter"), "Request body should not be empty"))
        Logger.debug("parameters -> " + parameters.toString())
        val validator = new PropertiesValidator(parameters).
          isLengthInRange("bucketName", 3 to 63).
          matches("bucketName", """[a-zA-Z].[a-zA-Z0-9\-]*""").
          isNumber("depthLevel").
          isNumberInRange("depthLevel", 1 to 100).
          isNumber("maxKeys").
          isNumberInRange("maxKeys", 1 to Int.MaxValue).
          isLengthInRange("excludeKey", 1 to 1024).
          isLengthInRange("includeKey", 1 to 1024).
          isLengthInRange("customCSS", 1 to 1024).
          oneOf("template", Template.values.foldLeft(List[String]())((l, v) => v.toString() :: l)).
          oneOf("outputTo", List("ZipArchive", "Bucket")).
          isLengthInRange("accessKeyID", 1 to 255).
          isLengthInRange("secretAccessKey", 1 to 255)

        if (validator.anyErrors) throw new BadRequestError(validator.toJSON(), "Form validation errors: " + validator.toString)
        else {
          val oldProperties = getOrInitializeProperties(uuid)
          val newProperties = oldProperties.update(parameters)
          if (oldProperties.bucketName != newProperties.bucketName) {
            try {
              s3Client(newProperties.bucketName).list("", "", 1, "").take(1) // ensure that service can access specified bucket
            } catch {
              case e: AmazonServiceException => throw new BadRequestError(Json.toJson(Seq(Json.toJson(Map("elementId" -> "bucketName", "errorMessage" -> e.message)))), e.message)
              case e: AmazonClientException => throw new BadRequestError(Json.toJson(Seq(Json.toJson(Map("elementId" -> "bucketName", "errorMessage" -> e.getMessage)))), e.getMessage)
            }
          }
          updateProperties(uuid, newProperties)
        }

        Ok("OK").withSession(
          request.session + ("uuid" -> uuid))

      } catch {
        case e: S3IndexException => {
          BadRequest(e.response)
        }
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