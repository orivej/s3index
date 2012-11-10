import play.api._
import model.IndexGenerator

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("S3Index started")
    Logger.info("Starting " + IndexGenerator.getClass().getName() + "...")
    IndexGenerator.start
  }  
  
  override def onStop(app: Application) {
    Logger.info("S3Index terminated")
  }  
    
}