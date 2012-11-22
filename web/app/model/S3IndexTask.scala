package model

import com.codeminders.s3simpleclient.AWSCredentials
import java.util.concurrent.atomic.AtomicReference
import java.io.OutputStream
import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class S3IndexTask(id: String,
    var properties: AtomicReference[Option[Properties]] = new AtomicReference(None),
    var status: AtomicReference[TaskStatus] = new AtomicReference(new TaskStatus(StatusType.error, "Please specify your bucket", 0)),
    var result: Option[Array[Byte]] = None){
  
	def updateStatus(newStatus: TaskStatus):Unit = {
	  this.status.set(newStatus)
	}
	
	def storeResult(result: Array[Byte]): Unit = {
	  this.result = Option(result)
	}
}

object OutputOption extends Enumeration {
     type OutputOption = Value
     val ZipArchive, Bucket = Value
     def fromString(value: String): OutputOption = value.trim().toLowerCase() match {
       case "bucket" => Bucket 
       case _ => ZipArchive
     }
}

object TemplateStyle extends Enumeration {
     type TemplateStyle = Value
     val Simple, Slim = Value
     def fromString(value: String): TemplateStyle = value.trim().toLowerCase() match {
       case "simple" => Simple
       case _ => Slim
     }
}

object FileListFormat extends Enumeration {
     type FileListFormat = Value
     val Full, Brief = Value
     def fromString(value: String): FileListFormat = value.trim().toLowerCase() match {
       case "full" => Full
       case _ => Brief
     }
}

import OutputOption._
import TemplateStyle._
import FileListFormat._

case class Properties(name: String,
    credentials: Option[AWSCredentials] = None,
    outputOption: OutputOption = ZipArchive,
    excludedPaths: Set[String] = Set("*/index.html"),
    includedPaths: Set[String] = Set(),
    template: TemplateStyle = Simple,
    fileListFormat: FileListFormat = Full, 
    directoriesAreLinks: Boolean = true,
    filesAreLinks: Boolean = true,
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
          "directoriesAreLinks" -> Json.toJson(directoriesAreLinks),
          "filesAreLinks" -> Json.toJson(filesAreLinks),
          "customCSS" -> Json.toJson(customCSS.toList)))
  }
}
