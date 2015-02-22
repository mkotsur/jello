package com.xebialabs.jello.http

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.jello.http.RequestExecutor._
import com.xebialabs.jello.system.dispatcher
import com.xebialabs.jello.system
import spray.client.pipelining._
import spray.http._
import spray.httpx.unmarshalling._

import scala.concurrent.Future
import scala.concurrent.duration._

object RequestExecutor {


}

trait RequestExecutor extends LazyLogging {

  private val loggingFunction = (i: HttpMessage) => logger.debug(i.entity.asString)

  def runRequest[T: FromResponseUnmarshaller](r: HttpRequest): Future[T] = {
    val pipeline = logRequest(loggingFunction) ~>
      sendReceive ~>
      logResponse(loggingFunction) ~>
      unmarshal[T]
    pipeline(r)
  }

}
