package model

import com.codeminders.s3simpleclient.AWSCredentials
import java.util.concurrent.atomic.AtomicReference
import java.io.OutputStream
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.templates.Html
import com.codeminders.s3simpleclient.model.Key
import com.codeminders.s3simpleclient.model.KeysTree
import play.api.Configuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.URL

case class S3IndexTask(id: String,
  var properties: AtomicReference[Option[Properties]] = new AtomicReference(None),
  var status: AtomicReference[TaskStatus] = new AtomicReference(new TaskStatus(StatusType.error, "Please specify your bucket", 0)),
  var result: Option[Array[Byte]] = None) {

  def updateStatus(newStatus: TaskStatus): Unit = {
    this.status.set(newStatus)
  }

  def storeResult(result: Array[Byte]): Unit = {
    this.result = Option(result)
  }
}

object OutputOption extends Enumeration {
  type OutputOption = Value
  val ZipArchive, Bucket = Value
}

object TemplateStyle extends Enumeration {
  type TemplateStyle = Value
  val Simple, Slim = Value
}

object FileListFormat extends Enumeration {
  type FileListFormat = Value
  val Full, Brief, NameDate, NameSize = Value
  def toHtml(format: FileListFormat): (Option[(KeysTree, Key)]) => List[Html] = format match {
    case Full => (k: Option[(KeysTree, Key)]) => k match {
        case None => List(Html("Name"), Html("Date"), Html("Size"), Html("Storage Class"), Html("Owner"))
        case Some((g,k)) => {
          val name = k.name.substring(g.name.size)
          List(Html("""<div class="file"><a href="%s">%s</a></div>""".format(name, name)), Html(k.lastModified), Html(k.objectSize.toString), Html(k.storageClass), Html(k.owner.displayName)) 
        }
      } 
    case Brief => (k: Option[(KeysTree, Key)]) => k match {
      case None => List(Html("Name"), Html("Date"), Html("Size"))
      case Some((g,k)) => {
        val name = k.name.substring(g.name.size)
        List(Html("""<div class="file"><a href="%s">%s</a></div>""".format(name, name)), Html(k.lastModified), Html(k.objectSize.toString))
      }
    }
    case NameDate => (k: Option[(KeysTree, Key)]) => k match {
      case None => List(Html("Name"), Html("Date"))
      case Some((g,k)) => {
        val name = k.name.substring(g.name.size)
        List(Html("""<div class="file"><a href="%s">%s</a></div>""".format(name, name)), Html(k.lastModified))
      } 
    }
    case NameSize => (k: Option[(KeysTree, Key)]) => k match {
      case None => List(Html("Name"), Html("Size"))
      case Some((g,k)) => {
        val name = k.name.substring(g.name.size)
        List(Html("""<div class="file"><a href="%s">%s</a></div>""".format(name, name)), Html(k.objectSize.toString))
      }
    }
  }

}

import OutputOption._
import TemplateStyle._
import FileListFormat._
import model.Utils._

class Properties(configuration: Configuration,
  val name: String = "",
  val credentials: Option[AWSCredentials] = None,
  val outputOption: OutputOption = ZipArchive,
  val excludedPaths: Set[String] = Set("*index.html", "css/*", "img/*"),
  val includedPaths: Set[String] = Set(),
  val template: TemplateStyle = Slim,
  val fileListFormat: FileListFormat = Brief,
  val title: String = "Index of @directory",
  val header: String = "Index of @directory",
  val footer: String = """Generated with <a href="%s">S3Index</a> on %s""".format("codeminders.com", new SimpleDateFormat("MMMMM dd yyyy", Locale.US).format(new Date())),
  val customCSS: Set[String] = Set()) {
  
  def stylesLocation:URL = configuration.getString("s3index.styles.location") match {
       case None => "styles"
       case Some(s) => s
  }
  
  def updateProperties(parameters: Map[String, Seq[String]]): Properties = {
      val name = if (parameters.contains("bucketName")) parameters.get("bucketName").get(0) else this.name
      val credentials = if(parameters.contains("accessKeyID") && !parameters("accessKeyID")(0).isEmpty && parameters.contains("secretAccessKey") && !parameters("secretAccessKey")(0).isEmpty)
          Option(new AWSCredentials(parameters("accessKeyID")(0), parameters("secretAccessKey")(0))) else this.credentials
      val outputOption = if (parameters.contains("outputTo") && !parameters.get("outputTo").isEmpty && !parameters.get("outputTo").get(0).isEmpty) OutputOption.withName(parameters.get("outputTo").get(0)) else this.outputOption
      val includedPaths = if (parameters.contains("includeKey")) parameters.get("includeKey").get.filter(!_.isEmpty()).toSet[String] else this.includedPaths
      val excludedPaths = if (parameters.contains("excludeKey")) parameters.get("excludeKey").get.filter(!_.isEmpty()).toSet[String] else this.excludedPaths
      val template = if (parameters.contains("template")) TemplateStyle.withName(parameters.get("template").get(0)) else this.template
      val fileListFormat = if (parameters.contains("fileListFormat")) FileListFormat.withName(parameters.get("fileListFormat").get(0)) else this.fileListFormat
      val footer = if (parameters.contains("footer") && !parameters.get("footer").isEmpty && !parameters.get("footer").get(0).isEmpty) parameters.get("footer").get(0) else this.footer
      val title = if (parameters.contains("title") && !parameters.get("title").isEmpty && !parameters.get("title").get(0).isEmpty) parameters.get("title").get(0) else this.title
      val header = if (parameters.contains("header") && !parameters.get("header").isEmpty && !parameters.get("header").get(0).isEmpty) parameters.get("header").get(0) else this.header
      val customCSS = if (parameters.contains("customCSS")) parameters.get("customCSS").get.filter(!_.isEmpty()).toSet[String] else this.customCSS
	  new Properties(this.configuration, name, credentials, outputOption, excludedPaths, includedPaths, template, fileListFormat, title, header, footer, customCSS)
  }
  
  def toJSON(): JsValue = {
    Json.toJson(
      Map("bucketName" -> Json.toJson(name),
        "accessKeyID" -> Json.toJson(if (credentials != None) credentials.get.accessKeyId else ""),
        "secretAccessKey" -> Json.toJson(if (credentials != None) credentials.get.secretKey else ""),
        "outputTo" -> Json.toJson(outputOption.toString()),
        "includeKey" -> Json.toJson(includedPaths.toList),
        "excludeKey" -> Json.toJson(excludedPaths.toList),
        "template" -> Json.toJson(template.toString()),
        "fileListFormat" -> Json.toJson(fileListFormat.toString()),
        "title" -> Json.toJson(title),
        "header" -> Json.toJson(header),
        "footer" -> Json.toJson(footer),
        "customCSS" -> Json.toJson(customCSS.toList),
        "styles" -> Json.toJson(TemplateStyle.values.foldLeft(List[JsValue]())((l, e) => Json.toJson(e.toString()) :: l)),
        "fileListFormats" -> Json.toJson((for (format <- FileListFormat.values) yield {
	          (format.toString() -> Json.toJson(FileListFormat.toHtml(format)(None).mkString(", ")))
	      }).toMap)))
  }
}
