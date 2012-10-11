package com.codeminders.s3simpleclient

import java.io.InputStream
import java.io.OutputStream
import com.codeminders.s3simpleclient.model._
import dispatch._
import scala.xml._
import java.io.File
import scala.io.Source
import java.io.ByteArrayInputStream
import java.io.BufferedInputStream

trait HTTPClient {
  
  def processRequest[T](url: Request, handler: (InputStream, String) => T): T = {
		  Http(url >> { handler })
  }
  
}

class SimpleS3() extends HTTPClient{
  
  private var cred_ : AWSCredentials = null
  
  private def withCredentials(credentials: AWSCredentials):SimpleS3 = {
    cred_ = credentials
    this
  }
  
  def credentials: AWSCredentials = if(cred_ ==  null) throw new IllegalStateException("Please authenticate yourself before signing the request") else cred_
  
  def bucket(name: String): Bucket = new Bucket(this, name)
  
}

object SimpleS3 {
  def apply(credentials: AWSCredentials): SimpleS3 = {
    (new SimpleS3() with HMACSingature).withCredentials(credentials)
  }
  
  def apply() = {
    new SimpleS3()
  }
}