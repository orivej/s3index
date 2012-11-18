package model

import play.api.libs.json._
import com.codeminders.s3simpleclient.AWSCredentials

class PropertiesValidator(properties: Map[String, Seq[String]], errors: List[Map[String, String]] = Nil) {

  def containsAndNotEmpty(key: String): PropertiesValidator = {
    if (!properties.contains(key) || properties(key).isEmpty || properties(key)(0).isEmpty())
      new PropertiesValidator(this.properties, newError(key, "Please fill in this parameter.") :: errors)
    else this
  }

  def newError(propertyId: String, message: String): Map[String, String] = {
    Map(propertyId -> message)
  }
  
  def isNumberInRange(key: String, range: Range): PropertiesValidator = {
    def notInRange(key: String, range: Range): PropertiesValidator = {
      new PropertiesValidator(this.properties, newError(key, "The value of this parameter should not be greater than " + range.end + " and not less than " + range.start + ".") :: errors)
    }
    try {
      if (properties.contains(key) && !properties(key).isEmpty && properties(key).exists(v =>
        v.isEmpty() || !range.contains(v.toInt)))
        notInRange(key, range)
      else this
    } catch {
      case e: NumberFormatException => notInRange(key, range)
    }
  }

  def isNumber(key: String): PropertiesValidator = {
    if (properties.contains(key) && !properties(key).isEmpty && properties(key).forall(_.forall(!_.isDigit)))
      new PropertiesValidator(this.properties, newError(key, "The value of this parameter should be a number.") :: errors)
    else this
  }
  
  def isLengthInRange(key: String, range: Range): PropertiesValidator = {
    if (properties.contains(key) && !properties(key).isEmpty && properties(key).forall(s => !range.contains(s.length())))
      new PropertiesValidator(this.properties, newError(key, "The length of this parameter should not be greater than " + range.end + " and not less than " + range.start + ".") :: errors)
    else this
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
      val depthLevel = if (properties.contains("depthLevel") && !properties.get("depthLevel").isEmpty && !properties.get("depthLevel").get(0).isEmpty) properties.get("depthLevel").get(0).toInt else current.depthLevel
      val includedPaths = if (properties.contains("includeKey")) properties.get("includeKey").get.filter(!_.isEmpty()).toSet[String] else current.includedPaths
      val excludedPaths = if (properties.contains("excludeKey")) properties.get("excludeKey").get.filter(!_.isEmpty()).toSet[String] else current.excludedPaths
      val template = if (properties.contains("template")) properties.get("template").get(0) else current.template
      val fileListFormat = if (properties.contains("fileListFormat")) properties.get("fileListFormat").get(0) else current.fileListFormat
      val directoriesAreLinks = if (properties.contains("directoriesAreLinks") && !properties.get("directoriesAreLinks").isEmpty && !properties.get("directoriesAreLinks").get(0).isEmpty) parseBoolean(properties.get("directoriesAreLinks").get(0)) else current.directoriesAreLinks
      val filesAreLinks = if (properties.contains("filesAreLinks") && !properties.get("filesAreLinks").isEmpty && !properties.get("filesAreLinks").get(0).isEmpty) parseBoolean(properties.get("filesAreLinks").get(0)) else current.filesAreLinks
      val customCSS = if (properties.contains("customCSS")) properties.get("customCSS").get.filter(!_.isEmpty()).toSet[String] else current.customCSS
	  Properties(name, credentials, depthLevel, excludedPaths, includedPaths, template, fileListFormat, directoriesAreLinks, filesAreLinks, customCSS)
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