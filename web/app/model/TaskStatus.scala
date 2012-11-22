package model

import play.api.libs.json.JsValue
import play.api.libs.json.Json

object StatusType extends Enumeration {
     type StatusType = Value
     val done, error, info = Value
}

import StatusType._

class TaskStatus(val status: StatusType, val message: String, val percentsDone: Int, val fileId: String = "") {

  def toJSON(): JsValue = {
    if(fileId.isEmpty()){
    	Json.toJson(Map( "status" -> Json.toJson(status.toString()), "message" -> Json.toJson(message), "percents" -> Json.toJson(percentsDone)))
    } else {
    	Json.toJson(Map( "status" -> Json.toJson(status.toString()), "message" -> Json.toJson(message), "percents" -> Json.toJson(percentsDone), "fileId" -> Json.toJson(fileId)))
    }
  }
  
}

object TaskStatus{
  def info (percents: Int, newMessage: String): TaskStatus = {
    new TaskStatus(StatusType.info, newMessage, percents)
  }
  
  def error (newMessage: String): TaskStatus = {
    new TaskStatus(StatusType.error, newMessage, 0)
  }
  
  def done (newMessage: String, fileId: String = ""): TaskStatus = {
    new TaskStatus(StatusType.done, newMessage, 100, fileId)
  }
}