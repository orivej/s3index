import play.api._
import play.api.Play.current
import play.api.mvc.Results._
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
import com.codeminders.scalaws.s3.AWSS3
import play.mvc.Result
import views.html.defaultpages.notFound

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("S3Index started")
  }  
  
  override def onStop(app: Application) {
    Logger.info("S3Index terminated")
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error("Uncaught exception", ex)
    InternalServerError(
    		views.html.errorPage(globals.settings)("The server encountered an internal error, please try again later.")
    )
  }
  
  override def onBadRequest(request: RequestHeader, error: String) = {
    Logger.warn("Bad request: %s, error: %s".format(request, error))
    BadRequest(
    		views.html.errorPage(globals.settings)("Bad request.")
    )
  }
  
  override def onHandlerNotFound(request: RequestHeader) = {
    Logger.warn("Handler not found for %s".format(request))
    NotFound(views.html.errorPage(globals.settings)("The page you requested was not found."))
  }
  
}

package object globals {
  lazy val settings = new ApplicationSettings(Play.application.configuration)
}