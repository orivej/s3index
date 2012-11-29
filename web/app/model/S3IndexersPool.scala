package model

import scala.actors.Actor
import play.api.Logger
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Queue

object S3IndexersPool extends Actor {
  
  private val tasks: Queue[S3IndexTask] = Queue()
  
  private val indexers: ArrayBuffer[S3Indexer] = new ArrayBuffer(1)
  
  override def start(): Actor = {
    start(4)
  }
  
  def start(numberOfIndexers: Int): Actor = {
    Logger.info("Launching %d S3 indexers.".format(numberOfIndexers))
    for (i <- (0 until numberOfIndexers)){
      indexers += new S3Indexer(i.toString())
      indexers(i).start()
    }
    super.start
  }
  
  def act() {
	  Logger.info(S3IndexersPool.this.getClass().getName() + " started.")
	  loop {
	    react {
	    	case t: S3IndexTask =>
	    	  Logger.info("Scheduling %s.".format(t.id))
	    	  tasks += t
	    	  if(tasks.size > 1) Logger.info("There are %d queued tasks.".format(tasks.size))
	    	  indexers.foreach(_ ! "newTask")
	    	case "getTask" =>
	    	  if(!tasks.isEmpty) sender ! tasks.dequeue
	    }
	
	  }
  }

}