package model

import com.codeminders.s3simpleclient.AWSCredentials

case class BucketProperties(var name: String, var credentials: Option[AWSCredentials], var depthLevel: Int, var excludedPaths: Set[String], var includedPaths: Set[String] ) {
	def this(name: String) = this(name, None, 100, Set(), Set())
	
}

