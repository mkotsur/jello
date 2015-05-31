package com.xebialabs

import akka.actor.ActorSystem
import com.xebialabs.jello.conf.DefaultConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}

package object jello extends DefaultConfig {

  implicit val system: ActorSystem = ActorSystem()

  implicit class PrintableFuture[T](f: Future[T]) {

    def thenPrint(): Future[T] = f.andThen {
      case Success(t) => println(t)
      case Failure(e) => e.printStackTrace()
    }
  }

  def askForInput(question: String): Future[Option[String]] = Future {
    println(question)
    StdIn.readLine() match {
      case "" | null => None
      case t => Some(t)
    }
  }

}
