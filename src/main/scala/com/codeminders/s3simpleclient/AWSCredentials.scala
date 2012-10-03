package com.codeminders.s3simpleclient

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class AWSCredentials(val accessKeyId: String, val secretKey: String)

object AWSCredentials {
  def apply(accessKeyId: String, secretKey: String): AWSCredentials = {
    if(accessKeyId.isEmpty() || secretKey.isEmpty()) throw new InvalidArgumentException("Arguments to AWSCredentials constructor could not be empty strings")
    new AWSCredentials(accessKeyId, secretKey)
  }

  def apply(file: InputStream): AWSCredentials = {

    val reader = new BufferedReader(new InputStreamReader(file))
    val iterator = Iterator.continually(reader.readLine()).takeWhile(_ != null)
    val accessKeyPattern = """^\s*accessKey\s*=\s*(.+)\s*""".r
    val secretKeyPattern = """^\s*secretKey\s*=\s*(.+)\s*""".r
    var id = ""
    var secret = ""
    for (line <- iterator) {
      line match {
        case accessKeyPattern(k) => id = k
        case secretKeyPattern(s) => secret = s
        case _ =>
      }
    }

    AWSCredentials(id, secret)
  }
}