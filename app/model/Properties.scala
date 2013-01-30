package model

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import org.apache.commons.codec.binary.Base64
import Template._
import FilesListFormat._

class Properties(val bucketName: String = "",
  val template: Template = Slim,
  val filesListFormat: FilesListFormat = Standard,
  val maxKeys: Int = 1000,
  val excludedPaths: Set[String] = Set(),
  val includedPaths: Set[String] = Set(),
  val apiVersion: Int = 1) {
  
  def update(parameters: Map[String, Seq[String]]): Properties = {
    new Properties(
      parameters.getOrElse("bucketName", Seq(this.bucketName))(0),
      Template.withName(parameters.getOrElse("template", Seq(this.template.toString()))(0)),
      FilesListFormat.withName(parameters.getOrElse("filesformat", Seq(this.filesListFormat.toString()))(0)),
      parameters.getOrElse("maxKeys", Seq(this.maxKeys.toString()))(0).toInt,
      parameters.getOrElse("includeKey", this.includedPaths).filter(!_.isEmpty()).toSet,
      parameters.getOrElse("excludeKey", this.excludedPaths).filter(!_.isEmpty()).toSet)
  }

  def toJSON(): JsValue = {
    Json.toJson(
      Map("bucketName" -> Json.toJson(bucketName),
        "includeKey" -> Json.toJson(includedPaths.toSeq),
        "excludeKey" -> Json.toJson(excludedPaths.toSeq),
        "template" -> Json.toJson(template.toString()),
        "maxKeys" -> Json.toJson(maxKeys),
        "filesformat" -> Json.toJson(filesListFormat.toString()),
        "apiversion" -> Json.toJson(apiVersion.toString)))
  }
  
  def toId(): String = {
    new Base64().encodeToString(new Compressor().compress(Json.stringify(toJSON())))
  }
  
  override def toString: String = {
    "[bucketName: %s, template: %s, filesListFormat: %s, maxKeys: %s, excludedPaths: %s, includedPaths: %s, apiVersion: %d".format(
        bucketName,
        template,
        filesListFormat,
        maxKeys,
        excludedPaths,
        includedPaths,
        apiVersion
        )
  }

}

object Properties {
  
  def apply(json: JsValue): Properties = {
    new Properties((json \ "bucketName").asOpt[String] match {
      case None => throw new BadRequestError(Json.toJson("Your ID doesn't contain a valid bucket name"), "Could not extract bucket name from %s".format(json.toString) )
      case Some(s) => s
    },
    Template.withName((json \ "template").asOpt[String] match {
      case None => "Simple"
      case Some(s) => s
    }),
    FilesListFormat.withName((json \ "filesformat").asOpt[String] match {
      case None => "Standard"
      case Some(s) => s
    }),
    (json \ "maxKeys").asOpt[Int] match {
      case None => Int.MaxValue
      case Some(i) => i
    },
    (json \ "excludeKey").asOpt[Seq[String]] match {
      case None => Set.empty[String]
      case Some(s) => s.toSet
    },
    (json \ "includeKey").asOpt[Seq[String]] match {
      case None => Set.empty[String]
      case Some(s) => s.toSet
    },
    (json \ "apiversion").asOpt[Int] match {
      case None => 1
      case Some(i) => i
    }
    )
  }
  
  def fromId(pid: String): Properties = {
    apply(Json.parse(new Compressor().decompressToString(new Base64().decode(pid))))
  }
}