package com.codeminders.s3index

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.PropertiesCredentials
import java.io.FileInputStream
import scala.collection.JavaConverters._
import com.amazonaws.services.s3.model.S3ObjectSummary
import scala.collection.mutable.MutableList

object S3IndexBuilder {

  abstract class Tree(name : String) {}

  class Leaf(name : String, value : S3ObjectSummary) extends Tree(name) {
  }

  class Branch(name : String) extends Tree(name) {
    var children = new MutableList[Tree]()
  }

  def main(args : Array[String]) : Unit = {
    val bucket = args(0)
    println("Listing " + bucket);
    val pstr = new FileInputStream("etc/AwsCredentials.properties");
    try {
      val s3 = new AmazonS3Client(new PropertiesCredentials(pstr));
      val root = buildTree(listBucket(s3, bucket), "", 0)
    } finally {
      pstr.close();
    }
  }

  def listBucket(s3 : AmazonS3, bucket : String) =
    s3.listObjects(bucket).getObjectSummaries().iterator().asScala.buffered

  def ident(n : Int) = {
    print("]"); print(n); print("]")
    for (i <- Range(0, n)) print(" ")
  }

  def buildTree(i : BufferedIterator[S3ObjectSummary], name : String, level : Int) : Tree = {

    ident(level); println(">"+name)
    val me = new Branch(name)
    while (i.hasNext) {
      val o = i.head
      val sname = o.getKey().split("/")

      if (sname.length <= level) {
        // end of current level
        return me
      }
      if (level != 0 && sname(level-1) != name) {
        // end of this branch        
        return me
      }

      if (sname.length == (level + 1)) {
        // new leaf
        ident(level); println("+"+sname(level))
        me.children += new Leaf(sname(level), i.next())
      } else {
        // new branch
        ident(level); println("*"+sname(level))
        me.children += buildTree(i, sname(level), level + 1)
      }
    }
    return me
  }

}