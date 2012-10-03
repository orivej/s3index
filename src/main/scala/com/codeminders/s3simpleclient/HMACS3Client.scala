package com.codeminders.s3simpleclient

import com.codeminders.s3simpleclient.model._
import java.io.InputStream
import dispatch._
import scala.xml._
import java.util.Date
import java.net.URLEncoder
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import scala.collection.immutable.Map
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64

trait HMACS3Client extends BasicS3Client {

  val dateUtils: DateUtils = new DateUtils()

  def sign(request: Request): Request = {
    val date: String = dateUtils.formatRfc822Date(new Date());
    val r = request <:< Map(("Date", date)) <:< Map(("Host", request.host.toHostString())) <:< Map(("X-Amz-Date", date))
    
    val md5Sum = r.headers.find((p) => if (p._1 == "Content-MD5") true else false) match {
      case None => ""
      case Some(x) => x._2
    }
    
    val contentType = r.headers.find((p) => if (p._1 == "Content-Type") true else false) match {
      case None => ""
      case Some(x) => x._2
    }
    
    val stringToSign = r.method + "\n" + 
      md5Sum + "\n" + 
      contentType + "\n" +
      "\n" +
      canonicalizeHeaders(r.headers) + "\n" +
      canonicalizeResourcePath("/s3index/")
    val signature: String = signAndBase64Encode(stringToSign, credentials.secretKey)
    
    r <:< Map(("Authorization", "AWS %s:%s".format(credentials.accessKeyId, signature)))
  }

  private def canonicalizeResourcePath(resourcePath: String): String = {
    if (resourcePath.isEmpty) {
      "/";
    } else {
      urlEncode(resourcePath).replace("%2F", "/")
    }
  }

  private def signAndBase64Encode(data: String, key: String): String = {
    try {
      val signature: Array[Byte] = sign(data.getBytes("UTF-8"), key.getBytes("UTF-8"));
      new String(Base64.encodeBase64(signature));
    } catch {
      case e: Exception =>
        throw new AuthenticationException("Unable to compute hash while signing request: " + e.getMessage(), e);
    }
  }

  private def hash(text: String): Array[Byte] = {
    try {
      val md: MessageDigest = MessageDigest.getInstance("SHA-256");
      md.update(text.getBytes("UTF-8"));
      return md.digest();
    } catch {
      case e: Exception =>
        throw new AuthenticationException("Unable to compute hash while signing request: " + e.getMessage(), e);
    }
  }

  private def sign(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    try {
      val mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      return mac.doFinal(data);
    } catch {
      case e: Exception =>
        throw new AuthenticationException("Unable to calculate a request signature: " + e.getMessage(), e);
    }
  }

  def canonicalizeHeaders(headers: List[(String, String)]): String = {
    import scala.collection.mutable.Map

    val tmpList: Map[String, String] = headers.foldLeft(Map[String, String]()) {
      (m, e) =>
        val k = e._1.toLowerCase().trim()
        val v = e._2.trim()
        if (k.startsWith("x-amz-")) {
          if (m.contains(k)) m += (k -> (v + "," + m(k)))
          else m += (k -> v)
        } else m
    }
    (tmpList.toSeq.sortBy(_._1) map { case (k, v) => k + ":" + v }).mkString("\n")
  }

  private def parse(input: String): Map[String, String] = {

    val eq = """([^?|^&|^=]+)=([^?|^&|^=]+)""".r

    val params = "[?]([^?]+)".r

    def parse(input: Seq[String], c: Map[String, String] = Map()): Map[String, String] = input.foldLeft(c)((c, s) => s match {
      case params(p) => parse(p.split("&"), c)
      case eq(k, v) => c ++ Map(k -> v)
      case e => c
    })

    parse(Array(input))
  }

  private def urlEncode(str: String): String = {
    URLEncoder.encode(str, "UTF-8")
      .replace("+", "%20").replace("*", "%2A")
      .replace("%7E", "~")
  }

  private def canonicalizeQueryString(parameters: Map[String, String]): String = {
    val sorted: SortedMap[String, String] = TreeMap(parameters.toSeq: _*)
    sorted map { case (k, v) => "" + urlEncode(k) + "=" + urlEncode(v) } mkString ("&")
  }

  def getBucket(bucketName: String, prefix: String = "", delimiter: String = "/", maxKeys: Int = 1000, marker: String = ""): (Array[Key], Array[String]) = {

    val req = sign(url("http://%s.s3.amazonaws.com/?prefix=%s&delimiter=%s&max-keys=%d&marker=%s".format(bucketName, prefix, delimiter, maxKeys, marker)))
    val xml = XML.loadString(Http(req >- { str =>
      str
    }))

    (Array(), Array())
  }
  def putObject(objectName: String, metadata: ObjectMetadata, data: InputStream) = ???
  def getObject(objectName: String): (InputStream, ObjectMetadata) = ???

}