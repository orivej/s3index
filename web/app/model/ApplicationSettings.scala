package model

class ApplicationSettings(configuration: play.api.Configuration) {
  
  lazy val applicationName: String = configuration.getString("s3index.view.applicationName").getOrElse("S3Index")
  lazy val applicationDescription: String = configuration.getString("s3index.view.applicationDescription").getOrElse("Generates index.html for all files in Amazon S3 bucket.")
  lazy val brandName: String = configuration.getString("s3index.view.brandName").getOrElse("Codeminders")
  lazy val brandLink: String = configuration.getString("s3index.view.brandLink").getOrElse("http://codeminders.com")
  lazy val yearUpdated: String = configuration.getString("s3index.view.yearUpdated").getOrElse("2012")
  lazy val stylesLocation: String = configuration.getString("s3index.styles.location").getOrElse("classpath://styles/")
  lazy val indexersNumber: Int = configuration.getInt("s3index.indexers.number").getOrElse(Runtime.getRuntime().availableProcessors())

}