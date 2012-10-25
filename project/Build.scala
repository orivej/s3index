import sbt._
import Keys._
import PlayProject._

object S3IndexBuild extends Build {

    val appVersion      = "1.0"

    val appDependencies = Seq(                                                                                                                                                 
    ) 

    lazy val root = Project(id = "s3index",
                            base = file("."), settings = Defaults.defaultSettings  ++ Seq(helloTask)) aggregate(s3simpleclient, web, clitools) dependsOn(s3simpleclient)

    lazy val clitools = Project(id = "clitools",
                           base = file("cli-tools")) dependsOn(s3simpleclient)

    lazy val s3simpleclient = Project(id = "s3-simple-client",
                           base = file("s3-simple-client"))

val hello = TaskKey[Unit]("hello", "Prints 'Hello World'")

  val helloTask = hello := {
	new play.core.StaticApplication(new java.io.File("web"))
  }


    lazy val web = PlayProject("s3index-web", appVersion, appDependencies, mainLang = SCALA, path = file("web")).settings( 
    )  dependsOn(s3simpleclient)
}
