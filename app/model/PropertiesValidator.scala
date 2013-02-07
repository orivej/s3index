package model

import play.api.libs.json._
import org.apache.commons.lang.StringUtils
import scala.util.matching.Regex

class PropertiesValidator(rules: Seq[JsValue => Seq[Option[(String, String)]]] = Seq.empty)  {

  private def validateWith(key: String, rule: String => Option[(String, String)])(properties: JsValue): Seq[Option[(String, String)]] = {
    (properties \ key).asOpt[Array[String]] match {
      case None => (properties \ key).asOpt[String] match {
        case None => Seq(None)
        case Some(s) => Seq(rule(s))
      }
      case Some(a) => a.map(rule(_))
    }
  }
  
  private def rule(condition: String => Boolean, key: String, message: String): (String => Option[(String, String)]) = {
    def r(v: String): Option[(String, String)] = {
      condition(v) match {
        case true => None
        case false => Some((key, message))
      } 
    }
    r(_)
  }

  def oneOf(key: String, values: Seq[String]): PropertiesValidator = {
    new PropertiesValidator(
        rules :+ 
        (validateWith(key, rule(v => values.exists(s => s == v), key, "The value of this parameter should be one of: " + values.mkString(", ") + "."))(_)))
  }

  def isNumberInRange(key: String, range: Range): PropertiesValidator = {
    new PropertiesValidator(
        rules :+
        (validateWith(key, rule(
            v => !v.isEmpty() && v.forall(_.isDigit) && range.contains(v.toInt),
            key,
            "The value of this parameter should be greater than " + (range.start - 1) + " and less than " + (range.end + 1) + ".")
            )(_)
        )
     )
  }

  def isNumber(key: String): PropertiesValidator = {
    new PropertiesValidator(
        rules :+
        (validateWith(key, rule(
            v =>
              try{
            	  v.forall(_.isDigit)
              } catch {
                case nfe: NumberFormatException => false
              },
              key,
            "The value of this parameter should be a number.")
            )(_)
        )
     )
  }

  def isLengthInRange(key: String, range: Range): PropertiesValidator = {
    new PropertiesValidator(
        rules :+
        (validateWith(key, rule(
            v => range.contains(v.length()),
            key,
            "The length of this parameter should be greater than " + (range.start - 1) + " and less than " + (range.end + 1) + ".")
            )(_)
        )
     )
  }

  def isLengthInRange(key: Seq[String], range: Range): PropertiesValidator = {
    key.foldLeft(this) {
      (pv, k) =>
        pv.isLengthInRange(k, range)
    }
  }

  def matches(key: String, pattern: String, message: String): PropertiesValidator = {
    new PropertiesValidator(
        rules :+
        (validateWith(key, rule(
            v => v.matches(pattern),
            key,
            message)
            )(_)
        )
     )
  }

  def validate(json: JsValue) {
    val errors = (for(rule <- rules) yield{
      rule(json)
    }).filter(!_.isEmpty).map(_.filter(!_.isEmpty).map(_.get)).flatten
    if(errors.size > 0) {
      throw new PropertiesValidationError(toJson(errors), "Properties validation failed: %s".format(errors))
    }
  }
  
  private def toJson(validationErrors: Seq[(String, String)]): JsValue = {
        val errsGroupedByName = validationErrors.groupBy(e => e._1)
        val e = errsGroupedByName.map {
          e =>
            (e._1, Json.toJson(e._2.map(_._2).mkString(" ")))
        }
        Json.toJson(e)
  }
  
  private def parseBoolean(value: String): Boolean = {
    val numberFormar = """[\d]+""".r
    value.toLowerCase().trim() match {
      case "on" | "true" | "1" => true
      case numberFormar(n) => n != 0
      case _ => false
    }
  }

}