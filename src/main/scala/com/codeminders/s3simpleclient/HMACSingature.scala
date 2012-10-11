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

trait HMACSingature extends HTTPClient {
  
  val dateUtils: DateUtils = new DateUtils()
  
  override def processRequest[T](url: Request, handler: (InputStream, String) => T): T = {
    super.processRequest(sign(url), handler)
  }
  
  def credentials: AWSCredentials

  private def sign(request: Request): Request = {
    val date: String = dateUtils.formatRfc822Date(new Date());
    val r = request <:< Map(("Date", date)) <:< Map(("Host", request.host.toHostString())) <:< Map(("X-Amz-Date", date))

    val md5Sum = r.headers.find((p) => p._1 == "Content-MD5") match {
      case None => ""
      case Some(x) => x._2
    }

    val contentType = r.headers.find((p) => p._1 == "Content-Type") match {
      case None => ""
      case Some(x) => x._2
    }

    val stringToSign = r.method + "\n" +
      md5Sum + "\n" +
      contentType + "\n" +
      "\n" +
      canonicalizeHeaders(r.headers) + "\n" +
      canonicalizeResourcePath(r.path)
    val signature: String = signAndBase64Encode(stringToSign, credentials.secretKey)

    r <:< Map(("Authorization", "AWS %s:%s".format(credentials.accessKeyId, signature)))
  }

  private def canonicalizeResourcePath(resourcePath: String): String = {    
    if (resourcePath.isEmpty) {
      "/";
    } else {
      val parsedPath = parseRequestPath(resourcePath)
      "/" + parsedPath.bucketName + urlEncode(parsedPath.keyName).replace("%2F", "/")
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

  private def canonicalizeHeaders(headers: List[(String, String)]): String = {
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

  private def parseRequestPath(input: String): ParsedPath = {

    val keyValuePair = """([^?|^&|^=]+)=([^?|^&|^=]+)""".r
    
    val keyOnly = """([^?|^&|^=]+)=""".r

    val params = "([^?]*)[?]([^?]+)".r

    val pathWithParameters = "[/]?([^/]+)(.*)[?]([^?]+)".r

    val path = "[/]?([^/]+)([^?]*)?".r

    def parse(s: String, bucket: String = "", key: String = ""): ParsedPath = s match {
      case pathWithParameters(bucket, key, r) => new ParsedPath(bucket, key, parseParameters(r.split("&")))
      case path(bucket, key) => new ParsedPath(bucket, key, Map())
      case path(bucket) => new ParsedPath(bucket, "/", Map())
      case e => throw new IllegalArgumentException("")
    }

    def parseParameters(input: Seq[String], c: Map[String, String] = Map()):Map[String, String] = input.foldLeft(c)((c, s) => s match {
      case params(r, p) => parseParameters(p.split("&"), c)
      case keyValuePair(k, v) => c + (k -> v)
      case keyOnly(k) => c + (k -> "")
      case e => throw new IllegalArgumentException("")
    })

    parse(input)
  }

  private class ParsedPath(val bucketName: String, val keyName: String, val parameters: Map[String, String]) {
    override def toString(): String = {
      val parametersString = if (!parameters.isEmpty) "?" + parameters.foldLeft(Array[String]())((a, e) => a ++ Array(("" + e._1 + "=" + e._2))).mkString("&") else ""
      "/" + bucketName + keyName + parametersString
    }
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

}