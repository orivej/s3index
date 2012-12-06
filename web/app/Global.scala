import play.api._
import play.api.Play.current
import play.api.mvc.Results._
import model.S3IndexersPool
import play.api.mvc.RequestHeader
import java.net.URLStreamHandler
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandlerFactory
import scala.collection.mutable.Map
import java.security.Security
import java.lang.reflect.Method
import scala.math._
import model.ApplicationSettings
import akka.actor.ActorSystem
import akka.actor.Props

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("S3Index started")
  }  
  
  override def onStop(app: Application) {
    Logger.info("S3Index terminated")
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error("Uncought exception", ex)
    val configuration = new ApplicationSettings(Play.application.configuration)

    InternalServerError(
        views.html.errorPage(configuration.applicationName, configuration.applicationDescription, configuration.brandName, configuration.brandLink, configuration.yearUpdated)
        ("The server encountered an internal error, please try again later.")
    )
  }
    
}

package object globals {
  lazy val s3Indexer = ActorSystem("S3Index").actorOf(Props(new S3IndexersPool(new ApplicationSettings(Play.application.configuration).indexersNumber)), name = "indexerPool")
  lazy val settings = new ApplicationSettings(Play.application.configuration)
}