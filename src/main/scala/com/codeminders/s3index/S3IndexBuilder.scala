package com.codeminders.s3index

import java.io.FileInputStream
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.MutableList
import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
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
    val pstr = new FileInputStream("etc/AwsCredentials.properties");
    try {
      println("Connecting to S3 ");
      val s3 = new AmazonS3Client(new PropertiesCredentials(pstr));
      
      println("Generating index files")
      generateIndex(s3, bucket, (new SimpleS3()).bucket(bucket).list())
    } finally {
      pstr.close();
    }
    println("Done")
  }
  
  def generateIndex(s3 : AmazonS3Client, bucket: String, root: KeysTree): Unit = {
    val indexName = root.name + INDEXFILE

    println("Generating " + indexName)

    val group = new StringTemplateGroup("mygroup", new File("etc"))
    val template = group.template("index")

    template.setAttributes(Map("title" -> root.name))

    for (g <- root.keyGroups) {
      template.setAggregate("children.{leaf,name}", false, g.name.substring(root.name.size))
    }
    for (k <- root.keys if(indexName != k.name)) {
    	  template.setAggregate("children.{leaf,name,date,size}", true, k.name.substring(root.name.size), k.metadata.lastModified, k.metadata.objectSize)
    }

    val indexData = template.toString
    val bytes = indexData.getBytes("UTF-8")
    val bais = new ByteArrayInputStream(bytes)
    
    val md  = new ObjectMetadata()
    md.setContentType("text/html; charset=UTF-8")
    md.setContentLength(bytes.length)

    s3.putObject(bucket, indexName, bais, md)
    bais.close()
    s3.setObjectAcl(bucket, indexName, CannedAccessControlList.PublicRead)

    for (g <- root.keyGroups) {
      generateIndex(s3, bucket, g)
    }

  }

}