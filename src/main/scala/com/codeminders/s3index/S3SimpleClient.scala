package com.codeminders.s3index
import sun.reflect.generics.reflectiveObjects.NotImplementedException

class Bucket(val client: S3BasicClient, val name: String) {
  
  def list(prefix: String = "", delimiter: String = "/") : KeysTree = {
    new KeysTree(client, "", name, prefix, delimiter)
  }
  
  def listAll() : KeysTree = list(delimiter = "")
  
}

class KeysTree(val client: S3BasicClient, val name: String, val bucketName: String, prefix: String = "", delimiter: String = "/")  extends Traversable[Key] {
  
  val (keys, commonPrefexes, marker) = client.getBucket(bucketName, prefix, delimiter)
  
  def keyGroups = commonPrefexes map { 
    (e => new KeysTree(client, e, bucketName, prefix + delimiter + e, delimiter)) 
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

class KeysGroup(val name: String,  val keys: Array[Key] = Array()) {
  override def toString() = "%s (%s)".format( name, keys.mkString(",") )   
}

case class Key(val name: String, val lastModified:String, val etag: String, val size: Long, val StorageClass: String ) {
  override def toString() = "%s %s %s %d %s".format( name, lastModified, etag, size, StorageClass ) 
}

trait DispatchS3BasicClient extends S3BasicClient{
  override def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = "") : (Array[Key], Array[String], String) = {
    throw new NotImplementedException
  }
}

trait S3BasicClient {
  def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = "") : (Array[Key], Array[String], String)
}

class SimpleS3 extends DispatchS3BasicClient{
  def bucket(name: String): Bucket = new Bucket(this, name)
}