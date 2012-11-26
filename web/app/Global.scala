import play.api._
import play.api.mvc.Results._
import model.IndexGenerator
import play.api.mvc.RequestHeader
import java.net.URLStreamHandler
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandlerFactory
import scala.collection.mutable.Map
import java.security.Security
import java.lang.reflect.Method

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("S3Index started")
    Logger.info("Starting " + IndexGenerator.getClass().getName() + "...")
    IndexGenerator.start
  }  
  
  override def onStop(app: Application) {
    Logger.info("S3Index terminated")
  }  
  
  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error("Uncought exception", ex)
    InternalServerError(views.html.errorPage("S3 Index Generator")("The server encountered an internal error, please try again later."))
  }
    
}