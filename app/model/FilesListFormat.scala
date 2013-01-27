package model

import play.api.templates.Html
import com.codeminders.scalaws.s3.model.S3ObjectSummary

sealed trait FilesListFormat {
  def value: String
  override def toString(): String = value
}

object FilesListFormat{
	object Standard extends FilesListFormat {
	  override def value: String = "Standard"
	}
	
	object Brief extends FilesListFormat {
	  override def value: String = "Brief"
	}
	
	object Full extends FilesListFormat {
	  override def value: String = "Full"
	}

  def values: Seq[String] = Seq("Standard", "Brief", "Full")
  def fromString(s: String): FilesListFormat = {
    s.trim().toLowerCase() match {
      case "standard" => Standard
      case "brief" => Brief
      case "full" => Full
    }
  }
  def withName(s: String) = fromString(s)
  
}
