package com.xebialabs.jello.http

import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.jello.system
import com.xebialabs.jello.system.dispatcher
import spray.client.pipelining._
import spray.http._
import spray.httpx.unmarshalling._

import scala.concurrent.Future


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
