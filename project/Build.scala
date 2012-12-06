import sbt._
import Keys._
import PlayProject._
import sbtassembly.Plugin._
import AssemblyKeys._

object S3IndexBuild extends Build {

    val appVersion      = "1.0"

    val appDependencies = Seq(                                                                                                                                                 
    ) 
    
    lazy val root = Project(id = "s3index",
                            base = file("."),
                            settings = Defaults.defaultSettings ++ Seq(
						    )
                            ) aggregate(s3simpleclient, web) dependsOn(s3simpleclient)

    lazy val s3simpleclient = Project(id = "s3-simple-client", 
                           base = file("s3-simple-client"))
                           
    lazy val web = PlayProject("s3index-web", appVersion, appDependencies, mainLang = SCALA, path = file("web")).settings( 
	testOptions in Test := Nil,
	scalaVersion := "2.9.2"
    )  dependsOn(s3simpleclient)
}
