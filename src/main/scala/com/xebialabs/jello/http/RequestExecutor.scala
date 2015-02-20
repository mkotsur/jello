package com.xebialabs.jello.http

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.jello.http.RequestExecutor._
import com.xebialabs.jello.http.RequestExecutor.system.dispatcher
import spray.client.pipelining._
import spray.http._
import spray.httpx.unmarshalling._

import scala.concurrent.Future
import scala.concurrent.duration._

object RequestExecutor {

  implicit val system: ActorSystem = ActorSystem()
  implicit val timeout: Timeout = Timeout(15.seconds)

}

trait RequestExecutor extends LazyLogging {

  def runRequest[T: FromResponseUnmarshaller](r: HttpRequest): Future[T] = {
    logger.debug(r.toString)
    val pipeline = sendReceive ~>
      unmarshal[T]
    pipeline(r)
  }

}
