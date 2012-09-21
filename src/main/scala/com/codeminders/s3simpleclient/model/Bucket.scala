package com.codeminders.s3simpleclient.model

import com.codeminders.s3simpleclient._

class Bucket(val client: BasicS3Client, val name: String) {
  
  def list(prefix: String = "", delimiter: String = "/") : KeysTree = {
    new KeysTree(client, "", name, prefix, delimiter)
  }
  
  def listAll() : KeysTree = list(delimiter = "")
  
  def key(name: String, metadata: ObjectMetadata = ObjectMetadata()): Key = {
    new Key(client, name, metadata)
  }
  
}

class KeysTree(val client: BasicS3Client, val name: String, val bucketName: String, prefix: String = "", delimiter: String = "/")  extends Traversable[Key] {
  
  val (keys, commonPrefexes) = client.getBucket(bucketName, prefix, delimiter)
  
  def keyGroups = commonPrefexes map { 
    (e => new KeysTree(client, e, bucketName, e, delimiter)) 
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