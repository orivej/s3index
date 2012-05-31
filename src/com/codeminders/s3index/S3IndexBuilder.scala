package com.codeminders.s3index

import java.io.FileInputStream
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.MutableList
import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.AmazonS3Client
import org.clapper.scalasti.StringTemplateGroup
import java.io.File

object S3IndexBuilder {

  abstract class Tree(_name : String) {
    val name = _name
  }

  class Leaf(name : String, _value : S3ObjectSummary) extends Tree(name) {

    val value = _value
    override def toString = name

    def getName() = name
    def getSize() = value.getSize()
    def getDate() = value.getLastModified()
  }

  class Branch(name : String) extends Tree(name) {
    var children = new MutableList[Tree]()
    override def toString = name + "(" + children + ")"
  }

  def main(args : Array[String]) : Unit = {
    val bucket = args(0)
    println("Listing " + bucket);
    val pstr = new FileInputStream("etc/AwsCredentials.properties");
    try {
      val s3 = new AmazonS3Client(new PropertiesCredentials(pstr));
      val objects = s3.listObjects(bucket).getObjectSummaries().iterator().asScala.buffered
      val root = buildTree(objects, "", 0)
      generateIndex(s3, root, "")
    } finally {
      pstr.close();
    }
  }

  def generateIndex(s3 : AmazonS3Client, root : Branch, path : String) : Unit = {
    val indexName = path + "/index.html"
    println("Generating " + indexName)

    val group = new StringTemplateGroup("mygroup", new File("etc"))
    val template = group.template("index")

    template.setAttributes(Map("title" -> path))

    for (i <- root.children) {
      if (i.isInstanceOf[Branch]) {
        val b = i.asInstanceOf[Branch];
        template.setAggregate("children.{leaf,name}", false, b.name)
      } else {
        val l = i.asInstanceOf[Leaf];
        template.setAggregate("children.{leaf,name,date,size}", true, l.name, l.getDate(), l.getSize())
      }
    }

    val indexData = template.toString
    println(indexData)

    for (i <- root.children) {
      if (i.isInstanceOf[Branch]) {
        val b = i.asInstanceOf[Branch];
        generateIndex(s3, b, path+"/"+b.name)
      }
    }
  }

  def buildTree(i : BufferedIterator[S3ObjectSummary], name : String, level : Int) : Branch = {

    val me = new Branch(name)
    while (i.hasNext) {
      val o = i.head
      val sname = o.getKey().split("/")

      if ((sname.length <= level) || (level != 0 && sname(level - 1) != name)) {
        // end of this branch        
        return me
      }

      if (sname.length == (level + 1)) {
        // new leaf
        me.children += new Leaf(sname(level), i.next())
      } else {
        // new branch
        me.children += buildTree(i, sname(level), level + 1)
      }
    }
    return me
  }

}