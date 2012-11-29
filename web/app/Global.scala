import play.api._
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

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("S3Index started")
    Logger.info("Starting " + S3IndexersPool.getClass().getName() + "...")
    val indexersNumber = Play.application(app).configuration.getInt("s3index.indexers.number") match {
      case None => 4
      case Some(n) => max(1, min(n, Runtime.getRuntime().availableProcessors() * 2))
    }
    S3IndexersPool.start(indexersNumber)
  }  
  
  override def onStop(app: Application) {
    Logger.info("S3Index terminated")
  }  
  
  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error("Uncought exception", ex)
    InternalServerError(views.html.errorPage("S3 Index Generator")("The server encountered an internal error, please try again later."))
  }
    
}