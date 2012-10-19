package com.codeminders.s3index

import java.io.FileInputStream
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.MutableList
import org.clapper.scalasti.StringTemplateGroup
import java.io.File
import java.io.InputStreamReader
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.codeminders.s3simpleclient._
import com.codeminders.s3simpleclient.model.KeysTree

object S3IndexBuilder {

  val INDEXFILE = "index.html"
  
  def main(args : Array[String]) : Unit = {
    val bucket = args(0)
    println("Connecting to S3 ");
    val s3 = SimpleS3(AWSCredentials(new FileInputStream("etc/AwsCredentials.properties")))
      
    println("Generating index files")
    generateIndex(s3, bucket, s3.bucket(bucket).list())
    println("Done")
  }
  
  def generateIndex(s3 : SimpleS3, bucket: String, root: KeysTree): Unit = {
    val indexName = root.name + INDEXFILE

    println("Generating " + indexName)

    val group = new StringTemplateGroup("mygroup", new File("etc"))
    val template = group.template("index")

    template.setAttributes(Map("title" -> root.name))

    for (g <- root.keyGroups) {
      template.setAggregate("children.{leaf,name}", false, g.name.substring(root.name.size))
    }
    for (k <- root.keys if(indexName != k.name)) {
    	  template.setAggregate("children.{leaf,name,date,size}", true, k.name.substring(root.name.size), k.lastModified, k.objectSize)
    }

    val indexData = template.toString
    val bytes = indexData.getBytes("UTF-8")
    
    val key = s3.bucket(bucket).key(indexName).withACL("public-read").withContentType("text/html; charset=UTF-8")
    
    key <<<  (new ByteArrayInputStream(bytes), bytes.length)
    
    for (g <- root.keyGroups) {
      generateIndex(s3, bucket, g)
    }

  }

}