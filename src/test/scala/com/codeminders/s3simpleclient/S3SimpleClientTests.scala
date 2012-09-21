package com.codeminders.s3simpleclient

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import java.io.InputStream
import java.io.ByteArrayInputStream
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream

@RunWith(classOf[JUnitRunner])
class S3SimpleClientTests extends FunSuite {

  test("List Bucket Operation") {
    val client = new SimpleS3() with BasicS3ClientMock
    val bucket = client.bucket("s3index")
    val tree = bucket.list()
    assert(1 === tree.keysNumber)
    assert(3 === tree.groupsNumber)
    assert(6 === tree.foldLeft(0) { (r, e) => r + 1 })
  }
  
  test("Put Object Operation and Get Object Operation") {
    val client = new SimpleS3() with BasicS3ClientMock
    val bucket = client.bucket("s3index")
    val key = bucket.key("1")
    key <<< new ByteArrayInputStream("Data of Object 1".getBytes("UTF-8"))
    val out = new ByteArrayOutputStream()
    key >>> out
    assert("Data of Object 1" === out.toString())
  }

}


