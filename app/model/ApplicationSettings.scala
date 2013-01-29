package model

import java.net.URL

class ApplicationSettings(configuration: play.api.Configuration) {
  
  lazy val backreferenceUrl: URL = new URL(configuration.getString("s3index.backreference.url").getOrElse("http://s3index.herokuapp.com/"))
  lazy val applicationName: String = configuration.getString("s3index.view.applicationName").getOrElse("S3Index")
  lazy val applicationDescription: String = configuration.getString("s3index.view.applicationDescription").getOrElse("Generates index.html for all files in Amazon S3 bucket.")
  lazy val brandName: String = configuration.getString("s3index.view.brandName").getOrElse("Codeminders")
  lazy val brandLink: String = configuration.getString("s3index.view.brandLink").getOrElse("http://codeminders.com")
  lazy val yearUpdated: String = configuration.getString("s3index.view.yearUpdated").getOrElse("2012")

}