package com.codeminders.s3simpleclient

import java.io.InputStream
import java.io.OutputStream
import com.codeminders.s3simpleclient.model._
import dispatch._
import scala.xml._

trait BasicS3Client {
  
  protected def sendRequest(url: Request): scala.xml.Node = {
    XML.loadString(Http(url >- { str =>
      str
    }))
  } 
  
  def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = ""): (Array[Key], Array[String]) = ???

  def putObject(objectName: String, metadata: ObjectMetadata, data: InputStream)

  def getObject(objectName: String): (InputStream, ObjectMetadata)
  
}



class SimpleS3() extends BasicS3Client{
  
  private var cred_ : AWSCredentials = null
  
  def credentials: AWSCredentials = if(cred_ ==  null) throw new IllegalStateException("Please authenticate yourself before submitting requests") else cred_
  
  private def credentials_=(cred: AWSCredentials) {
    this.cred_ = cred
  }
  
  def authenticate(credentials: AWSCredentials) {
    this.credentials = credentials
  }
  
  def bucket(name: String): Bucket = new Bucket(this, name)
  
  override def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = ""): (Array[Key], Array[String]) = {

    def extractKey(node: scala.xml.Node): Key =
      node match {
        case <Contents><Key>{ name }</Key><LastModified>{ lastModified }</LastModified><ETag>{ etag }</ETag><Size>{ size }</Size><StorageClass>{ storageClass }</StorageClass></Contents> => 
          new Key(this, name.text, ObjectMetadata(lastModified.text, etag.text, size.text.toInt, storageClass.text))
        case <Contents><Key>{ name }</Key><LastModified>{ lastModified }</LastModified><ETag>{ etag }</ETag><Size>{ size }</Size><Owner><ID>{ ownerId }</ID><DisplayName>{ ownerDisplayName }</DisplayName></Owner><StorageClass>{ storageClass }</StorageClass></Contents> => 
          new Key(this, name.text, ObjectMetadata(lastModified.text, etag.text, size.text.toInt, storageClass.text, new Owner(ownerId.text, ownerDisplayName.text)))
      }

    val xml = sendRequest(url("http://s3.amazonaws.com/%s/?prefix=%s&delimiter=%s&max-keys=%d&marker=%s".format(bucketName, prefix, delimiter, maxKeys, marker)))

    ((xml \ "Contents").foldLeft(Array[Key]())((a, b) => a ++ Array(extractKey(b))), (xml \ "CommonPrefixes" \ "Prefix").foldLeft(Array[String]())((a, b) => a ++ Array(b.text)))
  }

  override def putObject(objectName: String, metadata: ObjectMetadata, data: InputStream) {
    throw new UnsupportedOperationException
  }

  override def getObject(objectName: String): (InputStream, ObjectMetadata) = {
    throw new UnsupportedOperationException
  }
  
}