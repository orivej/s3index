package model

import com.codeminders.s3simpleclient.AWSCredentials
import java.util.concurrent.atomic.AtomicReference
import java.io.OutputStream
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.templates.Html
import com.codeminders.s3simpleclient.model.Key
import com.codeminders.s3simpleclient.model.KeysTree

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
        List(Html("""<div class="file"><a href="%s">%s</a></div>""".format(k.name, k.name)), Html(k.lastModified), Html(k.objectSize.toString))
      }
    }
    case NameDate => (k: Option[(KeysTree, Key)]) => k match {
      case None => List(Html("Name"), Html("Date"))
      case Some((g,k)) => {
        val name = k.name.substring(g.name.size)
        List(Html("""<div class="file"><a href="%s">%s</a></div>""".format(k.name, k.name)), Html(k.lastModified))
      } 
    }
    case NameSize => (k: Option[(KeysTree, Key)]) => k match {
      case None => List(Html("Name"), Html("Size"))
      case Some((g,k)) => {
        val name = k.name.substring(g.name.size)
        List(Html("""<div class="file"><a href="%s">%s</a></div>""".format(k.name, k.name)), Html(k.objectSize.toString))
      }
    }
  }

}

import OutputOption._
import TemplateStyle._
import FileListFormat._

case class Properties(name: String,
  credentials: Option[AWSCredentials] = None,
  outputOption: OutputOption = ZipArchive,
  excludedPaths: Set[String] = Set("*index.html", "css/*", "img/*"),
  includedPaths: Set[String] = Set(),
  template: TemplateStyle = Simple,
  fileListFormat: FileListFormat = Full,
  title: String = "Index of @dir@",
  header: String = "Index of @dir@",
  footer: String = "Created with S3Index (c) 2012",
  customCSS: Set[String] = Set()) {

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
