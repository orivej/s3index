package com.codeminders.s3simpleclient

import scala.xml._

class AmazonServiceException(val statusCode: Int, xml: Elem) extends Exception("%d: %s".format(statusCode, xml.toString)) {
  lazy val errorCode = xml \ "Code" text
  lazy val message = xml \ "Message" text
  lazy val resource = xml \ "Resource" text
  lazy val requestId = xml \ "RequestId" text
}