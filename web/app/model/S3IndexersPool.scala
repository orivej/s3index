package model

import akka.actor.Actor
import play.api.Logger
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Queue
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef

class S3IndexersPool(numberOfIndexers: Int) extends Actor {

  private val tasks: Queue[S3IndexTask] = Queue()

  private val indexers: ArrayBuffer[ActorRef] = new ArrayBuffer(1)

  val system = ActorSystem("S3Index")
  Logger.info("Launching %d S3 indexers.".format(numberOfIndexers))
  for (i <- (0 until numberOfIndexers)) {
    indexers += system.actorOf(Props(new S3Indexer(i.toString)), name = "indexer" + i)
  }
  Logger.info(S3IndexersPool.this.getClass().getName() + " started.")

  def receive = {
    case t: S3IndexTask =>
      Logger.info("Scheduling %s.".format(t.id))
      tasks += t
      if (tasks.size > 1) Logger.info("There are %d queued tasks.".format(tasks.size))
      indexers.foreach(_ ! "newTask")
    case "getTask" =>
      if (!tasks.isEmpty) sender ! tasks.dequeue

  }

}