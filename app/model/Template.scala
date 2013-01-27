package model

sealed trait Template {
  def value: String
  override def toString(): String = value
}

object Template{
	object Slim extends Template {
	  override def value: String = "Slim"
	}
	
	object Simple extends Template {
	  override def value: String = "Simple"
	}

  def values: Seq[String] = Seq("Simple", "Slim")
  def fromString(s: String): Template = {
    s.trim().toLowerCase() match {
      case "simple" => Simple
      case "slim" => Slim
    }
  }
  def withName(s: String) = fromString(s)
}
