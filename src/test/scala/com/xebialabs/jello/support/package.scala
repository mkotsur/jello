package com.xebialabs.jello

import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import com.xebialabs.jello
import com.xebialabs.jello.conf.JelloConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest._
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConversions._

package object support {

  implicit val executionContext = global

  /**
   * Allows to override some values in Jello config
   */
  implicit class JelloConfigWithOverrides(jc: JelloConfig) {
    def withOverrides(m: Map[String, _ <: AnyRef]): JelloConfig = {
      new JelloConfig(ConfigFactory.parseMap(m).withFallback(jc.conf))
    }
  }

  class ActorTestSugar extends TestKit(jello.system) with UnitTestSugar

  trait UnitTestSugar extends FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar {
    implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))
  }
}
