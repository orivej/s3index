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

  def generateIndex(s3: SimpleS3, bucket: String, root: KeysTree, outputFunction: (String, Array[Byte]) => Unit, status: (TaskStatus) => Unit, cssLinks: Seq[String], objectsDone: Int = 0, objectsLeft: Int = 1): Unit = {
    val indexName = root.name + INDEXFILE

    val template = views.html.index("S3: s3index/2012-01-10", "Index of s3index/2012-01-10", "Generated with S3Index")(_)

    val directories = for (g <- root.keyGroups) yield {
      val name = g.name.substring(root.name.size)
      List(Html("""<div class="dir"><a href="%s">%s</a></div>""".format(name, name)))
    }

    val files = for (k <- root.keys if (indexName != k.name)) yield {
      val name = k.name.substring(root.name.size)
      List(Html("""<div class="file"><a href="%s">%s</a></div>""".format(name, name)), Html(k.lastModified), Html(k.objectSize.toString))
    }

    val data = Array(List(Html("Name"), Html("Date"), Html("Size"))) ++ directories ++ files

    val indexData = template(data)(cssLinks).toString
    val bytes = indexData.getBytes("UTF-8")

    outputFunction(indexName, bytes)

    val percents = ((objectsDone.toFloat / objectsLeft.toFloat) * 100).toInt

    status(TaskStatus.info(percents, "Processing keys with prefix %s".format(if (root.name.isEmpty()) "/" else root.name)))

    var counter = objectsDone
    for (g <- root.keyGroups) {
      generateIndex(s3, bucket, g, outputFunction, status, cssLinks, (counter), objectsLeft * root.groupsNumber)
      counter += 1
    }

  }

  def generateIndexToArchive(taskId: String, s3: SimpleS3, properties: Properties, status: (TaskStatus) => Unit, storeResult: (Array[Byte]) => Unit) {
    val result = new ByteArrayOutputStream();
    val outputStream = new ZipOutputStream(result)
    val outputFunction = toArchive(outputStream)(_, _)    
    addStyleTo(properties.template.toString().toLowerCase(), outputFunction)
    val cssStyleLinks = "/css/%s.css".format(properties.template.toString().toLowerCase()) :: properties.customCSS.toList
    
    generateIndex(s3, properties.name, s3.bucket(properties.name).list(), outputFunction, status, cssStyleLinks)
    outputStream.close()
    storeResult(result.toByteArray())
    status(TaskStatus.done("Done", taskId))
  }
  
  def generateIndexToBucket(taskId: String, s3: SimpleS3, properties: Properties, status: (TaskStatus) => Unit, storeResult: (Array[Byte]) => Unit) {
    val result = new ByteArrayOutputStream();
    val outputFunction = toBucket(s3, s3.bucket(properties.name))(_, _)
    addStyleTo(properties.template.toString().toLowerCase(), outputFunction)
    val cssStyleLinks = "/css/%s.css".format(properties.template.toString().toLowerCase()) :: properties.customCSS.toList
    generateIndex(s3, properties.name, s3.bucket(properties.name).list(), outputFunction, status, cssStyleLinks)
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

  def addStyleTo(styleId: String, to: (String, Array[Byte]) => Unit) {
    def recursiveListFiles(f: File): Array[File] = {
      val list = f.listFiles
      list.filter(!_.isDirectory) ++ list.filter(_.isDirectory).flatMap(recursiveListFiles)
    }
    val styleFolderURL = getClass.getClassLoader().getResource("styles" + File.separator + styleId)
    if (styleFolderURL != null) {
      val styleLocation = styleFolderURL.toURI().getPath()
      for (styleResource <- recursiveListFiles(new File(styleFolderURL.toURI()))) {
        val data = FileUtils.readFileToByteArray(styleResource)
        val key = styleResource.getPath().substring(styleLocation.length() + 1)
        to(key, data)
      }
    } else {
      Logger.warn("Could not find style %s in classpath".format(styleId))
    }
  }

}