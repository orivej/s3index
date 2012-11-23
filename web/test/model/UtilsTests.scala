package model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

@RunWith(classOf[JUnitRunner])
class UtilsTests extends FunSuite {

  test("globe2Regex, simple expression") {
	  val re = Utils.globe2Regexp("successfull*")
	  assert(re.pattern.matcher("successfullTest").matches())
	  assert(!re.pattern.matcher("unsuccessfullTest").matches())
	}
  
  test("globe2Regex, emptyString") {
	  val re = Utils.globe2Regexp("")
	  assert(re.pattern.matcher("").matches())
	  assert(!re.pattern.matcher("successfullTest").matches())
	  assert(!re.pattern.matcher("unsuccessfullTest").matches())
	}
  
}