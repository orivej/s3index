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
  
    test("isNumberInRange, value is inside the range") {
	  val actual = new PropertiesValidator(Map("key" -> List("100"))).isNumberInRange("key", 0 to 100)
	  val expected = List()
	  assert(actual.toString === expected.toString)
	}

	test("isNumberInRange, value is outside the range") {
	  val actual = new PropertiesValidator(Map("key" -> List("1000"))).isNumberInRange("key", 0 to 100)
	  val expected = List(Map("key" -> "The value of this parameter should not be greater than 100 and not less than 0."))
	  assert(actual.toString === expected.toString)
	}
	
	test("isNumberInRange, empty parameters list") {
	  val validator = new PropertiesValidator(Map("key" -> List())).isNumberInRange("key", 0 to 100)
	  assert(!validator.anyErrors)
	}
	
	test("isNumberInRange, NAN") {
	  val actual = new PropertiesValidator(Map("key" -> List("test"))).isNumberInRange("key", 0 to 100)
	  val expected = List(Map("key" -> "The value of this parameter should not be greater than 100 and not less than 0."))
	  assert(actual.toString === expected.toString)
	}
	
	test("isNumber, correct number") {
	  val actual = new PropertiesValidator(Map("key" -> List("1000"))).isNumber("key")
	  assert(actual.toString === List().toString)
	}
	
	test("isNumber, NAN") {
	  val actual = new PropertiesValidator(Map("key" -> List("10s"))).isNumber("key")
	  val expected = List(Map("key" -> "The value of this parameter should be a number."))
	  assert(actual.toString === expected.toString)
	}
	
	test("isLengthInRange, value inside the range") {
	  val validator = new PropertiesValidator(Map("key" -> List("abcd", "dfef"))).isLengthInRange("key", 0 to 5)
	  assert(!validator.anyErrors)
	}
	
	test("isLengthInRange, value is outside the range") {
	  val actual = new PropertiesValidator(Map("key" -> List("abcd", "dfefff"))).isLengthInRange("key", 0 to 5)
	  val expected = List(Map("key" -> "The length of this parameter should not be greater than 5 and not less than 0.")) 
	  assert(actual.toString === expected.toString)
	}
	
	test("oneOf, correct value") {
	  val validator = new PropertiesValidator(Map("key" -> List("one", "two"))).oneOf("key", List("one", "two", "three"))
	  assert(!validator.anyErrors)
	}
	
	test("oneOf, incorrect value") {
	  val actual = new PropertiesValidator(Map("key" -> List("one", "four"))).oneOf("key", List("one", "two", "three"))
	  val expected = List(Map("key" -> "The value of this parameter should be one of: one, two, three.")) 
	  assert(actual.toString === expected.toString)
	}
	
	test("oneOf, empty list") {
	  val actual = new PropertiesValidator(Map("key" -> List("one", "four"))).oneOf("key", List())
	  val expected = List(Map("key" -> "The value of this parameter should be one of: .")) 
	  assert(actual.toString === expected.toString)
	}
	
}