package com.xebialabs

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, Matchers, FunSpec}
import org.scalatest.time.{Millis, Seconds, Span}


package object jello {

  trait TestSugar extends FunSpec with Matchers with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach {
    implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))
  }
}
