package model

import scala.util.matching.Regex
import scala.collection.mutable.StringBuilder

object Utils {
  
  def globe2Regexp(globe: String): Regex = {
    (globe.foldLeft(new StringBuilder("")){
      (s, c) =>
      c match {
        case '*' => s + '.' + '*'
        case '?' => s + '.'
        case _ => s + '[' + c + ']'
      }
    }).toString.r
  }

}