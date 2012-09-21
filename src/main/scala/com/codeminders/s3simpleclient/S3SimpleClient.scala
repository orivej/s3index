package com.codeminders.s3simpleclient

import java.io.InputStream
import java.io.OutputStream
import com.codeminders.s3simpleclient.model._
import com.codeminders.s3simpleclient.model.Key
import com.codeminders.s3simpleclient.model.ObjectMetadata

trait BasicS3Client {
  def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = "") : (Array[Key], Array[String])
  def putObject(objectName: String, metadata: ObjectMetadata, data: InputStream)
  def getObject(objectName: String): (InputStream, ObjectMetadata)
}

abstract class SimpleS3 extends BasicS3Client{
  def bucket(name: String): Bucket = new Bucket(this, name)
}