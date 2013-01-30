package model

import java.net.URL

class ApplicationSettings(configuration: play.api.Configuration) {
  
  lazy val backreferenceUrl: URL = new URL(configuration.getString("s3index.backreference.url").getOrElse("http://s3index.herokuapp.com/"))
  lazy val applicationName: String = configuration.getString("s3index.view.applicationName").getOrElse("S3Index")
  lazy val applicationDescription: String = configuration.getString("s3index.view.applicationDescription").getOrElse("Generates index.html for all files in Amazon S3 bucket.")
  lazy val brandName: String = configuration.getString("s3index.view.brandName").getOrElse("S3Index")
  lazy val brandLink: String = configuration.getString("s3index.view.brandLink").getOrElse("http://code.google.com/p/s3index/")
  lazy val aboutBrandLink: String = configuration.getString("s3index.view.aboutBrandLink").getOrElse("http://code.google.com/p/s3index/")
  lazy val contactBrandLink: String = configuration.getString("s3index.view.contactBrandLink").getOrElse("http://code.google.com/p/s3index/")
  lazy val yearUpdated: String = configuration.getString("s3index.view.yearUpdated").getOrElse("2012")
  lazy val idEncryptionKey: String = configuration.getString("s3index.idEncryptionKey").getOrElse("D56E765A")

}