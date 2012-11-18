package model

import com.codeminders.s3simpleclient.AWSCredentials
import java.util.concurrent.atomic.AtomicReference
import java.io.OutputStream
import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class S3IndexTask(id: String,
    var properties: AtomicReference[Option[Properties]] = new AtomicReference(None),
    var status: AtomicReference[TaskStatus] = new AtomicReference(new TaskStatus(2, "Please specify your bucket", 0)),
    var result: Option[Array[Byte]] = None){
}

case class Properties(name: String,
    credentials: Option[AWSCredentials] = None,
    depthLevel: Int = 100,
    excludedPaths: Set[String] = Set("*/index.html"),
    includedPaths: Set[String] = Set(),
    template: String = "Simple",
    fileListFormat: String = "Full", 
    directoriesAreLinks: Boolean = true,
    filesAreLinks: Boolean = true,
    customCSS: Set[String] = Set()) {
  
  def toJSON(): JsValue = {
    Json.toJson(
        Map("bucketName" -> Json.toJson(name),
          "accessKeyID" -> Json.toJson(if (credentials != None) credentials.get.accessKeyId else ""),
          "secretAccessKey" -> Json.toJson(if (credentials != None) credentials.get.secretKey else ""),
          "depthLevel" -> Json.toJson(depthLevel),
          "includeKey" -> Json.toJson(includedPaths.toList),
          "excludeKey" -> Json.toJson(excludedPaths.toList),
          "template" -> Json.toJson(template),
          "fileListFormat" -> Json.toJson(fileListFormat),
          "directoriesAreLinks" -> Json.toJson(directoriesAreLinks),
          "filesAreLinks" -> Json.toJson(filesAreLinks),
          "customCSS" -> Json.toJson(customCSS.toList)))
  }
}
