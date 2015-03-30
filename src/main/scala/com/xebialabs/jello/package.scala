package com.xebialabs

import akka.actor.ActorSystem
import com.xebialabs.jello.conf.DefaultConfig

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

package object jello extends DefaultConfig {

  implicit val system: ActorSystem = ActorSystem()

  implicit class PrintableFuture[T](f: Future[T]) {

    def thenPrint(): Future[T] = f.andThen {
        case Success(t) => println(t)
        case Failure(e) => e.printStackTrace()
      }
  }
}
