package model

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import java.io.InputStream
import java.io.ByteArrayInputStream
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

@RunWith(classOf[JUnitRunner])
class PropertiesValidatorTests extends FunSuite  {
  
  test("isInRange should ensure that value is within the specified range") {
	  val actual = new PropertiesValidator(Map("depthLevel" -> List("100"))).isNumberInRange("depthLevel", 0 to 100)
	  val expected = List()
	  assert(actual.toString === expected.toString)
	}

	test("isInRange should ensure that value is not greater than 100") {
	  val actual = new PropertiesValidator(Map("depthLevel" -> List("1000"))).isNumberInRange("depthLevel", 0 to 100)
	  val expected = List(Map("depthLevel" -> "The value of this parameter should not be greater than 100 and not less than 0."))
	  assert(actual.toString === expected.toString)
	}
	
	test("isInRange should ensure that value is not less than 0") {
	  val actual = new PropertiesValidator(Map("depthLevel" -> List("-2"))).isNumberInRange("depthLevel", 0 to 100)
	  val expected = List(Map("depthLevel" -> "The value of this parameter should not be greater than 100 and not less than 0."))
	  assert(actual.toString === expected.toString)
	}
	
	test("isInRange should ensure that all values are not less than 0") {
	  val actual = new PropertiesValidator(Map("depthLevel" -> List("0", "-2"))).isNumberInRange("depthLevel", 0 to 100)
	  val expected = List(Map("depthLevel" -> "The value of this parameter should not be greater than 100 and not less than 0."))
	  assert(actual.toString === expected.toString)
	}
	
	test("isInRange should not fail in case of empty parameters list") {
	  val validator = new PropertiesValidator(Map("depthLevel" -> List())).isNumberInRange("depthLevel", 0 to 100)
	  assert(!validator.anyErrors)
	}
	
	test("isNumber should ensure a value is a number") {
	  val actual = new PropertiesValidator(Map("depthLevel" -> List("1000"))).isNumber("depthLevel")
	  assert(actual.toString === List().toString)
	}
	
	test("isNumber should set an error in case a key's value is a string") {
	  val actual = new PropertiesValidator(Map("depthLevel" -> List("10s"))).isNumber("depthLevel")
	  val expected = List(Map("depthLevel" -> "The value of this parameter should be a number"))
	  assert(actual.toString === List().toString)
	}
	
}