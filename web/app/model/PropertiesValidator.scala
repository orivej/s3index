package model

import play.api.libs.json._
import com.codeminders.s3simpleclient.AWSCredentials

class PropertiesValidator(properties: Map[String, Seq[String]], errors: List[Map[String, String]] = Nil) {

  def newError(propertyId: String, message: String): Map[String, String] = {
    Map(propertyId -> message)
  }
  
  def validateWith(key: String, f: String => Boolean, errorMessage: String): PropertiesValidator = {
    if (properties.contains(key) && !properties(key).isEmpty && !properties(key).forall(v => f(v)))
        new PropertiesValidator(this.properties, newError(key, errorMessage) :: errors)
    else this
  }
  
  def oneOf(key: String, values: Seq[String]): PropertiesValidator = {
    validateWith(key, v => values.exists(s => s == v), "The value of this parameter should be one of: " + values.mkString(", ") + ".")
  }
  
  def isNumberInRange(key: String, range: Range): PropertiesValidator = {
    validateWith(key, v => v.forall(_.isDigit) && range.contains(v.toInt), "The value of this parameter should not be greater than " + range.end + " and not less than " + range.start + ".")
  }

  def isNumber(key: String): PropertiesValidator = {
    validateWith(key, v => v.forall(_.isDigit), "The value of this parameter should be a number.")
  }
  
  def isLengthInRange(key: String, range: Range): PropertiesValidator = {
    validateWith(key, v => range.contains(v.length()), "The length of this parameter should not be greater than " + range.end + " and not less than " + range.start + ".")
  }
  
  def isLengthInRange(key: Seq[String], range: Range): PropertiesValidator = {
    key.foldLeft(this){
     (pv, k) =>
       pv.isLengthInRange(k, range)
    }
  }
  
  def anyErrors(): Boolean = {
    !errors.isEmpty
  }

  def toJSON(): JsValue = {
    Json.toJson(errors.foldLeft(List[JsValue]()) {
      (l, e) => Json.toJson(e.foldLeft(Map[String, JsValue]())((m, e) => m ++ Map("elementId" -> Json.toJson(e._1), "errorMessage" -> Json.toJson(e._2)))) :: l
    })
  }
  
  def toProperties(current: Properties): Properties = {
      val name = if (properties.contains("bucketName")) properties.get("bucketName").get(0) else current.name
      val credentials = if(properties.contains("accessKeyID") && !properties("accessKeyID")(0).isEmpty && properties.contains("secretAccessKey") && !properties("secretAccessKey")(0).isEmpty)
          Option(new AWSCredentials(properties("accessKeyID")(0), properties("secretAccessKey")(0))) else current.credentials
      val outputOption = if (properties.contains("outputTo") && !properties.get("outputTo").isEmpty && !properties.get("outputTo").get(0).isEmpty) OutputOption.fromString(properties.get("outputTo").get(0)) else current.outputOption
      val includedPaths = if (properties.contains("includeKey")) properties.get("includeKey").get.filter(!_.isEmpty()).toSet[String] else current.includedPaths
      val excludedPaths = if (properties.contains("excludeKey")) properties.get("excludeKey").get.filter(!_.isEmpty()).toSet[String] else current.excludedPaths
      val template = if (properties.contains("template")) TemplateStyle.fromString(properties.get("template").get(0)) else current.template
      val fileListFormat = if (properties.contains("fileListFormat")) FileListFormat.fromString(properties.get("fileListFormat").get(0)) else current.fileListFormat
      val directoriesAreLinks = if (properties.contains("directoriesAreLinks") && !properties.get("directoriesAreLinks").isEmpty && !properties.get("directoriesAreLinks").get(0).isEmpty) parseBoolean(properties.get("directoriesAreLinks").get(0)) else current.directoriesAreLinks
      val filesAreLinks = if (properties.contains("filesAreLinks") && !properties.get("filesAreLinks").isEmpty && !properties.get("filesAreLinks").get(0).isEmpty) parseBoolean(properties.get("filesAreLinks").get(0)) else current.filesAreLinks
      val customCSS = if (properties.contains("customCSS")) properties.get("customCSS").get.filter(!_.isEmpty()).toSet[String] else current.customCSS
	  Properties(name, credentials, outputOption, excludedPaths, includedPaths, template, fileListFormat, directoriesAreLinks, filesAreLinks, customCSS)
  }
  
  private def parseBoolean(value: String): Boolean = {
    val numberFormar = """[\d]+""".r
    value.toLowerCase().trim() match {
	    case "on" | "true" | "1" => true
	    case numberFormar(n) => n != 0
	    case _  => false
    }
  }

  override def toString(): String = {
    errors.toString
  }

}