package com.xebialabs.jello

import akka.testkit.TestKit
import com.xebialabs.jello
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest._
import scala.concurrent.ExecutionContext.Implicits.global

package object support {

  implicit val executionContext = global


  class ActorTestSugar extends TestKit(jello.system) with UnitTestSugar

  trait UnitTestSugar extends FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar {
    implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))
  }
}
