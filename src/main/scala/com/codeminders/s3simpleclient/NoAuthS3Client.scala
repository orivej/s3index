package com.codeminders.s3simpleclient

import java.io.InputStream
import dispatch._
import scala.xml._
import com.codeminders.s3simpleclient.model.Key
import com.codeminders.s3simpleclient.model.ObjectMetadata

trait NoAuthS3Client extends BasicS3Client {
  def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = ""): (Array[Key], Array[String]) = {

    def extractKey(node: scala.xml.Node): Key =
      node match {
        case <Contents><Key>{ name }</Key><LastModified>{ lastModified }</LastModified><ETag>{ etag }</ETag><Size>{ size }</Size><StorageClass>{ storageClass }</StorageClass></Contents> => 
          new Key(this, name.text, ObjectMetadata(lastModified.text, etag.text, size.text.toInt, storageClass.text))
      }

    val req = url("http://%s.s3.amazonaws.com/?prefix=%s&delimiter=%s&max-keys=%d&marker=%s".format(bucketName, prefix, delimiter, maxKeys, marker))
    val xml = XML.loadString(Http(req >- { str =>
      str
    }))

    ((xml \ "Contents").foldLeft(Array[Key]())((a, b) => a ++ Array(extractKey(b))), (xml \ "CommonPrefixes" \ "Prefix").foldLeft(Array[String]())((a, b) => a ++ Array(b.text)))
  }

  override def putObject(objectName: String, metadata: ObjectMetadata, data: InputStream) {
    throw new UnsupportedOperationException
  }

  override def getObject(objectName: String): (InputStream, ObjectMetadata) = {
    throw new UnsupportedOperationException
  }
}