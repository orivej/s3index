package com.codeminders.s3simpleclient

import scala.xml._

class AmazonServiceException(val statusCode: Int, xml: Elem) extends Exception("%d: %s".format(statusCode, xml.toString)) {
  lazy val errorCode = xml \ "Code" text
  lazy val message = xml \ "Message" text
  lazy val resource = xml \ "Resource" text
  lazy val requestId = xml \ "RequestId" text
}

class NoSuchBucketException(xml: Elem) extends AmazonServiceException(404, xml){
  lazy val bucketName = xml \ "BucketName" text
  lazy val hostId = xml \ "HostId" text
}

object AmazonServiceException{
  def apply(statusCode: Int, xml: Elem): AmazonServiceException = {
    xml \ "Code" head match {
      case <Code>NoSuchBucket</Code> => new NoSuchBucketException(xml)
      case _ => new AmazonServiceException(statusCode, xml)
    }
  }
}
