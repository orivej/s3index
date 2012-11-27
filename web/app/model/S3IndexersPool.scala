package model

import scala.actors.Actor
import play.api.Logger
import scala.collection.mutable.ArrayBuffer

object S3IndexersPool extends Actor {
  
  private val indexers: ArrayBuffer[S3Indexer] = new ArrayBuffer(1)
  
  private var currentIndexer = 0 
  
  override def start(): Actor = {
    start(4)
  }
  
  def start(numberOfIndexers: Int): Actor = {
    Logger.info("Launching %d S3 indexers.".format(numberOfIndexers))
    for (i <- (0 until numberOfIndexers)){
      indexers += new S3Indexer()
      indexers(i).start()
    }
    super.start
  }
  
  def act() {
	  Logger.info(S3IndexersPool.this.getClass().getName() + " started.")
	  loop {
	    react {
	    	case t: S3IndexTask =>
	    	  Logger.info("Scheduling %s to indexer #%d.".format(t.id, currentIndexer))
	    	  indexers(currentIndexer) ! t
	    	  currentIndexer = (currentIndexer + 1) % indexers.length
	    }
	
	  }
  }

}