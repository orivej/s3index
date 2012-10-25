import sbt._
import Keys._
import PlayProject._

object S3IndexBuild extends Build {

    val appVersion      = "1.0"

    val appDependencies = Seq(                                                                                                                                                 
    ) 

    lazy val root = Project(id = "s3index",
                            base = file(".")) aggregate(s3simpleclient, web) dependsOn(s3simpleclient)

    lazy val s3simpleclient = Project(id = "s3simpleclient",
                           base = file("s3simpleclient"))

    lazy val web = PlayProject("s3index-web", appVersion, appDependencies, mainLang = SCALA, path = file("web")).settings( 
    )  dependsOn(s3simpleclient)
}
