package com.codeminders.s3simpleclient.model

import java.io.InputStream
import java.io.OutputStream
import org.apache.commons.io.IOUtils
import com.codeminders.s3simpleclient._

case class Key(val client: BasicS3Client, val name: String, metadata: ObjectMetadata = ObjectMetadata()) {
  override def toString() = "{name:%s, %s}".format( name, metadata )
  def <<< (data: InputStream) {
    client.putObject(name, metadata, data);
  }
  def >>>(out: OutputStream) {
    IOUtils.copy(client.getObject(name)._1, out)
  }
}