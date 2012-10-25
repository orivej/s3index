package com.codeminders.s3simpleclient.model

import java.io.InputStream
import java.io.OutputStream
import org.apache.commons.io.IOUtils
import com.codeminders.s3simpleclient._
import dispatch._
import java.io.ByteArrayInputStream
import scala.io.Source

class Key(val client: HTTPClient, val bucket: Bucket, val name: String) {

  var acl: String = "private"

  var contentType: String = "binary/octet-stream"

  var expires: Long = 0

  var storageClass: String = "STANDARD"

  var objectSize: Long = 0

  var etag: String = ""

  var lastModified: String = ""

  var userMetadata: scala.collection.mutable.Map[String, String] = scala.collection.mutable.HashMap()

  var owner: Owner = new Owner("", "")

  def <<<(data: InputStream, length: Long) {
    val req = url("http://s3.amazonaws.com/%s/%s".format(bucket.name, name)).PUT.copy(
      body = Some(new org.apache.http.entity.InputStreamEntity(data, length))) <:< Map(("Content-Type", contentType), ("x-amz-acl", acl))
    client.processRequest(req, (x: InputStream, charset: String) => None)
  }

  def >>>(out: OutputStream) {
    val req = url("http://s3.amazonaws.com/%s/%s".format(bucket.name, name))
    client.processRequest(req, (x: InputStream, charset: String) => IOUtils.copy(x, out))
  }

  override def toString() = "contentType:%s, expires:%d, storageClass:%s, size:%d, etag:%s, lastModified:%s, userMetadata:[%s]".format(contentType, expires, storageClass, objectSize, etag, lastModified, userMetadata.mkString(", "))

  def withContentType(contentType: String): Key = {
    this.contentType = contentType
    this
  }

  def withExpires(expires: Long): Key = {
    this.expires = expires
    this
  }

  def withStorageClass(storageClass: String): Key = {
    this.storageClass = storageClass
    this
  }

  def withSize(size: Long): Key = {
    this.objectSize = size
    this
  }

  def withEtag(etag: String): Key = {
    this.etag = etag
    this
  }

  def withLastModified(lastModified: String): Key = {
    this.lastModified = lastModified
    this
  }

  def withOwner(owner: Owner): Key = {
    this.owner = owner
    this
  }

  def withACL(acl: String): Key = {
    this.acl = acl
    this
  }
}

object Key {
  def apply(client: HTTPClient, bucket: Bucket, name: String):Key = new Key(client, bucket, name)
  
  def apply(client: HTTPClient, bucket: Bucket, name: String, lastModified:String, etag: String, size: Long, storageClass: String = "STANDARD", owner: Owner = new Owner("", "")):Key = 
    new Key(client, bucket, name).withLastModified(lastModified).withEtag(etag).withSize(size).withStorageClass(storageClass).withOwner(owner)
}