package model

import scala.util.matching.Regex
import scala.collection.mutable.StringBuilder
import java.io.File
import org.apache.commons.io.FileUtils
import java.io.IOException
import play.api.Play$
import play.api.Play
import java.net.URI
import java.net.URL
import java.net.URLStreamHandler
import java.net.URLConnection
import java.net.MalformedURLException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import org.apache.commons.io.IOUtils

object Utils {
  
  private val classLoader: ClassLoader = getClass.getClassLoader()
  
  def globe2Regexp(globe: String): Regex = {
    (globe.foldLeft(new StringBuilder("")){
      (s, c) =>
      c match {
        case '*' => s + '.' + '*'
        case '?' => s + '.'
        case _ => s + '[' + c + ']'
      }
    }).toString.r
  }
  
  implicit def StringToURL(location: String): URL = {
    val classpathURL = """classpath[:][/]?[/]?(.+)""".r
    val result = location match {
    	case classpathURL(url) => classLoader.getResource(url)
    	case _ => new URL(location)
    }
  	if(result == null) throw new MalformedURLException("Could not convert " + location + " to URL")
   	else result
  }
  
}