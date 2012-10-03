package com.codeminders.s3simpleclient

class AuthenticationException(val message: String, val exception: Exception) extends Exception(message, exception) {

}