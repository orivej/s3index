package model

import com.codeminders.s3simpleclient.AWSCredentials

case class BucketProperties(val name: String, val credentials: Option[AWSCredentials], val depthLevel: Int, val excludedPaths: Set[String], val includedPaths: Set[String] ) {
	def this(name: String) = this(name, None, 0, Set(), Set())
}

