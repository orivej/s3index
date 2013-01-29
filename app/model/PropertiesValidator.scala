package model

import play.api.libs.json._
import org.apache.commons.lang.StringUtils
import scala.util.matching.Regex

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
    validateWith(key, v => !v.isEmpty() && v.forall(_.isDigit) && range.contains(v.toInt), "The value of this parameter should not be greater than " + range.end + " and not less than " + range.start + ".")
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
  
  def matches(key: String, pattern: String): PropertiesValidator = {
    validateWith(key, v => v.matches(pattern), "The value of this parameter should contain only numbers or a-z characters.")
  }
  
  def anyErrors(): Boolean = {
    !errors.isEmpty
  }

  def toJSON(): JsValue = {
    Json.toJson(errors.foldLeft(List[JsValue]()) {
      (l, e) => Json.toJson(e.foldLeft(Map[String, JsValue]())((m, e) => m ++ Map("elementId" -> Json.toJson(e._1), "errorMessage" -> Json.toJson(e._2)))) :: l
    })
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