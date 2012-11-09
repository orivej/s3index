package model

import play.api.libs.json._

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

  override def toString(): String = {
    errors.toString
  }

}