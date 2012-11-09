import sbt._
import Keys._
import PlayProject._
import sbtassembly.Plugin._
import AssemblyKeys._

object S3IndexBuild extends Build {

    val appVersion      = "1.0"

    val appDependencies = Seq(                                                                                                                                                 
    ) 
    
    val helloTask = TaskKey[Unit]("hello", "Prints 'Hello World'") 
  
    def hello = (streams) map { (s) => {
    	s.log.info("Executing task Hello")
    }}
    
    val byeTask = TaskKey[Unit]("bye", "Prints 'Goodbye'") 
  
    def bye = (streams) map { (s) => {
    	s.log.info("Executing task Bye")
    }}

    lazy val root = Project(id = "s3index",
                            base = file("."),
                            settings = Defaults.defaultSettings ++ Seq(
						      helloTask <<= hello dependsOn (compile in Compile),
						      byeTask <<= bye dependsOn (helloTask)
						    )
                            ) aggregate(s3simpleclient, web, clitools) dependsOn(s3simpleclient)

    lazy val clitools = Project(id = "clitools",
                           base = file("cli-tools"), settings = Defaults.defaultSettings ++ assemblySettings ++ 
                           Seq( jarName in assembly := "cli-tools-" + appVersion + ".jar")
                           ) dependsOn(s3simpleclient)

    lazy val s3simpleclient = Project(id = "s3-simple-client",
                           base = file("s3-simple-client"))
                           
    lazy val web = PlayProject("s3index-web", appVersion, appDependencies, mainLang = SCALA, path = file("web")).settings( 
	testOptions in Test := Nil
    )  dependsOn(s3simpleclient)
}
