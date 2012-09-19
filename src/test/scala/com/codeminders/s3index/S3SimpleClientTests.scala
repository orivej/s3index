package com.codeminders.s3index

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

trait S3BasicClientMock extends S3BasicClient {
  override def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = "") : (Array[Key], Array[String], String) = prefix match {
    case "" => (Array(Key("1", "2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD")), Array("dir1", "dir2", "dir3"), "")
    case "/dir1" => (Array(Key("3", "2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD")), Array(), "")
    case "/dir2" => (Array(Key("2", "2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD"), Key("4", "2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD")), Array("dir4"), "")
    case "/dir3" => (Array(Key("5", "2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD")), Array(), "")
    case "/dir2/dir4" => (Array(Key("6", "2012-09-18T09:25:25.000Z", "d41d8cd98f00b204e9800998ecf8427e", 0, "STANDARD")), Array(), "")
  }
}

@RunWith(classOf[JUnitRunner])
class S3SimpleClientTests extends FunSuite {

  test("elem result should have passed width") {
    val client = new SimpleS3() with S3BasicClientMock
    val bucket = client.bucket("s3index")
    val tree = bucket.list()
    1 === tree.keysNumber
    3 === tree.groupsNumber
    6 === tree.foldLeft(0) { (r, e) => r + 1 }
  }

}


