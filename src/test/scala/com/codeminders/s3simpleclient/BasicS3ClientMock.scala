package com.codeminders.s3simpleclient

import java.io.InputStream
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import com.codeminders.s3simpleclient.model.Key
import com.codeminders.s3simpleclient.model.ObjectMetadata

trait BasicS3ClientMock extends BasicS3Client {
  
  private val objects: Map[String, Map[String, String]] = new HashMap
  
  override def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = "") : (Array[Key], Array[String]) = prefix match {
    case "" => (Array(Key(BasicS3ClientMock.this, "1", ObjectMetadata("2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD"))), Array("dir1/", "dir2/", "dir3/"))
    case "dir1/" => (Array(Key(BasicS3ClientMock.this, "dir1/3", ObjectMetadata("2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD"))), Array())
    case "dir2/" => (Array(Key(BasicS3ClientMock.this, "dir2/2", ObjectMetadata("2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD")), Key(BasicS3ClientMock.this, "dir2/4", ObjectMetadata("2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD"))), Array("dir2/dir4/"))
    case "dir3/" => (Array(Key(BasicS3ClientMock.this, "dir3/5", ObjectMetadata("2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD"))), Array())
    case "dir2/dir4/" => (Array(Key(BasicS3ClientMock.this, "dir2/dir4/6", ObjectMetadata("2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD"))), Array())
  }
  
  override def putObject(objectName: String, metadata: ObjectMetadata, data: InputStream) {
    objects.put(objectName, HashMap("data" -> IOUtils.toString(data, "UTF-8")) ++ metadata.toMap)
  }
  
  override def getObject(objectName: String): (InputStream, ObjectMetadata) = {
    if(objects.contains(objectName)){
    	(new ByteArrayInputStream(objects(objectName)("data").getBytes("UTF-8")), ObjectMetadata())
    } else {
    	throw new IllegalArgumentException
    }
  }
}