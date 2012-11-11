package model

import play.api.libs.json.JsValue

class S3IndexException(val statusCode: Int, val response: JsValue, val logMessage: String) extends Exception(logMessage){
}

class InternalError(response: JsValue, logMessage: String) extends S3IndexException(500, response, logMessage) {
}

class BadRequestError(response: JsValue, logMessage: String) extends S3IndexException(400, response, logMessage) {
}

