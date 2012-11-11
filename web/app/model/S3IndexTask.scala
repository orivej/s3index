package model

import com.codeminders.s3simpleclient.AWSCredentials
import java.util.concurrent.atomic.AtomicReference
import java.io.OutputStream

case class S3IndexTask(id: String,
    var properties: AtomicReference[Option[Properties]] = new AtomicReference(None),
    var status: AtomicReference[TaskStatus] = new AtomicReference(new TaskStatus(2, "Please specify your bucket", 0)),
    var result: Option[Array[Byte]] = None){
}

case class Properties(name: String, credentials: Option[AWSCredentials], depthLevel: Int, excludedPaths: Set[String], includedPaths: Set[String]) {
}

class PropertiesBuilder(val name: String, var credentials: Option[AWSCredentials] = None, var depthLevel: Int = 100, var excludedPaths: Set[String] = Set("*/index.html"), var includedPaths: Set[String] = Set()){
	
	def this(name: String) = this(name, None, 100, Set("*/index.html"), Set())
	
	def withCredentials(newCredentials: AWSCredentials): PropertiesBuilder = {
	  this.credentials = Option(newCredentials)
	  this
	}
	
	def withDepthLevel(newDepthLevel: Int): PropertiesBuilder = {
	  this.depthLevel = newDepthLevel
	  this
	}
	
	def withExcludedPaths(newExcludedPaths: Set[String]): PropertiesBuilder = {
	  this.excludedPaths = newExcludedPaths
	  this
	}
	
	def withIncludedPaths(newIncludedPaths: Set[String]): PropertiesBuilder = {
	  this.includedPaths = newIncludedPaths
	  this
	}
	
	def toProperties(): Properties = {
	  Properties(name, credentials, depthLevel, excludedPaths, includedPaths)
	}
	
}

