package com.codeminders.s3simpleclient.model

import scala.collection.immutable.Map
import scala.collection.immutable.HashMap

class ObjectMetadata {
  var contentType:String = "binary/octet-stream"
  var expires: Long = 0
  var storageClass: String = "STANDARD"
  var objectSize: Long = 0
  var etag: String = ""
  var lastModified:String = ""
  var userMetadata: scala.collection.mutable.Map[String, String] = scala.collection.mutable.HashMap()
  
  def withContentType(contentType:String):ObjectMetadata = {
    this.contentType = contentType
    this
  }
  
  def withExpires(expires: Long):ObjectMetadata = {
    this.expires = expires
    this
  }
  
  def withStorageClass(storageClass: String):ObjectMetadata = {
    this.storageClass = storageClass
    this
  }
  
  def withSize(size: Long):ObjectMetadata = {
    this.objectSize = size
    this
  }
  
  def withEtag(etag: String):ObjectMetadata = {
    this.etag = etag
    this
  }
  
  def withLastModified(lastModified: String):ObjectMetadata = {
    this.lastModified = lastModified
    this
  }
  
  def toMap(): Map[String, String] = {
    HashMap("contentType" -> contentType, 
       "expires" -> expires.toString(),
       "storageClass" -> storageClass,
       "objectSize" -> objectSize.toString(),
       "etag" -> etag,
       "lastModified" -> lastModified) ++ userMetadata
  }
  
  override def toString() = "contentType:%s, expires:%d, storageClass:%s, size:%d, etag:%s, lastModified:%s, userMetadata:[%s]".format( contentType
      , expires, storageClass, objectSize, etag, lastModified, userMetadata.mkString(", ") ) 
}

object ObjectMetadata {
  def apply() = new ObjectMetadata()
  
  def apply(lastModified:String, etag: String, size: Long, storageClass: String = "STANDARD"):ObjectMetadata = 
    new ObjectMetadata().withLastModified(lastModified).withEtag(etag).withSize(size).withStorageClass(storageClass)
}