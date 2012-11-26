package model

import scala.actors.Actor
import play.api._
import com.codeminders.s3simpleclient._
import com.codeminders.s3simpleclient.model._
import org.clapper.scalasti.StringTemplateGroup
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipOutputStream
import org.apache.commons.io.output.ByteArrayOutputStream
import java.util.zip.ZipEntry
import play.api.templates.Html
import org.apache.commons.io.FileUtils
import scala.util.matching.Regex

object IndexGenerator extends Actor {

  val INDEXFILE = "index.html"

  def act() {
    Logger.info(this.getClass().getName() + " started.")
    loop {
      react {
        case t: S3IndexTask =>
          try {
            if (t.properties.get() == None) {
              Logger.info("Got an empty task with id %s.".format(t.id))
              t.updateStatus(TaskStatus.error("Please set bucket name"))
            } else {
              val properties = t.properties.get().get
              if (properties.name.isEmpty()) {
                t.updateStatus(TaskStatus.error("Please set bucket name"))
                Logger.debug("Won't process - empty bucket name")
              } else {
                val s3 = properties.credentials match {
                  case None => SimpleS3()
                  case Some(c) => SimpleS3(AWSCredentials(c.accessKeyId, c.secretKey))
                }
                Logger.info("Started task %s, bucket %s. ".format(t.id, properties.name))
                if (properties.outputOption == OutputOption.Bucket && properties.credentials != None) {
                  generateIndexToBucket(t.id, s3, properties, t.updateStatus(_), t.storeResult(_))
                } else {
                  generateIndexToArchive(t.id, s3, properties, t.updateStatus(_), t.storeResult(_))
                }
                Logger.info("Finished task %s, bucket %s. ".format(t.id, properties.name))
              }
            }
          } catch {
            case e: Exception => {
              t.updateStatus(TaskStatus.error("The server encountered an internal error. Please try again later."))
              Logger.error(e.getMessage(), e)
            }
          }
      }
    }
  }

  def generateIndex(root: KeysTree, outputFunction: (String, Array[Byte]) => Unit, status: (TaskStatus) => Unit, properties: Properties): Unit = {
	  def generateIndex(root: KeysTree, outputFunction: (String, Array[Byte]) => Unit, status: (TaskStatus) => Unit, template: (String, Seq[Seq[Html]]) => Html, include: (String) => Boolean, keysFormatter: (Option[(KeysTree, Key)]) => List[Html], objectsDone: Int, objectsLeft: Int): Unit = {
	    val indexName = root.name + INDEXFILE
	    
	    val header = Array(keysFormatter(None))
	    
	    val parentLink = Array(List(Html("""<div class="back"><a href="..">..</a></div>""")))
	
	    val directories = for (g <- root.keyGroups if(include(g.name)) ) yield {
	      val name = g.name.substring(root.name.size)
	      List(Html("""<div class="dir"><a href="%s">%s</a></div>""".format(name, name)))
	    }
	
	    val files = for (k <- root.keys if(include(k.name))) yield {
	      keysFormatter(Option((root, k)))
	    }
	
	    val data = header ++ parentLink ++ directories ++ files
	
	    val indexData = template(if (root.name.isEmpty()) "/" else root.name, data).toString
	    val bytes = indexData.getBytes("UTF-8")
	
	    outputFunction(indexName, bytes)
	
	    val percents = ((objectsDone.toFloat / objectsLeft.toFloat) * 100).toInt
	
	    status(TaskStatus.info(percents, "Processing keys with prefix %s".format(if (root.name.isEmpty()) "/" else root.name)))
	
	    var counter = objectsDone
	    for (g <- root.keyGroups) {
	      generateIndex(g, outputFunction, status, template, include, keysFormatter, counter, objectsLeft * root.groupsNumber)
	      counter += 1
	    }
	  }
	  val cssStyleLinks = "/css/%s.css".format(properties.template.toString().toLowerCase()) :: properties.customCSS.toList
	  val template = views.html.index(properties.title, properties.header, Html(properties.footer), cssStyleLinks)(_, _)
	  val filter = keysFilter(properties.includedPaths.foldLeft(List[Regex]())((l, p) => Utils.globe2Regexp(p) :: l),
	      properties.excludedPaths.foldLeft(List[Regex]())((l, p) => Utils.globe2Regexp(p) :: l))(_)
	  generateIndex(root, outputFunction, status, template, filter, FileListFormat.toHtml(properties.fileListFormat)(_), 0, 1)
  }

  def generateIndexToArchive(taskId: String, s3: SimpleS3, properties: Properties, status: (TaskStatus) => Unit, storeResult: (Array[Byte]) => Unit) {
    val result = new ByteArrayOutputStream();
    val outputStream = new ZipOutputStream(result)
    val outputFunction = toArchive(outputStream)(_, _)    
    Utils.copyStyleTo(properties.stylesLocation, properties.template.toString().toLowerCase(), outputFunction)
    generateIndex(s3.bucket(properties.name).list(), outputFunction, status, properties)
    outputStream.close()
    storeResult(result.toByteArray())
    status(TaskStatus.done("Done", taskId))
  }
  
  def generateIndexToBucket(taskId: String, s3: SimpleS3, properties: Properties, status: (TaskStatus) => Unit, storeResult: (Array[Byte]) => Unit) {
    val result = new ByteArrayOutputStream();
    val outputFunction = toBucket(s3, s3.bucket(properties.name))(_, _)
    Utils.copyStyleTo(properties.stylesLocation, properties.template.toString().toLowerCase(), outputFunction)
    generateIndex(s3.bucket(properties.name).list(), outputFunction, status, properties)
    status(TaskStatus.done("Done"))
  }
  
  def toArchive(output: ZipOutputStream)(keyName: String, data: Array[Byte]) {
    output.putNextEntry(new ZipEntry(keyName));
    output.write(data)
    output.closeEntry();
  }

  def toBucket(s3: SimpleS3, bucket: Bucket)(keyName: String, data: Array[Byte]) {
    val key = bucket.key(keyName).withACL("public-read").withContentType("text/html; charset=UTF-8")
    key <<< (new ByteArrayInputStream(data), data.length)
  }

  def keysFilter(includedKeys: Seq[Regex], excludedKeys: Seq[Regex])(name: String): Boolean = {
    val r = if(!includedKeys.isEmpty){
    	includedKeys.exists(i => i.pattern.matcher(name).matches())
    } else (if(!excludedKeys.isEmpty){
      !excludedKeys.exists(e => e.pattern.matcher(name).matches())
    } else true)
    Logger.debug("keysFilter In -> %s, Out -> %s".format(name, r.toString()))
    r
  }
  
}