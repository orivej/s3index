package com.codeminders.s3simpleclient.model

import java.io.InputStream
import java.io.OutputStream
import org.apache.commons.io.IOUtils
import com.codeminders.s3simpleclient._
import dispatch._
import java.io.ByteArrayInputStream
import scala.io.Source

case class Key(val client: HTTPClient, val bucket:Bucket, val name: String, metadata: ObjectMetadata = ObjectMetadata()) {
  override def toString() = "{name:%s, %s}".format( name, metadata )
  
  def <<< (data: InputStream, length: Long) {
    putObject(bucket.name, name, metadata, data, length);
  }
  
  def >>>(out: OutputStream) {
    getObject(bucket.name, name, out)
  }
  
  def putObject(bucketName: String, objectName: String, metadata: ObjectMetadata, data: InputStream, length: Long, contentType: String = "text/plain") {
    val req = url("http://s3.amazonaws.com/%s/%s".format(bucketName, objectName)).PUT.copy(
    body=Some(new org.apache.http.entity.InputStreamEntity(data, length))) <:< Map(("Content-Type", contentType))
    client.processRequest(req, (x:InputStream, charset:String) => None)
  }

  def getObject(bucketName: String, objectName: String, out: OutputStream): ObjectMetadata = {
    val req = url("http://s3.amazonaws.com/%s/%s".format(bucketName, objectName))
    client.processRequest(req, (x:InputStream, charset:String) => IOUtils.copy(x, out))
    ObjectMetadata()
  }
}