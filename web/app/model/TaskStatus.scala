package model

import play.api.libs.json.JsValue
import play.api.libs.json.Json

class TaskStatus(val status: Int, val message: String, val percentsDone: Int, val fileId: String = "") {

  def toJSON(): JsValue = {
    if(fileId.isEmpty()){
    	Json.toJson(Map( "status" -> Json.toJson(status), "message" -> Json.toJson(message), "percents" -> Json.toJson(percentsDone)))
    } else {
    	Json.toJson(Map( "status" -> Json.toJson(status), "message" -> Json.toJson(message), "percents" -> Json.toJson(percentsDone), "fileId" -> Json.toJson(fileId)))
    }
  }
  
  def info (newMessage: String): TaskStatus = {
    new TaskStatus(0, newMessage, this.percentsDone, this.fileId)
  }
  
  def error (newMessage: String): TaskStatus = {
    new TaskStatus(2, newMessage, this.percentsDone, this.fileId)
  }
  
  def done (newMessage: String): TaskStatus = {
    new TaskStatus(1, newMessage, this.percentsDone, this.fileId)
  }
  
  def fileId (fileId: String): TaskStatus = {
    new TaskStatus(this.status, this.message, this.percentsDone, fileId)
  }
  
  def % (newPercentsDone: Int): TaskStatus = {
    new TaskStatus(this.status, this.message, newPercentsDone, this.fileId)
  }
}