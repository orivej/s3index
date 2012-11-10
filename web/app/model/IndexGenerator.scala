package model

import scala.actors.Actor
import play.api._

object IndexGenerator extends Actor {
  
  def act() {
    Logger.info(this.getClass().getName() + " started.")
    while (true) {
      receive {
        case b: BucketProperties =>
          Logger.debug("Processing bucket " + b.name)
          b.status.set(b.status.get() % 0 info ("Estimating processing time...")) 
          Thread.sleep(5 * 1000)
          b.status.set(b.status.get() % 14 info ("Processing keys within /photos/ ..."))
          Thread.sleep(5 * 1000)
          b.status.set(b.status.get() % 34 info ("Processing keys within /video/ ..."))
          Thread.sleep(5 * 1000)
          b.status.set(b.status.get() % 89 info ("Processing keys within /test/ ..."))
          Thread.sleep(5 * 1000)
          b.status.set(b.status.get() % 100 done ("Done."))
          Logger.debug("Finished processing of bucket " + b.name)
      }
    }
  }

}