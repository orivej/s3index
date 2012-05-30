package com.codeminders.s3index

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.PropertiesCredentials
import java.io.FileInputStream

import scala.collection.JavaConversions._

object S3IndexBuilder {

  def main(args : Array[String]) : Unit = {
    val bucket = args(0)
    println("Listing " + bucket);
    val pstr = new FileInputStream("etc/AwsCredentials.properties");
    try {
      val s3 = new AmazonS3Client(new PropertiesCredentials(pstr));
      listBucket(s3, bucket)
    } finally {
      pstr.close();
    }
  }

  def listBucket(s3 : AmazonS3, bucket:String) : Unit = {
    var res = new Array[String](3)
    val ol = s3.listObjects(bucket)
    val summaries = ol.getObjectSummaries().toIterable
    for(i <- summaries)
    {
      println(i.getKey())
    }
  }

}