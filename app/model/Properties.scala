package model

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import org.apache.commons.codec.binary.Base64
import Template._
import FilesListFormat._
import scala.collection._

class Properties(val bucketName: String = "",
  val template: Template = Slim,
  val filesListFormat: FilesListFormat = Standard,
  val maxKeys: Int = 1000,
  val excludedPaths: Set[String] = Set(),
  val includedPaths: Set[String] = Set(),
  val apiVersion: Int = 1) {

  private val compressor = new Compressor()

  def update(json: JsValue): Properties = {
    new Properties((json \ "bucketName").asOpt[String] match {
      case None => this.bucketName
      case Some(s) => s
    },
      Template.withName((json \ "template").asOpt[String] match {
        case None => this.template.toString()
        case Some(s) => s
      }),
      FilesListFormat.withName((json \ "filesformat").asOpt[String] match {
        case None => this.filesListFormat.toString()
        case Some(s) => s
      }),
      (json \ "maxKeys").asOpt[String] match {
        case None => (json \ "excludeKey").asOpt[Int] match {
          case None => this.maxKeys
          case Some(i) => i
        }
        case Some(s) => s.toInt
      },
      (json \ "excludeKey").asOpt[String] match {
        case None => {
          (json \ "excludeKey").asOpt[Seq[String]] match {
            case None => this.excludedPaths
            case Some(seq) => seq.toSet
          }
        }
        case Some(s) => Set(s)
      },
      (json \ "includeKey").asOpt[String] match {
        case None => {
          (json \ "includeKey").asOpt[Seq[String]] match {
            case None => this.includedPaths
            case Some(seq) => seq.toSet
          }
        }
        case Some(s) => Set(s)
      },
      (json \ "apiversion").asOpt[Int] match {
        case None => 1
        case Some(i) => i
      })
  }

  def toJSON(): JsValue = {
    val builder = Map.newBuilder[String, JsValue]
    builder += ("template" -> Json.toJson(template.toString()))
    builder += ("filesformat" -> Json.toJson(filesListFormat.toString()))
    builder += ("apiversion" -> Json.toJson(apiVersion.toString))
    builder += ("maxKeys" -> Json.toJson(maxKeys))
    if (!bucketName.isEmpty()) {
      builder += ("bucketName" -> Json.toJson(bucketName))
    }
    if (!includedPaths.isEmpty) {
      builder += ("includeKey" -> Json.toJson(includedPaths.toSeq))
    }
    if (!excludedPaths.isEmpty) {
      builder += ("excludeKey" -> Json.toJson(excludedPaths.toSeq))
    }
    Json.toJson(builder.result.toMap)
  }

  override def toString: String = {
    "[bucketName: %s, template: %s, filesListFormat: %s, maxKeys: %s, excludedPaths: %s, includedPaths: %s, apiVersion: %d".format(
      bucketName,
      template,
      filesListFormat,
      maxKeys,
      excludedPaths,
      includedPaths,
      apiVersion)
  }

  def toId(): String = {
    new Base64().encodeToString(compressor.compress(Json.stringify(toJSON())))
  }

}

object Properties {

  def apply(json: JsValue): Properties = {
    new Properties().update(json)
  }

}