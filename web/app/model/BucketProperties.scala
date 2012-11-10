package model

import com.codeminders.s3simpleclient.AWSCredentials
import java.util.concurrent.atomic.AtomicReference

case class BucketProperties(var name: String, var credentials: Option[AWSCredentials], var depthLevel: Int, var excludedPaths: Set[String], var includedPaths: Set[String], var status: AtomicReference[TaskStatus] ) {
	def this(name: String) = this(name, None, 100, Set("*/index.html"), Set(), new AtomicReference(new TaskStatus(2, "Please specify your bucket", 0)) )
	
}

