package model

import play.api.libs.json.JsValue
import play.api.libs.json.Json

class TaskStatus(val status: Int, val message: String, val percentsDone: Int) {

  def toJSON(): JsValue = {
    Json.toJson(Map( "status" -> Json.toJson(status), "message" -> Json.toJson(message), "percents" -> Json.toJson(percentsDone)))
  }
  
  def info (newMessage: String): TaskStatus = {
    new TaskStatus(0, newMessage, this.percentsDone)
  }
  
  def error (newMessage: String): TaskStatus = {
    new TaskStatus(2, newMessage, this.percentsDone)
  }
  
  def done (newMessage: String): TaskStatus = {
    new TaskStatus(1, newMessage, this.percentsDone)
  }
  
  def % (newPercentsDone: Int): TaskStatus = {
    new TaskStatus(this.status, this.message, newPercentsDone)
  }
}