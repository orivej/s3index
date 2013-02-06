package model

import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class S3IndexException(val statusCode: Int, val response: String, val logMessage: String) extends Exception(logMessage)

class InternalError(response: String, logMessage: String) extends S3IndexException(500, response, logMessage) {
    new S3IndexException(500, response, logMessage)
  }

class PropertiesValidationError(val errors: JsValue, val logMessage: String) extends Exception(logMessage)