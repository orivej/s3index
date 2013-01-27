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
  
  def copyStyleTo(stylesDirectory: URL, styleId: String, to: (String, Array[Byte]) => Unit, excludeFiles: Seq[Regex] = List()) {
//    def recursiveListFiles(f: File, filter: (File) => Boolean = (f) => true): Array[File] = {
//      val list = f.listFiles
//      list.filter((f) => !f.isDirectory && filter(f)) ++ list.filter(_.isDirectory).flatMap(recursiveListFiles(_, filter))
//    }
    val styleZip = new ZipInputStream(getClass.getResourceAsStream("/styles" + File.separator + styleId + ".zip"));
    var entry: ZipEntry = styleZip.getNextEntry()
	while (entry != null)
        {
            System.out.println("entry: " + entry.getName() + ", " + entry.getSize());
            val data = IOUtils.toByteArray(styleZip)
            to(entry.getName(), data)
            entry = styleZip.getNextEntry()
        }
//    val styleFolderLocation = new File(stylesDirectory.getFile(), styleId)
//    if (styleFolderLocation.isDirectory()) {
//        val styleLocation = styleFolderLocation.getPath()
//        for (styleResource <- (recursiveListFiles(new File(styleFolderLocation.toURI()), (f) => !f.getPath().endsWith("preview.gif")))) yield {
//          val data = FileUtils.readFileToByteArray(styleResource)
//          val key = styleResource.getPath().substring(styleLocation.length() + 1)
//          to(key, data)
//        }
//      } else {
//        throw new IOException("Could not find style %s in classpath".format(styleId))
//    }
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