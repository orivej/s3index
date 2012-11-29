package com.codeminders.s3simpleclient

import scala.xml._

case class AmazonServiceException(statusCode: Int, errorCode: String, message: String, resource: String, requestId: String) extends RuntimeException(message) {
  
}

object AmazonServiceException {
  def apply(statusCode: Int, xml: Elem): AmazonServiceException = {
    AmazonServiceException(statusCode, xml \ "Code" text, xml \ "Message" text, xml \ "Resource" text, xml \ "RequestId" text)
  }
}

