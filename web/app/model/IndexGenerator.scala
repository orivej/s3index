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

object IndexGenerator extends Actor {

  val INDEXFILE = "index.html"

  def act() {
    Logger.info(this.getClass().getName() + " started.")
    while (true) {
      receive {
        case t: S3IndexTask =>
          try {
            if(t.properties.get() == None){
              Logger.info("Got an empty task with id %s.".format(t.id))
            } else {
            	val properties = t.properties.get().get
	            val s3 = properties.credentials match {
            	  case None => SimpleS3()
            	  case Some(c) => SimpleS3(AWSCredentials(c.accessKeyId, c.secretKey))
            	}
	            Logger.info("Started task %s, bucket %s. ".format(t.id, properties.name))
	            val result = new ByteArrayOutputStream();
	            val outputStream = new ZipOutputStream(result)
	            generateIndex(t, s3, properties.name, s3.bucket(properties.name).list(), outputStream)
	            outputStream.close()
	            t.result = Option(result.toByteArray())
	            t.status.set(t.status.get() % 100 done ("Done") fileId (t.id))
	            Logger.info("Finished task %s, bucket %s. ".format(t.id, properties.name))
            }
          } catch {
            case e: Exception => {
              t.status.set(t.status.get() % 0 error ("The server encountered an internal error. Please try again later.") fileId (t.id))
              Logger.error(e.getMessage(), e)
            }
          }
      }
    }
  }

  def generateIndex(task: S3IndexTask, s3: SimpleS3, bucket: String, root: KeysTree, output: ZipOutputStream, objectsDone: Int = 0, objectsLeft: Int = 1): Unit = {
    val indexName = root.name + INDEXFILE

    val group = new StringTemplateGroup("mygroup")
    val template = group.template("index")

    template.setAttributes(Map("title" -> root.name))

    for (g <- root.keyGroups) {
      template.setAggregate("children.{leaf,name}", false, g.name.substring(root.name.size))
    }
    for (k <- root.keys if (indexName != k.name)) {
      template.setAggregate("children.{leaf,name,date,size}", true, k.name.substring(root.name.size), k.lastModified, k.objectSize)
    }

    val indexData = template.toString
    val bytes = indexData.getBytes("UTF-8")

    output.putNextEntry(new ZipEntry(indexName));
    output.write(bytes)
    output.closeEntry();
    
    val percents = ((objectsDone.toFloat / objectsLeft.toFloat) * 100).toInt
    
    task.status.set(task.status.get() % percents info ("Processing keys with prefix %s".format(if (root.name.isEmpty()) "/" else root.name)))

    var counter = objectsDone
    for (g <- root.keyGroups) {
      generateIndex(task, s3, bucket, g, output, (counter), objectsLeft * root.groupsNumber)
      counter += 1
    }

  }

}