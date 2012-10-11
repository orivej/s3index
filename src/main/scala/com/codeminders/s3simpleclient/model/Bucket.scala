package com.codeminders.s3simpleclient.model

import com.codeminders.s3simpleclient._
import dispatch._
import scala.xml._
import java.io.InputStream
import java.io.ByteArrayInputStream
import scala.io.Source

class Bucket(val client: HTTPClient, val name: String) {
  
  def list(prefix: String = "", delimiter: String = "/") : KeysTree = {
    new KeysTree(client, "", this, prefix, delimiter)
  }
  
  def listAll() : KeysTree = list(delimiter = "")
  
  def key(name: String, metadata: ObjectMetadata = ObjectMetadata()): Key = {
    new Key(client, this, name, metadata)
  }
  
  def getBucket(prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = ""): (Array[Key], Array[String]) = {

    def extractKey(node: scala.xml.Node): Key =
      node match {
        case <Contents><Key>{ name }</Key><LastModified>{ lastModified }</LastModified><ETag>{ etag }</ETag><Size>{ size }</Size><StorageClass>{ storageClass }</StorageClass></Contents> => 
          new Key(client, this, name.text, ObjectMetadata(lastModified.text, etag.text, size.text.toInt, storageClass.text))
        case <Contents><Key>{ name }</Key><LastModified>{ lastModified }</LastModified><ETag>{ etag }</ETag><Size>{ size }</Size><Owner><ID>{ ownerId }</ID><DisplayName>{ ownerDisplayName }</DisplayName></Owner><StorageClass>{ storageClass }</StorageClass></Contents> => 
          new Key(client, this, name.text, ObjectMetadata(lastModified.text, etag.text, size.text.toInt, storageClass.text, new Owner(ownerId.text, ownerDisplayName.text)))
      }
    
    val xml = client.processRequest(url("http://s3.amazonaws.com/%s/?prefix=%s&delimiter=%s&max-keys=%d&marker=%s".format(name, prefix, delimiter, maxKeys, marker)), (is:InputStream, charset:String) => XML.load(is))
    
    ((xml \ "Contents").foldLeft(Array[Key]())((a, b) => a ++ Array(extractKey(b))), (xml \ "CommonPrefixes" \ "Prefix").foldLeft(Array[String]())((a, b) => a ++ Array(b.text)))
  }
  
}

class KeysTree(val client: HTTPClient, val name: String, val bucket: Bucket, prefix: String = "", delimiter: String = "/")  extends Traversable[Key] {
  
  val (keys, commonPrefexes) = bucket.getBucket(prefix, delimiter)
  
  //TODO: Memoized
  def keyGroups = commonPrefexes map { 
    (e => new KeysTree(client, e, bucket, e, delimiter)) 
  }
  
  def keysNumber = keys.size
  
  def foreach[U](f: Key => U) = {
    keys.foreach(f)
    if (keyGroups.size > 0) {
      keyGroups.foreach(_.foreach(f))
    }
  }
  
  def groupsNumber = keyGroups.size
  
}