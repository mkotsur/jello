package com.xebialabs.jello

import scala.language.implicitConversions

package object domain {

  implicit class ExtendedString(s:String) {
  	def isNumber: Boolean = s.matches("\\d+")
  }

}
